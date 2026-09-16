package org.acme.service;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import org.acme.dto.FruitDTO;
import org.acme.mapping.FruitMapper;
import org.acme.repository.FruitRepository;

public class FruitService {
  private final FruitRepository fruitRepository;
  private final Tracer tracer;

  public FruitService(FruitRepository fruitRepository, Tracer tracer) {
    this.fruitRepository = fruitRepository;
    this.tracer = tracer;
  }

  public List<FruitDTO> getAllFruits() {
    return traced(tracer.spanBuilder("FruitService.getAllFruits"),
        () -> fruitRepository.findAll().stream().map(FruitMapper::map).toList());
  }

  public Optional<FruitDTO> getFruitByName(String name) {
    return traced(tracer.spanBuilder("FruitService.getFruitByName").setAttribute("arg.name", name),
        () -> fruitRepository.findByName(name).map(FruitMapper::map));
  }

  public FruitDTO createFruit(FruitDTO fruitDTO) {
    return traced(tracer.spanBuilder("FruitService.createFruit").setAttribute("arg.fruit", fruitDTO.toString()),
        () -> FruitMapper.map(fruitRepository.save(FruitMapper.map(fruitDTO))));
  }

  private static <T> T traced(SpanBuilder builder, Supplier<T> operation) {
    var span = builder.startSpan();
    try (var scope = span.makeCurrent()) {
      return operation.get();
    } catch (RuntimeException e) {
      span.recordException(e);
      span.setStatus(StatusCode.ERROR);
      throw e;
    } finally {
      span.end();
    }
  }
}
