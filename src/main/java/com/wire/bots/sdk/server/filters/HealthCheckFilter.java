package com.wire.bots.sdk.server.filters;

import ch.qos.logback.access.spi.IAccessEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.dropwizard.logging.filter.FilterFactory;

@JsonTypeName("healthcheck-filter-factory")
public class HealthCheckFilter implements FilterFactory<IAccessEvent> {
    @Override
    public Filter<IAccessEvent> build() {
        return new Filter<IAccessEvent>() {
            @Override
            public FilterReply decide(IAccessEvent event) {
                if (event.getRequestURI().equals("/healthcheck")) {
                    return FilterReply.DENY;
                } else {
                    return FilterReply.NEUTRAL;
                }
            }
        };
    }
}