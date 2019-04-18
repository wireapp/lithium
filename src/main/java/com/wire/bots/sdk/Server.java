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
import com.codahale.metrics.jmx.JmxReporter;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.wire.bots.sdk.crypto.CryptoDatabase;
import com.wire.bots.sdk.crypto.CryptoFile;
import com.wire.bots.sdk.crypto.storage.PgStorage;
import com.wire.bots.sdk.crypto.storage.RedisStorage;
import com.wire.bots.sdk.factories.CryptoFactory;
import com.wire.bots.sdk.factories.StorageFactory;
import com.wire.bots.sdk.healthchecks.Alice2Bob;
import com.wire.bots.sdk.healthchecks.CryptoHealthCheck;
import com.wire.bots.sdk.healthchecks.Outbound;
import com.wire.bots.sdk.healthchecks.StorageHealthCheck;
import com.wire.bots.sdk.server.resources.BotsResource;
import com.wire.bots.sdk.server.resources.MessageResource;
import com.wire.bots.sdk.server.resources.StatusResource;
import com.wire.bots.sdk.server.tasks.AvailablePrekeysTask;
import com.wire.bots.sdk.server.tasks.ConversationTask;
import com.wire.bots.sdk.state.FileState;
import com.wire.bots.sdk.state.PostgresState;
import com.wire.bots.sdk.state.RedisState;
import com.wire.bots.sdk.tools.AuthValidator;
import com.wire.bots.sdk.tools.Logger;
import com.wire.bots.sdk.tools.Util;
import com.wire.bots.sdk.user.Endpoint;
import com.wire.bots.sdk.user.UserClientRepo;
import com.wire.bots.sdk.user.UserMessageResource;
import com.wire.bots.sdk.user.model.User;
import io.dropwizard.Application;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.client.JerseyClientConfiguration;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.servlets.tasks.Task;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.federecio.dropwizard.swagger.SwaggerBundle;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;
import org.glassfish.jersey.media.multipart.MultiPartFeature;

