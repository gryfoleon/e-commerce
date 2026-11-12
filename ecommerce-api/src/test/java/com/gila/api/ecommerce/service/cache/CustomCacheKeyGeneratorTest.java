package com.gila.api.ecommerce.service.cache;

import static org.assertj.core.api.Assertions.assertThat;

import com.gila.api.ecommerce.model.query.ProductFilterCriteria;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

class CustomCacheKeyGeneratorTest {

    private final CustomCacheKeyGenerator generator = new CustomCacheKeyGenerator();
    private final Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt", "id"));

    private Object key(String name) {
        return generator.generate(this, null, criteria(name), pageable);
    }

    private static ProductFilterCriteria criteria(String name) {
        return new ProductFilterCriteria(name, null, null, null, null, null, null, null);
    }

    @Test
    void differentNamesProduceDifferentKeys() {
        assertThat(key("mouse")).isNotEqualTo(key("keyboard")).isNotEqualTo(key(null));
    }

    @Test
    void whitespaceIsNormalized() {
        assertThat(key("  wireless   mouse ")).isEqualTo(key("wireless mouse"));
    }

    @Test
    void caseIsIgnoredBecauseTheSearchIsCaseInsensitive() {
        assertThat(key("Camping Tent")).isEqualTo(key("camping  tent"));
    }
}
