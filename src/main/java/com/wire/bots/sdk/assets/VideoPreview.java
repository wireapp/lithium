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

public class VideoPreview implements IGeneric {
    private final String name;
    private final String mimeType;
    private final int h;
    private final int w;
    private final String messageId;
    private final long duration;
    private final int size;

    public VideoPreview(String name, String mimeType, long duration, int h, int w, int size, String messageId) throws Exception {
        this.name = name;
        this.mimeType = mimeType;
        this.h = h;
        this.w = w;
        this.messageId = messageId;
        this.duration = duration;
        this.size = size;
    }

    @Override
    public Messages.GenericMessage createGenericMsg() throws Exception {

        Messages.Asset.VideoMetaData.Builder audio = Messages.Asset.VideoMetaData.newBuilder()
                .setDurationInMillis(duration)
                .setHeight(h)
                .setWidth(w);

        Messages.Asset.Original.Builder original = Messages.Asset.Original.newBuilder()
                .setSize(size)
                .setName(name)
                .setMimeType(mimeType)
                .setVideo(audio.build());

        Messages.Asset asset = Messages.Asset.newBuilder()
                .setOriginal(original.build())
                .build();

        return Messages.GenericMessage.newBuilder()
                .setMessageId(messageId)
                .setAsset(asset)
                .build();
    }

    public String getName() {
        return name;
    }

    public String getMessageId() {
        return messageId;
    }

    public int getSize() {
        return size;
    }

    public String getMimeType() {
        return mimeType;
    }
}
