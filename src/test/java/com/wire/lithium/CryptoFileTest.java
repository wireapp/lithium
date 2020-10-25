package com.wire.lithium;//
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

import com.wire.xenon.crypto.CryptoFile;
import com.wire.xenon.models.otr.Missing;
import com.wire.xenon.models.otr.PreKey;
import com.wire.xenon.models.otr.PreKeys;
import com.wire.xenon.models.otr.Recipients;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class CryptoFileTest {

    private final static UUID bobId;
    private final static UUID aliceId;
    private final static String DATA = "data";
    private static CryptoFile alice;
    private static CryptoFile bob;
    private static PreKeys bobKeys;
    private static PreKeys aliceKeys;

    static {
        aliceId = UUID.randomUUID();
        bobId = UUID.randomUUID();
    }
    
    @BeforeClass
    public static void setUp() throws Exception {
        alice = new CryptoFile(DATA, aliceId);
        bob = new CryptoFile(DATA, bobId);

        ArrayList<PreKey> preKeys = bob.newPreKeys(0, 1);
        bobKeys = new PreKeys(preKeys, "bob", bobId);

        preKeys = alice.newPreKeys(0, 1);
        aliceKeys = new PreKeys(preKeys, "alice", aliceId);
    }

    @AfterClass
    public static void clean() throws IOException {
        alice.close();
        bob.close();
        Path rootPath = Paths.get(DATA);
        Files.walk(rootPath, FileVisitOption.FOLLOW_LINKS)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }

    @Test
    public void testAliceToBob() throws Exception {
        String text = "Hello Bob, This is Alice!";
        byte[] textBytes = text.getBytes();

        // Encrypt using prekeys
        Recipients encrypt = alice.encrypt(bobKeys, textBytes);

        String base64Encoded = encrypt.get(bobId, "bob");

        // Decrypt using initSessionFromMessage
        String decrypt = bob.decrypt(aliceId, "alice", base64Encoded);
        byte[] decode = Base64.getDecoder().decode(decrypt);

        assert Arrays.equals(decode, textBytes);
        assert text.equals(new String(decode));
    }

    @Test
    public void testBobToAlice() throws Exception {
        String text = "Hello Alice, This is Bob!";
        byte[] textBytes = text.getBytes();

        Recipients encrypt = bob.encrypt(aliceKeys, textBytes);

        String base64Encoded = encrypt.get(aliceId, "alice");

        // Decrypt using initSessionFromMessage
        String decrypt = alice.decrypt(bobId, "bob", base64Encoded);
        byte[] decode = Base64.getDecoder().decode(decrypt);

        assert Arrays.equals(decode, textBytes);
        assert text.equals(new String(decode));
    }

    @Test
    public void testSessions() throws Exception {
        String text = "Hello Alice, This is Bob, again!";
        byte[] textBytes = text.getBytes();

        Missing devices = new Missing();
        devices.add(aliceId, "alice");
        Recipients encrypt = bob.encrypt(devices, textBytes);

        String base64Encoded = encrypt.get(aliceId, "alice");

        // Decrypt using session
        String decrypt = alice.decrypt(bobId, "bob", base64Encoded);
        byte[] decode = Base64.getDecoder().decode(decrypt);

        assert Arrays.equals(decode, textBytes);
        assert text.equals(new String(decode));
    }
}
