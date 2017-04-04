package com.wire.bots.sdk.assets;

import com.waz.model.Messages;

import java.util.UUID;

public class Delete implements IGeneric {
    private final String delMessageId;

    public Delete(String delMessageId) {
        this.delMessageId = delMessageId;
    }

    @Override
    public Messages.GenericMessage createGenericMsg() throws Exception {
        Messages.MessageDelete.Builder del = Messages.MessageDelete.newBuilder()
                .setMessageId(delMessageId);

        return Messages.GenericMessage.newBuilder()
                .setMessageId(UUID.randomUUID().toString())
                .setDeleted(del)
                .build();
    }
}
