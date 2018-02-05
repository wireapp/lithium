package com.wire.bots.sdk.crypto;

import com.wire.bots.sdk.models.otr.Missing;
import com.wire.bots.sdk.models.otr.PreKey;
import com.wire.bots.sdk.models.otr.PreKeys;
import com.wire.bots.sdk.models.otr.Recipients;

import java.io.Closeable;
import java.util.ArrayList;

public interface Crypto extends Closeable {
    PreKey newLastPreKey() throws Exception;

    ArrayList<PreKey> newPreKeys(int from, int count) throws Exception;

    Recipients encrypt(PreKeys preKeys, byte[] content) throws Exception;

    Recipients encrypt(Missing missing, byte[] content) throws Exception;

    String decrypt(String userId, String clientId, String cypher) throws Exception;

    boolean isClosed();
}
