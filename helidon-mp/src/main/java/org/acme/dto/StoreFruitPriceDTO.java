package org.acme.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record StoreFruitPriceDTO(StoreDTO store, BigDecimal price) {
  public StoreFruitPriceDTO {
    if ((price != null) && (price.signum() < 0)) {
      throw new IllegalArgumentException("Price must be >= 0");
    }
  }
}
