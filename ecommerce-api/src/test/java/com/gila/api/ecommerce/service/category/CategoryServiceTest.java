package com.gila.api.ecommerce.service.category;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gila.api.ecommerce.error.exceptions.CategoryNotFoundException;
import com.gila.api.ecommerce.error.exceptions.DuplicateCategoryException;
import com.gila.api.ecommerce.error.exceptions.InvalidCategoryException;
import com.gila.api.ecommerce.model.dto.category.CategoryRequest;
import com.gila.api.ecommerce.model.dto.category.CategoryResponse;
import com.gila.api.ecommerce.model.entity.ProductCategory;
import com.gila.api.ecommerce.repository.category.CategoryRepository;
import com.gila.api.ecommerce.support.ProductFixtures;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.DataIntegrityViolationException;

class CategoryServiceTest {

    private CategoryRepository repository;
    private CategoryService self;
    private CategoryService service;

    @BeforeEach
    void setUp() {
        repository = mock(CategoryRepository.class);
        self = mock(CategoryService.class);
        service = new CategoryService(repository, self);
    }

    private ProductCategory category(int id, String name) {
        return ProductFixtures.category((short) id, name);
    }

    @Test
    void findById_existing_mapsToResponse() {
        when(repository.findById((short) 2)).thenReturn(Optional.of(category(2, "Books")));

        assertThat(service.findById((short) 2)).usingRecursiveComparison()
                .isEqualTo(new CategoryResponse((short) 2, "Books", ProductFixtures.CREATED_AT));
    }

    @Test
    void findById_missing_throwsNotFound() {
        when(repository.findById((short) 9)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById((short) 9)).isInstanceOf(CategoryNotFoundException.class)
                .hasFieldOrPropertyWithValue("identifier", "9");
    }

    @Test
    void findByName_paddedName_isStrippedBeforeLookup() {
        when(repository.findByNameIgnoreCase("books")).thenReturn(Optional.of(category(2, "Books")));

        assertThat(service.findByName("  books ").name()).isEqualTo("Books");
    }

    @Test
    void findByName_missing_throwsNotFound() {
        when(repository.findByNameIgnoreCase("x")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByName("x")).isInstanceOf(CategoryNotFoundException.class);
    }

    @Test
    void create_newName_savesAndReturnsResponse() {
        when(repository.existsByNameIgnoreCase("Toys")).thenReturn(false);
        when(repository.saveAndFlush(any())).thenReturn(category(5, "Toys"));

        CategoryResponse response = service.create(new CategoryRequest("Toys"));

        assertThat(response.id()).isEqualTo((short) 5);
        assertThat(response.name()).isEqualTo("Toys");
    }

