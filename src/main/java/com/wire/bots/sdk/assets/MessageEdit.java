package com.wire.bots.sdk.assets;

import com.waz.model.Messages;

import java.util.UUID;

public class MessageEdit implements IGeneric {
    private final UUID replacingMessageId;
    private final String text;
    private final UUID messageId = UUID.randomUUID();

    public MessageEdit(UUID replacingMessageId, String text) {
        this.replacingMessageId = replacingMessageId;
        this.text = text;
    }

    @Override
    public Messages.GenericMessage createGenericMsg() {
        Messages.Text.Builder text = Messages.Text.newBuilder()
                .setContent(this.text);

        Messages.MessageEdit.Builder messageEdit = Messages.MessageEdit.newBuilder()
                .setReplacingMessageId(replacingMessageId.toString())
                .setText(text);

        return Messages.GenericMessage.newBuilder()
                .setMessageId(getMessageId().toString())
                .setEdited(messageEdit)
                .build();
    }

    @Override
    public UUID getMessageId() {
        return messageId;
    }
}
