package com.wire.bots.sdk.user;

import com.wire.bots.sdk.MessageHandlerBase;
import com.wire.bots.sdk.WireClient;
import com.wire.bots.sdk.server.model.Payload;
import com.wire.bots.sdk.server.resources.MessageResourceBase;

import java.util.UUID;

public class UserMessageResource extends MessageResourceBase {
    private UserClientRepo userClientRepo;

    public UserMessageResource(MessageHandlerBase handler, UserClientRepo repo) {
        super(handler, repo);
        this.userClientRepo = repo;
    }

    void onNewMessage(UUID bot, UUID convId, Payload inbound) throws Exception {
        try (WireClient client = userClientRepo.getWireClient(bot, convId)) {
            handleMessage(inbound, client);
        }
    }

    void onUpdate(Payload payload) {
        handleUpdate(payload);
    }
}
