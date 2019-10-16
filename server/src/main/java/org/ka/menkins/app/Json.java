package org.ka.menkins.app;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

public class Json {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static <T> T from(byte[] content, Class<T> clazz) {
        try {
            return MAPPER.readValue(content, clazz);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] toBytes(Object object) {
        try {
            return MAPPER.writeValueAsBytes(object);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
