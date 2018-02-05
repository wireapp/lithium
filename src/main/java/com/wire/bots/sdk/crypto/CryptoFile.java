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

package com.wire.bots.sdk.crypto;

import com.wire.bots.sdk.models.otr.Missing;
import com.wire.bots.sdk.models.otr.PreKey;
import com.wire.bots.sdk.models.otr.PreKeys;
import com.wire.bots.sdk.models.otr.Recipients;
import com.wire.cryptobox.CryptoBox;
import com.wire.cryptobox.CryptoSession;
import com.wire.cryptobox.SessionMessage;

import javax.annotation.Nullable;
import java.io.File;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;

/**
 * Wrapper for the Crypto Box. This class is thread safe.
 */
public class CryptoFile implements Crypto {
    private final Object lock = new Object();
    private final CryptoBox box;

    /**
     * Opens the CryptoBox using given directory path
     * The given directory must exist and be writable.
     * <p/>
     * Note: Do not create multiple OtrManagers that operate on the same or
     * overlapping directories. Doing so results in undefined behaviour.
     *
     * @param uri The root storage directory of the box
     * @param botId Bot id
     * @throws Exception
     */
    public CryptoFile(String uri, String botId) throws Exception {
        String path = String.format("%s/%s", uri, botId);
        File data = new File(path);
        data.mkdirs();
        box = CryptoBox.open(path);
    }

    private static void saveSession(CryptoSession cryptoSession) throws Exception {
        if (cryptoSession != null) {
            cryptoSession.save();
        }
    }

    private static com.wire.cryptobox.PreKey toPreKey(PreKey preKey) {
        return new com.wire.cryptobox.PreKey(preKey.id, Base64.getDecoder().decode(preKey.key));
    }

    private static PreKey toPreKey(com.wire.cryptobox.PreKey preKey) {
        PreKey ret = new PreKey();
        ret.id = preKey.id;
        ret.key = Base64.getEncoder().encodeToString(preKey.data);
        return ret;
    }

    private static String createId(String userId, String clientId) {
        return String.format("%s_%s", userId, clientId);
    }

    /**
     * Generate a new last prekey.
     */
    @Override
    public PreKey newLastPreKey() throws Exception {
        return toPreKey(box.newLastPreKey());
    }

    /**
     * Generate a new batch of ephemeral prekeys.
     * <p/>
     * If <tt>start + num > 0xFFFE <tt/> the IDs wrap around and start
     * over at 0. Thus after any valid invocation of this method, the last generated
     * prekey ID is always <tt>(start + num) % (0xFFFE + 1)</tt>. The caller
     * can remember that ID and feed it back into {@link #newPreKeys} as the start
     * ID when the next batch of ephemeral keys needs to be generated.
     *
     * @param from  The ID (>= 0 and <= 0xFFFE) of the first prekey to generate.
     * @param count The total number of prekeys to generate (> 0 and <= 0xFFFE).
     */
    @Override
    public ArrayList<PreKey> newPreKeys(int from, int count) throws Exception {
        ArrayList<PreKey> ret = new ArrayList<>(count);
        for (com.wire.cryptobox.PreKey k : box.newPreKeys(from, count)) {
            PreKey prekey = toPreKey(k);
            ret.add(prekey);
        }
        return ret;
    }

    /**
     * For each prekey encrypt the content that is in the OtrMessage
     *
     * @param preKeys Prekeys
     * @param content Plain plain text content
     * @throws Exception throws Exception
     */
    @Override
    public Recipients encrypt(PreKeys preKeys, byte[] content) throws Exception {
        Recipients recipients = new Recipients();
        for (String userId : preKeys.keySet()) {
            HashMap<String, PreKey> clients = preKeys.get(userId);
            for (String clientId : clients.keySet()) {
                PreKey pk = clients.get(clientId);
                if (pk != null && pk.key != null) {
                    String id = createId(userId, clientId);
                    byte[] cipher = encryptFromPreKeys(id, pk, content);
                    String s = Base64.getEncoder().encodeToString(cipher);
                    recipients.add(userId, clientId, s);
                }
            }
        }
        return recipients;
    }

    /**
     * Append cipher to {@param #msg} for each device using crypto box session. Ciphers for those devices that still
     * don't have the session will be skipped and those must be encrypted using prekeys:
     *
     * @param missing List of device that are missing
     * @param content Plain text content to be encrypted
     */
    @Override
    public Recipients encrypt(Missing missing, byte[] content) throws Exception {
        Recipients recipients = new Recipients();
        for (String userId : missing.toUserIds()) {
            for (String clientId : missing.toClients(userId)) {
                String id = createId(userId, clientId);
                byte[] cipher = encryptFromSession(id, content);
                if (cipher != null) {
                    String s = Base64.getEncoder().encodeToString(cipher);
                    recipients.add(userId, clientId, s);
                }
            }
        }
        return recipients;
    }

    /**
     * Decrypt cipher either using existing session or it creates new session from this cipher and decrypts
     *
     * @param userId   Sender's User id
     * @param clientId Sender's Client id
     * @param cypher   Encrypted, Base64 encoded string
     * @return Decrypted Base64 encoded string
     * @throws Exception throws Exception
     */
    @Override
    public String decrypt(String userId, String clientId, String cypher) throws Exception {
        byte[] decode = Base64.getDecoder().decode(cypher);
        String id = createId(userId, clientId);

        synchronized (lock) {
            CryptoSession cryptoSession = null;
            try {
                cryptoSession = box.tryGetSession(id);
                if (cryptoSession != null) {
                    return Base64.getEncoder().encodeToString(cryptoSession.decrypt(decode));
                }
                SessionMessage sessionMessage = box.initSessionFromMessage(id, decode);
                cryptoSession = sessionMessage.getSession();
                return Base64.getEncoder().encodeToString(sessionMessage.getMessage());
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

    @Override
    public boolean isClosed() {
        synchronized (lock) {
            return box.isClosed();
        }
    }

    /**
     * Inits the session from the prekey and encrypts the given content
     *
     * @param id      Identifier in our case: userId_clientId @see {@link #createId}
     * @param content Unencrypted binary content to be encrypted
     * @return Cipher
     * @throws Exception throws Exception
     */
    private byte[] encryptFromPreKeys(String id, PreKey preKey, byte[] content) throws Exception {
        synchronized (lock) {
            CryptoSession cryptoSession = box.initSessionFromPreKey(id, toPreKey(preKey));
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
     * @throws Exception throws Exception
     */
    @Nullable
    private byte[] encryptFromSession(String id, byte[] content) throws Exception {
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
}
