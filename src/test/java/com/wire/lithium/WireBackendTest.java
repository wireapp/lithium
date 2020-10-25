package com.wire.lithium;

import com.waz.model.Messages;
import com.wire.bots.cryptobox.CryptoException;
import com.wire.xenon.MessageHandlerBase;
import com.wire.xenon.WireClient;
import com.wire.xenon.backend.models.*;
import com.wire.xenon.crypto.Crypto;
import com.wire.xenon.factories.CryptoFactory;
import com.wire.xenon.models.PingMessage;
import com.wire.xenon.models.otr.PreKeys;
import com.wire.xenon.models.otr.Recipients;
import com.wire.xenon.tools.Logger;
import io.dropwizard.setup.Environment;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.DropwizardTestSupport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class WireBackendTest {
    private static final String serviceAuth = "secret";
    private static final String BOT_CLIENT_DUMMY = "bot_client_dummy";
    private static final String USER_CLIENT_DUMMY = "user_client_dummy";
    private static final DropwizardTestSupport<Configuration> SUPPORT = new DropwizardTestSupport<>(
            _Server.class,
            null,
            ConfigOverride.config("token", serviceAuth),
            ConfigOverride.config("database.driverClass", "fs"),
            ConfigOverride.config("database.url", "data"));
    private WebTarget target;
    private CryptoFactory cryptoFactory;

    @Before
    public void beforeClass() throws Exception {
        SUPPORT.before();

        final _Server server = SUPPORT.getApplication();

        cryptoFactory = server.getCryptoFactory();

        target = server.getClient().target("http://localhost:" + SUPPORT.getLocalPort());
    }

    @After
    public void afterClass() {

        SUPPORT.after();
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
        Response res = newOtrMessageFromBackend(botId, userId, cypher);
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

        Response res = target
                .path("bots")
                .request()
                .header("Authorization", "Bearer " + serviceAuth)
                .post(Entity.entity(newBot, MediaType.APPLICATION_JSON_TYPE));

        assertThat(res.getStatus()).isEqualTo(201);

        return res.readEntity(NewBotResponseModel.class);
    }

    private Response newOtrMessageFromBackend(UUID botId, UUID userId, String cypher) {
        Payload payload = new Payload();
        payload.type = "conversation.otr-message-add";
        payload.from = userId;
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

    public static class _Server extends Server<Configuration> {
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