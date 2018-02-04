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
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.UUID;

public class FileAsset implements IGeneric, IAsset {

    static private final SecureRandom random = new SecureRandom();

    private final String id;
    private final String name;
    private final String mimeType;
    private final int size;
    private final byte[] encBytes;
    private final byte[] key = new byte[32];
    private String assetKey = null;
    private String assetToken;

    public FileAsset(FileAssetPreview preview) throws Exception {
        this.id = preview.getId();
        this.name = preview.getName();
        this.mimeType = preview.getMimeType();
        random.nextBytes(key);

        byte[] iv = new byte[16];
        random.nextBytes(iv);

        try (FileInputStream input = new FileInputStream(preview.getFile())) {
            byte[] bytes = Util.toByteArray(input);
            size = bytes.length;
            encBytes = Util.encrypt(key, bytes, iv);
        }
    }

    public FileAsset(File file, String mimeType) throws Exception {
        this.id = UUID.randomUUID().toString();
        this.name = file.getName();
        this.mimeType = mimeType;
        random.nextBytes(key);

        byte[] iv = new byte[16];
        random.nextBytes(iv);

        try (FileInputStream input = new FileInputStream(file)) {
            byte[] bytes = Util.toByteArray(input);
            size = bytes.length;
            encBytes = Util.encrypt(key, bytes, iv);
        }
    }

    @Override
    public Messages.GenericMessage createGenericMsg() throws Exception {

        // Original
        Messages.Asset.Original.Builder original = Messages.Asset.Original.newBuilder()
                .setSize(size)
                .setName(name)
                .setMimeType(mimeType);

        // Remote
        Messages.Asset.RemoteData.Builder remote = Messages.Asset.RemoteData.newBuilder()
                .setOtrKey(ByteString.copyFrom(key))
                .setSha256(ByteString.copyFrom(MessageDigest.getInstance("SHA-256").digest(getEncryptedData())))
                .setAssetId(assetKey);

        // Only set token on private assets
        if (assetToken != null) {
            remote.setAssetToken(assetToken);
        }

        Messages.Asset.Builder asset = Messages.Asset.newBuilder()
                .setOriginal(original)
                .setUploaded(remote);

        return Messages.GenericMessage.newBuilder()
                .setMessageId(id)
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
