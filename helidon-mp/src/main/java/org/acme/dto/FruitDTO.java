package org.acme.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.NotBlank;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record FruitDTO(Long id, @NotBlank(message = "Name is mandatory") String name, String description, List<StoreFruitPriceDTO> storePrices) {
  public FruitDTO {
    if (name == null) {
      throw new IllegalArgumentException("Name is mandatory");
    }

    if (storePrices == null) {
      storePrices = new ArrayList<>();
    }
  }
}
