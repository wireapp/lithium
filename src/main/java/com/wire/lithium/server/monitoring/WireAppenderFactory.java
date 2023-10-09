package com.wire.lithium.server.monitoring;

import ch.qos.logback.access.spi.IAccessEvent;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.Layout;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.DeferredProcessingAware;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.dropwizard.logging.common.AbstractAppenderFactory;
import io.dropwizard.logging.common.async.AsyncAppenderFactory;
import io.dropwizard.logging.common.filter.LevelFilterFactory;
import io.dropwizard.logging.common.layout.LayoutFactory;
import io.dropwizard.request.logging.layout.LogbackAccessRequestLayoutFactory;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Production console appender using logging to JSON.
 */
@JsonTypeName("json-console")
public class WireAppenderFactory<T extends DeferredProcessingAware> extends AbstractAppenderFactory<T> {

    @Override
    public Appender<T> build(
            LoggerContext loggerContext,
            String serviceName,
            LayoutFactory<T> layoutFactory,
            LevelFilterFactory<T> levelFilterFactory,
            AsyncAppenderFactory<T> asyncAppenderFactory) {

        final ConsoleAppender<T> appender = new ConsoleAppender<>();
        appender.setContext(loggerContext);
        appender.setTarget("System.out");

        final Filter<T> levelFilter = levelFilterFactory.build(threshold);
        Layout<T> layout;
        // this is quite ugly hack to achieve just a single name for the logger
        if (layoutFactory instanceof LogbackAccessRequestLayoutFactory) {
            layout = prepareAccessEventLayout(levelFilter);
        } else {
            layout = prepareLoggingEventLayout(levelFilter);
        }

        appender.setLayout(layout);
        appender.start();

        return appender;
    }

    // we know that T is either ILoggingEvent or IAccessEvent
    // so this is in a fact checked cast
    // moreover thanks to the generics erasure during runtime, its safe anyway
    @SuppressWarnings("unchecked")
    private Layout<T> prepareAccessEventLayout(Filter<T> levelFilter) {
        List<Filter<IAccessEvent>> ac = getFilterFactories().stream()
                .map(filter -> (Filter<IAccessEvent>) filter.build())
                .collect(Collectors.toList());
        ac.add((Filter<IAccessEvent>) levelFilter);
        return (Layout<T>) new AccessEventJsonLayout(ac);
    }

    @SuppressWarnings("unchecked")
    private Layout<T> prepareLoggingEventLayout(Filter<T> levelFilter) {
        List<Filter<ILoggingEvent>> ac = getFilterFactories().stream()
                .map(filter -> (Filter<ILoggingEvent>) filter.build())
                .collect(Collectors.toList());
        ac.add((Filter<ILoggingEvent>) levelFilter);
        return (Layout<T>) new LoggingEventJsonLayout(ac);
    }

}
