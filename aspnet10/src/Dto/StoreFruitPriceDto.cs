using System.ComponentModel.DataAnnotations;

namespace AspNet10.Dto;

public sealed record StoreFruitPriceDto(
    StoreDto Store,
    [Range(0, float.MaxValue, ErrorMessage = "Price must be >= 0")] float Price);
