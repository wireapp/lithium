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

import com.wire.bots.sdk.OtrManager;
import com.wire.bots.sdk.models.otr.Devices;
import com.wire.bots.sdk.models.otr.OtrMessage;
import com.wire.bots.sdk.models.otr.PreKeys;
import com.wire.cryptobox.CryptoException;
import com.wire.cryptobox.PreKey;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;

public class OtrManagerTest {

    private static OtrManager alice;
    private static OtrManager bob;

    private static PreKeys bobKeys;
    private static PreKeys aliceKeys;

    private final static String bobId = "bob";
    private final static String bobClientId = "bob_device";
    private final static String aliceId = "alice";
    private final static String aliceClientId = "alice_device";

    @Test
    public void testAliceToBob() throws CryptoException, IOException {
        String text = "Hello Bob, This is Alice!";
        byte[] textBytes = text.getBytes();

        OtrMessage msg = new OtrMessage(aliceClientId, textBytes);

        // Encrypt using prekeys
        alice.encrypt(bobKeys, msg);

        HashMap<String, byte[]> ciphers = msg.getRecipients().get(bobId);
        byte[] cipher = ciphers.get(bobClientId);

        String base64Encoded = Base64.getEncoder().encodeToString(cipher);
        System.out.printf("Alice -> (%s,%s) cipher: %s\n", bobId, bobClientId, base64Encoded);

        // Decrypt using initSessionFromMessage
        byte[] decrypt = bob.decrypt(aliceId, aliceClientId, base64Encoded);
        String text2 = new String(decrypt);

        boolean equals = Arrays.equals(decrypt, textBytes);
        assert equals;
        assert text.equals(text2);
    }

    @Test
    public void testBobToAlice() throws CryptoException, IOException {
        String text = "Hello Alice, This is Bob!";
        byte[] textBytes = text.getBytes();

        OtrMessage msg = new OtrMessage(bobClientId, textBytes);

        bob.encrypt(aliceKeys, msg);

        HashMap<String, byte[]> ciphers = msg.getRecipients().get(aliceId);
        byte[] cipher = ciphers.get(aliceClientId);

        String base64Encoded = Base64.getEncoder().encodeToString(cipher);
        System.out.printf("Bob -> (%s,%s) cipher: %s\n", aliceId, aliceClientId, base64Encoded);

        // Decrypt using initSessionFromMessage
        byte[] decrypt = alice.decrypt(bobId, bobClientId, base64Encoded);
        String text2 = new String(decrypt);

        boolean equals = Arrays.equals(decrypt, textBytes);
        assert equals;

        assert text.equals(text2);
    }

    @Test
    public void testSessions() throws CryptoException, IOException {
        String text = "Hello Alice, This is Bob, again!";
        byte[] textBytes = text.getBytes();

        OtrMessage msg = new OtrMessage(bobClientId, textBytes);

        Devices devices = new Devices();
        devices.add(aliceId, aliceClientId);
        bob.encrypt(devices, msg);

        HashMap<String, byte[]> ciphers = msg.getRecipients().get(aliceId);
        byte[] cipher = ciphers.get(aliceClientId);

        String base64Encoded = Base64.getEncoder().encodeToString(cipher);
        System.out.printf("Bob -> (%s,%s) cipher: %s\n", aliceId, aliceClientId, base64Encoded);

        // Decrypt using session
        byte[] decrypt = alice.decrypt(bobId, bobClientId, base64Encoded);
        String text2 = new String(decrypt);

        boolean equals = Arrays.equals(decrypt, textBytes);
        assert equals;

        assert text.equals(text2);
    }

    private static File mkTmpDir(String name) throws IOException {
        File tmpDir = File.createTempFile(name, "");
        tmpDir.delete();
        tmpDir.mkdir();
        return tmpDir;
    }

    @BeforeClass
    public static void setUp() throws CryptoException, IOException {
        File aliceDir = mkTmpDir("cryptobox-alice");
        alice = new OtrManager(aliceDir.getAbsolutePath());

        File bobDir = mkTmpDir("cryptobox-bob");
        bob = new OtrManager(bobDir.getAbsolutePath());

        PreKey[] preKeys = bob.newPreKeys(0, 1);
        bobKeys = getPreKeys(preKeys, bobClientId, bobId);

        preKeys = alice.newPreKeys(0, 1);
        aliceKeys = getPreKeys(preKeys, aliceClientId, aliceId);
    }

    private static PreKeys getPreKeys(PreKey[] preKeys, String clientId, String userId) {
        PreKeys bobKeys = new PreKeys();
        HashMap<String, com.wire.bots.sdk.models.otr.PreKey> devs = new HashMap<>();
        bobKeys.put(userId, devs);

        for (PreKey key : preKeys) {
            com.wire.bots.sdk.models.otr.PreKey k = new com.wire.bots.sdk.models.otr.PreKey();
            k.id = key.id;
            k.key = Base64.getEncoder().encodeToString(key.data);
            devs.put(clientId, k);

            System.out.printf("%s, %s, keyId: %s, prekey: %s\n", userId, clientId, k.id, k.key);
        }
        return bobKeys;
    }

    @AfterClass
    public static void clean() {
        alice.close();
        bob.close();
    }
}
