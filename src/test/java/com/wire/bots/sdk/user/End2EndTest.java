package com.wire.bots.sdk.user;

import com.google.protobuf.InvalidProtocolBufferException;
import com.waz.model.Messages;
import com.wire.bots.sdk.crypto.CryptoDatabase;
import com.wire.bots.sdk.crypto.storage.PgStorage;
import com.wire.bots.sdk.crypto.storage.RedisStorage;
import com.wire.bots.sdk.helpers.MemStorage;
import com.wire.bots.sdk.helpers.Util;
import com.wire.bots.sdk.models.otr.OtrMessage;
import com.wire.bots.sdk.server.model.NewBot;
import org.junit.AfterClass;
import org.junit.Test;

import java.io.IOException;
import java.util.Base64;
import java.util.Random;
import java.util.UUID;

public class End2EndTest {
    @AfterClass
    public static void clean() throws IOException {
        Util.deleteDir("data");
    }

    @Test
    public void testAliceToAlice() throws Exception {
        Random rnd = new Random();
        UUID aliceId = UUID.randomUUID();
        String client1 = "alice1_" + Math.abs(rnd.nextInt());

        NewBot state = new NewBot();
        state.id = aliceId;
        state.client = aliceId.toString();

        RedisStorage storage = new RedisStorage("localhost");

        CryptoDatabase aliceCrypto = new CryptoDatabase(aliceId, storage, "data/testAliceToAlice/1");
        CryptoDatabase aliceCrypto1 = new CryptoDatabase(aliceId, storage, "data/testAliceToAlice/2");

        DummyAPI api = new DummyAPI();
        api.addDevice(aliceId, client1, aliceCrypto1.box().newLastPreKey());

        UserClient aliceClient = new UserClient(state, null, aliceCrypto, api);

        for (int i = 0; i < 10; i++) {
            String text = "Hello Alice, This is Alice!";
            aliceClient.sendText(text);

            OtrMessage msg = api.getMsg();

            String cipher1 = msg.get(aliceId, client1);
            String decrypt = aliceCrypto1.decrypt(aliceId, msg.getSender(), cipher1);
            String s1 = getText(decrypt);
            assert text.equals(s1);
        }
    }

    @Test
    public void testAliceToBob() throws Exception {
        UUID bobId = UUID.randomUUID();
        UUID aliceId = UUID.randomUUID();
        String client1 = "bob1";

        NewBot state = new NewBot();
        state.id = aliceId;
        state.client = "alice1";

        MemStorage storage = new MemStorage();

        CryptoDatabase aliceCrypto = new CryptoDatabase(aliceId, storage, "data/testAliceToBob");
        CryptoDatabase bobCrypto = new CryptoDatabase(bobId, storage, "data/testAliceToBob");

        DummyAPI api = new DummyAPI();
        api.addDevice(bobId, client1, bobCrypto.box().newLastPreKey());

        UserClient aliceClient = new UserClient(state, null, aliceCrypto, api);

        for (int i = 0; i < 10; i++) {
            String text = "Hello Bob, This is Alice!";
            aliceClient.sendText(text);

            OtrMessage msg = api.getMsg();

            String cipher1 = msg.get(bobId, client1);
            String decrypt = bobCrypto.decrypt(aliceId, msg.getSender(), cipher1);
            String s1 = getText(decrypt);
            assert text.equals(s1);
        }
    }

