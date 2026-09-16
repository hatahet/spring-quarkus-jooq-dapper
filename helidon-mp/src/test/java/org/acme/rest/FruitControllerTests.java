package org.acme.rest;

import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import jakarta.ws.rs.core.Response.Status;

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

@io.helidon.microprofile.testing.junit5.HelidonTest
@io.helidon.microprofile.testing.junit5.DisableDiscovery
@io.helidon.microprofile.testing.junit5.AddJaxRs
@io.helidon.microprofile.testing.junit5.AddExtension(io.helidon.microprofile.config.ConfigCdiExtension.class)
@io.helidon.microprofile.testing.junit5.AddExtension(io.helidon.microprofile.telemetry.TelemetryCdiExtension.class)
@io.helidon.microprofile.testing.junit5.AddConfig(key = "otel.sdk.disabled", value = "true")
@io.helidon.microprofile.testing.junit5.AddBean(FruitController.class)
@io.helidon.microprofile.testing.junit5.AddBean(FruitService.class)
@io.helidon.microprofile.testing.junit5.AddBean(FruitControllerTests.MockRepository.class)
class FruitControllerTests {
  @jakarta.inject.Inject
  FruitRepository fruitRepository;

  @jakarta.inject.Inject
  jakarta.ws.rs.client.WebTarget target;

  @BeforeEach
  void resetRepository() {
    Mockito.reset(fruitRepository);
    RestAssured.port = target.getUri().getPort();
  }

  @jakarta.enterprise.context.ApplicationScoped
  public static class MockRepository {
    @jakarta.enterprise.inject.Produces
    @jakarta.inject.Singleton
    FruitRepository repository() {
      return Mockito.mock(FruitRepository.class);
    }
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

    Mockito.when(this.fruitRepository.findAll())
        .thenReturn(List.of(fruit));

    get("/fruits").then()
        .statusCode(Status.OK.getStatusCode())
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

    Mockito.verify(this.fruitRepository).findAll();
    Mockito.verifyNoMoreInteractions(this.fruitRepository);
  }

  @Test
  public void getFruitFound() {
    var fruit = createFruit();
    var fruitStorePrice = fruit.getStorePrices().stream().findFirst().get();
    var store = fruitStorePrice.getStore();

    Mockito.when(this.fruitRepository.findByName("Apple"))
        .thenReturn(Optional.of(fruit));

    get("/fruits/Apple").then()
        .statusCode(Status.OK.getStatusCode())
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

    Mockito.verify(this.fruitRepository).findByName("Apple");
    Mockito.verifyNoMoreInteractions(this.fruitRepository);
  }

  @Test
  public void getFruitNotFound() {
    Mockito.when(this.fruitRepository.findByName("Apple"))
        .thenReturn(Optional.empty());

    get("/fruits/Apple").then()
        .statusCode(Status.NOT_FOUND.getStatusCode());

    Mockito.verify(this.fruitRepository).findByName("Apple");
    Mockito.verifyNoMoreInteractions(this.fruitRepository);
  }

  @Test
  public void addFruit() {
    Mockito.when(this.fruitRepository.save(Mockito.any(Fruit.class)))
        .thenReturn(new Fruit(1L, "Grapefruit", "Summer fruit"));

    given()
        .contentType(ContentType.JSON)
        .body("{\"name\":\"Grapefruit\",\"description\":\"Summer fruit\"}")
        .when().post("/fruits")
        .then()
        .contentType(ContentType.JSON)
        .statusCode(Status.OK.getStatusCode())
        .body("id", is(1))
        .body("name", is("Grapefruit"))
        .body("description", is("Summer fruit"));

    Mockito.verify(this.fruitRepository).save(Mockito.any(Fruit.class));
    Mockito.verifyNoMoreInteractions(this.fruitRepository);
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
