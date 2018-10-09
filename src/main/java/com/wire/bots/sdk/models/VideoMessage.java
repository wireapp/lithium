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

/**
 */
public class VideoMessage extends MessageAssetBase {
    private long duration;
    private int width;
    private int height;

    public VideoMessage(String msgId, String convId, String clientId, String userId) {
        super(msgId, convId, clientId, userId);
    }

    public VideoMessage(MessageAssetBase base, Messages.Asset.VideoMetaData video) {
        super(base);
        setDuration(video.getDurationInMillis());
        setHeight(video.getHeight());
        setWidth(video.getWidth());
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public long getDuration() {
        return duration;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getHeight() {
        return height;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getWidth() {
        return width;
    }
}
