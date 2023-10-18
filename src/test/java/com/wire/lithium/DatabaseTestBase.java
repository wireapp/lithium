package com.wire.lithium;

import com.codahale.metrics.MetricRegistry;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.db.ManagedDataSource;
import org.flywaydb.core.Flyway;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

abstract public class DatabaseTestBase {
    protected static Flyway flyway;
    protected static Jdbi jdbi;

    @BeforeAll
    public static void initiate() {
        DataSourceFactory dataSourceFactory = new DataSourceFactory();
        dataSourceFactory.setDriverClass("org.postgresql.Driver");

        String envUrl = System.getenv("POSTGRES_URL");
        dataSourceFactory.setUrl("jdbc:postgresql://" + (envUrl != null ? envUrl : "localhost/lithium"));
        String envUser = System.getenv("POSTGRES_USER");
        if (envUser != null) dataSourceFactory.setUser(envUser);
        String envPassword = System.getenv("POSTGRES_PASSWORD");
        if (envPassword != null) dataSourceFactory.setPassword(envPassword);

        // Migrate DB if needed
        flyway = Flyway
                .configure()
                .cleanDisabled(false)
                .dataSource(dataSourceFactory.getUrl(), dataSourceFactory.getUser(), dataSourceFactory.getPassword())
                .baselineOnMigrate(true)
                .load();

        ManagedDataSource dataSource = dataSourceFactory.build(new MetricRegistry(), "CryptoPostgresTest");

        jdbi = Jdbi.create(dataSource).installPlugin(new SqlObjectPlugin());
    }

    @AfterAll
    public static void classCleanup() {
        flyway.clean();
    }
}
