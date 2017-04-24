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
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.UUID;

public class Picture implements IGeneric, IAsset {
    static private final SecureRandom random = new SecureRandom();

    private byte[] imageData;
    private String mimeType;
    private int width;
    private int height;
    private int size;
    private byte[] otrKey;
    private byte[] encBytes = null;
    private byte[] sha256;
    private String assetKey;
    private String assetToken;
    private boolean isPublic;
    private String retention = "volatile";
    private String messageId = UUID.randomUUID().toString();
    private long expires;

    public Picture(byte[] bytes, String mime) throws IOException {
        imageData = bytes;
        mimeType = mime;
        loadBufferImage();
    }

    public Picture(byte[] bytes) throws IOException {
        imageData = bytes;
        extractMimeType();
        loadBufferImage();
    }

    public Picture(String url) throws IOException {
        try (InputStream input = new URL(url).openStream()) {
            imageData = Util.toByteArray(input);
        }
        extractMimeType();
        loadBufferImage();
    }

    public Picture() {
    }

    @Override
    public String getMimeType() {
        return mimeType;
    }

    @Override
    public String getRetention() {
        return retention;
    }

    @Override
    public Messages.GenericMessage createGenericMsg() throws Exception {
        Messages.GenericMessage.Builder ret = Messages.GenericMessage.newBuilder()
                .setMessageId(messageId);

        Messages.Asset.ImageMetaData.Builder metaData = Messages.Asset.ImageMetaData.newBuilder()
                .setHeight(height)
                .setWidth(width)
                .setTag("medium");

        Messages.Asset.Original.Builder original = Messages.Asset.Original.newBuilder()
                .setSize(size)
                .setMimeType(mimeType)
                .setImage(metaData);

        Messages.Asset.RemoteData.Builder remoteData = Messages.Asset.RemoteData.newBuilder()
                .setAssetId(assetKey)
                .setOtrKey(ByteString.copyFrom(getOtrKey()))
                .setSha256(ByteString.copyFrom(getSha256()));

        if (assetToken != null)
            remoteData.setAssetToken(assetToken);

        Messages.Asset.Builder asset = Messages.Asset.newBuilder()
                .setUploaded(remoteData)
                .setOriginal(original);

        if (expires > 0) {
            Messages.Ephemeral.Builder ephemeral = Messages.Ephemeral.newBuilder()
                    .setAsset(asset)
                    .setExpireAfterMillis(expires);

            return ret
                    .setEphemeral(ephemeral)
                    .build();
        }
        return ret
                .setAsset(asset)
                .build();
    }

    @Override
    public byte[] getEncryptedData() {
        if (encBytes == null) {
            try {
                byte[] iv = new byte[16];
                random.nextBytes(iv);
                encBytes = Util.encrypt(getOtrKey(), imageData, iv);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return encBytes;
    }

    @Override
    public boolean isPublic() {
        return isPublic;
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

    public void setPublic(boolean isPublic) {
        this.isPublic = isPublic;
    }

    public void setRetention(String retention) {
        this.retention = retention;
    }

    public byte[] getOtrKey() {
        if (otrKey == null) {
            otrKey = new byte[32];
            random.nextBytes(otrKey);
        }
        return otrKey;
    }

    public byte[] getSha256() throws NoSuchAlgorithmException {
        if (sha256 == null) {
            sha256 = MessageDigest.getInstance("SHA-256").digest(encBytes);
        }
        return sha256;
    }

    public String getAssetKey() {
        return assetKey;
    }

    public String getAssetToken() {
        return assetToken;
    }

    public int getSize() {
        return size;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setOtrKey(byte[] otrKey) {
        this.otrKey = otrKey;
    }

    public void setSha256(byte[] sha256) {
        this.sha256 = sha256;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public void setExpires(long expires) {
        this.expires = expires;
    }

    public long getExpires() {
        return expires;
    }

    private void loadBufferImage() throws IOException {
        try (InputStream input = new ByteArrayInputStream(imageData)) {
            BufferedImage bufferedImage = ImageIO.read(input);
            width = bufferedImage.getWidth();
            height = bufferedImage.getHeight();
            size = imageData.length;
        }
    }

    private void extractMimeType() throws IOException {
        try (ByteArrayInputStream input = new ByteArrayInputStream(imageData)) {
            String contentType = URLConnection.guessContentTypeFromStream(input);
            mimeType = contentType != null ? contentType : "image/xyz";
        }
    }
}
