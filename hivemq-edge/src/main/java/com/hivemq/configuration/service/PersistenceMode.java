package com.hivemq.configuration.service;

import com.hivemq.extension.sdk.api.annotations.NotNull;

public enum PersistenceMode {
    /**
     * All persistent data like queued messages, retained messages subscriptions and so on, will be stored in RAM.
     */
    IN_MEMORY,

    FILE_NATIVE,

    FILE;

    private static final @NotNull PersistenceMode @NotNull [] VALUES = values();

    public static @NotNull PersistenceMode forCode(final int code) {
        try {
            return VALUES[code];
        } catch (final ArrayIndexOutOfBoundsException e) {
            throw new IllegalArgumentException("No persistence type found for code: " + code, e);
        }
    }
}
