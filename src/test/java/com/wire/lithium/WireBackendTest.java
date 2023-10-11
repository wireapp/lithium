package com.wire.lithium;

import com.waz.model.Messages;
import com.wire.bots.cryptobox.CryptoException;
import com.wire.lithium.models.NewBotResponseModel;
import com.wire.xenon.MessageHandlerBase;
import com.wire.xenon.WireClient;
import com.wire.xenon.backend.models.Conversation;
import com.wire.xenon.backend.models.NewBot;
import com.wire.xenon.backend.models.Payload;
import com.wire.xenon.backend.models.User;
import com.wire.xenon.crypto.Crypto;
import com.wire.xenon.factories.CryptoFactory;
import com.wire.xenon.models.PingMessage;
import com.wire.xenon.models.otr.PreKeys;
import com.wire.xenon.models.otr.Recipients;
import com.wire.xenon.tools.Logger;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.DropwizardTestSupport;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class WireBackendTest extends DatabaseTestBase {
    private String serviceAuth;
    private String BOT_CLIENT_DUMMY;
    private String USER_CLIENT_DUMMY;
    private DropwizardTestSupport<Configuration> support;

    private WebTarget target;
    private CryptoFactory cryptoFactory;

    @BeforeEach
    public void setup() throws Exception {
        serviceAuth = UUID.randomUUID().toString();
        BOT_CLIENT_DUMMY = UUID.randomUUID().toString();
        USER_CLIENT_DUMMY = UUID.randomUUID().toString();

        String envUrl = System.getenv("POSTGRES_URL");
        var databaseUrl = "jdbc:postgresql://" + (envUrl != null ? envUrl : "localhost/lithium");
        var envUser = System.getenv("POSTGRES_USER");
        var envPassword = System.getenv("POSTGRES_PASSWORD");
        var overrides = new LinkedList<ConfigOverride>();
        overrides.push(ConfigOverride.config("token", serviceAuth));
        overrides.push(ConfigOverride.config("database.driverClass", "org.postgresql.Driver"));
        overrides.push(ConfigOverride.config("database.url", databaseUrl));

        overrides.push(ConfigOverride.config("jerseyClient.timeout", "40s"));
        overrides.push(ConfigOverride.config("jerseyClient.connectionTimeout", "40s"));
        overrides.push(ConfigOverride.config("jerseyClient.connectionRequestTimeout", "40s"));
        overrides.push(ConfigOverride.config("jerseyClient.retries", "3"));

        if (envUser != null) {
            overrides.push(ConfigOverride.config("database.user", envUser));
        }
        if (envPassword != null) {
            overrides.push(ConfigOverride.config("database.password", envPassword));
        }

        // sad java noises..
        ConfigOverride[] arrs = new ConfigOverride[overrides.size()];
        for (int i = 0; i < overrides.size(); i++) {
            arrs[i] = overrides.get(i);
        }

        flyway.migrate();

        support = new DropwizardTestSupport<>(
                TestServer.class,
                null,
                arrs
        );

        support.before();

        final TestServer server = support.getApplication();

        cryptoFactory = server.getCryptoFactory();
        target = server.getClient().target("http://localhost:" + support.getLocalPort());
    }

    @AfterEach
    public void cleanup() {
        support.after();
        flyway.clean();
    }

    @Test
    public void incomingMessageFromBackendTest() throws CryptoException, IOException {
        final UUID botId = UUID.randomUUID();
        final UUID userId = UUID.randomUUID();
        final UUID convId = UUID.randomUUID();

        // Test GET /status
        final int status = target
                .path("status")
                .request()
                .get()
                .getStatus();
        assertThat(status).isEqualTo(200);

        // Test Bot added into conv. BE calls POST /bots with NewBot object
        NewBotResponseModel newBotResponseModel = newBotFromBE(botId, userId, convId);
        assertThat(newBotResponseModel.lastPreKey).isNotNull();
        assertThat(newBotResponseModel.preKeys).isNotNull();

        final Crypto crypto = cryptoFactory.create(botId);
        PreKeys preKeys = new PreKeys(newBotResponseModel.preKeys, USER_CLIENT_DUMMY, userId);

        // Test Ping message is sent to Echo by the BE. BE calls POST /bots/{botId}/messages with Payload obj
        Recipients encrypt = crypto.encrypt(preKeys, generatePingMessage());
        String cypher = encrypt.get(userId, USER_CLIENT_DUMMY);
        Response res = newOtrMessageFromBackend(botId, userId, convId, cypher);
        assertThat(res.getStatus()).isEqualTo(200);

        crypto.close();
    }

    private NewBotResponseModel newBotFromBE(UUID botId, UUID userId, UUID convId) {
        NewBot newBot = new NewBot();
        newBot.id = botId;
        newBot.locale = "en";
        newBot.token = "token_dummy";
        newBot.client = BOT_CLIENT_DUMMY;
        newBot.origin = new User();
        newBot.origin.id = userId;
        newBot.origin.name = "user_name";
        newBot.origin.handle = "user_handle";
        newBot.conversation = new Conversation();
        newBot.conversation.id = convId;
        newBot.conversation.name = "conv_name";
        newBot.conversation.creator = userId;
        newBot.conversation.members = new ArrayList<>();

        try (Response res = target
                .path("bots")
                .request()
                .header("Authorization", "Bearer " + serviceAuth)
                .post(Entity.entity(newBot, MediaType.APPLICATION_JSON_TYPE))) {

            assertThat(res.getStatus()).isEqualTo(201);

            return res.readEntity(NewBotResponseModel.class);
        }
    }

    private Response newOtrMessageFromBackend(UUID botId, UUID userId, UUID convId, String cypher) {
        Payload payload = new Payload();
        payload.type = "conversation.otr-message-add";
        payload.from = new Payload.Qualified(userId, "");
        payload.conversation = new Payload.Qualified(convId, "");
        payload.time = new Date().toString();
        payload.data = new Payload.Data();
        payload.data.sender = USER_CLIENT_DUMMY;
        payload.data.recipient = BOT_CLIENT_DUMMY;
        payload.data.text = cypher;

        return target
                .path("bots")
                .path(botId.toString())
                .path("messages")
                .request()
                .header("Authorization", "Bearer " + serviceAuth)
                .post(Entity.entity(payload, MediaType.APPLICATION_JSON_TYPE));
    }

    private byte[] generatePingMessage() {
        return Messages.GenericMessage.newBuilder()
                .setMessageId(UUID.randomUUID().toString())
                .setKnock(Messages.Knock.newBuilder().setHotKnock(false))
                .build()
                .toByteArray();
    }

    public static class TestServer extends Server<Configuration> {
        @Override
        protected MessageHandlerBase createHandler(Configuration configuration, Environment env) {
            return new MessageHandlerBase() {
                @Override
                public void onPing(WireClient client, PingMessage msg) {
                    Logger.info("onPing: %s user: %s", client.getId(), msg.getUserId());
                }
            };
        }
    }
}
