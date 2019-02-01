package com.wire.bots.sdk.crypto;

import com.wire.bots.cryptobox.CryptoException;
import com.wire.bots.sdk.models.otr.Missing;
import com.wire.bots.sdk.models.otr.PreKey;
import com.wire.bots.sdk.models.otr.PreKeys;
import com.wire.bots.sdk.models.otr.Recipients;

import java.io.Closeable;
import java.util.ArrayList;

public interface Crypto extends Closeable {
    PreKey newLastPreKey() throws CryptoException;

    ArrayList<PreKey> newPreKeys(int from, int count) throws CryptoException;

    Recipients encrypt(PreKeys preKeys, byte[] content) throws CryptoException;

    /**
     * Append cipher to {@param #msg} for each device using crypto box session. Ciphers for those devices that still
     * don't have the session will be skipped and those must be encrypted using prekeys:
     *
     * @param missing List of device that are missing
     * @param content Plain text content to be encrypted
     */
    Recipients encrypt(Missing missing, byte[] content) throws CryptoException;

    /**
     * Decrypt cipher either using existing session or it creates new session from this cipher and decrypts
     *
     * @param userId   Sender's User id
     * @param clientId Sender's Client id
     * @param cypher   Encrypted, Base64 encoded string
     * @return Decrypted Base64 encoded string
     * @throws CryptoException throws CryptoException
     */
    String decrypt(String userId, String clientId, String cypher) throws CryptoException;

    boolean isClosed();
}
