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
import com.wire.bots.sdk.Util;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.UUID;

public class Picture implements IGeneric, IAsset {
    static private final SecureRandom random = new SecureRandom();

    private final byte[] imageData;
    private final String mimeType;
    private final int width;
    private final int height;
    private final int size;

    private byte[] otrKey;
    private byte[] encBytes = null;
    private byte[] sha256Bytes;
    private String assetKey;
    private String assetToken;

    public Picture(byte[] bytes, String mime) throws IOException {
        imageData = bytes;
        mimeType = mime;
        BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(imageData));
        width = bufferedImage.getWidth();
        height = bufferedImage.getHeight();
        size = imageData.length;
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
    public Messages.GenericMessage createGenericMsg() throws Exception {
        getEncryptedData();

        ByteString sha256 = ByteString.copyFrom(sha256Bytes);

        Messages.Asset.ImageMetaData.Builder metaData = Messages.Asset.ImageMetaData.newBuilder()
                .setHeight(height)
                .setWidth(width)
                .setTag("medium");

        Messages.Asset.Original.Builder original = Messages.Asset.Original.newBuilder()
                .setSize(size)
                .setMimeType(mimeType)
                .setImage(metaData.build());

        Messages.Asset.RemoteData.Builder remoteData = Messages.Asset.RemoteData.newBuilder()
                .setAssetId(assetKey)
                .setOtrKey(ByteString.copyFrom(otrKey))
                .setSha256(sha256);

        if (assetToken != null)
            remoteData.setAssetToken(assetToken);

        Messages.Asset.Builder asset = Messages.Asset.newBuilder()
                .setUploaded(remoteData.build())
                .setOriginal(original.build());

        return Messages.GenericMessage.newBuilder()
                .setMessageId(UUID.randomUUID().toString())
                .setAsset(asset.build())
                .build();
    }

    @Override
    public byte[] getEncryptedData() {
        if (encBytes == null) {
            otrKey = new byte[32];
            byte[] iv = new byte[16];
            random.nextBytes(otrKey);
            random.nextBytes(iv);
            try {
                encBytes = Util.encrypt(otrKey, imageData, iv);
                sha256Bytes = MessageDigest.getInstance("SHA-256").digest(encBytes);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return encBytes;
    }

    @Override
    public boolean isPublic() {
        return false;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public byte[] getImageData() {
        return imageData;
    }

    public void setAssetKey(String assetKey) {
        this.assetKey = assetKey;
    }

    public void setAssetToken(String assetToken) {
        this.assetToken = assetToken;
    }

}
