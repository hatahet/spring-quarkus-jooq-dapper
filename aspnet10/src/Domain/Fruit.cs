namespace AspNet10.Domain;

public sealed class Fruit
{
    public long? Id { get; set; }
    public required string Name { get; set; }
    public string? Description { get; set; }
    public List<StoreFruitPrice> StorePrices { get; } = [];
}
