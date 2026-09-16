package org.acme.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record StoreDTO(Long id, String name, String currency, AddressDTO address) {
  public StoreDTO {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("Name is mandatory");
    }

    if (currency == null || currency.isBlank()) {
      throw new IllegalArgumentException("Currency is mandatory");
    }
  }
}
