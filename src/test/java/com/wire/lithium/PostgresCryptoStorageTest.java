package com.wire.lithium;

import com.wire.bots.cryptobox.IRecord;
import com.wire.bots.cryptobox.PreKey;
import com.wire.xenon.crypto.storage.JdbiStorage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Random;

public class PostgresCryptoStorageTest extends DatabaseTestBase {

    private JdbiStorage storage;

    @BeforeEach
    public void setUp() {
        flyway.migrate();
        storage = new JdbiStorage(jdbi);
    }

    @AfterEach
    public void clean() {
        flyway.clean();
    }

    @Test
    public void testFetchSession() {
        Random random = new Random();
        String id = "" + random.nextInt();
        String sid = "" + random.nextInt();

        IRecord record = storage.fetchSession(id, sid);
        Assertions.assertNull(record.getData());

        byte[] data = new byte[1024];
        random.nextBytes(data);

        record.persist(data);

        record = storage.fetchSession(id, sid);
        Assertions.assertNotNull(record.getData());
        Assertions.assertArrayEquals(data, record.getData());

        record.persist(data);
    }

    @Test
    public void testFetchIdentity() {
        Random random = new Random();
        String id = "" + random.nextInt();

        byte[] identity = storage.fetchIdentity(id);
        Assertions.assertNull(identity);

        identity = new byte[1024];
        random.nextBytes(identity);

        storage.insertIdentity(id, identity);

        byte[] control = storage.fetchIdentity(id);
        Assertions.assertNotNull(control);
        Assertions.assertArrayEquals(identity, control);
    }

    @Test
    public void testFetchLastPrekey() {
        Random random = new Random();
        String id = "" + random.nextInt();

        PreKey[] preKeys = storage.fetchPrekeys(id);
        Assertions.assertNull(preKeys);

        byte[] data = new byte[1024];
        random.nextBytes(data);
        PreKey preKey = new PreKey(0xFFFF, data);

        storage.insertPrekey(id, preKey.id, preKey.data);

        PreKey[] control = storage.fetchPrekeys(id);

        Assertions.assertNotNull(control);
        Assertions.assertEquals(1, control.length);

        PreKey controlKey = control[0];

        Assertions.assertEquals(preKey.id, controlKey.id);
        Assertions.assertArrayEquals(preKey.data, controlKey.data);
    }

    @Test
    public void testFetchPrekeys() {
        int SIZE = 10;
        Random random = new Random();
        String id = "" + random.nextInt();

        PreKey[] preKeys = storage.fetchPrekeys(id);
        Assertions.assertNull(preKeys);

        ArrayList<PreKey> prekeys = new ArrayList<>();
        for (int i = 0; i < SIZE; i++) {
            byte[] data = new byte[1024];
            random.nextBytes(data);
            PreKey preKey = new PreKey(i, data);
            prekeys.add(preKey);

            storage.insertPrekey(id, preKey.id, preKey.data);
        }

        PreKey[] control = storage.fetchPrekeys(id);

        Assertions.assertNotNull(control);
        Assertions.assertEquals(SIZE, control.length);
        for (int i = 0; i < SIZE; i++) {
            PreKey preKey = prekeys.get(i);
            PreKey controlKey = control[i];

            Assertions.assertEquals(preKey.id, controlKey.id);
            Assertions.assertArrayEquals(preKey.data, controlKey.data);
        }
    }

    @Test
    public void testPurge() {
        Random random = new Random();
        String id = "" + random.nextInt();

        //Identity
        byte[] data = new byte[1024];
        random.nextBytes(data);
        storage.insertIdentity(id, data);

        //Prekeys
        random.nextBytes(data);
        PreKey preKey = new PreKey(0xFFFF, data);
        storage.insertPrekey(id, preKey.id, preKey.data);

        //Session
        String sid = "" + random.nextInt();
        IRecord record = storage.fetchSession(id, sid);
        random.nextBytes(data);
        record.persist(data);

        storage.purge(id);

        byte[] identity = storage.fetchIdentity(id);
        Assertions.assertNull(identity);

        PreKey[] preKeys = storage.fetchPrekeys(id);
        Assertions.assertNull(preKeys);

        record = storage.fetchSession(id, sid);
        Assertions.assertNull(record.getData());
    }
}
