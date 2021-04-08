package com.wire.lithium.server.monitoring;

import ch.qos.logback.access.spi.IAccessEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.dropwizard.logging.filter.FilterFactory;


@JsonTypeName("status-filter-factory")
public class StatusCheckFilter implements FilterFactory<IAccessEvent> {
    @Override
    public Filter<IAccessEvent> build() {
        return new Filter<IAccessEvent>() {
            @Override
            public FilterReply decide(IAccessEvent event) {
                if (event.getRequestURI().contains("/status")) {
                    return FilterReply.DENY;
                }
                if (event.getRequestURI().contains("/swagger")) {
                    return FilterReply.DENY;
                }
                return FilterReply.NEUTRAL;
            }
        };
    }
}

