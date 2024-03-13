package com.wire.lithium.server.monitoring;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import org.slf4j.MDC;


public class MDCUtils {

    /**
     * Put value to MDC under given key.
     *
     * @param key   MDC key
     * @param value value to the key
     */
    public static void put(@NotNull final String key, @Nullable Object value) {
        if (value != null) {
            final String stringValue = value.toString().trim();
            if (!stringValue.isEmpty()) {
                MDC.put(key, stringValue);
            }
        }
    }

    /**
     * Remove key from the MDC.
     *
     * @param key key to be removed
     */
    public static void removeKey(@NotNull final String key) {
        MDC.remove(key);
    }
}
