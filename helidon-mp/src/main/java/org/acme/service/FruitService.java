package org.acme.service;


import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.acme.dto.FruitDTO;
import org.acme.mapping.FruitMapper;
import org.acme.repository.FruitRepository;

import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;

@ApplicationScoped
public class FruitService {
  private final FruitRepository fruitRepository;

  @Inject
  public FruitService(FruitRepository fruitRepository) {
    this.fruitRepository = fruitRepository;
  }

  @WithSpan("FruitService.getAllFruits")
  public List<FruitDTO> getAllFruits() {
    return this.fruitRepository.findAll().stream()
        .map(FruitMapper::map)
        .toList();
  }

  @WithSpan("FruitService.getFruitByName")
  public Optional<FruitDTO> getFruitByName(@SpanAttribute("arg.name") String name) {
    return this.fruitRepository.findByName(name)
        .map(FruitMapper::map);
  }

  @WithSpan("FruitService.createFruit")
  public FruitDTO createFruit(@SpanAttribute("arg.fruit") FruitDTO fruitDTO) {
    var fruit = FruitMapper.map(fruitDTO);
    var savedFruit = this.fruitRepository.save(fruit);
    return FruitMapper.map(savedFruit);
  }
}
