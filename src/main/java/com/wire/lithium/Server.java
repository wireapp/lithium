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

package com.wire.lithium;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.jmx.JmxReporter;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.wire.lithium.healthchecks.Alice2Bob;
import com.wire.lithium.healthchecks.CryptoHealthCheck;
import com.wire.lithium.healthchecks.Outbound;
import com.wire.lithium.healthchecks.StorageHealthCheck;
import com.wire.lithium.server.filters.AuthenticationFeature;
import com.wire.lithium.server.monitoring.RequestMdcFactoryFilter;
import com.wire.lithium.server.monitoring.StatusResource;
import com.wire.lithium.server.monitoring.VersionResource;
import com.wire.lithium.server.resources.BotsResource;
import com.wire.lithium.server.resources.MessageResource;
import com.wire.lithium.server.tasks.AvailablePrekeysTask;
import com.wire.lithium.server.tasks.ConversationTask;
import com.wire.xenon.Const;
import com.wire.xenon.MessageHandlerBase;
import com.wire.xenon.crypto.CryptoDatabase;
import com.wire.xenon.crypto.CryptoFile;
import com.wire.xenon.crypto.storage.JdbiStorage;
import com.wire.xenon.factories.CryptoFactory;
import com.wire.xenon.factories.StorageFactory;
import com.wire.xenon.state.FileState;
import com.wire.xenon.state.JdbiState;
import com.wire.xenon.tools.Logger;
import io.dropwizard.Application;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.servlets.tasks.Task;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.federecio.dropwizard.swagger.SwaggerBundle;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;
import org.flywaydb.core.Flyway;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;

import javax.annotation.Nullable;
import javax.ws.rs.client.Client;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;

/**
 * Entry point for your Application
 *
 * @param <Config> Dropwizard configuration
 */
public abstract class Server<Config extends Configuration> extends Application<Config> {
    protected ClientRepo repo;
    protected Config config;
    protected Environment environment;
    protected Client client;
    protected MessageHandlerBase messageHandler;
    protected Jdbi jdbi;

    /**
     * This method is called once by the sdk in order to create the main message handler
     *
     * @param config Configuration object (yaml)
     * @param env    Environment object
     * @return Instance of your class that implements {@link MessageHandlerBase}
     * @throws Exception allowed to throw exception
     */
    protected abstract MessageHandlerBase createHandler(Config config, Environment env) throws Exception;

    /**
     * Override this method to put your custom initialization
     * NOTE: MessageHandler is not yet set when this method is invoked!
     *
     * @param config Configuration object (yaml)
     * @param env    Environment object
     * @throws Exception allowed to throw exception
     */
    @SuppressWarnings("RedundantThrows") // this method can be overridden
    protected void initialize(Config config, Environment env) throws Exception {

    }

    /**
     * Override this method in case you need to add custom Resource and/or Task
     * {@link #addResource(Object)}
     * and {@link #addTask(io.dropwizard.servlets.tasks.Task)}
     *
     * @param config Configuration object (yaml)
     * @param env    Environment object
     * @throws Exception allowed to throw exception
     */
    @SuppressWarnings("RedundantThrows") // this method can be overridden
    protected void onRun(Config config, Environment env) throws Exception {

    }

    @Override
    public void initialize(Bootstrap<Config> bootstrap) {
        bootstrap.setConfigurationSourceProvider(new SubstitutingSourceProvider(
                bootstrap.getConfigurationSourceProvider(), new EnvironmentVariableSubstitutor(false)));
        bootstrap.addBundle(new SwaggerBundle<>() {
            @Override
            protected SwaggerBundleConfiguration getSwaggerBundleConfiguration(Config configuration) {
                return configuration.swagger;
            }
        });
    }

    @Override
    public void run(final Config config, Environment env) throws Exception {
        this.config = config;
        this.environment = env;

        System.setProperty(Const.WIRE_BOTS_SDK_TOKEN, config.token);
        System.setProperty(Const.WIRE_BOTS_SDK_API, config.apiHost);

        setupDatabase(config.database);

        jdbi = buildJdbi(config.database, env);

        client = createHttpClient(config, env);

        repo = createClientRepo();

        initialize(config, env);

        messageHandler = createHandler(config, env);

        addResources();

        initTelemetry();

        if (config.healthchecks) {
            runHealthChecks();
        }

        onRun(config, env);
    }

