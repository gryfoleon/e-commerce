package com.gila.api.ecommerce.service.product.impl;

import com.gila.api.ecommerce.configuration.cache.CacheNames;
import com.gila.api.ecommerce.error.exceptions.DuplicateSkuException;
import com.gila.api.ecommerce.error.exceptions.ImmutableSkuException;
import com.gila.api.ecommerce.error.exceptions.ProductNotFoundException;
import com.gila.api.ecommerce.mapper.ProductMapper;
import com.gila.api.ecommerce.model.dto.product.ProductCreateRequest;
import com.gila.api.ecommerce.model.dto.product.ProductResponse;
import com.gila.api.ecommerce.model.dto.product.ProductUpdateRequest;
import com.gila.api.ecommerce.model.entity.Product;
import com.gila.api.ecommerce.model.entity.ProductCategory;
import com.gila.api.ecommerce.repository.product.ProductRepository;
import com.gila.api.ecommerce.service.category.CategoryService;
import com.gila.api.ecommerce.service.product.ProductService;
import java.time.Clock;
import java.util.Optional;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class ProductServiceImpl implements ProductService {

    private static final String SKU_UNIQUE_CONSTRAINT = "uq_products_sku";

    private final ProductRepository productRepository;
    private final CategoryService categoryService;
    private final ProductMapper productMapper;
    private final Clock clock;

    public ProductServiceImpl(
            ProductRepository productRepository,
            CategoryService categoryService,
            ProductMapper productMapper,
            Clock clock) {
        this.productRepository = productRepository;
        this.categoryService = categoryService;
        this.productMapper = productMapper;
        this.clock = clock;
    }

    @Override
    @Cacheable(cacheNames = CacheNames.PRODUCT_DETAIL, keyGenerator = CacheNames.KEY_GENERATOR)
    @Transactional(readOnly = true)
    public ProductResponse findById(UUID id) {
        return productRepository.findByIdAndActiveTrue(id)
                .map(productMapper::toResponse)
                .orElseThrow(() -> new ProductNotFoundException(id.toString()));
    }

    @Override
    @CacheEvict(cacheNames = CacheNames.PRODUCT_QUERY, allEntries = true)
    @Transactional
    public ProductResponse create(ProductCreateRequest request) {
        ProductCategory category = categoryService.getByName(request.category());
        if (productRepository.existsBySku(request.sku())) {
            throw new DuplicateSkuException(request.sku());
        }
        try {
            Product saved = productRepository.saveAndFlush(productMapper.toNewEntity(request, category));
            log.info("Product created id={} sku={}", saved.getId(), saved.getSku());
            return productMapper.toResponse(saved);
        } catch (DataIntegrityViolationException exception) {
            if (isSkuConflict(exception)) {
                throw new DuplicateSkuException(request.sku(), exception);
            }
            throw exception;
        }
    }

    @Override
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.PRODUCT_DETAIL, key = "#result.id().toString()"),
            @CacheEvict(cacheNames = CacheNames.PRODUCT_QUERY, allEntries = true)})
    @Transactional
    public ProductResponse update(String idOrSku, ProductUpdateRequest request, Optional<Long> expectedVersion) {
        Product product = findActive(idOrSku);
        request.optionalSku()
                .filter(sku -> !sku.equals(product.getSku()))
                .ifPresent(sku -> {
                    throw new ImmutableSkuException(product.getSku(), sku);
                });
        expectedVersion
                .filter(version -> !version.equals(product.getVersion()))
                .ifPresent(version -> {
                    throw new ObjectOptimisticLockingFailureException(Product.class, product.getId());
                });
        ProductCategory category = categoryService.getByName(request.category());
        product.replaceDetails(request.name(), request.description(), category, request.price(), request.stock(),
                request.weightKg());
        Product saved = productRepository.saveAndFlush(product);
        log.info("Product updated id={} version={}", saved.getId(), saved.getVersion());
        return productMapper.toResponse(saved);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.PRODUCT_DETAIL, key = "#result.toString()"),
            @CacheEvict(cacheNames = CacheNames.PRODUCT_QUERY, allEntries = true)})
    @Transactional
    public UUID delete(String idOrSku) {
        Product product = findActive(idOrSku);
        product.softDelete(clock.instant());
        productRepository.saveAndFlush(product);
        log.info("Product soft-deleted id={} sku={}", product.getId(), product.getSku());
        return product.getId();
    }

    private Product findActive(String idOrSku) {
        String identifier = idOrSku.strip();
        return parseUuid(identifier)
                .flatMap(productRepository::findByIdAndActiveTrue)
                .or(() -> productRepository.findBySkuAndActiveTrue(identifier))
                .orElseThrow(() -> new ProductNotFoundException(identifier));
    }

    private static Optional<UUID> parseUuid(String value) {
        try {
            return Optional.of(UUID.fromString(value));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private static boolean isSkuConflict(DataIntegrityViolationException exception) {
        String message = exception.getMostSpecificCause().getMessage();
        return message != null && message.contains(SKU_UNIQUE_CONSTRAINT);
    }
}
