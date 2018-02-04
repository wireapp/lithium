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

import com.google.protobuf.ByteString;
import com.waz.model.Messages;
import com.wire.bots.sdk.tools.Util;

import java.security.MessageDigest;
import java.security.SecureRandom;

public class AudioAsset implements IGeneric, IAsset {
    static private final SecureRandom random = new SecureRandom();

    private final String messageId;
    private final byte[] encBytes;
    private final byte[] otrKey = new byte[32];
    private final String mimeType;
    private String assetKey;
    private String assetToken;

    public AudioAsset(byte[] bytes, AudioPreview preview) throws Exception {
        this.messageId = preview.getMessageId();
        this.mimeType = preview.getMimeType();

        random.nextBytes(otrKey);

        byte[] iv = new byte[16];
        random.nextBytes(iv);

        encBytes = Util.encrypt(otrKey, bytes, iv);
    }

    @Override
    public Messages.GenericMessage createGenericMsg() throws Exception {
        Messages.Asset.RemoteData.Builder remote = Messages.Asset.RemoteData.newBuilder()
                .setOtrKey(ByteString.copyFrom(otrKey))
                .setSha256(ByteString.copyFrom(MessageDigest.getInstance("SHA-256").digest(getEncryptedData())))
                .setAssetId(assetKey);

        // Only set token on private assets
        if (assetToken != null) {
            remote.setAssetToken(assetToken);
        }

        Messages.Asset asset = Messages.Asset.newBuilder()
                .setUploaded(remote.build())
                .build();

        return Messages.GenericMessage.newBuilder()
                .setMessageId(messageId)
                .setAsset(asset)
                .build();
    }

    public void setAssetKey(String assetKey) {
        this.assetKey = assetKey;
    }

    public void setAssetToken(String assetToken) {
        this.assetToken = assetToken;
    }

    @Override
    public String getMimeType() {
        return mimeType;
    }

    @Override
    public String getRetention() {
        return "volatile";
    }

    @Override
    public byte[] getEncryptedData() {
        return encBytes;
    }

    @Override
    public boolean isPublic() {
        return false;
    }

}
