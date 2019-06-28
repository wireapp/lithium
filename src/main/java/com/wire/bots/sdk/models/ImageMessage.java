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

package com.wire.bots.sdk.models;

import com.waz.model.Messages;

import java.util.UUID;

public class ImageMessage extends MessageAssetBase {
    private int height;
    private int width;
    private String tag;

    public ImageMessage(UUID messageId, UUID convId, String clientId, UUID userId) {
        super(messageId, convId, clientId, userId);
    }

    public ImageMessage(MessageAssetBase base, Messages.Asset.ImageMetaData image) {
        super(base);
        setHeight(image.getHeight());
        setWidth(image.getWidth());
        setTag(image.hasTag() ? image.getTag() : null);
    }

    public int getHeight() {
        return height;
    }

    public int getWidth() {
        return width;
    }

    public String getTag() {
        return tag;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }
}
