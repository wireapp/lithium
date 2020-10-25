package com.wire.lithium;

import com.codahale.metrics.MetricRegistry;
import com.wire.xenon.backend.models.Conversation;
import com.wire.xenon.backend.models.NewBot;
import com.wire.xenon.state.JdbiState;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.db.ManagedDataSource;
import org.flywaydb.core.Flyway;
import org.junit.Test;
import org.skife.jdbi.v2.DBI;

import java.util.UUID;

public class PostgresStateTest {

    @Test
    public void test() throws Exception {
        DataSourceFactory dataSourceFactory = new DataSourceFactory();
        dataSourceFactory.setDriverClass("org.postgresql.Driver");
        dataSourceFactory.setUrl("jdbc:postgresql://localhost/lithium");

        // Migrate DB if needed
        Flyway flyway = Flyway
                .configure()
                .dataSource(dataSourceFactory.getUrl(), dataSourceFactory.getUser(), dataSourceFactory.getPassword())
                .baselineOnMigrate(true)
                .load();
        flyway.migrate();

        ManagedDataSource dataSource = dataSourceFactory.build(new MetricRegistry(), "PostgresStateTest");

        DBI jdbi = new DBI(dataSource);

        UUID botId = UUID.randomUUID();

        JdbiState storage = new JdbiState(botId, jdbi);

        NewBot bot = new NewBot();
        bot.id = botId;
        bot.client = "client";
        bot.locale = "en";
        bot.token = "token";
        bot.conversation = new Conversation();
        bot.conversation.id = UUID.randomUUID();
        bot.conversation.name = "conv";

        boolean b = storage.saveState(bot);
        assert b;

        NewBot state = storage.getState();
        assert state != null;
        assert state.id.equals(bot.id);
        assert state.conversation.name.equals(bot.conversation.name);

        boolean removeState = storage.removeState();
        assert removeState;
    }
}
