package com.wire.bots.sdk.factories;

import com.wire.bots.cryptobox.CryptoException;
import com.wire.bots.sdk.crypto.Crypto;

import java.util.UUID;

public interface CryptoFactory {
    Crypto create(UUID botId) throws CryptoException;
}
