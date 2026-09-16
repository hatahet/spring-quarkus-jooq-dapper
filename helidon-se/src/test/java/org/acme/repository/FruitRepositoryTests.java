package org.acme.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.stream.IntStream;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.acme.domain.Fruit;
import org.jooq.SQLDialect;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class FruitRepositoryTests {
  @Container
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17");

  private static HikariDataSource pool;
  private static FruitRepository repository;

  @BeforeAll
  static void initializeDatabase() throws Exception {
    var config = new HikariConfig();
    config.setJdbcUrl(POSTGRES.getJdbcUrl());
    config.setUsername(POSTGRES.getUsername());
    config.setPassword(POSTGRES.getPassword());
    // A small pool makes connection leaks observable within a few requests.
    config.setMaximumPoolSize(2);
    config.setConnectionTimeout(1000);
    pool = new HikariDataSource(config);
    try (var connection = pool.getConnection(); var statement = connection.createStatement()) {
      for (String resource : new String[] {"schema.sql", "import.sql"}) {
        try (var stream = FruitRepositoryTests.class.getResourceAsStream("/" + resource)) {
          statement.execute(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
        }
      }
    }
    repository = new FruitRepository(DSL.using(pool, SQLDialect.POSTGRES));
  }

  @AfterAll
  static void closePool() {
    if (pool != null) {
      pool.close();
      assertThat(pool.isClosed()).isTrue();
    }
  }

  @Test
  void readsTheCompleteOrderedGraph() {
    var fruits = repository.findAll();
    assertThat(fruits).extracting(Fruit::getId).isSorted();
    // Another test may have inserted a fruit, but the reference seed remains intact.
    assertThat(fruits.stream().filter(fruit -> fruit.getId() <= 10)).hasSize(10);
    assertThat(fruits.stream().filter(fruit -> fruit.getId() <= 10)
        .mapToInt(fruit -> fruit.getStorePrices().size()).sum()).isEqualTo(34);
    var apple = repository.findByName("Apple").orElseThrow();
    assertThat(apple.getStorePrices()).hasSize(7);
    assertThat(apple.getStorePrices()).extracting(price -> price.getStore().getId()).isSorted();
    var price = apple.getStorePrices().getFirst();
    assertThat(price.getStore().getName()).isEqualTo("Store 1");
    assertThat(price.getStore().getAddress().address()).isEqualTo("123 Main St");
    assertThat(price.getPrice()).isEqualByComparingTo("1.29");
    assertThat(price.getFruit()).isSameAs(apple);
    assertThat(price.getId().fruitId()).isEqualTo(apple.getId());
    assertThat(price.getId().storeId()).isEqualTo(price.getStore().getId());
    assertThat(repository.findByName("Unknown")).isEmpty();
    assertThat(repository.findByName("' OR 1=1 --")).isEmpty();
  }

  @Test
  void insertsUsingTheSequenceAndReturnsConnectionsAfterFailure() {
    var saved = repository.save(new Fruit(null, "Grapefruit", "Summer fruit"));
    assertThat(saved.getId()).isGreaterThan(10L);
    var found = repository.findByName("Grapefruit").orElseThrow();
    assertThat(found.getId()).isEqualTo(saved.getId());
    assertThat(found.getDescription()).isEqualTo("Summer fruit");
    assertThat(found.getStorePrices()).isEmpty();
    assertThatThrownBy(() -> repository.save(new Fruit(null, "Apple", "Duplicate")))
        .isInstanceOf(DataAccessException.class);
    assertThat(repository.findByName("Apple")).isPresent();
    assertThat(pool.getHikariPoolMXBean().getActiveConnections()).isZero();
  }

  @Test
  void concurrentReadsReusePooledConnections() {
    IntStream.range(0, 40).parallel().forEach(ignored ->
        assertThat(repository.findByName("Apple").orElseThrow().getStorePrices()).hasSize(7));
    assertThat(pool.getHikariPoolMXBean().getTotalConnections()).isLessThanOrEqualTo(2);
    assertThat(pool.getHikariPoolMXBean().getActiveConnections()).isZero();
  }
}
