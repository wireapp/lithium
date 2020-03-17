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

public class MessageText implements IGeneric {
    private final Messages.Text.Builder builder = Messages.Text.newBuilder();

    private UUID messageId = UUID.randomUUID();

    public MessageText(String text) {
        builder.setExpectsReadConfirmation(true);
        setText(text);
    }

    public MessageText setMessageId(UUID messageId) {
        this.messageId = messageId;
        return this;
    }

    public MessageText setText(String text) {
        builder.setContent(text);
        return this;
    }

    public MessageText addMention(UUID mentionUser, int offset, int len) {
        Messages.Mention.Builder mention = Messages.Mention.newBuilder()
                .setUserId(mentionUser.toString())
                .setLength(len)
                .setStart(offset);

        builder.addMentions(mention);
        return this;
    }

    @Override
    public Messages.GenericMessage createGenericMsg() {
        return Messages.GenericMessage.newBuilder()
                .setMessageId(getMessageId().toString())
                .setText(builder)
                .build();
    }

    @Override
    public UUID getMessageId() {
        return messageId;
    }

    public Messages.Text.Builder getBuilder() {
        return builder;
    }
}
