using System.ComponentModel.DataAnnotations;

namespace AspNet10.Dto;

public sealed record StoreDto(
    long Id,
    [Required(ErrorMessage = "Name is mandatory")] string Name,
    [Required(ErrorMessage = "Currency is mandatory")] string Currency,
    AddressDto Address);
