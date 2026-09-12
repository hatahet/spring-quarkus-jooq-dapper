using System.ComponentModel.DataAnnotations;

namespace AspNet10.Dto;

public sealed record AddressDto(
    [Required(ErrorMessage = "Address is mandatory")] string Address,
    [Required(ErrorMessage = "City is mandatory")] string City,
    [Required(ErrorMessage = "Country is mandatory")] string Country);
