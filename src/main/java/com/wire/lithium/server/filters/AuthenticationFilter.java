package com.wire.lithium.server.filters;

import com.wire.xenon.Const;
import com.wire.xenon.tools.Logger;
import com.wire.xenon.tools.Util;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

@Provider
public class AuthenticationFilter implements ContainerRequestFilter {
    @Override
    public void filter(ContainerRequestContext requestContext) {
        String auth = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
        if (auth == null) {
            Exception cause = new IllegalArgumentException("Missing Authorization");
            throw new WebApplicationException(cause, Response.Status.UNAUTHORIZED);
        }

        String serviceToken = System.getProperty(Const.WIRE_BOTS_SDK_TOKEN, System.getenv("SERVICE_TOKEN"));

        if (!Util.compareAuthorizations(auth, serviceToken)) {
            Logger.warning("Wrong service token");
            Exception cause = new IllegalArgumentException("Wrong service token");
            throw new WebApplicationException(cause, Response.Status.UNAUTHORIZED);
        }

        requestContext.setProperty("wire-auth", Util.extractToken(auth));
    }
}