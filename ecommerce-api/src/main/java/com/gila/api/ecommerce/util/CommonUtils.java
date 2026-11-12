package com.gila.api.ecommerce.util;

import com.gila.api.ecommerce.model.dto.CustomPageRequest;
import java.util.Objects;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class CommonUtils {

    private CommonUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static Pageable buildPageable(
            CustomPageRequest customPageRequest, String sortProperty, Sort.Direction defaultDirection,
            String tieBreaker) {
        Sort.Direction direction = Objects.requireNonNullElse(customPageRequest.sortDirection(), defaultDirection);
        return PageRequest.of(customPageRequest.page(), customPageRequest.size(),
                Sort.by(direction, sortProperty, tieBreaker));
    }
}
