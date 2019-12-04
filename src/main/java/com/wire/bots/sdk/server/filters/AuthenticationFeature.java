package com.wire.bots.sdk.server.filters;

import io.swagger.annotations.Authorization;

import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.ext.Provider;

@Provider
public class AuthenticationFeature implements DynamicFeature {
    @Override
    public void configure(ResourceInfo resourceInfo, FeatureContext context) {
        if (resourceInfo.getResourceMethod().getAnnotation(Authorization.class) != null) {
            context.register(AuthenticationFilter.class);
        }
    }
}
