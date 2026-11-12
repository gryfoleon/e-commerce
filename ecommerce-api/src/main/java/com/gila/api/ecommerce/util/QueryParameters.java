package com.gila.api.ecommerce.util;

import com.gila.api.ecommerce.error.exceptions.UnsupportedQueryParameterException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Set;
import java.util.TreeSet;

public final class QueryParameters {

    public static final Set<String> PAGING = Set.of("page", "size", "sortBy", "sortDirection");

    private QueryParameters() {
    }

    public static void requireOnly(HttpServletRequest request, Set<String> allowed) {
        Set<String> all = new TreeSet<>(allowed);
        all.addAll(PAGING);
        request.getParameterMap().keySet().stream()
                .sorted()
                .filter(name -> !all.contains(name))
                .findFirst()
                .ifPresent(name -> {
                    throw new UnsupportedQueryParameterException(name, all);
                });
    }
}
