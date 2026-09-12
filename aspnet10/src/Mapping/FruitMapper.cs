using AspNet10.Domain;
using AspNet10.Dto;

namespace AspNet10.Mapping;

public static class FruitMapper
{
    public static FruitDto ToDto(Fruit fruit)
    {
        IReadOnlyList<StoreFruitPriceDto>? prices = fruit.StorePrices.Count == 0
            ? null
            : fruit.StorePrices.Select(ToDto).ToArray();

        return new FruitDto(fruit.Id, fruit.Name, fruit.Description, prices);
    }

    public static Fruit ToDomain(FruitDto fruit) => new()
    {
        Name = fruit.Name,
        Description = fruit.Description
    };

    private static StoreFruitPriceDto ToDto(StoreFruitPrice price)
    {
        var store = price.Store;
        var address = store.Address;

        return new StoreFruitPriceDto(
            new StoreDto(
                store.Id,
                store.Name,
                store.Currency,
                new AddressDto(address.Street, address.City, address.Country)),
            (float)price.Price);
    }
}
