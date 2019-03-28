package com.wire.bots.sdk.user;

import com.wire.bots.sdk.crypto.CryptoDatabase;
import com.wire.bots.sdk.helpers.MemStorage;
import com.wire.bots.sdk.helpers.Util;
import com.wire.bots.sdk.models.otr.OtrMessage;
import com.wire.bots.sdk.server.model.NewBot;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;

public class End2EndTest {
    private static String bobId;
    private static String aliceId;
    private static CryptoDatabase aliceCrypto;
    private static CryptoDatabase bobCrypto;
    private static NewBot bobState;
    private static NewBot aliceState;

    @BeforeClass
    public static void setUp() throws Exception {
        MemStorage storage = new MemStorage();

        aliceId = "alice";
        bobId = "bob";

        aliceCrypto = new CryptoDatabase(aliceId, storage);
        bobCrypto = new CryptoDatabase(bobId, storage);

        bobState = new NewBot();
        bobState.id = bobId;
        bobState.client = "bob";

        aliceState = new NewBot();
        aliceState.id = aliceId;
        aliceState.client = "alice";
    }

    @AfterClass
    public static void clean() throws IOException {
        aliceCrypto.box().close();
        bobCrypto.box().close();

        Util.deleteDir("data");
    }

    @Test
    public void testAliceToBob() throws Exception {
        String client1 = "bob1";
        String client2 = "bob2";
        String client3 = "alice3";

        DummyAPI api = new DummyAPI();
        api.addDevice(bobId, client1);
        api.addDevice(bobId, client2);
        api.addDevice(aliceId, client3);
        api.addLastKey(aliceId, aliceCrypto.box().newLastPreKey());
        api.addLastKey(bobId, bobCrypto.box().newLastPreKey());

        UserClient bobClient = new UserClient(bobState, null, bobCrypto, api);
        UserClient aliceClient = new UserClient(aliceState, null, aliceCrypto, api);

        for (int i = 0; i < 10; i++) {
            String text = "Hello Bob, This is Alice!";
            aliceClient.sendText(text);

            OtrMessage msg = api.getMsg();

            String cipher1 = msg.get(bobId, client1);
            bobClient.decrypt(bobId, client1, cipher1);
            
            String cipher2 = msg.get(bobId, client2);
            bobClient.decrypt(bobId, client2, cipher2);

            String cipher3 = msg.get(aliceId, client3);
            bobClient.decrypt(aliceId, client3, cipher3);
        }
    }
}
