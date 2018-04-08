import com.wire.bots.cryptobox.IRecord;
import com.wire.bots.cryptobox.StorageException;
import com.wire.bots.sdk.crypto.storage.RedisStorage;
import org.junit.Test;

import java.util.Arrays;
import java.util.Random;

public class RedisStorageTest {
    @Test
    public void test() throws StorageException {
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
}
