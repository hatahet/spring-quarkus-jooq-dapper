package org.acme.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.acme.ContainersConfig;
import org.acme.domain.Fruit;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@Import(ContainersConfig.class)
class FruitRepositoryTests {
	@Autowired
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
