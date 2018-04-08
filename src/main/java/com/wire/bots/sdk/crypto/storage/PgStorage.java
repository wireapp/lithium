package com.wire.bots.sdk.crypto.storage;

import com.wire.bots.cryptobox.IRecord;
import com.wire.bots.cryptobox.IStorage;
import com.wire.bots.cryptobox.PreKey;

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
    public IRecord fetchSession(String id, String sid) {
        String key = key(id, sid);
        try {
            Connection c = newConnection();
            PreparedStatement stmt = c.prepareStatement("SELECT data FROM sessions WHERE id = ? FOR UPDATE");
            stmt.setString(1, key);
            ResultSet rs = stmt.executeQuery();
            byte[] data = null;
            if (rs.next()) {
                data = rs.getBytes("data");
            }
            return new Record(data, c, key);
        } catch (Exception e) {
            System.out.println(e.getClass().getName() + ": " + e.getMessage());
            return null;
        }
    }

    @Override
    public byte[] fetchIdentity(String id) {
        try (Connection c = newConnection()) {
            PreparedStatement stmt = c.prepareStatement("SELECT data FROM identities WHERE id = ?");
            stmt.setString(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getBytes("data");
            }
        } catch (Exception e) {
            System.out.println(e.getClass().getName() + ": " + e.getMessage());
        }
        return null;
    }

    @Override
    public void insertIdentity(String id, byte[] data) {
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
            System.out.println(e.getClass().getName() + ": " + e.getMessage());
        } finally {
            try {
                if (c != null) {
                    c.commit();
                    c.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public PreKey[] fetchPrekeys(String id) {
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
            System.out.println(e.getClass().getName() + ": " + e.getMessage());
        }
        return ret == null ? null : ret.toArray(new PreKey[0]);
    }

    @Override
    public void insertPrekey(String id, int kid, byte[] data) {
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
            System.out.println(e.getClass().getName() + ": " + e.getMessage());
        } finally {
            try {
                if (c != null) {
                    c.commit();
                    c.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private Connection newConnection() throws SQLException, InterruptedException {
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

    class Record implements IRecord {
        private final byte[] data;
        private final Connection connection;
        private final String sid;

        Record(byte[] data, Connection connection, String sid) {
            this.data = data;
            this.connection = connection;
            this.sid = sid;
        }

        @Override
        public byte[] getData() {
            return data;
        }

        @Override
        public void persist(byte[] data) {
            if (data == null)
                return;

            String sql = "INSERT INTO sessions (id, data) VALUES (?, ?) ON CONFLICT (id) DO UPDATE SET data = EXCLUDED.data";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, sid);
                try (ByteArrayInputStream stream = new ByteArrayInputStream(data)) {
                    stmt.setBinaryStream(2, stream);
                }
                stmt.executeUpdate();
            } catch (Exception e) {
                System.out.println(e.getClass().getName() + ": " + e.getMessage());
            } finally {
                try {
                    connection.commit();
                    connection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
