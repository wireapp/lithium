package com.wire.bots.sdk;

import com.codahale.metrics.MetricRegistry;
import com.wire.bots.cryptobox.IRecord;
import com.wire.bots.cryptobox.PreKey;
import com.wire.xenon.crypto.storage.JdbiStorage;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.db.ManagedDataSource;
import org.flywaydb.core.Flyway;
import org.junit.Before;
import org.junit.Test;
import org.skife.jdbi.v2.DBI;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public class PostgresCryptoStorageTest {
    private DBI jdbi;

    @Before
    public void init() {
        DataSourceFactory dataSourceFactory = new DataSourceFactory();
        dataSourceFactory.setDriverClass("org.postgresql.Driver");
        dataSourceFactory.setUrl("jdbc:postgresql://localhost/lithium");
        dataSourceFactory.setUser("dejankovacevic");

        // Migrate DB if needed
        Flyway flyway = Flyway
                .configure()
                .dataSource(dataSourceFactory.getUrl(), dataSourceFactory.getUser(), dataSourceFactory.getPassword())
                .baselineOnMigrate(true)
                .load();
        flyway.migrate();

        ManagedDataSource dataSource = dataSourceFactory.build(new MetricRegistry(), "PostgresCryptoStorageTest");

        jdbi = new DBI(dataSource);
    }

    @Test
    public void testFetchSession() {
        JdbiStorage storage = new JdbiStorage(jdbi);

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
        JdbiStorage storage = new JdbiStorage(jdbi);

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
        JdbiStorage storage = new JdbiStorage(jdbi);

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
        JdbiStorage storage = new JdbiStorage(jdbi);

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
            PreKey controlKey = control[i];

            assert preKey.id == controlKey.id;
            assert Arrays.equals(preKey.data, controlKey.data);
        }
    }

    @Test
    public void testPurge() {
        JdbiStorage storage = new JdbiStorage(jdbi);

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
        assert identity == null;

        PreKey[] preKeys = storage.fetchPrekeys(id);
        assert preKeys == null;

        record = storage.fetchSession(id, sid);
        assert record.getData() == null;
    }
}
