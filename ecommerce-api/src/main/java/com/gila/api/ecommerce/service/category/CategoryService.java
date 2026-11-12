package com.gila.api.ecommerce.service.category;

import com.gila.api.ecommerce.configuration.cache.CacheNames;
import com.gila.api.ecommerce.error.exceptions.CategoryNotFoundException;
import com.gila.api.ecommerce.error.exceptions.DuplicateCategoryException;
import com.gila.api.ecommerce.error.exceptions.InvalidCategoryException;
import com.gila.api.ecommerce.model.dto.PageableResponse;
import com.gila.api.ecommerce.model.dto.category.CategoryRequest;
import com.gila.api.ecommerce.model.dto.category.CategoryResponse;
import com.gila.api.ecommerce.model.entity.ProductCategory;
import com.gila.api.ecommerce.repository.category.CategoryRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class CategoryService {

    private static final String NAME_UNIQUE_CONSTRAINT = "uq_product_categories_name";

    private final CategoryRepository categoryRepository;
    private final CategoryService self;

    public CategoryService(CategoryRepository categoryRepository, @Lazy CategoryService self) {
        this.categoryRepository = categoryRepository;
        this.self = self;
    }

    @Cacheable(cacheNames = CacheNames.CATEGORY_DETAIL, keyGenerator = CacheNames.KEY_GENERATOR)
    @Transactional(readOnly = true)
    public CategoryResponse findById(Short id) {
        return categoryRepository.findById(id)
                .map(CategoryService::toResponse)
                .orElseThrow(() -> new CategoryNotFoundException(id.toString()));
    }

    @Cacheable(cacheNames = CacheNames.CATEGORY_NAME, key = "#name.strip().toLowerCase()")
    @Transactional(readOnly = true)
    public CategoryResponse findByName(String name) {
        return categoryRepository.findByNameIgnoreCase(name.strip())
                .map(CategoryService::toResponse)
                .orElseThrow(() -> new CategoryNotFoundException(name));
    }

    @Cacheable(cacheNames = CacheNames.CATEGORY_LIST, keyGenerator = CacheNames.KEY_GENERATOR)
    @Transactional(readOnly = true)
    public PageableResponse<CategoryResponse> findAll(Pageable pageable) {
        return PageableResponse.from(categoryRepository.findAll(pageable).map(CategoryService::toResponse));
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.CATEGORY_LIST, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.CATEGORY_NAMES, allEntries = true)})
    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        if (categoryRepository.existsByNameIgnoreCase(request.name())) {
            throw new DuplicateCategoryException(request.name());
        }
        try {
            ProductCategory saved = categoryRepository.saveAndFlush(new ProductCategory(request.name()));
            log.info("Category created id={} name={}", saved.getId(), saved.getName());
            return toResponse(saved);
        } catch (DataIntegrityViolationException exception) {
            throw translate(exception, request.name());
        }
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.CATEGORY_DETAIL, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.CATEGORY_NAME, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.CATEGORY_LIST, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.CATEGORY_NAMES, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.PRODUCT_DETAIL, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.PRODUCT_QUERY, allEntries = true)})
    @Transactional
    public CategoryResponse update(Short id, CategoryRequest request) {
        ProductCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException(id.toString()));
        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(request.name(), id)) {
            throw new DuplicateCategoryException(request.name());
        }
        category.rename(request.name());
        try {
            ProductCategory saved = categoryRepository.saveAndFlush(category);
            log.info("Category updated id={} name={}", saved.getId(), saved.getName());
            return toResponse(saved);
        } catch (DataIntegrityViolationException exception) {
            throw translate(exception, request.name());
        }
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.CATEGORY_DETAIL, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.CATEGORY_NAME, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.CATEGORY_LIST, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.CATEGORY_NAMES, allEntries = true)})
    @Transactional
    public void delete(Short id) {
        ProductCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException(id.toString()));
        categoryRepository.delete(category);
        categoryRepository.flush();
        log.info("Category deleted id={} name={}", id, category.getName());
    }

    @Cacheable(cacheNames = CacheNames.CATEGORY_NAMES, key = "'all'")
    @Transactional(readOnly = true)
    public List<String> findAllNames() {
        return categoryRepository.findAll().stream().map(ProductCategory::getName).sorted().toList();
    }

    @Transactional(readOnly = true)
    public ProductCategory getByName(String name) {
        return categoryRepository.findByNameIgnoreCase(name.strip())
                .orElseThrow(() -> new InvalidCategoryException(name, categoryRepository.findAll().stream()
                        .map(ProductCategory::getName).sorted().toList()));
    }

    @Transactional(readOnly = true)
    public Map<String, Short> findIdsByName() {
        return categoryRepository.findAll().stream()
                .collect(Collectors.toMap(ProductCategory::getName, ProductCategory::getId));
    }

    public Optional<String> findCanonicalName(String category) {
        if (category == null || category.isBlank()) {
            return Optional.empty();
        }
        String wanted = category.strip();
        return self.findAllNames().stream().filter(name -> name.equalsIgnoreCase(wanted)).findFirst();
    }

    public String resolveCanonicalName(String category) {
        return findCanonicalName(category)
                .orElseThrow(() -> new InvalidCategoryException(category, self.findAllNames()));
    }

    private static RuntimeException translate(DataIntegrityViolationException exception, String name) {
        String message = exception.getMostSpecificCause().getMessage();
        if (message != null && message.contains(NAME_UNIQUE_CONSTRAINT)) {
            return new DuplicateCategoryException(name, exception);
        }
        return exception;
    }

    private static CategoryResponse toResponse(ProductCategory category) {
        return new CategoryResponse(category.getId(), category.getName(), category.getCreatedAt());
    }
}
