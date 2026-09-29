package com.hivemq.edge.adapters.redis.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public enum RedisDataType {
    STRING("STRING"),
    LIST("LIST"),
    HASH("HASH");

    private static final Map<String, RedisDataType> BY_LABEL;

    static {
        final Map<String, RedisDataType> temp = new HashMap<>();
        for (final RedisDataType e : values()) {
            temp.put(e.label, e);
        }
        BY_LABEL = Collections.unmodifiableMap(temp);
    }

    public final String label;

    RedisDataType(final String label) {
        this.label = label;
    }


    public static RedisDataType valueOfLabel(final String label) {
        return BY_LABEL.get(label);
    }

    @Override
    public String toString() {
        return this.label;
    }
}
