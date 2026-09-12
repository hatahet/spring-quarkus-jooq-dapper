using System.ComponentModel.DataAnnotations;

namespace AspNet10.Dto;

public sealed record FruitDto(
    long? Id,
    [Required(ErrorMessage = "Name is mandatory")] string Name,
    string? Description,
    IReadOnlyList<StoreFruitPriceDto>? StorePrices);
