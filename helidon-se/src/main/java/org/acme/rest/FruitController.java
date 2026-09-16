package org.acme.rest;

import io.helidon.http.BadRequestException;
import io.helidon.http.Status;
import io.helidon.webserver.http.HttpRules;
import io.helidon.webserver.http.HttpService;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import jakarta.validation.Validator;
import org.acme.dto.FruitDTO;
import org.acme.service.FruitService;

public final class FruitController implements HttpService {
  private final FruitService fruitService;
  private final Validator validator;

  public FruitController(FruitService fruitService, Validator validator) {
    this.fruitService = fruitService;
    this.validator = validator;
  }

  @Override
  public void routing(HttpRules routing) {
    routing.get("/", (request, response) -> response.send(fruitService.getAllFruits()))
        .get("/{name}", this::getFruit)
        .post("/", this::addFruit);
  }

  private void getFruit(ServerRequest request, ServerResponse response) {
    var fruit = fruitService.getFruitByName(request.path().pathParameters().get("name"));
    if (fruit.isPresent()) {
      response.send(fruit.get());
    } else {
      response.status(Status.NOT_FOUND_404).send();
    }
  }

  private void addFruit(ServerRequest request, ServerResponse response) {
    FruitDTO fruit;
    try {
      fruit = request.content().as(FruitDTO.class);
    } catch (io.helidon.http.media.jackson.JacksonRuntimeException e) {
      throw new BadRequestException("Invalid fruit JSON", e);
    }
    if (fruit == null || !validator.validate(fruit).isEmpty()) {
      throw new BadRequestException("Name is mandatory");
    }
    response.send(fruitService.createFruit(fruit));
  }
}
