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

public class Text implements IGeneric {
    private final String text;
    private final long expires;
    private String messageId = UUID.randomUUID().toString();

    public Text(String text) {
        this.text = text;
        this.expires = 0;
    }

    public Text(String text, long expires) {
        this.text = text;
        this.expires = expires;
    }

    @Override
    public Messages.GenericMessage createGenericMsg() {
        Messages.GenericMessage.Builder ret = Messages.GenericMessage.newBuilder()
                .setMessageId(messageId);

        Messages.Text.Builder text = Messages.Text.newBuilder()
                .setContent(this.text);

        if (expires > 0) {
            Messages.Ephemeral.Builder ephemeral = Messages.Ephemeral.newBuilder()
                    .setText(text)
                    .setExpireAfterMillis(expires);

            return ret
                    .setEphemeral(ephemeral)
                    .build();
        }

        return ret
                .setText(text)
                .build();
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }
}