    private Client createHttpClient(Config config, Environment env) {
        return new JerseyClientBuilder(env)
                .using(config.getJerseyClient())
                .withProvider(MultiPartFeature.class)
                .withProvider(JacksonJsonProvider.class)
                .build(getName());
    }

    protected ClientRepo createClientRepo() {
        StorageFactory storageFactory = getStorageFactory();
        CryptoFactory cryptoFactory = getCryptoFactory();
        return new ClientRepo(getClient(), cryptoFactory, storageFactory);
    }

    @Nullable
    protected Jdbi buildJdbi(Configuration.Database database, Environment env) {
        if (database.getDriverClass().equalsIgnoreCase("fs"))
            return null;

        return Jdbi
                .create(database.build(env.metrics(), getName()))
                .installPlugin(new SqlObjectPlugin());
    }

    protected void setupDatabase(Configuration.Database database) {
        if (!database.getDriverClass().equalsIgnoreCase("fs")) {
            Flyway flyway = Flyway
                    .configure()
                    .dataSource(database.getUrl(), database.getUser(), database.getPassword())
                    .baselineOnMigrate(database.baseline)
                    .load();
            flyway.migrate();
        }
    }

    public StorageFactory getStorageFactory() {
        if (config.database.getDriverClass().equalsIgnoreCase("fs")) {
            return botId -> new FileState(config.database.getUrl(), botId);
        }

        return botId -> new JdbiState(botId, getJdbi());
    }

    public CryptoFactory getCryptoFactory() {
        if (config.database.getDriverClass().equalsIgnoreCase("fs")) {
            return (botId) -> new CryptoFile(config.database.getUrl(), botId);
        }

        return (botId) -> new CryptoDatabase(botId, new JdbiStorage(getJdbi()));
    }

    private void addResources() {
        /* --- Wire Common --- */
        addResource(new VersionResource()); // add version endpoint
        addResource(new StatusResource()); // empty status for k8s
        addResource(new RequestMdcFactoryFilter()); // MDC data
        /* //- Wire Common --- */

        botResource();
        messageResource();

        addTask(new ConversationTask(getRepo()));
        addTask(new AvailablePrekeysTask(getRepo()));
    }

    protected void messageResource() {
        addResource(new MessageResource(messageHandler, getRepo()));
    }

    protected void botResource() {
        StorageFactory storageFactory = getStorageFactory();
        CryptoFactory cryptoFactory = getCryptoFactory();

        addResource(new BotsResource(messageHandler, storageFactory, cryptoFactory));
    }

    protected void addTask(Task task) {
        environment.admin().addTask(task);
    }

    protected void addResource(Object component) {
        environment.jersey().register(component);
    }

    private void initTelemetry() {
        /* --- Wire Common --- */
        environment.jersey().register(new RequestMdcFactoryFilter());
        /* //- Wire Common --- */

        final CryptoFactory cryptoFactory = getCryptoFactory();
        final StorageFactory storageFactory = getStorageFactory();

        registerFeatures();

        environment.healthChecks().register("Storage", new StorageHealthCheck(storageFactory));
        environment.healthChecks().register("Crypto", new CryptoHealthCheck(cryptoFactory));
        environment.healthChecks().register("Alice2Bob", new Alice2Bob(cryptoFactory));
        environment.healthChecks().register("Outbound", new Outbound(getClient()));

        environment.metrics().register("logger.errors", (Gauge<Integer>) Logger::getErrorCount);
        environment.metrics().register("logger.warnings", (Gauge<Integer>) Logger::getWarningCount);

        JmxReporter jmxReporter = JmxReporter.forRegistry(environment.metrics())
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build();
        jmxReporter.start();
    }

    private void runHealthChecks() {
        Logger.info("Running health checks...");
        final SortedMap<String, HealthCheck.Result> results = environment.healthChecks().runHealthChecks();
        for (String name : results.keySet()) {
            final HealthCheck.Result result = results.get(name);
            if (!result.isHealthy()) {
                Logger.error("%s failed with: %s", name, result.getMessage());
                throw new RuntimeException(result.getError());
            }
        }
    }

    protected void registerFeatures() {
        this.environment.jersey().register(AuthenticationFeature.class);
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

    public Jdbi getJdbi() {
        return jdbi;
    }
}
