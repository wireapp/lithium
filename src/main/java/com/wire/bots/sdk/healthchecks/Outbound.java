package com.wire.bots.sdk.healthchecks;

import com.codahale.metrics.health.HealthCheck;
import com.wire.bots.sdk.API;
import com.wire.bots.sdk.tools.Logger;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;

public class Outbound extends HealthCheck {
    private final Client client;

    public Outbound(Client client) {
        this.client = client;
    }

    @Override
    protected Result check() {
        API api = new API(client, null);
        Response options = api.options();
        String s = options.readEntity(String.class);
        int status = options.getStatus();
        Logger.info("Outbound healthcheck. %s Status: %d", s, status);
        return status == 401 ? Result.healthy() : Result.unhealthy(String.format("%s. status: %d", s, status));
    }
}