import javax.ws.rs.client.Client;
import java.net.URI;
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
    protected Client client;
    protected MessageHandlerBase messageHandler;

    /**
     * This method is called once by the sdk in order to create the main message handler
     *
     * @param config Configuration object (yaml)
     * @param env    Environment object
     * @return Instance of your class that implements {@see @MessageHandlerBase}
     */
    protected abstract MessageHandlerBase createHandler(Config config, Environment env) throws Exception;

    /**
     * Override this method to put your custom initialization
     * NOTE: ClientRepo is not yet set at this stage. messageHandler is also not set
     *
     * @param config Configuration object (yaml)
     * @param env    Environment object
     */
    protected void initialize(Config config, Environment env) throws Exception {

    }

    /**
     * Override this method in case you need to add custom Resource and/or Task
     * {@link #addResource(Object, io.dropwizard.setup.Environment)}
     * and {@link #addTask(io.dropwizard.servlets.tasks.Task, io.dropwizard.setup.Environment)}
     *
     * @param config Configuration object (yaml)
     * @param env    Environment object
     */
    protected void onRun(Config config, Environment env) throws Exception {

    }

    @Override
    public void initialize(Bootstrap<Config> bootstrap) {
        bootstrap.setConfigurationSourceProvider(
                new SubstitutingSourceProvider(bootstrap.getConfigurationSourceProvider(),
                        new EnvironmentVariableSubstitutor(false)
                )
        );

        bootstrap.addBundle(new SwaggerBundle<Config>() {
            @Override
            protected SwaggerBundleConfiguration getSwaggerBundleConfiguration(Config configuration) {
                return configuration.getSwagger();
            }
        });
    }

    @Override
    public void run(final Config config, Environment env) throws Exception {
        this.config = config;
        this.environment = env;

        JerseyClientConfiguration jerseyCfg = config.getJerseyClientConfiguration();
        jerseyCfg.setChunkedEncodingEnabled(false);
        jerseyCfg.setGzipEnabled(false);
        jerseyCfg.setGzipEnabledForRequests(false);

        client = new JerseyClientBuilder(environment)
                .using(jerseyCfg)
                .withProvider(MultiPartFeature.class)
                .withProvider(JacksonJsonProvider.class)
                .build(getName());

        initialize(config, env);

        messageHandler = createHandler(config, env);

        if (config.userMode) {
            runInUserMode(messageHandler);
        }

        repo = runInBotMode(env, messageHandler);

        initTelemetry(env);

        onRun(config, env);
    }

    public StorageFactory getStorageFactory() {
        if (config.db.driver == null) {
            return botId -> new RedisState(botId, config.db);
        }
        if (config.db.driver.equals("fs")) {
            return botId -> new FileState(botId, config.db);
        }
        if (config.db.driver.equals("postgresql")) {
            return botId -> new PostgresState(botId, config.db);
        }
        return botId -> new RedisState(botId, config.db);
    }

    public CryptoFactory getCryptoFactory() {
        if (config.db.driver == null) {
            return redisCryptoFactory();
        }
        if (config.db.driver.equals("fs")) {
            return fileCryptoFactory();
        }
        if (config.db.driver.equals("postgresql")) {
            return postgresCryptoFactory();
        }
        return redisCryptoFactory();
    }

    private CryptoFactory fileCryptoFactory() {
        return (botId) -> new CryptoFile(botId, config.db);
    }

    private CryptoFactory postgresCryptoFactory() {
        return (botId) -> {
            PgStorage storage = new PgStorage(config.db.user,
                    config.db.password,
                    config.db.database,
                    config.db.host,
                    config.db.port);
            return new CryptoDatabase(botId, storage);
        };
    }

    private CryptoFactory redisCryptoFactory() {
        return (botId) -> {
            RedisStorage storage = new RedisStorage(config.db.host,
                    config.db.port,
                    config.db.password);
            return new CryptoDatabase(botId, storage);
        };
    }

    private ClientRepo runInBotMode(Environment env, MessageHandlerBase handler) {
        StorageFactory storageFactory = getStorageFactory();
        CryptoFactory cryptoFactory = getCryptoFactory();

        ClientRepo repo = new ClientRepo(client, cryptoFactory, storageFactory);

        addResource(new StatusResource(), env);

        botResource(config, env, handler);
        messageResource(config, env, handler, repo);

        addTask(new ConversationTask(repo), env);
        addTask(new AvailablePrekeysTask(repo), env);

        return repo;
    }

    private void runInUserMode(MessageHandlerBase handler) throws Exception {
        Logger.info("Starting in User Mode");

        String email = Configuration.propOrEnv("email", true);
        String password = Configuration.propOrEnv("password", true);

        StorageFactory storageFactory = getStorageFactory();
        CryptoFactory cryptoFactory = getCryptoFactory();

        UserClientRepo clientRepo = new UserClientRepo(client, cryptoFactory, storageFactory);

        Endpoint ep = new Endpoint(client, cryptoFactory, storageFactory);
        User user = ep.signIn(email, password, true);
        Logger.info("Logged in as: %s userId: %s:%s token: %s",
                email,
                user.getUserId(),
                user.getClientId(),
                user.getToken());

        UserMessageResource userMessageResource = new UserMessageResource(user.getUserId(), handler, clientRepo);
        String wss = Util.getWss(user.getToken(), user.getClientId());

        ep.connectWebSocket(userMessageResource, new URI(wss));
    }

    protected void messageResource(Config config, Environment env, MessageHandlerBase handler, ClientRepo repo) {
        AuthValidator validator = new AuthValidator(config.getAuth());
        addResource(new MessageResource(handler, validator, repo), env);
    }

    protected void botResource(Config config, Environment env, MessageHandlerBase handler) {
        StorageFactory storageFactory = getStorageFactory();
        CryptoFactory cryptoFactory = getCryptoFactory();
        AuthValidator authValidator = new AuthValidator(config.getAuth());

        addResource(new BotsResource(handler, storageFactory, cryptoFactory, authValidator), env);
    }

    protected void addTask(Task task, Environment env) {
        env.admin().addTask(task);
    }

    protected void addResource(Object component, Environment env) {
        env.jersey().register(component);
    }

    private void initTelemetry(Environment env) {
        final CryptoFactory cryptoFactory = getCryptoFactory();
        final StorageFactory storageFactory = getStorageFactory();

        env.healthChecks().register("Storage", new StorageHealthCheck(storageFactory));
        env.healthChecks().register("Crypto", new CryptoHealthCheck(cryptoFactory));
        env.healthChecks().register("Alice2Bob", new Alice2Bob(cryptoFactory));
        env.healthChecks().register("Outbound", new Outbound(client));

        env.metrics().register("logger.errors", (Gauge<Integer>) Logger::getErrorCount);
        env.metrics().register("logger.warnings", (Gauge<Integer>) Logger::getWarningCount);

        JmxReporter jmxReporter = JmxReporter.forRegistry(env.metrics())
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build();
        jmxReporter.start();
    }

    public ClientRepo getRepo() {
        return repo;
    }

    public Config getConfig() {
        return config;
    }

    public Environment getEnvironment() {
        return environment;
    }

    public Client getClient() {
        return client;
    }
}
