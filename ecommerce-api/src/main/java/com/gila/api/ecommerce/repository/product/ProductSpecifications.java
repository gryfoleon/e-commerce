package com.gila.api.ecommerce.repository.product;

import com.gila.api.ecommerce.model.entity.Product;
import com.gila.api.ecommerce.model.query.ProductFilterCriteria;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

public final class ProductSpecifications {

    private static final char LIKE_ESCAPE = '\\';

    private ProductSpecifications() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static Specification<Product> from(ProductFilterCriteria criteria) {
        List<Specification<Product>> parts = new ArrayList<>();
        parts.add(isActive());
        if (criteria.name() != null) {
            parts.add(nameMatches(criteria.name()));
        }
        if (criteria.sku() != null) {
            parts.add(skuContains(criteria.sku()));
        }
        if (criteria.category() != null) {
            parts.add(categoryEquals(criteria.category()));
        }
        if (criteria.minPrice() != null) {
            parts.add(priceAtLeast(criteria.minPrice()));
        }
        if (criteria.maxPrice() != null) {
            parts.add(priceAtMost(criteria.maxPrice()));
        }
        if (criteria.minStock() != null) {
            parts.add(stockAtLeast(criteria.minStock()));
        }
        if (criteria.minWeightKg() != null) {
            parts.add(weightAtLeast(criteria.minWeightKg()));
        }
        if (criteria.maxWeightKg() != null) {
            parts.add(weightAtMost(criteria.maxWeightKg()));
        }
        return Specification.allOf(parts);
    }

    static Specification<Product> nameMatches(String terms) {
        List<String> patterns = Arrays.stream(terms.strip().split("\\s+"))
                .filter(term -> !term.isEmpty())
                .map(term -> "%" + escapeLike(term) + "%")
                .toList();
        return (root, query, cb) -> cb.or(patterns.stream()
                .map(pattern -> cb.like(cb.lower(root.<String>get("name")), pattern.toLowerCase(Locale.ROOT),
                        LIKE_ESCAPE))
                .toArray(Predicate[]::new));
    }

    private static String escapeLike(String term) {
        return term.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    static Specification<Product> isActive() {
        return (root, query, cb) -> cb.isTrue(root.get("active"));
    }

    static Specification<Product> skuContains(String fragment) {
        return (root, query, cb) -> cb.like(root.get("sku"), "%" + fragment + "%");
    }

    static Specification<Product> categoryEquals(String canonicalCategory) {
        return (root, query, cb) -> cb.equal(root.get("category").get("name"), canonicalCategory);
    }

    static Specification<Product> priceAtLeast(BigDecimal min) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.<BigDecimal>get("price"), min);
    }

    static Specification<Product> priceAtMost(BigDecimal max) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.<BigDecimal>get("price"), max);
    }

    static Specification<Product> stockAtLeast(int min) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.<Integer>get("stock"), min);
    }

    static Specification<Product> weightAtLeast(BigDecimal min) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.<BigDecimal>get("weightKg"), min);
    }

    static Specification<Product> weightAtMost(BigDecimal max) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.<BigDecimal>get("weightKg"), max);
    }
}
