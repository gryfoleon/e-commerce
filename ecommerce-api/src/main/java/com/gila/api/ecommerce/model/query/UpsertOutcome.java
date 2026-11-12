package com.gila.api.ecommerce.model.query;

public record UpsertOutcome(long inserted, long updated) {

    public static final UpsertOutcome EMPTY = new UpsertOutcome(0, 0);

    public UpsertOutcome plus(UpsertOutcome other) {
        return new UpsertOutcome(inserted + other.inserted, updated + other.updated);
    }
}
