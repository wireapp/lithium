package com.wire.bots.sdk;

import com.wire.bots.cryptobox.IRecord;
import com.wire.bots.cryptobox.PreKey;
import com.wire.bots.cryptobox.StorageException;
import com.wire.bots.sdk.crypto.storage.RedisStorage;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public class RedisStorageTest {
    @Test
    public void testFetchSession() throws StorageException {
        RedisStorage storage = new RedisStorage("localhost");
        Random random = new Random();
        String id = "" + random.nextInt();
        String sid = "" + random.nextInt();

        IRecord record = storage.fetchSession(id, sid);
        assert record.getData() == null;

        byte[] data = new byte[1024];
        random.nextBytes(data);

        record.persist(data);

        record = storage.fetchSession(id, sid);
        assert record.getData() != null;
        assert Arrays.equals(data, record.getData());

        record.persist(data);
    }

    @Test
    public void testFetchIdentity() {
        RedisStorage storage = new RedisStorage("localhost");
        Random random = new Random();
        String id = "" + random.nextInt();

        byte[] identity = storage.fetchIdentity(id);
        assert identity == null;

        identity = new byte[1024];
        random.nextBytes(identity);

        storage.insertIdentity(id, identity);

        byte[] control = storage.fetchIdentity(id);
        assert control != null;
        assert Arrays.equals(identity, control);
    }

    @Test
    public void testFetchLastPrekey() {
        RedisStorage storage = new RedisStorage("localhost");
        Random random = new Random();
        String id = "" + random.nextInt();

        PreKey[] preKeys = storage.fetchPrekeys(id);
        assert preKeys == null;

        byte[] data = new byte[1024];
        random.nextBytes(data);
        PreKey preKey = new PreKey(0xFFFF, data);

        storage.insertPrekey(id, preKey.id, preKey.data);

        PreKey[] control = storage.fetchPrekeys(id);

        assert control != null;
        assert control.length == 1;

        PreKey controlKey = control[0];

        assert preKey.id == controlKey.id;
        assert Arrays.equals(preKey.data, controlKey.data);
    }

    @Test
    public void testFetchPrekeys() {
        int SIZE = 10;
        RedisStorage storage = new RedisStorage("localhost");
        Random random = new Random();
        String id = "" + random.nextInt();

        PreKey[] preKeys = storage.fetchPrekeys(id);
        assert preKeys == null;

        ArrayList<PreKey> prekeys = new ArrayList<>();
        for (int i = 0; i < SIZE; i++) {
            byte[] data = new byte[1024];
            random.nextBytes(data);
            PreKey preKey = new PreKey(i, data);
            prekeys.add(preKey);

            storage.insertPrekey(id, preKey.id, preKey.data);
        }

        PreKey[] control = storage.fetchPrekeys(id);

        assert control != null;
        assert control.length == SIZE;
        for (int i = 0; i < SIZE; i++) {
            PreKey preKey = prekeys.get(i);
            PreKey controlKey = control[SIZE - i - 1];//redis storage returns keys in reverse order

            assert preKey.id == controlKey.id;
            assert Arrays.equals(preKey.data, controlKey.data);
        }
    }
}
