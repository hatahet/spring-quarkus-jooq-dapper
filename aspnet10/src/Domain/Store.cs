namespace AspNet10.Domain;

public sealed class Store
{
    public required long Id { get; init; }
    public required string Name { get; init; }
    public required string Currency { get; init; }
    public required Address Address { get; init; }
}
