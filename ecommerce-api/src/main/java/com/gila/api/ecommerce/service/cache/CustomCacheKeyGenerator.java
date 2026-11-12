package com.gila.api.ecommerce.service.cache;

import com.gila.api.ecommerce.model.query.ProductFilterCriteria;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component("customCacheKeyGenerator")
public class CustomCacheKeyGenerator implements KeyGenerator {

    private static final String ABSENT = "-";
    private static final String SEPARATOR = "|";

    @Override
    public Object generate(Object target, Method method, Object... params) {
        return Arrays.stream(params).map(CustomCacheKeyGenerator::format).collect(Collectors.joining(SEPARATOR));
    }

    private static String format(Object param) {
        return switch (param) {
            case null -> ABSENT;
            case UUID id -> id.toString();
            case ProductFilterCriteria criteria -> format(criteria);
            case Pageable pageable -> format(pageable);
            default -> param.toString();
        };
    }

    private static String format(ProductFilterCriteria c) {
        return String.join(SEPARATOR,
                "name=" + text(normalizeName(c.name())),
                "sku=" + text(c.sku()),
                "cat=" + text(c.category() == null ? null : c.category().strip().toLowerCase(Locale.ROOT)),
                "minP=" + number(c.minPrice()),
                "maxP=" + number(c.maxPrice()),
                "minS=" + (c.minStock() == null ? ABSENT : c.minStock().toString()),
                "minW=" + number(c.minWeightKg()),
                "maxW=" + number(c.maxWeightKg()));
    }

    private static String format(Pageable pageable) {
        if (pageable.isUnpaged()) {
            return "unpaged";
        }
        String sort = pageable.getSort().stream()
                .map(order -> order.getProperty() + ":" + order.getDirection())
                .collect(Collectors.joining(","));
        return "p=" + pageable.getPageNumber() + SEPARATOR + "s=" + pageable.getPageSize() + SEPARATOR
                + "sort=" + (sort.isEmpty() ? ABSENT : sort);
    }

    private static String normalizeName(String name) {
        return name == null ? null : name.strip().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private static String text(String value) {
        return value == null ? ABSENT : value;
    }

    private static String number(BigDecimal value) {
        return value == null ? ABSENT : value.stripTrailingZeros().toPlainString();
    }
}
