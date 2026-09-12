package org.acme.e2e;

import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;

import jakarta.ws.rs.core.Response.Status;

import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import io.quarkus.test.junit.QuarkusIntegrationTest;

import io.restassured.http.ContentType;


// Note: There isn't an equivalent of this test in the Spring projects.
// It tests the application as a prod mode application, from a different process.
// The Dev Services database is initialized with the same schema and seed data as every other test database.
@QuarkusIntegrationTest
@TestMethodOrder(OrderAnnotation.class)
public class FruitControllerIT {
	private static final int DEFAULT_ORDER = 1;

	@Test
	@Order(DEFAULT_ORDER)
	public void getFruitNotFound() {
		get("/fruits/XXXX").then()
			.statusCode(Status.NOT_FOUND.getStatusCode());
	}

    @Test
    @Order(DEFAULT_ORDER + 1)
    public void addFruit() {
        get("/fruits").then()
                .body("$.size()", is(10));

        given()
                .contentType(ContentType.JSON)
                .body("{\"name\":\"Pomelo\",\"description\":\"Exotic fruit\"}")
                .when().post("/fruits")
                .then()
                .contentType(ContentType.JSON)
                .statusCode(Status.OK.getStatusCode())
                .body("id", greaterThanOrEqualTo(1))
                .body("name", is("Pomelo"))
                .body("description", is("Exotic fruit"));

        get("/fruits").then()
                .body("$.size()", is(11));
    }


	@Test
	@Order(DEFAULT_ORDER+2)
	public void getFruitFound() {
		get("/fruits/Pomelo").then()
			.statusCode(Status.OK.getStatusCode())
			.contentType(ContentType.JSON)
			.body("id", greaterThanOrEqualTo(1))
			.body("name", is("Pomelo"))
			.body("description", is("Exotic fruit"));
	}

    @Test
    @Order(DEFAULT_ORDER+3)
    public void getAll() {
        get("/fruits").then()
                .statusCode(Status.OK.getStatusCode())
                .contentType(ContentType.JSON)
                .body("$.size()", is(11))
                .body("[10].id", greaterThanOrEqualTo(11))
                .body("[10].name", is("Pomelo"))
                .body("[10].description", is("Exotic fruit"));
    }

}
