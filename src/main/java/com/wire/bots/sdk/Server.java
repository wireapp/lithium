//
// Wire
// Copyright (C) 2016 Wire Swiss GmbH
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program. If not, see http://www.gnu.org/licenses/.
//

package com.wire.bots.sdk;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.health.HealthCheck;
import com.wire.bots.sdk.crypto.Crypto;
import com.wire.bots.sdk.crypto.CryptoFile;
import com.wire.bots.sdk.factories.CryptoFactory;
import com.wire.bots.sdk.factories.StorageFactory;
import com.wire.bots.sdk.factories.WireClientFactory;
import com.wire.bots.sdk.server.resources.BotsResource;
import com.wire.bots.sdk.server.resources.MessageResource;
import com.wire.bots.sdk.server.resources.StatusResource;
import com.wire.bots.sdk.server.tasks.AvailablePrekeysTask;
import com.wire.bots.sdk.server.tasks.BroadcastAllTask;
import com.wire.bots.sdk.server.tasks.ConversationTask;
import com.wire.bots.sdk.storage.FileStorage;
import com.wire.bots.sdk.storage.Storage;
import com.wire.bots.sdk.tools.AuthValidator;
import com.wire.bots.sdk.tools.Logger;
import com.wire.bots.sdk.tools.Util;
import com.wire.bots.sdk.user.Endpoint;
import com.wire.bots.sdk.user.UserClient;
import io.dropwizard.Application;
import io.dropwizard.servlets.tasks.Task;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Entry point for your Application
 *
 * @param <Config>
 */
public abstract class Server<Config extends Configuration> extends Application<Config> {
    protected ClientRepo repo;
    protected Config config;
    protected Environment environment;

    /**
     * This method is called once by the sdk in order to create the main message handler
     *
     * @param config Configuration object (yaml)
     * @param env    Environment object
     * @return Instance of your class that implements {@see @MessageHandlerBase}
     */
    protected abstract MessageHandlerBase createHandler(Config config, Environment env);

    /**
     * Override this method in case you need to add custom Resource and/or Task {@link #addResource(Object, io.dropwizard.setup.Environment)}
     * and {@link #addTask(io.dropwizard.servlets.tasks.Task, io.dropwizard.setup.Environment)}
     *
     * @param config Configuration object (yaml)
     * @param env    Environment object
     */
    protected void onRun(Config config, Environment env) {
    }

    @Override
    public void initialize(Bootstrap<Config> configBootstrap) {
    }

    @Override
    public void run(final Config config, Environment env) throws Exception {
        this.config = config;
        this.environment = env;

        initTelemetry(config, env);

        if (!runInUserMode(config, env)) {
            runInBotMode(config, env);
        }

        onRun(config, env);
    }

    protected WireClientFactory getWireClientFactory(Config config) {
        return botId -> {
            Crypto crypto = getCryptoFactory(config).create(botId);
            Storage storage = getStorageFactory(config).create(botId);
            return new BotClient(crypto, storage);
        };
    }

    protected StorageFactory getStorageFactory(Config config) {
        return botId -> new FileStorage(config.cryptoDir, botId);
    }

    protected CryptoFactory getCryptoFactory(Config config) {
        return (botId) -> new CryptoFile(config.cryptoDir, botId);
    }

    private void runInBotMode(Config config, Environment env) {
        WireClientFactory wireClientFactory = getWireClientFactory(config);
        StorageFactory storageFactory = getStorageFactory(config);

        repo = new ClientRepo(wireClientFactory, storageFactory);

        MessageHandlerBase handler = createHandler(config, env);

        addResource(new StatusResource(), env);

        botResource(config, env, handler);
        messageResource(config, env, handler);

        addTask(new BroadcastAllTask(config, repo), env);
        addTask(new ConversationTask(repo), env);
        addTask(new AvailablePrekeysTask(repo), env);
    }

    private boolean runInUserMode(Config config, Environment env) throws Exception {
        String email = System.getProperty("email");
        String password = System.getProperty("password");

        if (email != null && password != null) {
            StorageFactory storageFactory = getStorageFactory(config);
            WireClientFactory userClientFactory = (botId) -> {
                Crypto crypto = getCryptoFactory(config).create(botId);
                Storage storage = storageFactory.create(botId);
                return new UserClient(crypto, storage);
            };

            repo = new ClientRepo(userClientFactory, storageFactory);

            Endpoint ep = new Endpoint(config);
            String userId = ep.signIn(email, password, true);
            Logger.info(String.format("Logged in as User: %s userId: %s", email, userId));

            AuthValidator validator = new AuthValidator(config.getAuth());
            MessageHandlerBase handler = createHandler(config, env);
            ep.connectWebSocket(new MessageResource(handler, validator, repo));
            return true;
        }
        return false;
    }

    protected void messageResource(Config config, Environment env, MessageHandlerBase handler) {
        AuthValidator validator = new AuthValidator(config.getAuth());
        addResource(new MessageResource(handler, validator, repo), env);
    }

    protected void botResource(Config config, Environment env, MessageHandlerBase handler) {
        StorageFactory storageFactory = getStorageFactory(config);
        CryptoFactory cryptoFactory = getCryptoFactory(config);
        AuthValidator authValidator = new AuthValidator(config.getAuth());

        addResource(new BotsResource(handler, storageFactory, cryptoFactory, authValidator), env);
    }

    protected void addTask(Task task, Environment env) {
        env.admin().addTask(task);
    }

    protected void addResource(Object component, Environment env) {
        env.jersey().register(component);
    }

    private void initTelemetry(final Config conf, Environment env) {
        env.healthChecks().register("ok", new HealthCheck() {
            @Override
            protected Result check() throws Exception {
                return Result.healthy();
            }
        });

        env.healthChecks().register("data volume", new HealthCheck() {
            @Override
            protected Result check() throws Exception {
                Storage storage = getStorageFactory(conf).create("test");
                if (!storage.status()) {
                    Logger.error("Failed storage test: %s", conf.getCryptoDir());
                    return Result.unhealthy(conf.getCryptoDir());
                }
                return Result.healthy();
            }
        });

        //todo: implement crypto.status()
        env.healthChecks().register("Crypto", new HealthCheck() {
            @Override
            protected Result check() throws Exception {
                try (Crypto crypto = getCryptoFactory(conf).create("test")) {
                    return Result.healthy();
                }
            }
        });

        env.healthChecks().register("JCEPolicy", new HealthCheck() {
            @Override
            protected Result check() throws Exception {
                byte[] otrKey = new byte[32];
                byte[] iv = new byte[16];
                byte[] data = new byte[1024];

                Random random = new Random();
                random.nextBytes(otrKey);
                random.nextBytes(iv);
                random.nextBytes(data);
                Util.encrypt(otrKey, data, iv);
                return Result.healthy();
            }
        });

        env.metrics().register("logger.errors", (Gauge<Integer>) Logger::getErrorCount);

        JmxReporter jmxReporter = JmxReporter.forRegistry(env.metrics())
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build();
        jmxReporter.start();
    }
}
