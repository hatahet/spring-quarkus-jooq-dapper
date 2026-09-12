using AspNet10.Domain;
using AspNet10.Repository;
using Dapper;
using Npgsql;
using Testcontainers.PostgreSql;

namespace AspNet10.Tests;

public sealed class FruitRepositoryTests(PostgreSqlFixture fixture)
    : IClassFixture<PostgreSqlFixture>
{
    [Fact]
    [Trait("Category", "Integration")]
    public async Task ReadsJoinedGraphAndSavesWithSequence()
    {
        var repository = new FruitRepository(fixture.DataSource);

        var apple = await repository.FindByNameAsync("Apple");
        Assert.NotNull(apple);
        Assert.Equal(7, apple.StorePrices.Count);
        Assert.Equal("Store 1", apple.StorePrices[0].Store.Name);

        var saved = await repository.SaveAsync(new Fruit
        {
            Name = "Grapefruit",
            Description = "Summer fruit"
        });

        Assert.Equal(11, saved.Id);
        var reloaded = await repository.FindByNameAsync("Grapefruit");
        Assert.Equal("Summer fruit", reloaded!.Description);
    }
}

public sealed class PostgreSqlFixture : IAsyncLifetime
{
    private readonly PostgreSqlContainer _container = new PostgreSqlBuilder("postgres:17")
        .WithDatabase("fruits")
        .WithUsername("fruits")
        .WithPassword("fruits")
        .Build();

    public NpgsqlDataSource DataSource { get; private set; } = null!;

    public async Task InitializeAsync()
    {
        await _container.StartAsync();
        DataSource = NpgsqlDataSource.Create(_container.GetConnectionString());

        var script = await File.ReadAllTextAsync(Path.Combine(AppContext.BaseDirectory, "db.sql"));
        await using var connection = await DataSource.OpenConnectionAsync();
        await connection.ExecuteAsync(script);
    }

    public async Task DisposeAsync()
    {
        if (DataSource is not null)
        {
            await DataSource.DisposeAsync();
        }
        await _container.DisposeAsync();
    }
}
