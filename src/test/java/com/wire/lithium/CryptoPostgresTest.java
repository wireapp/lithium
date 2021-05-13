package com.wire.lithium;

import com.wire.bots.cryptobox.CryptoBox;
import com.wire.bots.cryptobox.CryptoDb;
import com.wire.bots.cryptobox.IStorage;
import com.wire.bots.cryptobox.PreKey;
import com.wire.lithium.helpers.Util;
import com.wire.xenon.crypto.storage.JdbiStorage;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class CryptoPostgresTest extends DatabaseTestBase {
    private String rootFolder;
    private String bobId;
    private String aliceId;
    private CryptoDb alice;
    private CryptoDb bob;
    private PreKey[] bobKeys;
    private PreKey[] aliceKeys;
    private IStorage storage;

    @BeforeEach
    public void setUp() throws Exception {
        rootFolder = "lithium-crypto-test-" + UUID.randomUUID();
        flyway.migrate();
        storage = new JdbiStorage(jdbi);

        aliceId = UUID.randomUUID().toString();
        bobId = UUID.randomUUID().toString();

        alice = new CryptoDb(aliceId, storage, rootFolder);
        bob = new CryptoDb(bobId, storage, rootFolder);

        bobKeys = bob.newPreKeys(0, 1);
        aliceKeys = alice.newPreKeys(0, 1);
    }

    @AfterEach
    public void clean() throws IOException {
        alice.close();
        bob.close();
        Util.deleteDir(rootFolder);
        flyway.clean();
    }

    @Test
    public void testAliceToBob() throws Exception {
        String text = "Hello Bob, This is Alice!";

        // Encrypt using prekeys
        byte[] cipher = alice.encryptFromPreKeys(bobId, bobKeys[0], text.getBytes());

        // Decrypt using initSessionFromMessage
        byte[] decrypt = bob.decrypt(aliceId, cipher);

        Assertions.assertArrayEquals(decrypt, text.getBytes());
        Assertions.assertEquals(text, new String(decrypt));
    }

    @Test
    public void testBobToAlice() throws Exception {
        String text = "Hello Alice, This is Bob!";

        byte[] cipher = bob.encryptFromPreKeys(aliceId, aliceKeys[0], text.getBytes());

        // Decrypt using initSessionFromMessage
        byte[] decrypt = alice.decrypt(bobId, cipher);

        Assertions.assertArrayEquals(decrypt, text.getBytes());
        Assertions.assertEquals(text, new String(decrypt));
    }

    @Test
    public void testSessions() throws Exception {
        String text = "Hello Alice, This is Bob!";

        byte[] cipher = bob.encryptFromPreKeys(aliceId, aliceKeys[0], text.getBytes());

        // Decrypt using initSessionFromMessage
        byte[] decrypt = alice.decrypt(bobId, cipher);

        Assertions.assertArrayEquals(decrypt, text.getBytes());
        Assertions.assertEquals(text, new String(decrypt));

        // and then from session
        text += " From session this time!";

        cipher = bob.encryptFromSession(aliceId, text.getBytes());

        // Decrypt using session
        decrypt = alice.decrypt(bobId, cipher);

        Assertions.assertArrayEquals(decrypt, text.getBytes());
        Assertions.assertEquals(text, new String(decrypt));
    }

    @Test
    public void testIdentity() throws Exception {
        final String carlId = UUID.randomUUID().toString();
        final String dir = rootFolder + "/" + carlId;

        CryptoDb carl = new CryptoDb(carlId, storage);
        PreKey[] carlPrekeys = carl.newPreKeys(0, 8);

        var daveId = UUID.randomUUID().toString();
        var davePath = String.format("%s/%s", rootFolder, daveId);
        var dave = CryptoBox.open(davePath);
        var davePrekeys = dave.newPreKeys(0, 8);

        String text = "Hello Bob, This is Carl!";

        // Encrypt using prekeys
        byte[] cipher = dave.encryptFromPreKeys(carlId, carlPrekeys[0], text.getBytes());
        byte[] decrypt = carl.decrypt(daveId, cipher);
        Assertions.assertArrayEquals(decrypt, text.getBytes());
        Assertions.assertEquals(text, new String(decrypt));

        carl.close();
        Util.deleteDir(dir);

        cipher = dave.encryptFromSession(carlId, text.getBytes());
        carl = new CryptoDb(carlId, storage);
        decrypt = carl.decrypt(daveId, cipher);

        Assertions.assertArrayEquals(decrypt, text.getBytes());
        Assertions.assertEquals(text, new String(decrypt));

        carl.close();
        Util.deleteDir(dir);

        carl = new CryptoDb(carlId, storage);

        cipher = carl.encryptFromPreKeys(daveId, davePrekeys[0], text.getBytes());
        decrypt = dave.decrypt(carlId, cipher);
        Assertions.assertArrayEquals(decrypt, text.getBytes());
        Assertions.assertEquals(text, new String(decrypt));

        carl.close();
    }

    @Test
    public void testSynchronousSingleSession() throws Exception {
        // initialize test with first prekeys
        String text = "Hello Alice, This is Bob!";

        byte[] cipher = bob.encryptFromPreKeys(aliceId, aliceKeys[0], text.getBytes());

        // Decrypt using initSessionFromMessage
        byte[] decrypt = alice.decrypt(bobId, cipher);

        Assertions.assertArrayEquals(decrypt, text.getBytes());
        Assertions.assertEquals(text, new String(decrypt));

        // and then run sessions tests
        Date s = new Date();
        for (int i = 0; i < 100; i++) {
            text = "Hello Alice, This is Bob, again! " + i;

            cipher = bob.encryptFromSession(aliceId, text.getBytes());

            // Decrypt using session
            decrypt = alice.decrypt(bobId, cipher);

            Assertions.assertArrayEquals(decrypt, text.getBytes());
            Assertions.assertEquals(text, new String(decrypt));

            text = "Hey Bob, How's life? " + i;

            cipher = alice.encryptFromSession(bobId, text.getBytes());

            // Decrypt using session
            decrypt = bob.decrypt(aliceId, cipher);

            Assertions.assertArrayEquals(decrypt, text.getBytes());
            Assertions.assertEquals(text, new String(decrypt));
        }
        Date e = new Date();
        long delta = e.getTime() - s.getTime();

        System.out.printf("Count: %,d,  Elapsed: %,d ms\n", 100, delta);
    }

    @Test
    // TODO fix me
    @Disabled("This test fails with more executors then 1")
    public void testConcurrentSingleSession() throws Exception {
        final String text = "Hello Alice, This is Bob, again! ";

        var cipher = bob.encryptFromPreKeys(aliceId, aliceKeys[0], text.getBytes());
        var decrypt = alice.decrypt(bobId, cipher);
        Assertions.assertArrayEquals(text.getBytes(), decrypt);

        ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(4);
        final AtomicInteger counter = new AtomicInteger(0);

        var testFailed = new AtomicBoolean(false);
        for (int i = 0; i < 100; i++) {
            executor.execute(() -> {
                try {
                    bob.encryptFromSession(aliceId, text.getBytes());
                    counter.getAndIncrement();
                } catch (Exception e) {
                    System.out.println("testConcurrentSessions: " + e);
                    e.printStackTrace();
                    testFailed.set(true);
                }
            });
        }
        Date s = new Date();
        executor.shutdown();
        // we don't care if it has to shut it down or not
        //noinspection ResultOfMethodCallIgnored
        executor.awaitTermination(60, TimeUnit.SECONDS);
        Date e = new Date();
        long delta = e.getTime() - s.getTime();

        System.out.printf("Count: %,d,  Elapsed: %,d ms\n", counter.get(), delta);
        if (testFailed.get()) {
            Assertions.fail("See logs");
        }
    }

    @Test
    public void testConcurrentMultipleSessions() throws Exception {
        final var count = 100;
        var aliceId = UUID.randomUUID().toString();
        CryptoDb alice = new CryptoDb(aliceId, storage);
        PreKey[] aliceKeys = alice.newPreKeys(0, count);

        final AtomicInteger counter = new AtomicInteger(0);
        byte[] bytes = ("Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello " +
                "Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello " +
                "Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello " +
                "Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello ").getBytes();


        var boxes = new ArrayList<CryptoDb>();

        for (int i = 0; i < count; i++) {
            String bobId = UUID.randomUUID().toString();
            CryptoDb bob = new CryptoDb(bobId, storage);
            bob.encryptFromPreKeys(aliceId, aliceKeys[i], bytes);
            boxes.add(bob);
        }

        ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(24);
        Date s = new Date();
        var testFailed = new AtomicBoolean(false);
        for (CryptoDb bob : boxes) {
            executor.execute(() -> {
                try {
                    bob.encryptFromSession(aliceId, bytes);
                    counter.getAndIncrement();
                } catch (Exception e) {
                    System.out.println("testConcurrentDifferentCBSessions: " + e);
                    e.printStackTrace();
                    testFailed.set(true);
                }
            });
        }

        executor.shutdown();
        // we don't care if it has to shut it down or not
        //noinspection ResultOfMethodCallIgnored
        executor.awaitTermination(60, TimeUnit.SECONDS);

        Date e = new Date();
        long delta = e.getTime() - s.getTime();

        System.out.printf("testConcurrentMultipleSessions: Count: %,d,  Elapsed: %,d ms, avg: %.1f/sec\n",
                counter.get(), delta, (count * 1000f) / delta);

        for (CryptoDb bob : boxes) {
            bob.close();
        }
        alice.close();
        if (testFailed.get()) {
            Assertions.fail("See logs");
        }
    }
}
