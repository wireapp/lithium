import com.wire.bots.cryptobox.CryptoBox;
import com.wire.bots.cryptobox.CryptoDb;
import com.wire.bots.cryptobox.PreKey;
import com.wire.bots.sdk.crypto.storage.PgStorage;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class CryptoPostgresTest {
    private static String bobId;
    private static String aliceId;
    private static CryptoDb alice;
    private static CryptoDb bob;
    private static PreKey[] bobKeys;
    private static PreKey[] aliceKeys;
    private static PgStorage storage;

    @BeforeClass
    public static void setUp() throws Exception {
        Random random = new Random();
        aliceId = "" + random.nextInt();
        bobId = "" + random.nextInt();

        storage = new PgStorage("dejankovacevic", "password", "postgres", "localhost", 5432);
        alice = new CryptoDb(aliceId, storage);
        bob = new CryptoDb(bobId, storage);

        bobKeys = bob.newPreKeys(0, 1);
        aliceKeys = alice.newPreKeys(0, 1);
    }

    @AfterClass
    public static void clean() throws IOException {
        alice.close();
        bob.close();

        Util.deleteDir("data");
    }

    @Test
    public void testAliceToBob() throws Exception {
        String text = "Hello Bob, This is Alice!";

        // Encrypt using prekeys
        byte[] cipher = alice.encryptFromPreKeys(bobId, bobKeys[0], text.getBytes());

        // Decrypt using initSessionFromMessage
        byte[] decrypt = bob.decrypt(aliceId, cipher);

        assert Arrays.equals(decrypt, text.getBytes());
        assert text.equals(new String(decrypt));
    }

    @Test
    public void testBobToAlice() throws Exception {
        String text = "Hello Alice, This is Bob!";

        byte[] cipher = bob.encryptFromPreKeys(aliceId, aliceKeys[0], text.getBytes());

        // Decrypt using initSessionFromMessage
        byte[] decrypt = alice.decrypt(bobId, cipher);

        assert Arrays.equals(decrypt, text.getBytes());
        assert text.equals(new String(decrypt));
    }

    @Test
    public void testSessions() throws Exception {
        String text = "Hello Alice, This is Bob, again!";

        byte[] cipher = bob.encryptFromSession(aliceId, text.getBytes());

        // Decrypt using session
        byte[] decrypt = alice.decrypt(bobId, cipher);

        assert Arrays.equals(decrypt, text.getBytes());
        assert text.equals(new String(decrypt));
    }

    @Test
    public void testIdentity() throws Exception {
        Random random = new Random();
        final String carlId = "" + random.nextInt();
        final String dir = "data/" + carlId;

        CryptoDb carl = new CryptoDb(carlId, storage);
        PreKey[] carlPrekeys = carl.newPreKeys(0, 8);

        String daveId = "" + random.nextInt();
        String davePath = String.format("data/%s", daveId);
        CryptoBox dave = CryptoBox.open(davePath);
        PreKey[] davePrekeys = dave.newPreKeys(0, 8);

        String text = "Hello Bob, This is Carl!";

        // Encrypt using prekeys
        byte[] cipher = dave.encryptFromPreKeys(carlId, carlPrekeys[0], text.getBytes());
        byte[] decrypt = carl.decrypt(daveId, cipher);
        assert Arrays.equals(decrypt, text.getBytes());
        assert text.equals(new String(decrypt));

        carl.close();
        Util.deleteDir(dir);

        cipher = dave.encryptFromSession(carlId, text.getBytes());
        carl = new CryptoDb(carlId, storage);
        decrypt = carl.decrypt(daveId, cipher);

        assert Arrays.equals(decrypt, text.getBytes());
        assert text.equals(new String(decrypt));

        carl.close();
        Util.deleteDir(dir);

        carl = new CryptoDb(carlId, storage);

        cipher = carl.encryptFromPreKeys(daveId, davePrekeys[0], text.getBytes());
        decrypt = dave.decrypt(carlId, cipher);
        assert Arrays.equals(decrypt, text.getBytes());
        assert text.equals(new String(decrypt));

        carl.close();
    }

    @Test
    public void testSynchronousSingleSession() throws Exception {
        Date s = new Date();
        for (int i = 0; i < 100; i++) {
            String text = "Hello Alice, This is Bob, again! " + i;

            byte[] cipher = bob.encryptFromSession(aliceId, text.getBytes());

            // Decrypt using session
            byte[] decrypt = alice.decrypt(bobId, cipher);

            assert Arrays.equals(decrypt, text.getBytes());
            assert text.equals(new String(decrypt));

            text = "Hey Bob, How's life? " + i;

            cipher = alice.encryptFromSession(bobId, text.getBytes());

            // Decrypt using session
            decrypt = bob.decrypt(aliceId, cipher);

            assert Arrays.equals(decrypt, text.getBytes());
            assert text.equals(new String(decrypt));
        }
        Date e = new Date();
        long delta = e.getTime() - s.getTime();

        System.out.printf("Count: %,d,  Elapsed: %,d ms\n", 100, delta);
    }

    @Test
    public void testConcurrentSingleSession() throws Exception {
        final String text = "Hello Alice, This is Bob, again! ";

        bob.encryptFromPreKeys(aliceId, aliceKeys[0], text.getBytes());

        ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(4);
        final AtomicInteger counter = new AtomicInteger(0);

        for (int i = 0; i < 100; i++) {
            executor.execute(() -> {
                try {
                    bob.encryptFromSession(aliceId, text.getBytes());
                    counter.getAndIncrement();
                } catch (Exception e) {
                    System.out.println("testConcurrentSessions: " + e.toString());
                }
            });
        }
        Date s = new Date();
        executor.shutdown();
        executor.awaitTermination(60, TimeUnit.SECONDS);
        Date e = new Date();
        long delta = e.getTime() - s.getTime();

        System.out.printf("Count: %,d,  Elapsed: %,d ms\n", counter.get(), delta);
    }

    @Test
    public void testConcurrentMultipleSessions() throws Exception {
        final int count = 100;
        Random random = new Random();
        String aliceId = "" + random.nextInt();
        CryptoDb alice = new CryptoDb(aliceId, storage);
        PreKey[] aliceKeys = alice.newPreKeys(0, count);

        final AtomicInteger counter = new AtomicInteger(0);
        byte[] bytes = ("Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello " +
                "Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello " +
                "Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello " +
                "Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello ").getBytes();


        ArrayList<CryptoDb> boxes = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            String bobId = "" + random.nextInt();
            CryptoDb bob = new CryptoDb(bobId, storage);
            bob.encryptFromPreKeys(aliceId, aliceKeys[i], bytes);
            boxes.add(bob);
        }

        ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(24);
        Date s = new Date();
        for (CryptoDb bob : boxes) {
            executor.execute(() -> {
                try {
                    bob.encryptFromSession(aliceId, bytes);
                    counter.getAndIncrement();
                } catch (Exception e) {
                    System.out.println("testConcurrentDifferentCBSessions: " + e.toString());
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(60, TimeUnit.SECONDS);

        Date e = new Date();
        long delta = e.getTime() - s.getTime();

        System.out.printf("testConcurrentMultipleSessions: Count: %,d,  Elapsed: %,d ms, avg: %.1f/sec\n",
                counter.get(), delta, (count * 1000f) / delta);

        for (CryptoDb bob : boxes) {
            bob.close();
        }
        alice.close();
    }
}