    @Test
    public void testMultiDeviceRedis() throws Exception {
        Random rnd = new Random();
        UUID bobId = UUID.randomUUID();
        UUID aliceId = UUID.randomUUID();
        String client1 = "bob1_" + rnd.nextInt();
        String client2 = "bob2_" + rnd.nextInt();
        String client3 = "alice3_" + rnd.nextInt();
        String aliceCl = "alice_" + rnd.nextInt();

        NewBot state = new NewBot();
        state.id = aliceId;
        state.client = aliceCl;

        RedisStorage storage = new RedisStorage("localhost");

        CryptoDatabase aliceCrypto1 = new CryptoDatabase(aliceId, storage, "data/testMultiDevice/alice/1");
        CryptoDatabase bobCrypto1 = new CryptoDatabase(bobId, storage, "data/testMultiDevice/bob/1");
        CryptoDatabase bobCrypto2 = new CryptoDatabase(bobId, storage, "data/testMultiDevice/bob/2");

        DummyAPI api = new DummyAPI();
        api.addDevice(bobId, client1, bobCrypto1.box().newLastPreKey());
        api.addDevice(bobId, client2, bobCrypto2.box().newLastPreKey());
        api.addDevice(aliceId, client3, aliceCrypto1.box().newLastPreKey());

        CryptoDatabase aliceCrypto = new CryptoDatabase(aliceId, storage, "data/testMultiDevice/alice");

        UserClient aliceClient = new UserClient(state, null, aliceCrypto, api);

        for (int i = 0; i < 10; i++) {
            String text = "Hello Bob, This is Alice!";
            aliceClient.sendText(text);

            OtrMessage msg = api.getMsg();
            String sender = msg.getSender();

            String cipher1 = msg.get(bobId, client1);
            String decrypt = bobCrypto1.decrypt(aliceId, sender, cipher1);
            String s1 = getText(decrypt);
            assert text.equals(s1);

            String cipher2 = msg.get(bobId, client2);
            String decrypt2 = bobCrypto2.decrypt(aliceId, sender, cipher2);
            String s2 = getText(decrypt2);
            assert text.equals(s2);

            String cipher3 = msg.get(aliceId, client3);
            String decrypt3 = aliceCrypto1.decrypt(aliceId, sender, cipher3);
            String s3 = getText(decrypt3);
            assert text.equals(s3);
        }
    }

    @Test
    public void testMultiDevicePostgres() throws Exception {
        Random rnd = new Random();
        UUID bobId = UUID.randomUUID();
        UUID aliceId = UUID.randomUUID();
        String client1 = "bob1_" + rnd.nextInt();
        String client2 = "bob2_" + rnd.nextInt();
        String client3 = "alice3_" + rnd.nextInt();
        String aliceCl = "alice_" + rnd.nextInt();

        NewBot state = new NewBot();
        state.id = aliceId;
        state.client = aliceCl;

        PgStorage storage = new PgStorage();

        CryptoDatabase aliceCrypto1 = new CryptoDatabase(aliceId, storage, "data/testMultiDevicePostgres/alice/1");
        CryptoDatabase bobCrypto1 = new CryptoDatabase(bobId, storage, "data/testMultiDevicePostgres/bob/1");
        CryptoDatabase bobCrypto2 = new CryptoDatabase(bobId, storage, "data/testMultiDevicePostgres/bob/2");

        DummyAPI api = new DummyAPI();
        api.addDevice(bobId, client1, bobCrypto1.box().newLastPreKey());
        api.addDevice(bobId, client2, bobCrypto2.box().newLastPreKey());
        api.addDevice(aliceId, client3, aliceCrypto1.box().newLastPreKey());

        CryptoDatabase aliceCrypto = new CryptoDatabase(aliceId, storage, "data/testMultiDevicePostgres/alice");

        UserClient aliceClient = new UserClient(state, null, aliceCrypto, api);

        for (int i = 0; i < 10; i++) {
            String text = "Hello Bob, This is Alice!";
            aliceClient.sendText(text);

            OtrMessage msg = api.getMsg();
            String sender = msg.getSender();

            String cipher1 = msg.get(bobId, client1);
            String decrypt = bobCrypto1.decrypt(aliceId, sender, cipher1);
            String s1 = getText(decrypt);
            assert text.equals(s1);

            String cipher2 = msg.get(bobId, client2);
            String decrypt2 = bobCrypto2.decrypt(aliceId, sender, cipher2);
            String s2 = getText(decrypt2);
            assert text.equals(s2);

            String cipher3 = msg.get(aliceId, client3);
            String decrypt3 = aliceCrypto1.decrypt(aliceId, sender, cipher3);
            String s3 = getText(decrypt3);
            assert text.equals(s3);
        }
    }

    private String getText(String decrypt) throws InvalidProtocolBufferException {
        byte[] decoded = Base64.getDecoder().decode(decrypt);
        Messages.GenericMessage genericMessage = Messages.GenericMessage.parseFrom(decoded);
        return genericMessage.getText().getContent();
    }
}
