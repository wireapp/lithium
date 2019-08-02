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

package com.wire.bots.sdk;

import com.wire.bots.sdk.assets.*;
import com.wire.bots.sdk.crypto.Crypto;
import com.wire.bots.sdk.models.AssetKey;
import com.wire.bots.sdk.models.otr.PreKey;
import com.wire.bots.sdk.server.model.Conversation;
import com.wire.bots.sdk.server.model.NewBot;
import com.wire.bots.sdk.server.model.User;
import com.wire.bots.sdk.tools.Util;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.*;

/**
 *
 */
public class BotClient extends WireClientBase implements WireClient {

    private final API api;

    BotClient(NewBot state, Crypto crypto, API api) {
        super(api, crypto, state);
        this.api = api;
    }

    @Override
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
        int offset = txt.indexOf('@');
        int end = txt.indexOf(' ', offset);
        MessageText generic = new MessageText(txt, 0, mention, offset, end - offset);
        postGenericMessage(generic);
        return generic.getMessageId();
    }

    @Override
    public UUID sendDirectText(String txt, UUID userId) throws Exception {
        MessageText generic = new MessageText(txt);
        postGenericMessage(generic, userId.toString());
        return generic.getMessageId();
    }

    @Override
    public UUID sendLinkPreview(String url, String title, IGeneric image) throws Exception {
        LinkPreview generic = new LinkPreview(url, title, image.createGenericMsg().getAsset());
        postGenericMessage(generic);
        return generic.getMessageId();
    }

    @Override
    public UUID sendDirectLinkPreview(String url, String title, IGeneric image, String userId) throws Exception {
        LinkPreview msg = new LinkPreview(url, title, image.createGenericMsg().getAsset());
        postGenericMessage(msg, userId);
        return msg.getMessageId();
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
    public UUID sendDirectPicture(byte[] bytes, String mimeType, String userId) throws Exception {
        Picture image = new Picture(bytes, mimeType);

        AssetKey assetKey = uploadAsset(image);
        image.setAssetKey(assetKey.key);
        image.setAssetToken(assetKey.token);

        postGenericMessage(image, userId);
        return image.getMessageId();
    }

    @Override
    public UUID sendPicture(IGeneric image) throws Exception {
        postGenericMessage(image);
        return image.getMessageId();
    }

    @Override
    public UUID sendDirectPicture(IGeneric image, String userId) throws Exception {
        postGenericMessage(image, userId);
        return image.getMessageId();
    }

    @Override
    public UUID sendAudio(byte[] bytes, String name, String mimeType, long duration) throws Exception {
        AudioPreview preview = new AudioPreview(bytes, name, mimeType, duration);
        AudioAsset audioAsset = new AudioAsset(bytes, preview);

        postGenericMessage(preview);

        AssetKey assetKey = uploadAsset(audioAsset);
        audioAsset.setAssetKey(assetKey.key);
        audioAsset.setAssetToken(assetKey.token);

        // post original + remote asset message
        postGenericMessage(audioAsset);
        return audioAsset.getMessageId();
    }

    @Override
    public UUID sendVideo(byte[] bytes, String name, String mimeType, long duration, int h, int w) throws Exception {
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

        // post original
        postGenericMessage(preview);

        // upload asset to backend
        AssetKey assetKey = uploadAsset(asset);
        asset.setAssetKey(assetKey.key);
        asset.setAssetToken(assetKey.token);

        // post remote asset message
        postGenericMessage(asset);
        return asset.getMessageId();
    }

    @Override
    public UUID sendDirectFile(File f, String mime, String userId) throws Exception {
        UUID messageId = UUID.randomUUID();
        FileAssetPreview preview = new FileAssetPreview(f.getName(), mime, f.length(), messageId);
        FileAsset asset = new FileAsset(f, mime, messageId);

        // post original
        postGenericMessage(preview, userId);

        // upload asset to backend
        AssetKey assetKey = uploadAsset(asset);
        asset.setAssetKey(assetKey.key);
        asset.setAssetToken(assetKey.token);

        // post remote asset message
        postGenericMessage(asset, userId);
        return asset.getMessageId();
    }

    @Override
    public UUID sendDirectFile(IGeneric preview, IGeneric asset, String userId) throws Exception {
        // post original
        postGenericMessage(preview, userId);

        // post remote asset message
        postGenericMessage(asset, userId);
        return asset.getMessageId();
    }

    @Override
    public UUID ping() throws Exception {
        Ping generic = new Ping();
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
    public User getSelf() {
        return api.getSelf();
    }

    @Override
    public Collection<User> getUsers(Collection<String> userIds) {
        return api.getUsers(userIds);
    }

    @Override
    public User getUser(String userId) {
        Collection<User> users = api.getUsers(Collections.singleton(userId));
        return users.iterator().next();
    }

    @Override
    public Conversation getConversation() {
        return api.getConversation();
    }

    @Override
    public void acceptConnection(UUID user) {
        // bots cannot accept connections
    }

    @Override
    public void uploadPreKeys(ArrayList<PreKey> preKeys) throws IOException {
        api.uploadPreKeys((preKeys));
    }

    @Override
    public ArrayList<Integer> getAvailablePrekeys() {
        return api.getAvailablePrekeys();
    }

    @Override
    public byte[] downloadProfilePicture(String assetKey) throws IOException {
        return api.downloadAsset(assetKey, null);
    }

    @Override
    public AssetKey uploadAsset(IAsset asset) throws Exception {
        return api.uploadAsset(asset);
    }

    @Override
    public void call(String content) throws Exception {
        postGenericMessage(new Calling(content));
    }
}
