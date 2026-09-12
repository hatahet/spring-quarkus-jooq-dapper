package org.acme.repository;

import static java.util.Objects.requireNonNull;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.noCondition;
import static org.jooq.impl.DSL.sequence;
import static org.jooq.impl.DSL.table;
import static org.jooq.impl.DSL.val;
import static org.jooq.impl.SQLDataType.BIGINT;
import static org.jooq.impl.SQLDataType.NUMERIC;
import static org.jooq.impl.SQLDataType.VARCHAR;
import static org.springframework.transaction.annotation.Propagation.SUPPORTS;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

import org.acme.domain.Address;
import org.acme.domain.Fruit;
import org.acme.domain.Store;
import org.acme.domain.StoreFruitPrice;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.Sequence;
import org.jooq.Table;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class FruitRepository {
  private static final Table<Record> FRUITS = table(name("fruits"));
  private static final Field<Long> ID = field(name("id"), BIGINT);
  private static final Field<String> NAME = field(name("name"), VARCHAR);
  private static final Field<String> DESCRIPTION = field(name("description"), VARCHAR);
  private static final Sequence<Long> FRUITS_SEQUENCE = sequence(name("fruits_seq"), BIGINT);

  private static final Table<Record> F = FRUITS.as("f");
  private static final Table<Record> SFP = table(name("store_fruit_prices")).as("sfp");
  private static final Table<Record> S = table(name("stores")).as("s");

  private static final Field<Long> FRUIT_ID = field(name("f", "id"), BIGINT);
  private static final Field<String> FRUIT_NAME = field(name("f", "name"), VARCHAR);
  private static final Field<String> FRUIT_DESCRIPTION = field(name("f", "description"), VARCHAR);
  private static final Field<Long> PRICE_FRUIT_ID = field(name("sfp", "fruit_id"), BIGINT);
  private static final Field<Long> PRICE_STORE_ID = field(name("sfp", "store_id"), BIGINT);
  private static final Field<BigDecimal> PRICE = field(name("sfp", "price"), NUMERIC);
  private static final Field<Long> STORE_ID = field(name("s", "id"), BIGINT);
  private static final Field<String> STORE_NAME = field(name("s", "name"), VARCHAR);
  private static final Field<String> STORE_CURRENCY = field(name("s", "currency"), VARCHAR);
  private static final Field<String> STORE_ADDRESS = field(name("s", "address"), VARCHAR);
  private static final Field<String> STORE_CITY = field(name("s", "city"), VARCHAR);
  private static final Field<String> STORE_COUNTRY = field(name("s", "country"), VARCHAR);

  private final DSLContext dsl;

  public FruitRepository(DSLContext dsl) {
    this.dsl = dsl;
  }

  @Transactional(propagation = SUPPORTS, readOnly = true)
  public Optional<Fruit> findByName(String name) {
    return fetchFruits(FRUIT_NAME.eq(name)).stream().findFirst();
  }

  @Transactional(propagation = SUPPORTS, readOnly = true)
  public List<Fruit> findAll() {
    return fetchFruits(noCondition());
  }

  public Fruit save(Fruit fruit) {
    Field<Long> id = fruit.getId() == null ? FRUITS_SEQUENCE.nextval() : val(fruit.getId());
    Long savedId = this.dsl.insertInto(FRUITS)
        .set(ID, id)
        .set(NAME, fruit.getName())
        .set(DESCRIPTION, fruit.getDescription())
        .returningResult(ID)
        .fetchOne(ID);

    fruit.setId(requireNonNull(savedId, "Insert did not return a fruit id"));
    return fruit;
  }

  private List<Fruit> fetchFruits(Condition condition) {
    var rows = this.dsl
        .select(
            FRUIT_ID,
            FRUIT_NAME,
            FRUIT_DESCRIPTION,
            STORE_ID,
            STORE_NAME,
            STORE_CURRENCY,
            STORE_ADDRESS,
            STORE_CITY,
            STORE_COUNTRY,
            PRICE)
        .from(F)
        .leftJoin(SFP).on(PRICE_FRUIT_ID.eq(FRUIT_ID))
        .leftJoin(S).on(STORE_ID.eq(PRICE_STORE_ID))
        .where(condition)
        .orderBy(FRUIT_ID, STORE_ID)
        .fetch();

    return mapRows(rows);
  }

  private static List<Fruit> mapRows(Result<? extends Record> rows) {
    var fruits = new LinkedHashMap<Long, Fruit>();

    for (var row : rows) {
      Long fruitId = row.get(FRUIT_ID);
      Fruit fruit = fruits.computeIfAbsent(fruitId, ignored -> {
        var mappedFruit = new Fruit(fruitId, row.get(FRUIT_NAME), row.get(FRUIT_DESCRIPTION));
        mappedFruit.setStorePrices(new ArrayList<>());
        return mappedFruit;
      });

      Long storeId = row.get(STORE_ID);
      if (storeId != null) {
        var address = new Address(
            row.get(STORE_ADDRESS),
            row.get(STORE_CITY),
            row.get(STORE_COUNTRY));
        var store = new Store(
            storeId,
            row.get(STORE_NAME),
            address,
            row.get(STORE_CURRENCY));
        fruit.getStorePrices().add(new StoreFruitPrice(store, fruit, row.get(PRICE)));
      }
    }

    return List.copyOf(fruits.values());
  }
}
