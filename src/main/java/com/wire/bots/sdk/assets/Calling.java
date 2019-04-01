package com.wire.bots.sdk.assets;

import com.waz.model.Messages;

import java.util.UUID;

public class Calling implements IGeneric {
    private final String content;
    private final UUID messageId = UUID.randomUUID();

    public Calling(String content) {
        this.content = content;
    }

    @Override
    public Messages.GenericMessage createGenericMsg() {
        Messages.GenericMessage.Builder ret = Messages.GenericMessage.newBuilder()
                .setMessageId(getMessageId().toString());

        Messages.Calling.Builder calling = Messages.Calling.newBuilder()
                .setContent(content);

        return ret
                .setCalling(calling)
                .build();
    }

    @Override
    public UUID getMessageId() {
        return messageId;
    }
}
