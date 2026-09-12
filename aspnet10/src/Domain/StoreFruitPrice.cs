namespace AspNet10.Domain;

public sealed class StoreFruitPrice
{
    public required StoreFruitPriceId Id { get; init; }
    public required Store Store { get; init; }
    public required Fruit Fruit { get; init; }
    public required decimal Price { get; init; }
}
