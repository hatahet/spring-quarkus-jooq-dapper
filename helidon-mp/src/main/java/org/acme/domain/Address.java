package org.acme.domain;

import jakarta.validation.constraints.NotBlank;

public record Address(
    @NotBlank(message = "Address is mandatory")
    String address,

    @NotBlank(message = "City is mandatory")
    String city,

    @NotBlank(message = "Country is mandatory")
    String country
) {}
