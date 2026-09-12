using AspNet10.Domain;
using Dapper;
using Npgsql;

namespace AspNet10.Repository;

public sealed class FruitRepository(NpgsqlDataSource dataSource) : IFruitRepository
{
    private const string SelectSql = """
        SELECT f.id          AS "FruitId",
               f.name        AS "FruitName",
               f.description AS "FruitDescription",
               s.id          AS "StoreId",
               s.name        AS "StoreName",
               s.currency    AS "StoreCurrency",
               s.address     AS "StoreAddress",
               s.city        AS "StoreCity",
               s.country     AS "StoreCountry",
               sfp.price     AS "Price"
          FROM fruits f
          LEFT JOIN store_fruit_prices sfp ON sfp.fruit_id = f.id
          LEFT JOIN stores s ON s.id = sfp.store_id
        """;

    private const string InsertSql = """
        INSERT INTO fruits (id, name, description)
        VALUES (nextval('fruits_seq'), @Name, @Description)
        RETURNING id
        """;

    public async Task<IReadOnlyList<Fruit>> FindAllAsync(
        CancellationToken cancellationToken = default)
    {
        const string sql = SelectSql + " ORDER BY f.id, s.id";
        var rows = await QueryAsync(sql, null, cancellationToken);
        return MapRows(rows);
    }

    public async Task<Fruit?> FindByNameAsync(
        string name,
        CancellationToken cancellationToken = default)
    {
        const string sql = SelectSql + " WHERE f.name = @Name ORDER BY f.id, s.id";
        var rows = await QueryAsync(sql, new { Name = name }, cancellationToken);
        return MapRows(rows).SingleOrDefault();
    }

    public async Task<Fruit> SaveAsync(
        Fruit fruit,
        CancellationToken cancellationToken = default)
    {
        await using var connection = await dataSource.OpenConnectionAsync(cancellationToken);
        await using var transaction = await connection.BeginTransactionAsync(cancellationToken);

        var command = new CommandDefinition(
            InsertSql,
            new { fruit.Name, fruit.Description },
            transaction,
            cancellationToken: cancellationToken);
        fruit.Id = await connection.QuerySingleAsync<long>(command);

        await transaction.CommitAsync(cancellationToken);
        return fruit;
    }

    private async Task<IEnumerable<FruitRow>> QueryAsync(
        string sql,
        object? parameters,
        CancellationToken cancellationToken)
    {
        await using var connection = await dataSource.OpenConnectionAsync(cancellationToken);
        return await connection.QueryAsync<FruitRow>(
            new CommandDefinition(sql, parameters, cancellationToken: cancellationToken));
    }

    private static IReadOnlyList<Fruit> MapRows(IEnumerable<FruitRow> rows)
    {
        var fruits = new Dictionary<long, Fruit>();

        foreach (var row in rows)
        {
            if (!fruits.TryGetValue(row.FruitId, out var fruit))
            {
                fruit = new Fruit
                {
                    Id = row.FruitId,
                    Name = row.FruitName,
                    Description = row.FruitDescription
                };
                fruits.Add(row.FruitId, fruit);
            }

            if (row.StoreId is { } storeId)
            {
                var store = new Store
                {
                    Id = storeId,
                    Name = row.StoreName!,
                    Currency = row.StoreCurrency!,
                    Address = new Address(row.StoreAddress!, row.StoreCity!, row.StoreCountry!)
                };
                fruit.StorePrices.Add(new StoreFruitPrice
                {
                    Id = new StoreFruitPriceId(storeId, row.FruitId),
                    Store = store,
                    Fruit = fruit,
                    Price = row.Price!.Value
                });
            }
        }

        return fruits.Values.ToArray();
    }

    private sealed class FruitRow
    {
        public long FruitId { get; init; }
        public required string FruitName { get; init; }
        public string? FruitDescription { get; init; }
        public long? StoreId { get; init; }
        public string? StoreName { get; init; }
        public string? StoreCurrency { get; init; }
        public string? StoreAddress { get; init; }
        public string? StoreCity { get; init; }
        public string? StoreCountry { get; init; }
        public decimal? Price { get; init; }
    }
}
