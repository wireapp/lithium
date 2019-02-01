package com.wire.bots.sdk.state;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wire.bots.sdk.Configuration;
import com.wire.bots.sdk.exceptions.MissingStateException;
import com.wire.bots.sdk.server.model.NewBot;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.UUID;

public class RedisState implements State {
    private final static ObjectMapper mapper = new ObjectMapper();
    private static final int TIMEOUT = 5000;
    private static JedisPool pool;

    private final UUID botId;
    private final Configuration.DB conf;

    public RedisState(String botId, Configuration.DB conf) {
        this.botId = UUID.fromString(botId);
        this.conf = conf;
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

    private static JedisPool pool(Configuration.DB conf) {
        if (pool == null) {
            JedisPoolConfig poolConfig = buildPoolConfig();
            if (conf.password != null && conf.port != null)
                pool = new JedisPool(poolConfig, conf.host, conf.port, TIMEOUT, conf.password);
            else if (conf.port != null)
                pool = new JedisPool(poolConfig, conf.host, conf.port, TIMEOUT);
            else
                pool = new JedisPool(poolConfig, conf.host);
        }
        return pool;
    }

    @Override
    public boolean saveState(NewBot newBot) throws IOException {
        try (Jedis jedis = getConnection()) {
            String value = mapper.writeValueAsString(newBot);
            jedis.set(botId.toString(), value);
            return true;
        }
    }

    @Override
    public NewBot getState() throws IOException {
        try (Jedis jedis = getConnection()) {
            String json = jedis.get(botId.toString());
            if (json == null)
                throw new MissingStateException(botId);
            return mapper.readValue(json, NewBot.class);
        }
    }

    @Override
    public boolean removeState() {
        try (Jedis jedis = getConnection()) {
            jedis.del(botId.toString());
            return true;
        }
    }

    @Override
    public ArrayList<NewBot> listAllStates() {
        return null;
    }

    @Override
    public boolean saveFile(String filename, String content) {
        return false;
    }

    @Override
    public String readFile(String filename) {
        return null;
    }

    @Override
    public boolean deleteFile(String filename) {
        return false;
    }

    @Override
    public boolean saveGlobalFile(String filename, String content) {
        return false;
    }

    @Override
    public String readGlobalFile(String filename) {
        return null;
    }

    @Override
    public boolean deleteGlobalFile(String filename) {
        return false;
    }

    private Jedis getConnection() {
        return pool(conf).getResource();
    }
}
