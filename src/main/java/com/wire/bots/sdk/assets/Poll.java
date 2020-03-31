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


public class Poll implements IGeneric {
    private final Messages.Composite.Builder poll;
    private final UUID messageId;

    public Poll() {
        this(UUID.randomUUID());
    }

    public Poll(UUID messageId) {
        this.messageId = messageId;
        poll = Messages.Composite.newBuilder();
        poll.setExpectsReadConfirmation(true);
    }

    public Poll addText(String str) {
        Messages.Text.Builder text = Messages.Text.newBuilder()
                .setContent(str);

        Messages.Composite.Item textItem = Messages.Composite.Item.newBuilder()
                .setText(text)
                .build();

        poll.addItems(textItem);
        return this;
    }

    public Poll addText(MessageText msg) {
        Messages.Composite.Item textItem = Messages.Composite.Item.newBuilder()
                .setText(msg.getBuilder())
                .build();

        poll.addItems(textItem);
        return this;
    }

    public Poll addButton(String buttonId, String caption) {
        Messages.Button.Builder button = Messages.Button.newBuilder()
                .setText(caption)
                .setId(buttonId);

        Messages.Composite.Item.Builder buttonItem = Messages.Composite.Item.newBuilder()
                .setButton(button);

        poll.addItems(buttonItem);
        return this;
    }

    @Override
    public Messages.GenericMessage createGenericMsg() {
        return Messages.GenericMessage.newBuilder()
                .setMessageId(getMessageId().toString())
                .setComposite(poll)
                .build();
    }

    @Override
    public UUID getMessageId() {
        return messageId;
    }

}
