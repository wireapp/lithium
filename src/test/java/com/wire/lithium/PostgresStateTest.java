package com.wire.lithium;

import com.wire.xenon.backend.models.Conversation;
import com.wire.xenon.backend.models.NewBot;
import com.wire.xenon.state.JdbiState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

public class PostgresStateTest extends DatabaseTestBase {

    private JdbiState storage;
    private UUID botId;

    @BeforeEach
    public void setup() {
        flyway.migrate();
        botId = UUID.randomUUID();
        storage = new JdbiState(botId, jdbi);
    }

    @AfterEach
    public void teardown() {
        flyway.clean();
    }

    @Test
    public void test() throws Exception {
        NewBot bot = new NewBot();
        bot.id = botId;
        bot.client = "client";
        bot.locale = "en";
        bot.token = "token";
        bot.conversation = new Conversation();
        bot.conversation.id = UUID.randomUUID();
        bot.conversation.name = "conv";

        boolean b = storage.saveState(bot);
        Assertions.assertTrue(b);

        NewBot state = storage.getState();
        Assertions.assertNotNull(state);
        Assertions.assertEquals(bot.id, state.id);
        Assertions.assertEquals(bot.conversation.name, state.conversation.name);

        boolean removeState = storage.removeState();
        Assertions.assertTrue(removeState);
    }
}
