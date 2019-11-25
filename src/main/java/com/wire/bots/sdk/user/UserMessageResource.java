package com.wire.bots.sdk.user;

import com.wire.bots.cryptobox.CryptoException;
import com.wire.bots.sdk.MessageHandlerBase;
import com.wire.bots.sdk.WireClient;
import com.wire.bots.sdk.crypto.Crypto;
import com.wire.bots.sdk.factories.CryptoFactory;
import com.wire.bots.sdk.factories.StorageFactory;
import com.wire.bots.sdk.server.model.NewBot;
import com.wire.bots.sdk.server.model.Payload;
import com.wire.bots.sdk.server.resources.MessageResourceBase;
import com.wire.bots.sdk.tools.Logger;

import javax.ws.rs.client.Client;
import java.util.UUID;

public class UserMessageResource extends MessageResourceBase {
    private UUID userId;
    private StorageFactory storageFactory;
    private CryptoFactory cryptoFactory;
    private Client client;

    public UserMessageResource(MessageHandlerBase handler) {
        super(handler, null, null);
    }

    @Override
    protected boolean isValid(String auth) {
        return true;
    }

    void onNewMessage(UUID messageId, UUID convId, Payload payload) throws Exception {
        onNewMessage(messageId, userId, convId, payload);
    }

    void onNewMessage(UUID messageId, UUID from, UUID convId, Payload payload) throws Exception {
        if (from == null) {
            Logger.warning("onNewMessage: %s userId is null", payload.type);
            return;
        }
        if (convId == null) {
            Logger.warning("onNewMessage: %s convId is null", payload.type);
            return;
        }

        try {
            Crypto crypto = cryptoFactory.create(from);
            NewBot state = storageFactory.create(from).getState();
            API api = new API(client, convId, state.token);
            WireClient client = new UserClient(state, convId, crypto, api);

            handleMessage(messageId, payload, client);
        } catch (CryptoException e) {
            Logger.error("onNewMessage: msg: %s, conv: %s, %s", messageId, convId, e);
        }
    }

    void onUpdate(UUID id, Payload payload) {
        handleUpdate(id, payload);
    }

    UserMessageResource addUserId(UUID userId) {
        this.userId = userId;
        return this;
    }

    UserMessageResource addStorageFactory(StorageFactory storageFactory) {
        this.storageFactory = storageFactory;
        return this;
    }

    UserMessageResource addCryptoFactory(CryptoFactory cryptoFactory) {
        this.cryptoFactory = cryptoFactory;
        return this;
    }

    UserMessageResource addClient(Client client) {
        this.client = client;
        return this;
    }
}
