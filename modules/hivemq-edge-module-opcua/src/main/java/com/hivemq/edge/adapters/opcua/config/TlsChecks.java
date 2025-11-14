/*
 * Copyright 2023-present HiveMQ GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hivemq.edge.adapters.opcua.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum TlsChecks {

    @JsonProperty("NONE")
    NONE("NONE"),

    @JsonProperty("APPLICATION_URI")
    APPLICATION_URI("APPLICATION_URI"),

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
