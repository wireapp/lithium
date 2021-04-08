package com.wire.lithium.server.monitoring;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.filter.Filter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Layout used on Wire production services in the ELK stack.
 */
public class LoggingEventJsonLayout extends AbstractJsonLayout<ILoggingEvent> {

    public LoggingEventJsonLayout(List<Filter<ILoggingEvent>> filters) {
        super(filters);
    }

    @Override
    public String doLayout(ILoggingEvent event) {
        if (shouldIgnoreEvent(event)) {
            return null;
        }

        final Map<String, Object> jsonMap = new LinkedHashMap<>(6);

        jsonMap.put("@timestamp", formatTime(event));
        jsonMap.put("type", "log");
        jsonMap.put("message", event.getFormattedMessage());

        jsonMap.put("logger", event.getLoggerName());
        jsonMap.put("level", event.getLevel().levelStr);
        jsonMap.put("threadName", event.getThreadName());

        if (event.getThrowableProxy() != null) {
            jsonMap.put("exception", exception(event.getThrowableProxy()));
        }

        return finalizeLog(jsonMap, event.getFormattedMessage());
    }

    private Map<String, String> exception(IThrowableProxy proxy) {
        final Map<String, String> jsonMap = new LinkedHashMap<>(3);
        jsonMap.put("stacktrace", ThrowableProxyUtil.asString(proxy));
        jsonMap.put("message", proxy.getMessage());
        jsonMap.put("class", proxy.getClassName());
        return jsonMap;
    }
}
