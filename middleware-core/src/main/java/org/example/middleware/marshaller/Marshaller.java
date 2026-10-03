package org.example.middleware.marshaller;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

public class Marshaller {

    private static final ObjectMapper mapper = new ObjectMapper();

    private Marshaller() {}

    public static String marshal(Object object) {
        try {
            return mapper.writeValueAsString(object);
        } catch (JacksonException e) {
            throw new RuntimeException("Failed while trying to marshal object to JSON", e);
        }
    }

    public static <T> T unmarshal(String payload, Class<T> clazz) {
        try {
            return mapper.readValue(payload, clazz);
        } catch (JacksonException e) {
            throw new RuntimeException("Failed while trying to unmarshal JSON to " + clazz.getSimpleName(), e);
        }
    }
}