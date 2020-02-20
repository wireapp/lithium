package com.wire.bots.sdk.assets;

import com.waz.model.Messages;

import java.util.UUID;

public class DeliveryReceipt implements IGeneric {
    private final UUID firstMessageId;
    private final UUID messageId;

    public DeliveryReceipt(UUID originalMessageId) {
        this.firstMessageId = originalMessageId;
        this.messageId = UUID.randomUUID();
    }

    @Override
    public Messages.GenericMessage createGenericMsg() {
        Messages.Confirmation.Builder confirmation = Messages.Confirmation.newBuilder()
                .setFirstMessageId(firstMessageId.toString())
                .setType(Messages.Confirmation.Type.DELIVERED);

        return Messages.GenericMessage.newBuilder()
                .setMessageId(getMessageId().toString())
                .setConfirmation(confirmation)
                .build();
    }

    @Override
    public UUID getMessageId() {
        return messageId;
    }
}
