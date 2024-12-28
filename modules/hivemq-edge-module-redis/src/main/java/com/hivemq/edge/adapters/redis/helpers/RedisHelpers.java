package com.hivemq.edge.adapters.redis.helpers;

import com.hivemq.edge.adapters.redis.config.RedisAdapterConfig;
import org.jetbrains.annotations.NotNull;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class RedisHelpers {
    // Initialize Jedis Pool
    public JedisPool initJedisPool(final @NotNull RedisAdapterConfig adapterConfig) {
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        JedisPool jedisPool;
        if(!adapterConfig.getPassword().isEmpty()) {
            if (!adapterConfig.getUsername().isEmpty()){
                jedisPool = new JedisPool(jedisPoolConfig,adapterConfig.getServer(),adapterConfig.getPort(),60, adapterConfig.getUsername(),adapterConfig.getPassword());
            } else {
                jedisPool = new JedisPool(jedisPoolConfig,adapterConfig.getServer(),adapterConfig.getPort(),60,adapterConfig.getPassword());
            }
        } else {
            jedisPool = new JedisPool(jedisPoolConfig, adapterConfig.getServer(), adapterConfig.getPort(), 60);
        }
        return jedisPool;
    }
}
