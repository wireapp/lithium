package com.wire.bots.sdk.assets;

import com.waz.model.Messages;

import java.util.UUID;

public class Calling implements IGeneric {
    private final String content;

    public Calling(String content) {
        this.content = content;
    }

    @Override
    public Messages.GenericMessage createGenericMsg() throws Exception {
        String messageId = UUID.randomUUID().toString();

        Messages.GenericMessage.Builder ret = Messages.GenericMessage.newBuilder()
                .setMessageId(messageId);

        Messages.Calling.Builder calling = Messages.Calling.newBuilder()
                .setContent(content);

        return ret
                .setCalling(calling)
                .build();
    }
}
