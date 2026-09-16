package org.acme.rest;

import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;


import org.acme.domain.Address;
import org.acme.domain.Fruit;
import org.acme.domain.Store;
import org.acme.domain.StoreFruitPrice;
import org.acme.repository.FruitRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.junit.jupiter.api.BeforeEach;
import org.acme.service.FruitService;
import io.restassured.RestAssured;

import io.restassured.http.ContentType;

class FruitControllerTests {
  private static io.helidon.webserver.WebServer server;
  private static jakarta.validation.ValidatorFactory validators;
  private static final FruitRepository fruitRepository = Mockito.mock(FruitRepository.class);

  @org.junit.jupiter.api.BeforeAll
  static void startServer() {
    validators = jakarta.validation.Validation.byDefaultProvider().configure()
        .messageInterpolator(new org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator())
        .buildValidatorFactory();
    var config = io.helidon.config.Config.builder()
        .sources(io.helidon.config.ConfigSources.create(java.util.Map.of("server.port", "0")))
        .disableEnvironmentVariablesSource().disableSystemPropertiesSource().build();
    var service = new FruitService(fruitRepository, io.opentelemetry.api.OpenTelemetry.noop().getTracer("test"));
    server = org.acme.Main.serverBuilder(config, service, validators.getValidator()).build().start();
    RestAssured.port = server.port();
  }

  @org.junit.jupiter.api.AfterAll
  static void stopServer() {
    server.stop();
    validators.close();
    RestAssured.reset();
  }

  @BeforeEach
  void resetRepository() {
    Mockito.reset(fruitRepository);
  }



  private static Fruit createFruit() {
    var price = BigDecimal.valueOf(1.29);
    var store = new Store(1L, "Some Store", new Address("123 Some St", "Some City", "USA"), "USD");
    var fruit = new Fruit(1L, "Apple", "Hearty Fruit");
    fruit.setStorePrices(List.of(new StoreFruitPrice(store, fruit, price)));

    return fruit;
  }

  @Test
  public void getAll() {
    var fruit = createFruit();
    var fruitStorePrice = fruit.getStorePrices().getFirst();
    var store = fruitStorePrice.getStore();

    Mockito.when(fruitRepository.findAll())
        .thenReturn(List.of(fruit));

    get("/fruits").then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body("$.size()", is(1))
        .body("[0].id", is(1))
        .body("[0].name", is("Apple"))
        .body("[0].description", is("Hearty Fruit"))
        .body("[0].storePrices[0].price", is(fruitStorePrice.getPrice().floatValue()))
        .body("[0].storePrices[0].store.name", is(store.getName()))
        .body("[0].storePrices[0].store.address.address", is(store.getAddress().address()))
        .body("[0].storePrices[0].store.address.city", is(store.getAddress().city()))
        .body("[0].storePrices[0].store.address.country", is(store.getAddress().country()))
        .body("[0].storePrices[0].store.currency", is(store.getCurrency()));

    Mockito.verify(fruitRepository).findAll();
    Mockito.verifyNoMoreInteractions(fruitRepository);
  }

  @Test
  public void getFruitFound() {
    var fruit = createFruit();
    var fruitStorePrice = fruit.getStorePrices().stream().findFirst().get();
    var store = fruitStorePrice.getStore();

    Mockito.when(fruitRepository.findByName("Apple"))
        .thenReturn(Optional.of(fruit));

    get("/fruits/Apple").then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body("id", is(1))
        .body("name", is("Apple"))
        .body("description", is("Hearty Fruit"))
        .body("storePrices[0].price", is(fruitStorePrice.getPrice().floatValue()))
        .body("storePrices[0].store.name", is(store.getName()))
        .body("storePrices[0].store.address.address", is(store.getAddress().address()))
        .body("storePrices[0].store.address.city", is(store.getAddress().city()))
        .body("storePrices[0].store.address.country", is(store.getAddress().country()))
        .body("storePrices[0].store.currency", is(store.getCurrency()));

    Mockito.verify(fruitRepository).findByName("Apple");
    Mockito.verifyNoMoreInteractions(fruitRepository);
  }

  @Test
  public void getFruitNotFound() {
    Mockito.when(fruitRepository.findByName("Apple"))
        .thenReturn(Optional.empty());

    get("/fruits/Apple").then()
        .statusCode(404);

    Mockito.verify(fruitRepository).findByName("Apple");
    Mockito.verifyNoMoreInteractions(fruitRepository);
  }

  @Test
  public void addFruit() {
    Mockito.when(fruitRepository.save(Mockito.any(Fruit.class)))
        .thenReturn(new Fruit(1L, "Grapefruit", "Summer fruit"));

    given()
        .contentType(ContentType.JSON)
        .body("{\"name\":\"Grapefruit\",\"description\":\"Summer fruit\"}")
        .when().post("/fruits")
        .then()
        .contentType(ContentType.JSON)
        .statusCode(200)
        .body("id", is(1))
        .body("name", is("Grapefruit"))
        .body("description", is("Summer fruit"));

    Mockito.verify(fruitRepository).save(Mockito.any(Fruit.class));
    Mockito.verifyNoMoreInteractions(fruitRepository);
  }

  @Test
  void optionalEmptyFieldsAreOmitted() {
    Mockito.when(fruitRepository.findByName("Pear"))
        .thenReturn(Optional.of(new Fruit(2L, "Pear", "")));
    get("/fruits/Pear").then().statusCode(200)
        .body("containsKey('description')", is(false))
        .body("containsKey('storePrices')", is(false));
  }

  @Test
  void invalidNameIsRejected() {
    for (String body : List.of("{\"name\":\"   \"}", "{}", "{\"name\":null}")) {
      given().contentType(ContentType.JSON).body(body).post("/fruits").then().statusCode(400);
    }
    Mockito.verifyNoInteractions(fruitRepository);
  }

  @Test
  void malformedJsonIsRejected() {
    given().contentType(ContentType.JSON).body("{").post("/fruits").then().statusCode(400);
    Mockito.verifyNoInteractions(fruitRepository);
  }
}
