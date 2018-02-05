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

import com.wire.bots.sdk.crypto.Crypto;
import com.wire.bots.sdk.crypto.CryptoFile;
import com.wire.bots.sdk.models.otr.Missing;
import com.wire.bots.sdk.models.otr.PreKey;
import com.wire.bots.sdk.models.otr.PreKeys;
import com.wire.bots.sdk.models.otr.Recipients;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;

public class CryptoFileTest {

    private final static String bobId = "bob";
    private final static String bobClientId = "bob_device";
    private final static String aliceId = "alice";
    private final static String aliceClientId = "alice_device";
    private final static String DATA = "./data";
    private static Crypto alice;
    private static Crypto bob;
    private static PreKeys bobKeys;
    private static PreKeys aliceKeys;

    @BeforeClass
    public static void setUp() throws Exception {
        alice = new CryptoFile(DATA, aliceId);
        bob = new CryptoFile(DATA, bobId);

        ArrayList<PreKey> preKeys = bob.newPreKeys(0, 1);
        bobKeys = getPreKeys(preKeys, bobClientId, bobId);

        preKeys = alice.newPreKeys(0, 1);
        aliceKeys = getPreKeys(preKeys, aliceClientId, aliceId);
    }

    private static File mkTmpDir(String name) throws IOException {
        File tmpDir = File.createTempFile(name, "");
        tmpDir.delete();
        tmpDir.mkdir();
        return tmpDir;
    }

    private static PreKeys getPreKeys(ArrayList<PreKey> array, String clientId, String userId) {
        HashMap<String, PreKey> devs = new HashMap<>();
        for (PreKey key : array) {
            devs.put(clientId, key);
        }

        PreKeys keys = new PreKeys();
        keys.put(userId, devs);
        return keys;
    }

    @AfterClass
    public static void clean() throws IOException {
        alice.close();
        bob.close();
    }

    @Test
    public void testAliceToBob() throws Exception {
        String text = "Hello Bob, This is Alice!";
        byte[] textBytes = text.getBytes();

        // Encrypt using prekeys
        Recipients encrypt = alice.encrypt(bobKeys, textBytes);

        String base64Encoded = encrypt.get(bobId, bobClientId);

        // Decrypt using initSessionFromMessage
        String decrypt = bob.decrypt(aliceId, aliceClientId, base64Encoded);
        byte[] decode = Base64.getDecoder().decode(decrypt);

        assert Arrays.equals(decode, textBytes);
        assert text.equals(new String(decode));
    }

    @Test
    public void testBobToAlice() throws Exception {
        String text = "Hello Alice, This is Bob!";
        byte[] textBytes = text.getBytes();

        Recipients encrypt = bob.encrypt(aliceKeys, textBytes);

        String base64Encoded = encrypt.get(aliceId, aliceClientId);

        // Decrypt using initSessionFromMessage
        String decrypt = alice.decrypt(bobId, bobClientId, base64Encoded);
        byte[] decode = Base64.getDecoder().decode(decrypt);

        assert Arrays.equals(decode, textBytes);
        assert text.equals(new String(decode));
    }

    @Test
    public void testSessions() throws Exception {
        String text = "Hello Alice, This is Bob, again!";
        byte[] textBytes = text.getBytes();

        Missing devices = new Missing();
        devices.add(aliceId, aliceClientId);
        Recipients encrypt = bob.encrypt(devices, textBytes);

        String base64Encoded = encrypt.get(aliceId, aliceClientId);

        // Decrypt using session
        String decrypt = alice.decrypt(bobId, bobClientId, base64Encoded);
        byte[] decode = Base64.getDecoder().decode(decrypt);

        assert Arrays.equals(decode, textBytes);
        assert text.equals(new String(decode));
    }
}