    @Test
    void create_existingNameIgnoringCase_throwsDuplicateWithoutSaving() {
        when(repository.existsByNameIgnoreCase("toys")).thenReturn(true);

        assertThatThrownBy(() -> service.create(new CategoryRequest("toys")))
                .isInstanceOf(DuplicateCategoryException.class);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void create_raceOnUniqueConstraint_translatesToDuplicate() {
        when(repository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("x",
                new RuntimeException("violates unique constraint \"uq_product_categories_name\"")));

        assertThatThrownBy(() -> service.create(new CategoryRequest("Toys")))
                .isInstanceOf(DuplicateCategoryException.class);
    }

    @Test
    void create_otherIntegrityViolation_isRethrownUntranslated() {
        DataIntegrityViolationException other = new DataIntegrityViolationException("x", new RuntimeException("other"));
        when(repository.saveAndFlush(any())).thenThrow(other);

        assertThatThrownBy(() -> service.create(new CategoryRequest("Toys"))).isSameAs(other);
    }

    @Test
    void update_existing_renamesAndSaves() {
        ProductCategory existing = category(3, "Old");
        when(repository.findById((short) 3)).thenReturn(Optional.of(existing));
        when(repository.existsByNameIgnoreCaseAndIdNot("New", (short) 3)).thenReturn(false);
        when(repository.saveAndFlush(existing)).thenReturn(existing);

        CategoryResponse response = service.update((short) 3, new CategoryRequest("New"));

        assertThat(response.name()).isEqualTo("New");
    }

    @Test
    void update_nameUsedByAnotherCategory_throwsDuplicate() {
        when(repository.findById((short) 3)).thenReturn(Optional.of(category(3, "Old")));
        when(repository.existsByNameIgnoreCaseAndIdNot("Taken", (short) 3)).thenReturn(true);

        assertThatThrownBy(() -> service.update((short) 3, new CategoryRequest("Taken")))
                .isInstanceOf(DuplicateCategoryException.class);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void update_missing_throwsNotFound() {
        when(repository.findById((short) 3)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update((short) 3, new CategoryRequest("New")))
                .isInstanceOf(CategoryNotFoundException.class);
    }

    @Test
    void delete_existing_deletesAndFlushes() {
        ProductCategory existing = category(3, "Old");
        when(repository.findById((short) 3)).thenReturn(Optional.of(existing));

        service.delete((short) 3);

        verify(repository).delete(existing);
        verify(repository).flush();
    }

    @Test
    void delete_missing_throwsNotFoundWithoutDeleting() {
        when(repository.findById((short) 3)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete((short) 3)).isInstanceOf(CategoryNotFoundException.class);
        verify(repository, never()).delete(any());
    }

    @Test
    void findAllNames_returnsNamesSortedAlphabetically() {
        when(repository.findAll()).thenReturn(List.of(category(1, "Toys"), category(2, "Books"),
                category(3, "Electronics")));

        assertThat(service.findAllNames()).containsExactly("Books", "Electronics", "Toys");
    }

    @Test
    void getByName_caseInsensitiveMatch_returnsEntity() {
        ProductCategory books = category(2, "Books");
        when(repository.findByNameIgnoreCase("books")).thenReturn(Optional.of(books));

        assertThat(service.getByName(" books ")).isSameAs(books);
    }

    @Test
    void getByName_unknown_throwsInvalidCategoryListingSortedAllowedValues() {
        when(repository.findByNameIgnoreCase("nope")).thenReturn(Optional.empty());
        when(repository.findAll()).thenReturn(List.of(category(1, "Toys"), category(2, "Books")));

        assertThatThrownBy(() -> service.getByName("nope")).isInstanceOf(InvalidCategoryException.class)
                .hasMessageContaining("Books, Toys");
    }

    @Test
    void findIdsByName_mapsNameToId() {
        when(repository.findAll()).thenReturn(List.of(category(1, "Toys"), category(2, "Books")));

        assertThat(service.findIdsByName()).isEqualTo(Map.of("Toys", (short) 1, "Books", (short) 2));
    }

    @Test
    void findCanonicalName_differentCase_returnsStoredSpelling() {
        when(self.findAllNames()).thenReturn(List.of("Books", "Toys"));

        assertThat(service.findCanonicalName("  tOYS ")).contains("Toys");
    }

    @Test
    void findCanonicalName_unknown_returnsEmpty() {
        when(self.findAllNames()).thenReturn(List.of("Books"));

        assertThat(service.findCanonicalName("cars")).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void findCanonicalName_blankOrNull_returnsEmptyWithoutQueryingNames(String value) {
        assertThat(service.findCanonicalName(value)).isEmpty();
        verify(self, never()).findAllNames();
    }

    @Test
    void resolveCanonicalName_known_returnsCanonical() {
        when(self.findAllNames()).thenReturn(List.of("Books"));

        assertThat(service.resolveCanonicalName("BOOKS")).isEqualTo("Books");
    }

    @Test
    void resolveCanonicalName_unknown_throwsInvalidCategory() {
        when(self.findAllNames()).thenReturn(List.of("Books"));

        assertThatThrownBy(() -> service.resolveCanonicalName("cars")).isInstanceOf(InvalidCategoryException.class);
    }
}
