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

public class OT implements IGeneric {
    public enum Operation {
        RETAIN,//obsolete
        INSERT,
        DELETE,
    }

    private final Operation operation;
    private final String text;
    private final int offset;
    private final Integer length;

    public OT(Operation operation, int offset, Integer length) {
        this.offset = offset;
        this.operation = operation;
        this.length = length;
        this.text = null;
    }

    public OT(Operation operation, int offset, String text) {
        this.text = text;
        this.offset = offset;
        this.operation = operation;
        this.length = null;
    }

    @Override
    public Messages.GenericMessage createGenericMsg() throws Exception {
        Messages.GenericMessage.Builder ret = Messages.GenericMessage.newBuilder()
                .setMessageId(UUID.randomUUID().toString());

        Messages.OT.Builder ot = Messages.OT.newBuilder()
                .setOffset(offset)
                .setType(Messages.OT.Type.valueOf(operation.ordinal()));

        if (text != null)
            ot.setText(text);

        if (length != null)
            ot.setLength(length);

        return ret
                .setOt(ot)
                .build();
    }
}
