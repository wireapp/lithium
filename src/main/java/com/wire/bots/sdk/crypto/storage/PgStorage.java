package com.wire.bots.sdk.crypto.storage;

import com.wire.bots.cryptobox.IRecord;
import com.wire.bots.cryptobox.IStorage;
import com.wire.bots.cryptobox.PreKey;
import com.wire.bots.cryptobox.StorageException;
import com.wire.bots.sdk.tools.Logger;

import java.io.ByteArrayInputStream;
import java.sql.*;
import java.util.ArrayList;

public class PgStorage implements IStorage {
    private final String user;
    private final String password;
    private final String db;
    private final String host;
    private final int port;

    public PgStorage(String user, String password, String db, String host, int port) {
        this.user = user;
        this.password = password;
        this.db = db;
        this.host = host;
        this.port = port;
    }

    @Override
    public IRecord fetchSession(String id, String sid) throws StorageException {
        try {
            Connection c = newConnection();
            PreparedStatement stmt = c.prepareStatement("SELECT data FROM sessions WHERE id = ? FOR UPDATE");

            String key = key(id, sid);
            stmt.setString(1, key);
            ResultSet rs = stmt.executeQuery();
            byte[] data = null;
            if (rs.next()) {
                data = rs.getBytes("data");
            }
            return new Record(id, sid, data, c);
        } catch (Exception e) {
            throw new StorageException(String.format("fetchSession: %s %s", sid, e));
        }
    }

    @Override
    public byte[] fetchIdentity(String id) throws StorageException {
        try (Connection c = newConnection()) {
            PreparedStatement stmt = c.prepareStatement("SELECT data FROM identities WHERE id = ?");
            stmt.setString(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getBytes("data");
            }
        } catch (Exception e) {
            throw new StorageException(String.format("fetchIdentity: %s %s", id, e));
        }
        return null;
    }

    @Override
    public void insertIdentity(String id, byte[] data) throws StorageException {
        String sql = "INSERT INTO identities (id, data) VALUES (?, ?) ON CONFLICT (id) DO NOTHING";
        Connection c = null;
        try {
            c = newConnection();
            PreparedStatement stmt = c.prepareStatement(sql);
            stmt.setString(1, id);
            try (ByteArrayInputStream stream = new ByteArrayInputStream(data)) {
                stmt.setBinaryStream(2, stream);
            }
            stmt.executeUpdate();
        } catch (Exception e) {
            throw new StorageException(String.format("insertIdentity: %s %s", id, e));
        } finally {
            commit(c);
        }
    }

    @Override
    public PreKey[] fetchPrekeys(String id) throws StorageException {
        ArrayList<PreKey> ret = null;
        try (Connection c = newConnection()) {
            PreparedStatement stmt = c.prepareStatement("SELECT kid, data FROM prekeys WHERE id = ?");
            stmt.setString(1, id);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                if (ret == null)
                    ret = new ArrayList<>();

                int kid = rs.getInt("kid");
                byte[] data = rs.getBytes("data");
                PreKey preKey = new PreKey(kid, data);
                ret.add(preKey);
            }
        } catch (Exception e) {
            throw new StorageException(String.format("fetchPrekeys: %s %s", id, e));
        }
        return ret == null ? null : ret.toArray(new PreKey[0]);
    }

    @Override
    public void insertPrekey(String id, int kid, byte[] data) throws StorageException {
        String sql = "INSERT INTO prekeys (id, kid, data) VALUES (?, ?, ?) ON CONFLICT (id, kid) DO NOTHING";
        Connection c = null;
        try {
            c = newConnection();
            PreparedStatement stmt = c.prepareStatement(sql);
            stmt.setString(1, id);
            stmt.setInt(2, kid);
            try (ByteArrayInputStream stream = new ByteArrayInputStream(data)) {
                stmt.setBinaryStream(3, stream);
            }
            stmt.executeUpdate();
        } catch (Exception e) {
            throw new StorageException(String.format("insertPrekey: %s key: %d %s", id, kid, e));
        } finally {
            commit(c);
        }
    }

    private Connection newConnection() throws InterruptedException {
        while (true) {
            try {
                String url = String.format("jdbc:postgresql://%s:%d/%s", host, port, db);
                Connection connection = DriverManager.getConnection(url, user, password);
                connection.setAutoCommit(false);
                return connection;
            } catch (Exception e) {
                Thread.sleep(1000);
            }
        }
    }

    private String key(String id, String sid) {
        return String.format("%s-%s", id, sid);
    }

    private void commit(Connection c) {
        try {
            if (c != null) {
                c.commit();
                c.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    class Record implements IRecord {
        private final String id;
        private final String sid;
        private final byte[] data;
        private final Connection connection;

        Record(String id, String sid, byte[] data, Connection connection) {
            this.id = id;
            this.sid = sid;
            this.data = data;
            this.connection = connection;
        }

        @Override
        public byte[] getData() {
            return data;
        }

        @Override
        public void persist(byte[] data) {
            final String sql = "INSERT INTO sessions (id, data) VALUES (?, ?) " +
                    "ON CONFLICT (id) DO UPDATE SET data = EXCLUDED.data";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                if (data != null) {
                    String key = key(id, sid);
                    stmt.setString(1, key);
                    try (ByteArrayInputStream stream = new ByteArrayInputStream(data)) {
                        stmt.setBinaryStream(2, stream);
                    }
                    stmt.executeUpdate();
                }
            } catch (Exception e) {
                Logger.error("persist: %s %s", sid, e);
            } finally {
                commit(connection);
            }
        }
    }
}
