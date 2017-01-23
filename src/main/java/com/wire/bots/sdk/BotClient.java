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
import com.wire.bots.sdk.models.AssetKey;
import com.wire.bots.sdk.models.otr.Devices;
import com.wire.bots.sdk.models.otr.OtrMessage;
import com.wire.bots.sdk.models.otr.PreKeys;
import com.wire.bots.sdk.server.model.Conversation;
import com.wire.bots.sdk.server.model.User;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

/**
 *
 */
class BotClient implements WireClient {
    private final String botId;
    private final String conversationId;
    private final String clientId;
    private final JerseyClient jerseyClient;
    private final OtrManager otrManager;
    private final Devices devices;

    public BotClient(OtrManager otrManager, String botId, String convId, String clientId, String token) {
        this.botId = botId;
        this.conversationId = convId;
        this.clientId = clientId;
        this.jerseyClient = new JerseyClient(token);
        this.otrManager = otrManager;
        this.devices = getDevices();
    }

    @Override
    public void sendText(String txt) throws Exception {
        postGenericMessage(new Text(txt));
    }

    @Override
    public void sendText(String txt, long expires) throws Exception {
        postGenericMessage(new Text(txt, expires));
    }

    @Override
    public void sendLinkPreview(String url, String title) throws Exception {
        postGenericMessage(new LinkPreview(url, title));
    }

    @Override
    public void sendPicture(byte[] bytes, String mimeType) throws Exception {
        Picture image = new Picture(bytes, mimeType);

        AssetKey assetKey = jerseyClient.uploadAsset(image);
        image.setAssetKey(assetKey.key);
        image.setAssetToken(assetKey.token);

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
        FileAssetPreview preview = new FileAssetPreview(f, mime);
        FileAsset asset = new FileAsset(preview);

        // post preview
        postGenericMessage(preview);

        // upload asset to backend
        AssetKey assetKey = jerseyClient.uploadAsset(asset);
        asset.setAssetKey(assetKey.key);
        asset.setAssetToken(assetKey.token);

        // post original + remote asset message
        postGenericMessage(asset);
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
        return conversationId;
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
    public void sendDelivery(String msgId) throws Exception {
        postGenericMessage(new Confirmation(msgId));
    }

    @Override
    @Deprecated
    public void acceptConnection(String user) throws IOException {

    }

    private void postGenericMessage(IGeneric generic) throws Exception {
        OtrMessage msg = new OtrMessage(clientId, generic.createGenericMsg().toByteArray());

        // Try to encrypt the msg for those devices that we have the session already
        otrManager.encrypt(devices, msg);

        Devices res = jerseyClient.sendMessage(msg);
        if (!res.hasMissing()) {
            // Fetch preKeys for the missing devices from the Backend
            PreKeys preKeys = jerseyClient.getPreKeys(res.missing);

            // Encrypt msg for those devices that were missing. This time using preKeys
            otrManager.encrypt(preKeys, msg);

            // todo: update this.devices here

            res = jerseyClient.sendMessage(msg, true);
            if (!res.hasMissing()) {
                Logger.error(String.format("Failed to send otr message to %d devices. Bot: %s",
                        res.size(),
                        botId));
            }
        }
    }

    /**
     * This method will send an empty message to BE and collect the list of missing client ids
     * When empty message is sent the Backend will respond with error 412 and a list of missing clients.
     *
     * @return List of all participants in this conversation and their clientIds
     */
    private Devices getDevices() {
        try {
            return jerseyClient.sendMessage(new OtrMessage(clientId));
        } catch (IOException e) {
            Logger.error(e.getMessage());
            return new Devices();
        }
    }
}
