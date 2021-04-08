package com.wire.lithium.healthchecks;

import com.codahale.metrics.health.HealthCheck;
import com.wire.lithium.server.monitoring.MDCUtils;
import com.wire.xenon.crypto.Crypto;
import com.wire.xenon.factories.CryptoFactory;
import com.wire.xenon.models.otr.PreKeys;
import com.wire.xenon.models.otr.Recipients;
import com.wire.xenon.tools.Logger;

import java.util.Arrays;
import java.util.Base64;
import java.util.UUID;

public class Alice2Bob extends HealthCheck {
    private final CryptoFactory cryptoFactory;

    public Alice2Bob(CryptoFactory cryptoFactory) {
        this.cryptoFactory = cryptoFactory;
    }

    @Override
    protected Result check() {
        try {
            MDCUtils.put("healthCheck", "Alice2Bob"); // tag the logs with health check
            Logger.debug("Starting Alice2Bob healthcheck");

            UUID aliceId = UUID.randomUUID();
            UUID bobId = UUID.randomUUID();

            Crypto alice = cryptoFactory.create(aliceId);
            Crypto bob = cryptoFactory.create(bobId);
            PreKeys bobKeys = new PreKeys(bob.newPreKeys(0, 1), "bob", bobId);

            String text = "Hello Bob, This is Alice!";
            byte[] textBytes = text.getBytes();

            // Encrypt using prekeys
            Recipients encrypt = alice.encrypt(bobKeys, textBytes);

            String base64Encoded = encrypt.get(bobId, "bob");

            // Decrypt using initSessionFromMessage
            String decrypt = bob.decrypt(aliceId, "alice", base64Encoded);
            byte[] decode = Base64.getDecoder().decode(decrypt);

            alice.close();
            bob.close();

            if (!Arrays.equals(decode, textBytes))
                return Result.unhealthy("!Arrays.equals(decode, textBytes)");

            if (!text.equals(new String(decode)))
                return Result.unhealthy("!text.equals(new String(decode))");

            return Result.healthy();
        } catch (Exception e) {
            Logger.exception("Exception during Alice2Bob health check.", e);
            return Result.unhealthy(e.getMessage());
        } finally {
            Logger.debug("Finished Alice2Bob healthcheck");
        }
    }
}
