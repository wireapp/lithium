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

import java.util.ArrayList;
import java.util.UUID;


public class Poll implements IGeneric {
    private final UUID messageId = UUID.randomUUID();
    private final String body;
    private final ArrayList<String> buttons;

    public Poll(String body, ArrayList<String> buttons) {
        this.body = body;
        this.buttons = buttons;
    }

    @Override
    public Messages.GenericMessage createGenericMsg() {
        Messages.Poll.Builder poll = Messages.Poll.newBuilder()
                .setBody(body)
                .addAllButtons(buttons);

        return Messages.GenericMessage.newBuilder()
                .setMessageId(getMessageId().toString())
                .setPoll(poll)
                .build();
    }

    @Override
    public UUID getMessageId() {
        return messageId;
    }
}
