package com.hivemq.edge.adapters.opcua.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum TlsChecks {

    @JsonProperty("NONE")
    NONE("NONE"),

    @JsonProperty("STANDARD")
    STANDARD("STANDARD"),

    @JsonProperty("ALL")
    ALL("ALL");

    private final @NotNull String tlsChecks;

    TlsChecks(final @NotNull String tlsChecks) {
        this.tlsChecks = tlsChecks;
    }

    @Override
    public String toString() {
        return tlsChecks;
    }

    /**
     * Jackson creator method for deserialization.
     *
     * @param value the string value from JSON
     * @return the corresponding TlsChecks
     */
    @JsonCreator
    public static @Nullable TlsChecks fromString(final @Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        for (final var mode : values()) {
            if (mode.name().equalsIgnoreCase(value) ||
                    mode.name().replace("_", "").equalsIgnoreCase(value)) {
                return mode;
            }
        }
        return null;
    }
}
