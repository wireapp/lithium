package com.wire.bots.sdk.user;

import com.wire.bots.sdk.MessageHandlerBase;
import com.wire.bots.sdk.WireClient;
import com.wire.bots.sdk.server.model.InboundMessage;
import com.wire.bots.sdk.server.resources.MessageResourceBase;

public class UserMessageResource extends MessageResourceBase {
    private UserClientRepo repo;

    public UserMessageResource(MessageHandlerBase handler, UserClientRepo repo) {
        super(handler, repo);
        this.repo = repo;
    }

    void onNewMessage(String bot, String convId, InboundMessage inbound) throws Exception {
        WireClient client = repo.getWireClient(bot, convId);
        if (client != null) {
            handleMessage(inbound, client);
        }
    }
}
