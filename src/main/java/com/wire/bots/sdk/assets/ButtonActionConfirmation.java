package com.wire.bots.sdk.assets;

import com.waz.model.Messages;

import java.util.UUID;

public class ButtonActionConfirmation implements IGeneric {
    private final UUID messageId = UUID.randomUUID();
    private final UUID refMsgId;
    private final String buttonId;

    public ButtonActionConfirmation(UUID refMsgId, String buttonId) {
        this.refMsgId = refMsgId;
        this.buttonId = buttonId;
    }

    @Override
    public Messages.GenericMessage createGenericMsg() {
        final Messages.ButtonActionConfirmation.Builder confirmation = Messages.ButtonActionConfirmation.newBuilder()
                .setButtonId(buttonId)
                .setReferenceMessageId(refMsgId.toString());

        return Messages.GenericMessage.newBuilder()
                .setMessageId(getMessageId().toString())
                .setButtonActionConfirmation(confirmation)
                .build();
    }

    @Override
    public UUID getMessageId() {
        return messageId;
    }
}
