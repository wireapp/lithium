//
// Wire
// Copyright (C) 2016 Wire Swiss GmbH
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program. If not, see http://www.gnu.org/licenses/.
//

package com.wire.bots.sdk.assets;

import com.waz.model.Messages;

import java.util.UUID;

public class MessageEphemeral implements IGeneric {
    private final Messages.Ephemeral.Builder builder = Messages.Ephemeral.newBuilder();

    private UUID messageId = UUID.randomUUID();

    public MessageEphemeral(long mills) {
        builder.setExpireAfterMillis(mills);
    }

    public MessageEphemeral setText(String text) {
        final Messages.Text.Builder textBuilder = Messages.Text.newBuilder()
                .setContent(text);
        builder.setText(textBuilder);
        return this;
    }

    @Override
    public Messages.GenericMessage createGenericMsg() {
        return Messages.GenericMessage.newBuilder()
                .setMessageId(getMessageId().toString())
                .setEphemeral(builder)
                .build();
    }

    @Override
    public UUID getMessageId() {
        return messageId;
    }

    public MessageEphemeral setMessageId(UUID messageId) {
        this.messageId = messageId;
        return this;
    }

}
