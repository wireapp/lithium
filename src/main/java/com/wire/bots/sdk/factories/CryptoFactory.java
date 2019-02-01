package com.wire.bots.sdk.factories;

import com.wire.bots.cryptobox.CryptoException;
import com.wire.bots.sdk.crypto.Crypto;

public interface CryptoFactory {
    Crypto create(String botId) throws CryptoException;
}
