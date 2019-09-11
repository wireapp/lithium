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

package com.wire.bots.sdk.user;

import com.wire.bots.sdk.WireClient;
import com.wire.bots.sdk.WireClientBase;
import com.wire.bots.sdk.assets.*;
import com.wire.bots.sdk.crypto.Crypto;
import com.wire.bots.sdk.exceptions.HttpException;
import com.wire.bots.sdk.models.AssetKey;
import com.wire.bots.sdk.server.model.Conversation;
import com.wire.bots.sdk.server.model.NewBot;
import com.wire.bots.sdk.server.model.User;
import com.wire.bots.sdk.tools.Util;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.*;

public class UserClient extends WireClientBase implements WireClient {
    private final API api;
    private final UUID conv;

    UserClient(NewBot state, UUID conv, Crypto crypto, API api) {
        super(api, crypto, state);
        this.api = api;
        this.conv = conv;
    }

    public UUID sendText(String txt) throws Exception {
        MessageText generic = new MessageText(txt);
        postGenericMessage(generic);
        return generic.getMessageId();
    }

    @Override
    public UUID sendText(String txt, long expires) throws Exception {
        MessageText generic = new MessageText(txt, expires);
        postGenericMessage(generic);
        return generic.getMessageId();
    }

    @Override
    public UUID sendText(String txt, UUID mention) throws Exception {
        int offset = Util.mentionStart(txt);
        int len = Util.mentionLen(txt);
        MessageText generic = new MessageText(txt, 0, mention, offset, len);
        postGenericMessage(generic);
        return generic.getMessageId();
    }

    @Override
    public UUID sendDirectText(String txt, UUID userId) throws Exception {
        MessageText generic = new MessageText(txt);
        postGenericMessage(generic, userId);
        return generic.getMessageId();
    }

    @Override
    public UUID sendLinkPreview(String url, String title, IGeneric image) throws Exception {
        LinkPreview generic = new LinkPreview(url, title, image.createGenericMsg().getAsset());
        postGenericMessage(generic);
        return generic.getMessageId();
    }

    @Override
    public UUID sendDirectLinkPreview(String url, String title, IGeneric image, UUID userId) throws Exception {
        LinkPreview generic = new LinkPreview(url, title, image.createGenericMsg().getAsset());
        postGenericMessage(generic, userId);
        return generic.getMessageId();
    }

    @Override
    public UUID sendPicture(byte[] bytes, String mimeType) throws Exception {
        Picture image = new Picture(bytes, mimeType);

        AssetKey assetKey = uploadAsset(image);
        image.setAssetKey(assetKey.key);
        image.setAssetToken(assetKey.token);

        postGenericMessage(image);
        return image.getMessageId();
    }

    @Override
    public UUID sendDirectPicture(byte[] bytes, String mimeType, UUID userId) throws Exception {
        return sendPicture(bytes, mimeType);
    }

    @Override
    public UUID sendDirectPicture(IGeneric image, UUID userId) throws Exception {
        postGenericMessage(image);
        return image.getMessageId();
    }

    @Override
    public AssetKey uploadAsset(IAsset asset) throws Exception {
        return api.uploadAsset(asset);
    }

    @Override
    public UUID sendPicture(IGeneric image) throws Exception {
        postGenericMessage(image);
        return image.getMessageId();
    }

    @Override
    public UUID sendAudio(byte[] bytes, String name, String mimeType, long duration) throws Exception {
        AudioPreview preview = new AudioPreview(bytes, name, mimeType, duration);
        AudioAsset audioAsset = new AudioAsset(bytes, preview);

        postGenericMessage(preview);

        AssetKey assetKey = api.uploadAsset(audioAsset);
        audioAsset.setAssetKey(assetKey.key);
        audioAsset.setAssetToken(assetKey.token);

        // post original + remote asset message
        postGenericMessage(audioAsset);
        return audioAsset.getMessageId();
    }

