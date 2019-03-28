package com.wire.bots.sdk.user;

import com.google.protobuf.InvalidProtocolBufferException;
import com.waz.model.Messages;
import com.wire.bots.sdk.crypto.CryptoDatabase;
import com.wire.bots.sdk.helpers.MemStorage;
import com.wire.bots.sdk.models.otr.OtrMessage;
import com.wire.bots.sdk.server.model.NewBot;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Base64;

public class End2EndTest {
    private static CryptoDatabase aliceCrypto;
    private static CryptoDatabase bobCrypto1;
    private static CryptoDatabase bobCrypto2;

    private static NewBot aliceState;

    @BeforeClass
    public static void setUp() throws Exception {
        MemStorage storage = new MemStorage();

        String aliceId = "alice";
        String bobId = "bob";

        aliceCrypto = new CryptoDatabase(aliceId, storage);
        bobCrypto1 = new CryptoDatabase(bobId, storage);
        bobCrypto2 = new CryptoDatabase(bobId, storage);

        aliceState = new NewBot();
        aliceState.id = aliceId;
        aliceState.client = "alice";
    }

    @Test
    public void testAliceToBob() throws Exception {
        String bobId = "bob";
        String client1 = "bob1";
        String client2 = "bob2";

        DummyAPI api = new DummyAPI();
        api.addDevice(bobId, client1);
        api.addDevice(bobId, client2);

        api.addLastKey(bobId, client1, bobCrypto1.box().newLastPreKey());
        api.addLastKey(bobId, client2, bobCrypto2.box().newLastPreKey());

        UserClient aliceClient = new UserClient(aliceState, null, aliceCrypto, api);

        for (int i = 0; i < 10; i++) {
            String text = "Hello Bob, This is Alice!";
            aliceClient.sendText(text);

            OtrMessage msg = api.getMsg();

            String cipher1 = msg.get(bobId, client1);
            String decrypt = bobCrypto1.decrypt(bobId, client1, cipher1);
            String s1 = getText(decrypt);
            assert text.equals(s1);

            String cipher2 = msg.get(bobId, client2);
            String decrypt2 = bobCrypto2.decrypt(bobId, client2, cipher2);
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
