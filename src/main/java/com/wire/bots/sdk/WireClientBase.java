package com.wire.bots.sdk;

import com.wire.bots.cryptobox.CryptoException;
import com.wire.bots.sdk.assets.IGeneric;
import com.wire.bots.sdk.crypto.Crypto;
import com.wire.bots.sdk.exceptions.HttpException;
import com.wire.bots.sdk.models.otr.*;
import com.wire.bots.sdk.server.model.NewBot;
import com.wire.bots.sdk.tools.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

public class WireClientBase {
    protected final Backend api;
    protected final Crypto crypto;
    protected final NewBot state;
    protected Devices devices = null;

    protected WireClientBase(Backend api, Crypto crypto, NewBot state) {
        this.api = api;
        this.crypto = crypto;
        this.state = state;
    }

    /**
     * Encrypt whole message for participants in the conversation.
     * Implements the fallback for the 412 error code and missing
     * devices.
     *
     * @param generic generic message to be sent
     * @throws Exception CryptoBox exception
     */
    protected void postGenericMessage(IGeneric generic) throws Exception {
        byte[] content = generic.createGenericMsg().toByteArray();

        // Try to encrypt the msg for those devices that we have the session already
        Recipients encrypt = encrypt(content, getAllDevices());
        OtrMessage msg = new OtrMessage(getDeviceId(), encrypt);

        Devices res = api.sendMessage(msg, false);
        if (!res.hasMissing()) {
            // Fetch preKeys for the missing devices from the Backend
            PreKeys preKeys = api.getPreKeys(res.missing);

            Logger.debug("Fetched %d preKeys for %d devices. Bot: %s", preKeys.count(), res.size(), getId());

            // Encrypt msg for those devices that were missing. This time using preKeys
            encrypt = crypto.encrypt(preKeys, content);
            msg.add(encrypt);

            // reset devices so they could be pulled next time
            devices = null;

            res = api.sendMessage(msg, true);
            if (!res.hasMissing()) {
                Logger.error(String.format("Failed to send otr message to %d devices. Bot: %s",
                        res.size(),
                        getId()));
            }
        }
    }

    protected void postGenericMessage(IGeneric generic, UUID userId) throws Exception {
        // Try to encrypt the msg for those devices that we have the session already
        Missing all = getAllDevices();
        Missing missing = new Missing();
        for (UUID u : all.toUserIds()) {
            if (userId.equals(u)) {
                Collection<String> clients = all.toClients(u);
                missing.add(u, clients);
            }
        }

        byte[] content = generic.createGenericMsg().toByteArray();

        Recipients encrypt = encrypt(content, missing);
        OtrMessage msg = new OtrMessage(getDeviceId(), encrypt);

        Devices res = api.sendPartialMessage(msg, userId);
        if (!res.hasMissing()) {
            // Fetch preKeys for the missing devices from the Backend
            PreKeys preKeys = api.getPreKeys(res.missing);

            Logger.debug("Fetched %d preKeys for %d devices. Bot: %s", preKeys.count(), res.size(), getId());

            // Encrypt msg for those devices that were missing. This time using preKeys
            encrypt = crypto.encrypt(preKeys, content);
            msg.add(encrypt);

            // reset devices so they could be pulled next time
            devices = null;

            res = api.sendMessage(msg, true);
            if (!res.hasMissing()) {
                Logger.error(String.format("Failed to send otr message to %d devices. Bot: %s",
                        res.size(),
                        getId()));
            }
        }
    }

    public void send(IGeneric message) throws Exception {
        postGenericMessage(message);
    }

    public void send(IGeneric message, UUID userId) throws Exception {
        postGenericMessage(message, userId);
    }

    public UUID getId() {
        return state.id;
    }

    public String getDeviceId() {
        return state.client;
    }

    public UUID getConversationId() {
        return state.conversation.id;
    }

    public void close() throws IOException {
        crypto.close();
    }

    public boolean isClosed() {
        return crypto.isClosed();
    }

    public Recipients encrypt(byte[] content, Missing missing) throws CryptoException {
        return crypto.encrypt(missing, content);
    }

    public String decrypt(UUID userId, String clientId, String cypher) throws CryptoException {
        return crypto.decrypt(userId, clientId, cypher);
    }

    public PreKey newLastPreKey() throws CryptoException {
        return crypto.newLastPreKey();
    }

    public ArrayList<PreKey> newPreKeys(int from, int count) throws CryptoException {
        return crypto.newPreKeys(from, count);
    }

    private Missing getAllDevices() throws HttpException {
        return getDevices().missing;
    }

    /**
     * This method will send an empty message to BE and collect the list of missing client ids
     * When empty message is sent the Backend will respond with error 412 and a list of missing clients.
     *
     * @return List of all participants in this conversation and their clientIds
     */
    private Devices getDevices() throws HttpException {
        if (devices == null || devices.hasMissing()) {
            String deviceId = getDeviceId();
            OtrMessage msg = new OtrMessage(deviceId, new Recipients());
            devices = api.sendMessage(msg);
        }
        return devices;
    }
}