    @Override
    public UUID sendVideo(byte[] bytes, String name, String mimeType, long duration, int h, int w)
            throws Exception {
        UUID messageId = UUID.randomUUID();
        VideoPreview preview = new VideoPreview(name, mimeType, duration, h, w, bytes.length, messageId);
        VideoAsset asset = new VideoAsset(bytes, mimeType, messageId);

        postGenericMessage(preview);

        AssetKey assetKey = uploadAsset(asset);
        asset.setAssetKey(assetKey.key);
        asset.setAssetToken(assetKey.token);

        // post original + remote asset message
        postGenericMessage(asset);
        return asset.getMessageId();
    }

    @Override
    public UUID sendFile(File f, String mime) throws Exception {
        UUID messageId = UUID.randomUUID();
        FileAssetPreview preview = new FileAssetPreview(f.getName(), mime, f.length(), messageId);
        FileAsset asset = new FileAsset(f, mime, messageId);

        postGenericMessage(preview);

        // upload asset to backend
        AssetKey assetKey = api.uploadAsset(asset);
        asset.setAssetKey(assetKey.key);
        asset.setAssetToken(assetKey.token);

        // post original + remote asset message
        postGenericMessage(asset);
        return asset.getMessageId();
    }

    @Override
    public UUID sendDirectFile(IGeneric preview, IGeneric asset, UUID userId) throws Exception {
        postGenericMessage(preview);
        postGenericMessage(asset);
        return asset.getMessageId();
    }

    @Override
    public UUID sendDirectFile(File f, String mime, UUID userId) throws Exception {
        return sendFile(f, mime);
    }

    @Override
    public UUID ping() throws Exception {
        Ping generic = new Ping();
        postGenericMessage(generic);
        return generic.getMessageId();
    }

    @Override
    public UUID sendReaction(UUID msgId, String emoji) throws Exception {
        Reaction generic = new Reaction(msgId, emoji);
        postGenericMessage(generic);
        return generic.getMessageId();
    }

    @Override
    public UUID deleteMessage(UUID msgId) throws Exception {
        MessageDelete generic = new MessageDelete(msgId);
        postGenericMessage(generic);
        return generic.getMessageId();
    }

    @Override
    public UUID editMessage(UUID replacingMessageId, String text) throws Exception {
        MessageEdit generic = new MessageEdit(replacingMessageId, text);
        postGenericMessage(generic);
        return generic.getMessageId();
    }

    @Override
    public byte[] downloadAsset(String assetKey, String assetToken, byte[] sha256Challenge, byte[] otrKey)
            throws Exception {
        byte[] cipher = api.downloadAsset(assetKey, assetToken);
        byte[] sha256 = MessageDigest.getInstance("SHA-256").digest(cipher);
        if (!Arrays.equals(sha256, sha256Challenge))
            throw new Exception("Failed sha256 check");

        return Util.decrypt(otrKey, cipher);
    }

    @Override
    public User getSelf() throws HttpException {
        return api.getSelf();
    }

    @Override
    public UUID getConversationId() {
        return conv;
    }

    @Override
    public Collection<User> getUsers(Collection<UUID> userIds) throws HttpException {
        return api.getUsers(userIds);
    }

    @Override
    public User getUser(UUID userId) throws HttpException {
        Collection<User> users = api.getUsers(Collections.singleton(userId));
        return users.iterator().next();
    }

    @Override
    public Conversation getConversation() throws IOException {
        return api.getConversation();
    }

    @Override
    public void acceptConnection(UUID user) throws Exception {
        api.acceptConnection(user);
    }

    @Override
    public void uploadPreKeys(ArrayList<com.wire.bots.sdk.models.otr.PreKey> preKeys) {
        api.uploadPreKeys(preKeys);
    }

    @Override
    public ArrayList<Integer> getAvailablePrekeys() {
        return api.getAvailablePrekeys(state.client);
    }

    @Override
    public byte[] downloadProfilePicture(String assetKey) throws Exception {
        return api.downloadAsset(assetKey, null);
    }

    @Override
    public void call(String content) throws Exception {
        postGenericMessage(new Calling(content));
    }
}
