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

import com.waz.model.Messages;
import com.wire.bots.sdk.Logger;
import com.wire.bots.sdk.OtrManager;
import com.wire.bots.sdk.Util;
import com.wire.bots.sdk.WireClient;
import com.wire.bots.sdk.assets.*;
import com.wire.bots.sdk.models.AssetKey;
import com.wire.bots.sdk.models.otr.Devices;
import com.wire.bots.sdk.models.otr.OtrMessage;
import com.wire.bots.sdk.models.otr.PreKeys;
import com.wire.bots.sdk.server.model.Conversation;
import com.wire.bots.sdk.server.model.User;
import com.wire.cryptobox.CryptoException;
import com.wire.cryptobox.PreKey;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;

public class UserClient implements WireClient {

    private final String botId;
    private final String convId;
    private final String clientId;
    private final UserJerseyClient jerseyClient;
    private final OtrManager otrManager;
    private final Devices devices;

    public UserClient(OtrManager otrManager, String botId, String convId, String clientId, String token) {
        this.botId = botId;
        this.convId = convId;
        this.clientId = clientId;
        this.jerseyClient = new UserJerseyClient(this.convId, token);
        this.otrManager = otrManager;
        this.devices = getDevices();
    }

    public void sendText(String txt) throws Exception {
        postGenericMessage(new Text(txt));
    }

    @Override
    public void sendText(String txt, long expires) throws Exception {
        postGenericMessage(new Text(txt, expires));
    }

    @Override
    public void sendLinkPreview(String url, String title, IGeneric image) throws Exception {
        postGenericMessage(new LinkPreview(url, title, image.createGenericMsg().getAsset()));
    }

    @Override
    public void sendPicture(byte[] bytes, String mimeType) throws Exception {
        Picture image = new Picture(bytes, mimeType);

        AssetKey assetKey = uploadAsset(image);
        image.setAssetKey(assetKey.key);
        image.setAssetToken(assetKey.token);

        postGenericMessage(image);
    }

    @Override
    public AssetKey uploadAsset(IAsset asset) throws Exception {
        return jerseyClient.uploadAsset(asset);
    }

    @Override
    public void sendPicture(IGeneric image) throws Exception {
        postGenericMessage(image);
    }

    @Override
    public void sendAudio(byte[] bytes, String name, String mimeType, long duration) throws Exception {
        AudioPreview preview = new AudioPreview(bytes, name, mimeType, duration);
        AudioAsset audioAsset = new AudioAsset(bytes, preview);

        postGenericMessage(preview);

        AssetKey assetKey = jerseyClient.uploadAsset(audioAsset);
        audioAsset.setAssetKey(assetKey.key);
        audioAsset.setAssetToken(assetKey.token);

        // post original + remote asset message
        postGenericMessage(audioAsset);
    }

    @Override
    public void sendFile(File f, String mime) throws Exception {
        // post original asset message (preview)
        FileAssetPreview assetPreview = new FileAssetPreview(f, mime);

        postGenericMessage(assetPreview);

        FileAsset asset = new FileAsset(assetPreview);

        // upload asset to backend
        AssetKey assetKey = jerseyClient.uploadAsset(asset);
        asset.setAssetKey(assetKey.key);
        asset.setAssetToken(assetKey.token);

        // post original + remote asset message
        postGenericMessage(asset);
    }

    @Override
    public void sendDelivery(String msgId) throws Exception {
        postGenericMessage(new Confirmation(msgId));
    }

    @Override
    public byte[] downloadAsset(String assetKey, String assetToken, byte[] sha256Challenge, byte[] otrKey) throws Exception {
        byte[] cipher = jerseyClient.downloadAsset(assetKey, assetToken);
        byte[] sha256 = MessageDigest.getInstance("SHA-256").digest(cipher);
        if (!Arrays.equals(sha256, sha256Challenge))
            throw new Exception("Failed sha256 check");

        return Util.decrypt(otrKey, cipher);
    }

    @Override
    public String getId() {
        return botId;
    }

    @Override
    public String getConversationId() {
        return convId;
    }

    @Override
    public String getDeviceId() {
        return clientId;
    }

    @Override
    public Collection<User> getUsers(ArrayList<String> userIds) throws IOException {
        return jerseyClient.getUsers(userIds);
    }

    @Override
    public Conversation getConversation() throws IOException {
        return jerseyClient.getConversation();
    }

    @Override
    public void acceptConnection(String user) throws IOException {
        jerseyClient.setConnectionStatus(user, "accepted");
    }

    private void postGenericMessage(IGeneric generic) throws Exception {
        OtrMessage msg = new OtrMessage(clientId);

        Messages.GenericMessage genMsg = generic.createGenericMsg();
        msg.setContent(genMsg.toByteArray());

        otrManager.encrypt(devices, msg);

        Devices missing = jerseyClient.sendMessage(msg);
        if (!missing.hasMissing()) {
            PreKeys preKeys = jerseyClient.getPreKeys(missing.missing);

            otrManager.encrypt(preKeys, msg);

            missing = jerseyClient.sendMessage(msg);

            if (!missing.hasMissing()) {
                Logger.warning(String.format("Sending otr message with missing %d devices. Conv: %s",
                        missing.size(),
                        convId));
                missing = jerseyClient.sendMessage(msg, true);
            }

            if (!missing.hasMissing()) {
                Logger.error(String.format("Failed to send otr message to %d devices. Conv: %s",
                        missing.size(),
                        convId));
            }
        }
    }

    private Devices getDevices() {
        try {
            return jerseyClient.sendMessage(new OtrMessage(clientId));
        } catch (IOException e) {
            Logger.error(e.getMessage());
            return new Devices();
        }
    }

    @Override
    public void close() throws IOException {
        otrManager.close();
    }

    @Override
    public byte[] decrypt(String userId, String clientId, String cypher) throws CryptoException {
        return otrManager.decrypt(userId, clientId, cypher);
    }

    @Override
    public com.wire.bots.sdk.models.otr.PreKey newLastPreKey() throws CryptoException {
        PreKey preKey = otrManager.newLastPreKey();

        com.wire.bots.sdk.models.otr.PreKey key = new com.wire.bots.sdk.models.otr.PreKey();
        key.id = preKey.id;
        key.key = Base64.getEncoder().encodeToString(preKey.data);

        return key;
    }

    @Override
    public ArrayList<com.wire.bots.sdk.models.otr.PreKey> newPreKeys(int from, int count) throws CryptoException {
        ArrayList<com.wire.bots.sdk.models.otr.PreKey> ret = new ArrayList<>(count);

        com.wire.cryptobox.PreKey[] preKeys = otrManager.newPreKeys(from, count);
        for (com.wire.cryptobox.PreKey k : preKeys) {
            com.wire.bots.sdk.models.otr.PreKey prekey = new com.wire.bots.sdk.models.otr.PreKey();
            prekey.id = k.id;
            prekey.key = Base64.getEncoder().encodeToString(k.data);
            ret.add(prekey);
        }
        return ret;
    }

    @Override
    public void uploadPreKeys(ArrayList<com.wire.bots.sdk.models.otr.PreKey> preKeys) throws IOException {
        jerseyClient.uploadPreKeys(preKeys);
    }

    @Override
    public ArrayList<Integer> getAvailablePrekeys() {
        return jerseyClient.getAvailablePrekeys(clientId);
    }

    @Override
    public boolean isClosed() {
        return otrManager.isClosed();
    }

    @Override
    public byte[] downloadProfilePicture(String assetKey) throws IOException {
        return jerseyClient.downloadAsset(assetKey, null);
    }
}
