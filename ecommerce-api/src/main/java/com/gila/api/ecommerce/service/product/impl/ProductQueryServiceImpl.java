package com.gila.api.ecommerce.service.product.impl;

import com.gila.api.ecommerce.configuration.cache.CacheNames;
import com.gila.api.ecommerce.mapper.ProductMapper;
import com.gila.api.ecommerce.model.dto.PageableResponse;
import com.gila.api.ecommerce.model.dto.product.ProductSearchDTO;
import com.gila.api.ecommerce.model.query.ProductFilterCriteria;
import com.gila.api.ecommerce.repository.product.ProductRepository;
import com.gila.api.ecommerce.repository.product.ProductSpecifications;
import com.gila.api.ecommerce.service.product.ProductQueryService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductQueryServiceImpl implements ProductQueryService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    public ProductQueryServiceImpl(ProductRepository productRepository, ProductMapper productMapper) {
        this.productRepository = productRepository;
        this.productMapper = productMapper;
    }

    @Override
    @Cacheable(cacheNames = CacheNames.PRODUCT_QUERY, keyGenerator = CacheNames.KEY_GENERATOR)
    @Transactional(readOnly = true)
    public PageableResponse<ProductSearchDTO> findPage(ProductFilterCriteria criteria, Pageable pageable) {
        return PageableResponse.from(productRepository.findAll(ProductSpecifications.from(criteria), pageable)
                .map(productMapper::toSearchDto));
    }
}
