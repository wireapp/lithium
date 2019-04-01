package com.wire.bots.sdk.user;

import com.wire.bots.cryptobox.CryptoException;
import com.wire.bots.sdk.MessageHandlerBase;
import com.wire.bots.sdk.WireClient;
import com.wire.bots.sdk.server.model.Payload;
import com.wire.bots.sdk.server.resources.MessageResourceBase;
import com.wire.bots.sdk.tools.Logger;

import java.util.UUID;

public class UserMessageResource extends MessageResourceBase {
    private final UUID owner;
    private UserClientRepo userClientRepo;

    public UserMessageResource(UUID owner, MessageHandlerBase handler, UserClientRepo repo) {
        super(handler, repo);
        this.owner = owner;
        this.userClientRepo = repo;
    }

    void onNewMessage(UUID userId, UUID convId, Payload payload) throws Exception {
        if (userId == null) {
            Logger.warning("onNewMessage: %s userId is null", payload.type);
            return;
        }
        if (convId == null) {
            Logger.warning("onNewMessage: %s convId is null", payload.type);
            return;
        }

        try (WireClient client = userClientRepo.getWireClient(userId, convId)) {
            handleMessage(payload, client);
        } catch (CryptoException e) {
            Logger.error("onNewMessage:(%s:%s) from: %s:%s %s %s",
                    owner,
                    payload.data.recipient,
                    userId,
                    payload.data.sender,
                    e.code,
                    payload.type);
            //respondWithError(userId, convId);
        }
    }

    void onUpdate(Payload payload) {
        handleUpdate(payload);
    }

    private void respondWithError(UUID userId, UUID convId) {
        try (WireClient client = userClientRepo.getWireClient(userId, convId)) {
            client.sendReaction(UUID.randomUUID().toString(), "");
        } catch (Exception e) {
            Logger.error("MessageResource::respondWithError: user: %s, conv: %s, %s", userId, convId, e);
        }
    }
}
