package com.wire.bots.sdk.crypto.storage;

import com.wire.bots.cryptobox.IRecord;
import com.wire.bots.cryptobox.IStorage;
import com.wire.bots.cryptobox.PreKey;
import com.wire.bots.cryptobox.StorageException;
import com.wire.bots.sdk.tools.Logger;

import java.io.ByteArrayInputStream;
import java.sql.*;
import java.util.ArrayList;
import java.util.Properties;

public class PgStorage implements IStorage {
    private final String user;
    private final String password;
    private final String url;
    private final String db;
    private final String host;
    private final int port;

    public PgStorage() {
        this.user = null;
        this.password = null;
        this.db = "postgres";
        this.host = "localhost";
        this.port = 5432;
        this.url = "jdbc:postgresql://localhost:5432/postgres";
    }

    public PgStorage(String user, String password, String db, String host, int port) {
        this.user = user;
        this.password = password;
        this.db = db;
        this.host = host;
        this.port = port;
        this.url = null;
    }

    public PgStorage(String user, String password, String url) {
        this.user = user;
        this.password = password;
        this.url = url;
        this.db = null;
        this.host = null;
        this.port = 0;
    }

    @Override
    public IRecord fetchSession(String id, String sid) throws StorageException {
        try {
            Connection c = newConnection();
            PreparedStatement stmt = c.prepareStatement("SELECT data FROM sessions WHERE id = ? AND sid = ? FOR UPDATE");
            stmt.setString(1, id);
            stmt.setString(2, sid);
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
        String sql = "INSERT INTO identities (id, data) VALUES (?, ?) ON CONFLICT (id) DO UPDATE SET data = EXCLUDED.data";
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
        String sql = "INSERT INTO prekeys (id, kid, data) VALUES (?, ?, ?) ON CONFLICT (id, kid) DO UPDATE SET data = EXCLUDED.data";
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

    @Override
    public void purge(String id) throws StorageException {
        Connection c = null;
        try {
            c = newConnection();
            PreparedStatement stmt = c.prepareStatement("DELETE FROM identities WHERE id = ?");
            stmt.setString(1, id);
            stmt.executeUpdate();
            stmt = c.prepareStatement("DELETE FROM prekeys WHERE id = ?");
            stmt.setString(1, id);
            stmt.executeUpdate();
            stmt = c.prepareStatement("DELETE FROM sessions WHERE id = ?");
            stmt.setString(1, id);
            stmt.executeUpdate();
        } catch (Exception e) {
            throw new StorageException(String.format("purge: %s %s", id, e));
        } finally {
            commit(c);
        }
    }

    private Connection newConnection() throws InterruptedException {
        while (true) {
            try {
                String url = this.url != null
                        ? this.url
                        : String.format("jdbc:postgresql://%s:%d/%s", host, port, db);
                Connection connection;
                Properties info = new java.util.Properties();
                if (user != null && !user.isEmpty()) {
                    info.put("user", user);
                }
                if (password != null && !password.isEmpty()) {
                    info.put("password", password);
                }
                connection = DriverManager.getConnection(url, info);
                connection.setAutoCommit(false);
                return connection;
            } catch (Exception e) {
                Thread.sleep(1000);
            }
        }
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
            final String sql = "INSERT INTO sessions (id, sid, data) VALUES (?, ?, ?) " +
                    "ON CONFLICT (id, sid) DO UPDATE SET data = EXCLUDED.data";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                if (data != null) {
                    stmt.setString(1, id);
                    stmt.setString(2, sid);
                    try (ByteArrayInputStream stream = new ByteArrayInputStream(data)) {
                        stmt.setBinaryStream(3, stream);
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
