package com.wire.bots.sdk.healthchecks;

import com.codahale.metrics.health.HealthCheck;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wire.bots.sdk.factories.StorageFactory;
import com.wire.bots.sdk.server.model.NewBot;
import com.wire.bots.sdk.state.State;
import com.wire.bots.sdk.tools.Logger;
import com.wire.bots.sdk.tools.Util;

public class StorageHealthCheck extends HealthCheck {
    private final StorageFactory storageFactory;

    public StorageHealthCheck(StorageFactory storageFactory) {
        this.storageFactory = storageFactory;
    }

    @Override
    protected HealthCheck.Result check() {
        try {
            Logger.debug("Starting StorageHealthCheck healthcheck");
            ObjectMapper objectMapper = new ObjectMapper();
            byte[] resource = Util.getResource("newBot.json");
            NewBot newBot = objectMapper.readValue(resource, NewBot.class);
            State state = storageFactory.create(newBot.id);
            return state.saveState(newBot)
                    ? HealthCheck.Result.healthy()
                    : HealthCheck.Result.unhealthy("Failed to save the state");
        } catch (Exception e) {
            return Result.unhealthy(e.getMessage());
        } finally {
            Logger.debug("Finished StorageHealthCheck healthcheck");
        }
    }
}
