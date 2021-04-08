package com.wire.lithium.server.monitoring;

import org.slf4j.MDC;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

public class MDCUtils {

    /**
     * Put value to MDC under given key.
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
     */
    public static void removeKey(@NotNull final String key) {
        MDC.remove(key);
    }
}
