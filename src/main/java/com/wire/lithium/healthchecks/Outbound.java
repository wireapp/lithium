package com.wire.lithium.healthchecks;

import com.codahale.metrics.health.HealthCheck;
import com.wire.lithium.API;
import com.wire.lithium.server.monitoring.MDCUtils;
import com.wire.xenon.tools.Logger;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;

public class Outbound extends HealthCheck {
    private final Client client;

    public Outbound(Client client) {
        this.client = client;
    }

    @Override
    protected Result check() {
        try {
            MDCUtils.put("healthCheck", "Outbound"); // tag the logs with health check

            Logger.debug("Starting Outbound healthcheck");
            API api = new API(client, null);
            Response response = api.status();
            String s = response.readEntity(String.class);
            int status = response.getStatus();
            return status == 200 ? Result.healthy() : Result.unhealthy(String.format("%s. status: %d", s, status));
        } catch (Exception e) {
            final String message = String.format("Unable to reach: %s, error: %s", API.host(), e.getMessage());
            Logger.exception(message, e);
            return Result.unhealthy(message);
        } finally {
            Logger.debug("Finished Outbound healthcheck");
        }
    }
}
