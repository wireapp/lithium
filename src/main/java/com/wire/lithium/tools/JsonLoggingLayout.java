package com.wire.lithium.tools;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.LayoutBase;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Layout used on Wire production services in the ELK stack.
 */
public class JsonLoggingLayout extends LayoutBase<ILoggingEvent> {

    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_DATE_TIME.withZone(ZoneOffset.UTC);

    @Override
    public String doLayout(ILoggingEvent event) {
        final Map<String, Object> jsonMap = new LinkedHashMap<>(7);

        jsonMap.put("@timestamp", formatTime(event));
        jsonMap.put("message", event.getFormattedMessage());
        jsonMap.put("logger", event.getLoggerName());
        jsonMap.put("level", event.getLevel().levelStr);
        jsonMap.put("thread_name", event.getThreadName());

        // log mdc properties
        final Map<String, String> mdc = event.getMDCPropertyMap();
        // taken from Nginx's X-Request-Id
        if (mdc.containsKey("infra_request")) {
            jsonMap.put("infra_request", mdc.get("infra_request"));
        }
        // unique id for request in the application
        if (mdc.containsKey("app_request")) {
            jsonMap.put("app_request", mdc.get("app_request"));
        }

        if (event.getThrowableProxy() != null) {
            jsonMap.put("exception", exception(event.getThrowableProxy()));
        }

        try {
            final String json = new ObjectMapper().writeValueAsString(jsonMap);
            return json + CoreConstants.LINE_SEPARATOR;
        } catch (JsonProcessingException e) {
            // as we are serializing just maps then this should not happen...
            e.printStackTrace();
            return String.format(
                    "It was not possible to log! Log message: %s, Exception message %s, Exception %s %s",
                    event.getFormattedMessage(),
                    e.getMessage(),
                    e.toString(),
                    CoreConstants.LINE_SEPARATOR);
        }
    }

    private Map<String, String> exception(IThrowableProxy proxy) {
        final Map<String, String> jsonMap = new LinkedHashMap<>(3);
        jsonMap.put("stacktrace", ThrowableProxyUtil.asString(proxy));
        jsonMap.put("message", proxy.getMessage());
        jsonMap.put("class", proxy.getClassName());
        return jsonMap;
    }

    private String formatTime(ILoggingEvent event) {
        return dateTimeFormatter.format(Instant.ofEpochMilli(event.getTimeStamp()));
    }
}
