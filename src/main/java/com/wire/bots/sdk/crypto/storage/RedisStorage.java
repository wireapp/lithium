package com.wire.bots.sdk.crypto.storage;

import com.wire.bots.cryptobox.IRecord;
import com.wire.bots.cryptobox.IStorage;
import com.wire.bots.cryptobox.PreKey;
import com.wire.bots.cryptobox.StorageException;
import com.wire.bots.sdk.tools.Logger;
import com.wire.bots.sdk.tools.Util;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;

public class RedisStorage implements IStorage {
    private static final byte[] EMPTY = new byte[0];
    private static int timeout = 5000;
    private static JedisPool pool;
    private final String host;
    private final Integer port;
    private final String password;

    public RedisStorage(String host, Integer port, String password, int timeout) {
        this.host = host;
        this.port = port;
        this.password = password;
        RedisStorage.timeout = timeout;
    }

    public RedisStorage(String host, Integer port, String password) {
        this.host = host;
        this.port = port;
        this.password = password;
    }

    public RedisStorage(String host, Integer port) {
        this.host = host;
        this.port = port;
        this.password = null;
    }

    public RedisStorage(String host) {
        this.host = host;
        this.password = null;
        this.port = null;
    }

    private static JedisPoolConfig buildPoolConfig() {
        final JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(1100);
        poolConfig.setMaxIdle(16);
        poolConfig.setMinIdle(16);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        poolConfig.setTestWhileIdle(true);
        poolConfig.setMinEvictableIdleTimeMillis(Duration.ofSeconds(60).toMillis());
        poolConfig.setTimeBetweenEvictionRunsMillis(Duration.ofSeconds(30).toMillis());
        poolConfig.setNumTestsPerEvictionRun(3);
        poolConfig.setBlockWhenExhausted(true);
        return poolConfig;
    }

    private static JedisPool pool(String host, Integer port, String password) {
        if (pool == null) {
            JedisPoolConfig poolConfig = buildPoolConfig();
            if (password != null && port != null)
                pool = new JedisPool(poolConfig, host, port, timeout, password);
            else if (port != null)
                pool = new JedisPool(poolConfig, host, port, timeout);
            else
                pool = new JedisPool(poolConfig, host);
        }
        return pool;
    }

    @Override
    public IRecord fetchSession(String id, String sid) throws StorageException {
        Jedis jedis = getConnection();
        String key = key(id, sid);
        byte[] data = jedis.getSet(key.getBytes(), EMPTY);
        if (data == null) {
            Logger.debug("fetchSession: %s missing", sid);
            return new Record(id, sid, null, jedis);
        }

        for (int i = 0; i < 200 && data.length == 0; i++) {
            sleep(10);
            data = jedis.getSet(key.getBytes(), EMPTY);
        }

        if (data.length == 0) {
            Logger.warning("fetchSession: WARNING %s timeout", sid);
            jedis.del(key);
            throw new StorageException("Redis Timeout when fetching Session: " + sid);
        }

        Logger.debug("fetchSession: %s size: %d", sid, data.length);
        return new Record(id, sid, data, jedis);
    }

    @Override
    public byte[] fetchIdentity(String id) {
        byte[] bytes = null;
        try (Jedis jedis = getConnection()) {
            String key = String.format("id_%s", id);
            Boolean exists = jedis.exists(key.getBytes());
            if (!exists) {
                Logger.debug("fetchIdentity: %s, missing", key);
                return null;
            }
            bytes = jedis.get(key.getBytes());
            MessageDigest md = MessageDigest.getInstance("SHA1");
            Logger.debug("fetchIdentity: %s hash: %s", key, Util.digest(md, bytes));
        } catch (NoSuchAlgorithmException ignore) {

        }
        return bytes;
    }

    @Override
    public void insertIdentity(String id, byte[] data) {
        try (Jedis jedis = getConnection()) {
            String key = String.format("id_%s", id);
            Boolean exists = jedis.exists(key.getBytes());
            if (!exists)
                jedis.set(key.getBytes(), data);
            Logger.debug("insertIdentity: %s len: %d", id, data.length);
        }
    }

    private void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
        }
    }

    private String key(String id, String sid) {
        return String.format("ses_%s-%s", id, sid);
    }

    private Jedis getConnection() {
        return pool(host, port, password).getResource();
    }

    @Override
    public void insertPrekey(String id, int kid, byte[] data) {
        try (Jedis jedis = getConnection()) {
            String key = String.format("pk_%s", id);
            PreKey preKey = new PreKey(kid, data);
            ByteBuffer byteBuffer = toByteBuffer(preKey);
            jedis.lpush(key.getBytes(), byteBuffer.array());
        }
    }

    @Override
    public void purge(String id) {

    }

    @Override
    public PreKey[] fetchPrekeys(String id) {
        try (Jedis jedis = getConnection()) {
            String key = String.format("pk_%s", id);
            Long llen = jedis.llen(key);
            PreKey[] ret = new PreKey[llen.intValue()];
            for (int i = 0; i < llen.intValue(); i++) {
                byte[] raw = jedis.lindex(key.getBytes(), i);
                ByteBuffer byteBuffer = ByteBuffer.wrap(raw);
                PreKey preKey = toPreKey(byteBuffer);
                ret[i] = preKey;
            }
            return ret.length == 0 ? null : ret;
        }
    }

    private PreKey toPreKey(ByteBuffer byteBuffer) {
        byte[] raw = byteBuffer.array();
        int kid = byteBuffer.getInt();
        byte[] data = new byte[raw.length - byteBuffer.position()];
        System.arraycopy(raw, byteBuffer.position(), data, 0, data.length);
        return new PreKey(kid, data);
    }

    private ByteBuffer toByteBuffer(PreKey preKey) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(4 + preKey.data.length);
        byteBuffer.putInt(preKey.id);
        byteBuffer.put(preKey.data);
        return byteBuffer;
    }

    private class Record implements IRecord {
        private final byte[] data;
        private final Jedis connection;
        private final String id;
        private final String sid;

        Record(String id, String sid, byte[] data, Jedis connection) {
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
            String key = key(id, sid);
            if (data != null) {
                connection.set(key.getBytes(), data);
                Logger.debug("persistSession: %s size: %d", sid, data.length);
            } else {
                connection.del(key);
                Logger.debug("persistSession: %s deleted", sid);
            }
            connection.close();
        }
    }
}