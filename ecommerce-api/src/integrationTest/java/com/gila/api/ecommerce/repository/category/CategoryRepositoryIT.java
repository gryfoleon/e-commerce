package com.gila.api.ecommerce.repository.category;

import static org.assertj.core.api.Assertions.assertThat;

import com.gila.api.ecommerce.configuration.IntegrationTestContainersConfiguration;
import com.gila.api.ecommerce.model.entity.ProductCategory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("IT")
@Import(IntegrationTestContainersConfiguration.class)
class CategoryRepositoryIT {

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void findByNameIgnoreCase_ignoresCase() {
        assertThat(categoryRepository.findByNameIgnoreCase("eLeCtRoNiCs"))
                .get()
                .extracting(ProductCategory::getName)
                .isEqualTo("Electronics");
    }

    @Test
    void findByNameIgnoreCase_returnsEmptyWhenMissing() {
        assertThat(categoryRepository.findByNameIgnoreCase("Unknown")).isEmpty();
    }

    @Test
    void existsByNameIgnoreCase_returnsTrueForExistingName() {
        assertThat(categoryRepository.existsByNameIgnoreCase("BOOKS")).isTrue();
    }

    @Test
    void existsByNameIgnoreCase_returnsFalseForMissingName() {
        assertThat(categoryRepository.existsByNameIgnoreCase("Unknown")).isFalse();
    }

    @Test
    void existsByNameIgnoreCaseAndIdNot_returnsFalseForSameCategory() {
        Short booksId = categoryRepository.findByNameIgnoreCase("Books").orElseThrow().getId();

        assertThat(categoryRepository.existsByNameIgnoreCaseAndIdNot("books", booksId)).isFalse();
    }

    @Test
    void existsByNameIgnoreCaseAndIdNot_returnsTrueForDifferentCategory() {
        Short electronicsId = categoryRepository.findByNameIgnoreCase("Electronics").orElseThrow().getId();

        assertThat(categoryRepository.existsByNameIgnoreCaseAndIdNot("books", electronicsId)).isTrue();
    }
}
