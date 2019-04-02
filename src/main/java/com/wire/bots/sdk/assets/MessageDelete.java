package com.wire.bots.sdk.assets;

import com.waz.model.Messages;

import java.util.UUID;

public class MessageDelete implements IGeneric {
    private final UUID delMessageId;
    private final UUID messageId = UUID.randomUUID();

    public MessageDelete(UUID delMessageId) {
        this.delMessageId = delMessageId;
    }

    @Override
    public Messages.GenericMessage createGenericMsg() {
        Messages.MessageDelete.Builder del = Messages.MessageDelete.newBuilder()
                .setMessageId(delMessageId.toString());

        return Messages.GenericMessage.newBuilder()
                .setMessageId(getMessageId().toString())
                .setDeleted(del)
                .build();
    }

    @Override
    public UUID getMessageId() {
        return messageId;
    }
}
