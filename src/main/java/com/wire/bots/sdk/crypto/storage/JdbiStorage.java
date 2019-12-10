package com.wire.bots.sdk.crypto.storage;

import com.wire.bots.cryptobox.IRecord;
import com.wire.bots.cryptobox.IStorage;
import com.wire.bots.cryptobox.PreKey;
import org.skife.jdbi.v2.DBI;

import javax.annotation.Nullable;
import java.util.List;

public class JdbiStorage implements IStorage {
    private final SessionsDAO sessionsDAO;
    private final IdentitiesDAO identitiesDAO;
    private final PrekeysDAO prekeysDAO;

    public JdbiStorage(DBI jdbi) {
        sessionsDAO = jdbi.onDemand(SessionsDAO.class);
        identitiesDAO = jdbi.onDemand(IdentitiesDAO.class);
        prekeysDAO = jdbi.onDemand(PrekeysDAO.class);
    }

    @Override
    public IRecord fetchSession(String id, String sid) {
        Session session = sessionsDAO.get(id, sid);
        return new Record(id, sid, session == null ? null : session.data); //todo implement commit on UPDATE
    }

    @Override
    public byte[] fetchIdentity(String id) {
        return identitiesDAO.get(id);
    }

    @Override
    public void insertIdentity(String id, byte[] data) {
        identitiesDAO.insert(id, data);
    }

    @Override
    @Nullable
    public PreKey[] fetchPrekeys(String id) {
        List<PreKey> preKeys = prekeysDAO.get(id);
        if (preKeys.isEmpty())
            return null;

        PreKey[] ret = new PreKey[preKeys.size()];
        return preKeys.toArray(ret);
    }

    @Override
    public void insertPrekey(String id, int kid, byte[] data) {
        prekeysDAO.insert(id, kid, data);
    }

    @Override
    public void purge(String id) {
        sessionsDAO.delete(id);
        identitiesDAO.delete(id);
        prekeysDAO.delete(id);
    }

    class Record implements IRecord {
        private final String id;
        private final String sid;
        private final byte[] data;

        Record(String id, String sid, byte[] data) {
            this.id = id;
            this.sid = sid;
            this.data = data;
        }

        @Override
        public byte[] getData() {
            return data;
        }

        @Override
        public void persist(byte[] update) {
            if (update != null) {
                sessionsDAO.insert(id, sid, update);
                //todo implement commits
            }
        }
    }
}
