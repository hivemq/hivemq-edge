package com.hivemq.edge.adapters.redis.helpers;

import com.hivemq.edge.adapters.redis.config.RedisAdapterConfig;
import org.jetbrains.annotations.NotNull;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.*;

public class RedisHelpers {
    // Initialize Jedis Pool
    public JedisPool initJedisPool(final @NotNull RedisAdapterConfig adapterConfig) {
        final JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        final JedisPool jedisPool;
        if(!adapterConfig.getPassword().isEmpty()) {
            if (!adapterConfig.getUsername().isEmpty()){
                jedisPool = new JedisPool(jedisPoolConfig,adapterConfig.getServer(),adapterConfig.getPort(),180, adapterConfig.getUsername(),adapterConfig.getPassword());
            } else {
                jedisPool = new JedisPool(jedisPoolConfig,adapterConfig.getServer(),adapterConfig.getPort(),180,adapterConfig.getPassword());
            }
        } else {
            jedisPool = new JedisPool(jedisPoolConfig, adapterConfig.getServer(), adapterConfig.getPort(), 180);
        }
        return jedisPool;
    }

    public UnifiedJedis initUnifiedJedis(final @NotNull RedisAdapterConfig adapterConfig) {
        final HostAndPort redisAddress = new HostAndPort(adapterConfig.getServer(), adapterConfig.getPort());
        final JedisClientConfig jedisClientConfig;

        if(!adapterConfig.getPassword().isEmpty()) {
            if (!adapterConfig.getUsername().isEmpty()){
                jedisClientConfig = DefaultJedisClientConfig.builder()
                        .timeoutMillis(180000)
                        .user(adapterConfig.getUsername())
                        .password(adapterConfig.getPassword())
                        .build();
            } else {
                jedisClientConfig = DefaultJedisClientConfig.builder()
                        .timeoutMillis(180000)
                        .password(adapterConfig.getPassword())
                        .build();
            }
        } else {
            jedisClientConfig = DefaultJedisClientConfig.builder()
                    .timeoutMillis(180000)
                    .build();
        }

        return new UnifiedJedis(redisAddress, jedisClientConfig);
    }
}
