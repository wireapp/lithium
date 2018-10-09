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

/**
 */
public class MessageAssetBase extends MessageBase {
    private String assetKey;
    private String assetToken;
    private byte[] otrKey;
    private String mimeType;
    private long size;
    private byte[] sha256;
    private String name;

    public MessageAssetBase(String msgId, String convId, String clientId, String userId) {
        super(msgId, convId, clientId, userId);
    }

    MessageAssetBase(MessageAssetBase msg) {
        super(msg.messageId, msg.conversationId, msg.clientId, msg.userId);
        assetKey = msg.assetKey;
        assetToken = msg.assetToken;
        otrKey = msg.otrKey;
        mimeType = msg.mimeType;
        size = msg.size;
        sha256 = msg.sha256;
        name = msg.name;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public long getSize() {
        return size;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getAssetToken() {
        return assetToken;
    }

    public void setAssetToken(String assetToken) {
        this.assetToken = assetToken;
    }

    public void setOtrKey(byte[] otrKey) {
        this.otrKey = otrKey;
    }

    public byte[] getOtrKey() {
        return otrKey;
    }

    public String getAssetKey() {
        return assetKey;
    }

    public void setAssetKey(String assetKey) {
        this.assetKey = assetKey;
    }

    public void setSha256(byte[] sha256) {
        this.sha256 = sha256;
    }

    public byte[] getSha256() {
        return sha256;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
