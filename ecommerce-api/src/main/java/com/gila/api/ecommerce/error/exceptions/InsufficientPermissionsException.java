package com.gila.api.ecommerce.error.exceptions;

import java.util.Collection;

public class InsufficientPermissionsException extends RuntimeException {

    public InsufficientPermissionsException(Collection<String> requiredPermissions) {
        super("Insufficient permissions. Required one of: " + String.join(", ", requiredPermissions));
    }
}
