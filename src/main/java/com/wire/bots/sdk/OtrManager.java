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

import com.wire.bots.sdk.models.otr.*;
import com.wire.cryptobox.*;
import com.wire.cryptobox.PreKey;
import com.wire.bots.sdk.models.otr.Devices;
import com.wire.bots.sdk.models.otr.OtrMessage;

import java.io.Closeable;
import java.util.Base64;
import java.util.HashMap;

/**
 * Wrapper for the Crypto Box. This class is thread safe.
 */
public class OtrManager implements Closeable {
    private final Object lock = new Object();
    private static final int MAX_PREKEY_ID = 0xFFFE;

    private final CryptoBox box;

    /**
     * Opens the CryptoBox using given directory path
     * The given directory must exist and be writable.
     * <p/>
     * Note: Do not create multiple OtrManagers that operate on the same or
     * overlapping directories. Doing so results in undefined behaviour.
     *
     * @param cryptoDir The root storage directory of the box
     * @throws CryptoException
     */
    public OtrManager(String cryptoDir) throws CryptoException {
        box = CryptoBox.open(cryptoDir);
    }

    /**
     * Generate a new last prekey.
     */
    public PreKey newLastPreKey() throws CryptoException {
        return box.newLastPreKey();
    }

    /**
     * Generate a new batch of ephemeral prekeys.
     * <p/>
     * If <tt>start + num > {@link #MAX_PREKEY_ID}<tt/> the IDs wrap around and start
     * over at 0. Thus after any valid invocation of this method, the last generated
     * prekey ID is always <tt>(start + num) % ({@link #MAX_PREKEY_ID} + 1)</tt>. The caller
     * can remember that ID and feed it back into {@link #newPreKeys} as the start
     * ID when the next batch of ephemeral keys needs to be generated.
     *
     * @param fromId The ID (>= 0 and <= {@link #MAX_PREKEY_ID}) of the first prekey to generate.
     * @param count  The total number of prekeys to generate (> 0 and <= {@link #MAX_PREKEY_ID}).
     */
    public PreKey[] newPreKeys(int fromId, int count) throws CryptoException {
        return box.newPreKeys(fromId, count);
    }

    /**
     * For each prekey encrypt the content that is in the OtrMessage
     *
     * @param preKeys Prekeys
     * @param msg     Final object containing ciphers for all clients. This will be sent to BE
     * @return Final object that can be now sent to BE. This is NOT a new object!
     * @throws CryptoException
     */
    public OtrMessage encrypt(PreKeys preKeys, OtrMessage msg) throws CryptoException {
        for (String userId : preKeys.keySet()) {
            HashMap<String, com.wire.bots.sdk.models.otr.PreKey> clients = preKeys.get(userId);
            for (String clientId : clients.keySet()) {
                com.wire.bots.sdk.models.otr.PreKey pk = clients.get(clientId);
                if (pk != null && pk.key != null) {
                    PreKey preKey = new PreKey(pk.id, Base64.getDecoder().decode(pk.key));
                    String id = createId(userId, clientId);

                    byte[] cipher = encryptFromPreKeys(id, preKey, msg.getContent());
                    msg.add(userId, clientId, cipher);
                }
            }
        }
        return msg;
    }

    /**
     * Append cipher to {@param #msg} for each device using crypto box session. Ciphers for those devices that still
     * don't have the session will be skipped and those must be encrypted using prekeys:
     * {@link #encrypt(PreKeys, OtrMessage)} encrypt
     *
     * @param devices List of device ids
     * @param msg     Message containing the plain text content
     * @return The same object {@param #msg} with be returned but with ciphers appended for each device
     */
    public OtrMessage encrypt(Devices devices, OtrMessage msg) throws CryptoException {
        byte[] content = msg.getContent();
        for (String userId : devices.getUserIds()) {
            for (String clientId : devices.getClients(userId)) {
                String id = createId(userId, clientId);

                byte[] cipher = encryptFromSession(id, content);
                if (cipher != null)
                    msg.add(userId, clientId, cipher);
            }
        }
        return msg;
    }

    /**
     * Tries to decrypt the cipher either using existing session or it inits new session (and decrypts) using this cipher
     *
     * @param userId   User id
     * @param clientId Device id
     * @param cypher   Encrypted, Base64 encoded string
     * @return Decrypted blob
     * @throws CryptoException
     */
    public byte[] decrypt(String userId, String clientId, String cypher) throws CryptoException {
        byte[] decode = Base64.getDecoder().decode(cypher);
        String id = createId(userId, clientId);

        synchronized (lock) {
            CryptoSession cryptoSession = null;
            try {
                cryptoSession = box.tryGetSession(id);
                if (cryptoSession != null) {
                    return cryptoSession.decrypt(decode);
                }

                SessionMessage sessionMessage = box.initSessionFromMessage(id, decode);
                cryptoSession = sessionMessage.getSession();
                return sessionMessage.getMessage();
            } finally {
                saveSession(cryptoSession);
            }
        }
    }

    /**
     * Closes CryptoBox object. After this method is invoked no more operations on this object can be done
     */
    @Override
    public void close() {
        synchronized (lock) {
            box.close();
        }
    }

    /**
     * Closes all previously opened sessions
     */
    public void closeAllSessions() {
        synchronized (lock) {
            box.closeAllSessions();
        }
    }

    /**
     * Inits the session from the prekey and encrypts the given content
     *
     * @param id      Identifier in our case: userId_clientId @see {@link #createId}
     * @param content Unencrypted binary content to be encrypted
     * @return Cipher
     * @throws CryptoException
     */
    private byte[] encryptFromPreKeys(String id, PreKey preKey, byte[] content) throws CryptoException {
        synchronized (lock) {
            CryptoSession cryptoSession = box.initSessionFromPreKey(id, preKey);
            try {
                return cryptoSession.encrypt(content);
            } finally {
                saveSession(cryptoSession);
            }
        }
    }

    /**
     * Tries to fetch/open a session for the given id if it exists on the hdd and encrypts the given content
     *
     * @param id      Identifier in our case: userId_clientId @see {@link #createId}
     * @param content Unencrypted binary content to be encrypted
     * @return Cipher or NULL in case there is no session for the given {@param #id}
     * @throws CryptoException
     */
    private byte[] encryptFromSession(String id, byte[] content) throws CryptoException {
        synchronized (lock) {
            CryptoSession session = null;
            try {
                session = box.tryGetSession(id);
                if (session != null) {
                    return session.encrypt(content);
                }
            } finally {
                saveSession(session);
            }
        }
        return null;
    }

    private void saveSession(CryptoSession cryptoSession) throws CryptoException {
        if (cryptoSession != null) {
            cryptoSession.save();
        }
    }

    private static String createId(String userId, String clientId) {
        return String.format("%s_%s", userId, clientId);
    }
}
