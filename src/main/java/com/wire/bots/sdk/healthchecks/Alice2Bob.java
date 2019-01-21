package com.wire.bots.sdk.healthchecks;

import com.codahale.metrics.health.HealthCheck;
import com.wire.bots.sdk.crypto.Crypto;
import com.wire.bots.sdk.factories.CryptoFactory;
import com.wire.bots.sdk.models.otr.PreKeys;
import com.wire.bots.sdk.models.otr.Recipients;

import java.util.Arrays;
import java.util.Base64;
import java.util.UUID;

public class Alice2Bob extends HealthCheck {
    private final CryptoFactory cryptoFactory;

    public Alice2Bob(CryptoFactory cryptoFactory) {
        this.cryptoFactory = cryptoFactory;
    }

    @Override
    protected Result check() throws Exception {
        String aliceId = UUID.randomUUID().toString();
        String bobId = UUID.randomUUID().toString();

        Crypto alice = cryptoFactory.create(aliceId);
        Crypto bob = cryptoFactory.create(bobId);
        PreKeys bobKeys = new PreKeys(bob.newPreKeys(0, 1), bobId, bobId);

        String text = "Hello Bob, This is Alice!";
        byte[] textBytes = text.getBytes();

        // Encrypt using prekeys
        Recipients encrypt = alice.encrypt(bobKeys, textBytes);

        String base64Encoded = encrypt.get(bobId, bobId);

        // Decrypt using initSessionFromMessage
        String decrypt = bob.decrypt(aliceId, aliceId, base64Encoded);
        byte[] decode = Base64.getDecoder().decode(decrypt);

        alice.close();
        bob.close();

        if (!Arrays.equals(decode, textBytes))
            return Result.unhealthy("!Arrays.equals(decode, textBytes)");

        if (!text.equals(new String(decode)))
            return Result.unhealthy("!text.equals(new String(decode))");

        return Result.healthy();
    }
}
