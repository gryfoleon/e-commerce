package com.gila.api.ecommerce.error.exceptions;

import java.util.Collection;

public final class UnsupportedQueryParameterException extends RuntimeException {

    public static final String FIELD = "queryParameter";

    private final String parameter;

    public UnsupportedQueryParameterException(String parameter, Collection<String> allowed) {
        super("Query parameter '" + parameter + "' is not supported by this endpoint. Allowed parameters: "
                + String.join(", ", allowed) + ".");
        this.parameter = parameter;
    }

    public String getParameter() {
        return parameter;
    }
}
