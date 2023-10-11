package com.wire.lithium.healthchecks;

import com.codahale.metrics.health.HealthCheck;
import com.wire.lithium.API;
import com.wire.lithium.server.monitoring.MDCUtils;
import com.wire.xenon.tools.Logger;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.core.Response;

public class Outbound extends HealthCheck {
    private final Client client;

    public Outbound(Client client) {
        this.client = client;
    }

    @Override
    protected Result check() {
        MDCUtils.put("healthCheck", "Outbound"); // tag the logs with health check
        Logger.debug("Starting Outbound healthcheck");
        API api = new API(client, null);

        try (Response response = api.status()) {
            String s = response.readEntity(String.class);
            int status = response.getStatus();
            return status == 200 ? Result.healthy() : Result.unhealthy(String.format("%s. status: %d", s, status));
        } catch (Exception e) {
            final String message = String.format("Unable to reach: %s, error: %s", api.getWireHost(), e.getMessage());
            Logger.exception(e, message);
            return Result.unhealthy(message);
        } finally {
            Logger.debug("Finished Outbound healthcheck");
        }
    }
}
