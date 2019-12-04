package com.wire.bots.sdk.server.filters;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.util.Objects;

@Provider
public class AuthenticationFilter implements ContainerRequestFilter {
    @Override
    public void filter(ContainerRequestContext requestContext) {
        String auth = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);

        String[] split = auth.split(" ");

        if (split.length != 2) {
            Exception cause = new IllegalArgumentException("Bad Authorization");
            throw new WebApplicationException(cause, Response.Status.BAD_REQUEST);
        }

        String type = split[0];
        String token = split[1];

        if (!Objects.equals(type, "Bearer")) {
            Exception cause = new IllegalArgumentException("Wrong wrong token type");
            throw new WebApplicationException(cause, Response.Status.BAD_REQUEST);
        }

        String serviceToken = System.getenv("SERVICE_TOKEN");

        if (!Objects.equals(token, serviceToken)) {
            Exception cause = new IllegalArgumentException("Wrong service token");
            throw new WebApplicationException(cause, Response.Status.FORBIDDEN);
        }

        requestContext.setProperty("wire-auth", token);
    }
}