package org.acme.domain;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

public class StoreFruitPrice {
  private StoreFruitPriceId id;

  private Store store;

  private Fruit fruit;

  @NotNull
  @DecimalMin(value = "0.00", message = "Price must be >= 0")
  @Digits(integer = 10, fraction = 2)
  private BigDecimal price;

  public StoreFruitPrice() {}

  public StoreFruitPrice(Store store, Fruit fruit, BigDecimal price) {
    this.store = store;
    this.fruit = fruit;
    this.price = price;
    this.id = new StoreFruitPriceId(store, fruit);
  }

  public StoreFruitPriceId getId() { return id; }
  public void setId(StoreFruitPriceId id) { this.id = id; }

  public Store getStore() { return store; }
  public void setStore(Store store) {
    this.store = store;
    this.id = new StoreFruitPriceId((store != null) ? store.getId() : null,
        (this.id != null) ? this.id.fruitId() : null);
  }

  public Fruit getFruit() { return fruit; }
  public void setFruit(Fruit fruit) {
    this.fruit = fruit;
    this.id = new StoreFruitPriceId((this.id != null) ? this.id.storeId() : null,
        (fruit != null) ? fruit.getId() : null);
  }

  public BigDecimal getPrice() { return price; }
  public void setPrice(BigDecimal price) { this.price = price; }

}
