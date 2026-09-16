package org.acme.domain;

import java.io.Serializable;

public record StoreFruitPriceId(
    Long storeId,
    Long fruitId
) implements Serializable {

  public StoreFruitPriceId(Store store, Fruit fruit) {
    this((store != null) ? store.getId() : null, (fruit != null) ? fruit.getId() : null);
  }
}
