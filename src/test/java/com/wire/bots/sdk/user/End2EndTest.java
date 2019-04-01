package com.wire.bots.sdk.user;

import com.google.protobuf.InvalidProtocolBufferException;
import com.waz.model.Messages;
import com.wire.bots.sdk.crypto.CryptoDatabase;
import com.wire.bots.sdk.helpers.MemStorage;
import com.wire.bots.sdk.helpers.Util;
import com.wire.bots.sdk.models.otr.OtrMessage;
import com.wire.bots.sdk.server.model.NewBot;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.Base64;

public class End2EndTest {
    private static CryptoDatabase aliceCrypto;
    private static CryptoDatabase bobCrypto1;
    private static CryptoDatabase bobCrypto2;

    private static String aliceId = "alice";
    private static String alice = "alice1";
    private static String bobId = "bob";

    @BeforeClass
    public static void setUp() throws Exception {
        MemStorage storage = new MemStorage();

        aliceCrypto = new CryptoDatabase(aliceId, storage);
        bobCrypto1 = new CryptoDatabase(bobId, storage);
        bobCrypto2 = new CryptoDatabase(bobId, storage);
    }

    @AfterClass
    public static void clean() throws IOException {
        aliceCrypto.close();
        bobCrypto1.close();
        bobCrypto2.close();

        Util.deleteDir("data");
    }

    @Test
    public void testAliceToBob() throws Exception {
        String bobId = "bob";
        String client1 = "bob1";
        String client2 = "bob2";

        NewBot state = new NewBot();
        state.id = aliceId;
        state.client = alice;

        DummyAPI api = new DummyAPI();
        api.addDevice(bobId, client1);
        api.addDevice(bobId, client2);

        api.addLastKey(bobId, client1, bobCrypto1.box().newLastPreKey());
        api.addLastKey(bobId, client2, bobCrypto2.box().newLastPreKey());

        UserClient aliceClient = new UserClient(state, null, aliceCrypto, api);

        for (int i = 0; i < 10; i++) {
            String text = "Hello Bob, This is Alice!";
            aliceClient.sendText(text);

            OtrMessage msg = api.getMsg();

            String cipher1 = msg.get(bobId, client1);
            String decrypt = bobCrypto1.decrypt(aliceId, msg.getSender(), cipher1);
            String s1 = getText(decrypt);
            assert text.equals(s1);

            String cipher2 = msg.get(bobId, client2);
            String decrypt2 = bobCrypto2.decrypt(aliceId, msg.getSender(), cipher2);
            String s2 = getText(decrypt2);
            assert text.equals(s2);
        }
    }

    private String getText(String decrypt) throws InvalidProtocolBufferException {
        byte[] decoded = Base64.getDecoder().decode(decrypt);
        Messages.GenericMessage genericMessage = Messages.GenericMessage.parseFrom(decoded);
        return genericMessage.getText().getContent();
    }
}
