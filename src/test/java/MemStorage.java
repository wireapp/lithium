import com.wire.bots.cryptobox.IRecord;
import com.wire.bots.cryptobox.IStorage;
import com.wire.bots.cryptobox.PreKey;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class MemStorage implements IStorage {
    private final ConcurrentHashMap<String, Record> sessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, byte[]> identities = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ArrayList<PreKey>> prekeys = new ConcurrentHashMap<>();

    @Override
    public IRecord fetchSession(String id, String sid) {
        String key = key(id, sid);
        Record record = sessions.computeIfAbsent(key, k -> null);
        if (record == null)
            return new Record(key, null);

        for (int i = 0; i < 1000 && record.locked; i++) {
            sleep(1);
            record = sessions.get(key);
        }
        record.locked = true;
        //sessions.put(key, record);
        return new Record(key, record.data);
    }

    @Override
    public byte[] fetchIdentity(String id) {
        return identities.get(id);
    }

    @Override
    public void insertIdentity(String id, byte[] data) {
        identities.put(id, data);
    }

    @Override
    public PreKey[] fetchPrekeys(String id) {
        ArrayList<PreKey> ret = prekeys.get(id);
        return ret == null ? null : ret.toArray(new PreKey[0]);
    }

    @Override
    public void insertPrekey(String id, int kid, byte[] data) {
        PreKey preKey = new PreKey(kid, data);
        ArrayList<PreKey> list = prekeys.computeIfAbsent(id, k -> new ArrayList<>());
        list.add(preKey);
    }

    private void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
        }
    }

    private String key(String id, String sid) {
        return String.format("%s-%s", id, sid);
    }

    private class Record implements IRecord {
        boolean locked;
        //private final String key;
        private byte[] data;

        Record(String key, byte[] data) {
            //this.key = key;
            this.data = data;
        }

        @Override
        public byte[] getData() {
            return data;
        }

        @Override
        public void persist(byte[] data) {
            this.data = data;
            //sessions.put(key, this);
        }
    }
}
