package com.wire.lithium.healthchecks;

import com.codahale.metrics.health.HealthCheck;
import com.wire.lithium.API;
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
            Logger.debug("Starting Outbound healthcheck");
            API api = new API(client, null);
            Response options = api.options();
            String s = options.readEntity(String.class);
            int status = options.getStatus();
            return status == 401 ? Result.healthy() : Result.unhealthy(String.format("%s. status: %d", s, status));
        } catch (Exception e) {
            final String message = String.format("Unable to reach: %s, error: %s", API.host(), e.getMessage());
            return Result.unhealthy(message);
        } finally {
            Logger.debug("Finished Outbound healthcheck");
        }
    }
}
