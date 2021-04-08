package com.wire.lithium.healthchecks;

import com.codahale.metrics.health.HealthCheck;
import com.wire.lithium.server.monitoring.MDCUtils;
import com.wire.xenon.backend.models.NewBot;
import com.wire.xenon.factories.StorageFactory;
import com.wire.xenon.state.State;
import com.wire.xenon.tools.Logger;

import java.util.UUID;

public class StorageHealthCheck extends HealthCheck {
    private final StorageFactory storageFactory;

    public StorageHealthCheck(StorageFactory storageFactory) {
        this.storageFactory = storageFactory;
    }

    @Override
    protected HealthCheck.Result check() {
        try {
            MDCUtils.put("healthCheck", "StorageHealthCheck"); // tag the logs with health check

            Logger.debug("Starting StorageHealthCheck healthcheck");
            NewBot newBot = new NewBot();
            newBot.id = UUID.randomUUID();
            State state = storageFactory.create(newBot.id);
            return state.saveState(newBot)
                    ? HealthCheck.Result.healthy()
                    : HealthCheck.Result.unhealthy("Failed to save the state");
        } catch (Exception e) {
            Logger.exception("Exception during StorageHealthCheck.", e);
            return Result.unhealthy(e.getMessage());
        } finally {
            Logger.debug("Finished StorageHealthCheck healthcheck");
        }
    }
}
