package com.wire.lithium.server.monitoring;

import ch.qos.logback.access.spi.IAccessEvent;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.LayoutBase;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.DeferredProcessingAware;
import ch.qos.logback.core.spi.FilterReply;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.logging.MDC;

import javax.annotation.Nullable;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Layout base that can convert log map to JSON.
 */
abstract public class AbstractJsonLayout<T extends DeferredProcessingAware> extends LayoutBase<T> {
    protected static final DateTimeFormatter dateTimeFormatter =
            DateTimeFormatter.ISO_DATE_TIME.withZone(ZoneOffset.UTC);

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final List<Filter<T>> filters;

    protected AbstractJsonLayout(List<Filter<T>> filters) {
        this.filters = filters;
    }

    protected boolean shouldIgnoreEvent(T event) {
        for (Filter<T> filter : filters) {
            if (filter.decide(event) == FilterReply.DENY) {
                return true;
            }
        }
        return false;
    }

    protected String formatTimeStamp(final long timestamp) {
        return dateTimeFormatter.format(Instant.ofEpochMilli(timestamp));
    }

    protected String formatTime(final ILoggingEvent event) {
        return formatTimeStamp(event.getTimeStamp());
    }

    protected String formatTime(final IAccessEvent event) {
        return formatTimeStamp(event.getTimeStamp());
    }

    protected String finalizeLog(final Map<String, Object> jsonMap) {
        return finalizeLog(jsonMap, null);
    }

    /**
     * Puts MDC to log and uses Object Mapper to create final log string.
     * <p>
     * Catches [JsonProcessingException] during processing, returns formatted string anyway.
     */
    protected String finalizeLog(Map<String, Object> jsonMap, @Nullable final String logMessage) {
        // put all MDC values to the final map
        MDC.getMap().forEach(jsonMap::put);

        try {
            final String json = objectMapper.writeValueAsString(jsonMap);
            return json + CoreConstants.LINE_SEPARATOR;
        } catch (JsonProcessingException e) {
            final String message = logMessage != null ? logMessage : "http request";
            // as we are serializing just maps then this should not happen...
            e.printStackTrace();
            return String.format(
                    "It was not possible to log %s! Exception message %s, Exception %s %s",
                    message,
                    e.getMessage(),
                    e,
                    CoreConstants.LINE_SEPARATOR);
        }
    }
}
