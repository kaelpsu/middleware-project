package org.example.middleware.identification;

import java.util.UUID;

public record ObjectID(String value) {

    public ObjectID {
        if (value == null || value.isBlank()) {
            value = UUID.randomUUID().toString();
        }
    }

    public static ObjectID generate() {
        return new ObjectID(UUID.randomUUID().toString());
    }

    @Override
    public String toString() {
        return value;
    }
}
