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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.UUID;

public class FileAsset implements IGeneric, IAsset {
    static private final SecureRandom random = new SecureRandom();

    private final String mimeType;
    private final UUID messageId;
    private final byte[] encBytes;
    private final byte[] otrKey;
    private final byte[] sha256;

    private String assetKey;
    private String assetToken;

    public FileAsset(File file, String mimeType, UUID messageId) throws Exception {
        this(readFile(file), mimeType, messageId);
    }

    public FileAsset(byte[] bytes, String mimeType, UUID messageId) throws Exception {
        this.mimeType = mimeType;
        this.messageId = messageId;

        otrKey = newOtrKey();
        encBytes = encrypt(bytes);
        sha256 = getSha256(encBytes);
    }

    public FileAsset(String assetKey, String assetToken, byte[] sha256, byte[] otrKey, UUID messageId) {
        this.messageId = messageId;
        this.assetKey = assetKey;
        this.assetToken = assetToken;
        this.sha256 = sha256;
        this.otrKey = otrKey;
        mimeType = null;
        encBytes = null;
    }

    @Override
    public Messages.GenericMessage createGenericMsg() {
        // Remote
        Messages.Asset.RemoteData.Builder remote = Messages.Asset.RemoteData.newBuilder()
                .setOtrKey(ByteString.copyFrom(otrKey))
                .setSha256(ByteString.copyFrom(sha256))
                .setAssetId(assetKey)
                .setAssetToken(assetToken);

        Messages.Asset.Builder asset = Messages.Asset.newBuilder()
                .setUploaded(remote);

        return Messages.GenericMessage.newBuilder()
                .setMessageId(getMessageId().toString())
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
        return "expiring";
    }

    @Override
    public byte[] getEncryptedData() {
        return encBytes;
    }

    @Override
    public boolean isPublic() {
        return false;
    }

    @Override
    public UUID getMessageId() {
        return messageId;
    }

    private static byte[] getSha256(byte[] bytes) throws NoSuchAlgorithmException {
        return MessageDigest.getInstance("SHA-256").digest(bytes);
    }

    private static byte[] newOtrKey() {
        byte[] otrKey = new byte[32];
        random.nextBytes(otrKey);
        return otrKey;
    }

    private static byte[] readFile(File file) throws IOException {
        byte[] bytes;
        try (FileInputStream input = new FileInputStream(file)) {
            bytes = Util.toByteArray(input);
        }
        return bytes;
    }

    private byte[] encrypt(byte[] bytes) throws Exception {
        byte[] iv = new byte[16];
        random.nextBytes(iv);
        return Util.encrypt(otrKey, bytes, iv);
    }
}
