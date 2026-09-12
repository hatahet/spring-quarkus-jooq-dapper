package org.acme.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import jakarta.inject.Inject;

import org.acme.domain.Fruit;
import org.junit.jupiter.api.Test;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@TestTransaction
class FruitRepositoryTests {
	@Inject
	FruitRepository fruitRepository;

	@Test
	public void findByName() {
		Optional<Fruit> apple = this.fruitRepository.findByName("Apple");
		assertThat(apple).isPresent();
		assertThat(apple.orElseThrow().getStorePrices())
			.hasSize(7)
			.first()
			.extracting(price -> price.getStore().getName())
			.isEqualTo("Store 1");

		var savedFruit = this.fruitRepository.save(new Fruit(null, "Grapefruit", "Summer fruit"));
		// Other tests share this sequence; consumed values are not rolled back.
		assertThat(savedFruit.getId()).isNotNull().isGreaterThan(10L);

		Optional<Fruit> fruit = this.fruitRepository.findByName("Grapefruit");
		assertThat(fruit)
			.isNotNull()
			.isPresent()
			.get()
			.extracting(Fruit::getName, Fruit::getDescription)
			.containsExactly("Grapefruit", "Summer fruit");

		assertThat(fruit.orElseThrow().getId()).isEqualTo(savedFruit.getId());
	}
}
