package com.wire.lithium;
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

import com.wire.lithium.helpers.Util;
import com.wire.xenon.crypto.CryptoFile;
import com.wire.xenon.models.otr.Missing;
import com.wire.xenon.models.otr.PreKey;
import com.wire.xenon.models.otr.PreKeys;
import com.wire.xenon.models.otr.Recipients;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.UUID;

public class CryptoFileTest {

    private UUID bobId;
    private String bobClientId;
    private UUID aliceId;
    private String aliceClientId;

    private String rootFolder;
    private CryptoFile alice;
    private CryptoFile bob;
    private PreKeys bobKeys;
    private PreKeys aliceKeys;

    @BeforeEach
    public void setUp() throws Exception {
        rootFolder = "lithium-test-data-" + UUID.randomUUID();

        aliceId = UUID.randomUUID();
        aliceClientId = aliceClientId + "-client";
        bobId = UUID.randomUUID();
        bobClientId = bobId + "-client";

        alice = new CryptoFile(rootFolder, aliceId);
        bob = new CryptoFile(rootFolder, bobId);

        ArrayList<PreKey> preKeys = bob.newPreKeys(0, 1);
        bobKeys = new PreKeys(preKeys, bobClientId, bobId);

        preKeys = alice.newPreKeys(0, 1);
        aliceKeys = new PreKeys(preKeys, aliceClientId, aliceId);
    }

    @AfterEach
    public void clean() throws IOException {
        alice.close();
        bob.close();
        Util.deleteDir(rootFolder);
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

        Assertions.assertArrayEquals(decode, textBytes);
        Assertions.assertEquals(text, new String(decode));
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

        Assertions.assertArrayEquals(decode, textBytes);
        Assertions.assertEquals(text, new String(decode));
    }

    @Test
    public void testSessions() throws Exception {
        String text = "Hello Alice, This is Bob, again!";
        byte[] textBytes = text.getBytes();

        Missing devices = new Missing();
        devices.add(aliceId, aliceClientId);

        // use keys first
        Recipients encrypt = bob.encrypt(aliceKeys, textBytes);

        String base64Encoded = encrypt.get(aliceId, aliceClientId);

        // Decrypt using initSessionFromMessage
        String decrypt = alice.decrypt(bobId, bobClientId, base64Encoded);
        byte[] decode = Base64.getDecoder().decode(decrypt);

        Assertions.assertArrayEquals(decode, textBytes);
        Assertions.assertEquals(text, new String(decode));

        // and then session
        text += " from session this time!";
        textBytes = text.getBytes();
        encrypt = bob.encrypt(devices, textBytes);

        base64Encoded = encrypt.get(aliceId, aliceClientId);

        // Decrypt using session
        decrypt = alice.decrypt(bobId, bobClientId, base64Encoded);
        decode = Base64.getDecoder().decode(decrypt);

        Assertions.assertArrayEquals(decode, textBytes);
        Assertions.assertEquals(text, new String(decode));
    }
}
