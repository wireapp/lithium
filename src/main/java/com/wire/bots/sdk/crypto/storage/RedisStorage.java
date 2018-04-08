package com.wire.bots.sdk.crypto.storage;

import com.wire.bots.cryptobox.IRecord;
import com.wire.bots.cryptobox.IStorage;
import com.wire.bots.cryptobox.PreKey;
import com.wire.bots.cryptobox.StorageException;
import com.wire.bots.sdk.tools.Logger;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.time.Duration;

public class RedisStorage implements IStorage {
    private static final byte[] EMPTY = new byte[0];
    private static final int TIMEOUT = 5000;
    private final JedisPoolConfig poolConfig = buildPoolConfig();
    private final JedisPool pool;

    public RedisStorage(String host, int port, String password) {
        pool = new JedisPool(poolConfig, host, port, TIMEOUT, password);
    }

    public RedisStorage(String host, int port) {
        pool = new JedisPool(poolConfig, host, port, TIMEOUT);
    }

    public RedisStorage(String host) {
        pool = new JedisPool(poolConfig, host);
    }

    @Override
    public IRecord fetchSession(String id, String sid) throws StorageException {
        Jedis jedis = getConnection();
        String key = key(id, sid);
        byte[] data = jedis.getSet(key.getBytes(), EMPTY);
        if (data == null) {
            Logger.info("redis: fetch   key: %s size: %d\n", key, 0);
            return new Record(key, null, jedis);
        }

        for (int i = 0; i < 1000 && data.length == 0; i++) {
            sleep(5);
            data = jedis.getSet(key.getBytes(), EMPTY);
        }

        if (data.length == 0) {
            throw new StorageException("Redis Timeout when fetching Session with key: " + key);
        }

        Logger.info("redis: fetch   key: %s size: %d\n", key, data.length);
        return new Record(key, data, jedis);
    }

    @Override
    public byte[] fetchIdentity(String id) {
        try (Jedis jedis = getConnection()) {
            String key = String.format("id_%s", id);
            return jedis.get(key.getBytes());
        }
    }

    @Override
    public void insertIdentity(String id, byte[] data) {
        try (Jedis jedis = getConnection()) {
            String key = String.format("id_%s", id);
            jedis.set(key.getBytes(), data);
        }
    }

    @Override
    public PreKey[] fetchPrekeys(String id) {
        try (Jedis jedis = getConnection()) {

            String key = String.format("pk_%s", id);
            Long llen = jedis.llen(key);
            PreKey[] ret = new PreKey[llen.intValue()];
            for (int i = 0; i < llen.intValue(); i++) {
                byte[] data = jedis.lindex(key.getBytes(), i);
                ret[i] = new PreKey(i, data);
            }
            return ret;
        }
    }

    @Override
    public void insertPrekey(String id, int kid, byte[] data) {
        try (Jedis jedis = getConnection()) {
            String key = String.format("pk_%s", id);
            jedis.lpush(key.getBytes(), data);
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

    private JedisPoolConfig buildPoolConfig() {
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

    private Jedis getConnection() {
        return pool.getResource();
    }

    private class Record implements IRecord {
        private final String key;
        private final byte[] data;
        private final Jedis jedis;

        Record(String key, byte[] data, Jedis jedis) {
            this.key = key;
            this.data = data;
            this.jedis = jedis;
        }

        @Override
        public byte[] getData() {
            return data;
        }

        @Override
        public void persist(byte[] data) {
            jedis.set(key.getBytes(), data);
            Logger.info("redis: persist key: %s size: %d\n", key, data.length);
            jedis.close();
        }
    }
}
