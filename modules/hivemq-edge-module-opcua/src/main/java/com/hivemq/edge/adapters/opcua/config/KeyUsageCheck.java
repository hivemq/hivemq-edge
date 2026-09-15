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

/**
 * The {@code keyUsage} axis: how strictly the certificate's declared purpose is enforced.
 *
 * <p>Strictest value is {@link #SERVER_AUTH}, which is therefore the default when the axis is
 * omitted.
 */
public enum KeyUsageCheck {

    /** Neither the KeyUsage nor the ExtendedKeyUsage extension is checked. */
    @JsonProperty("NONE")
    NONE,

    /** The KeyUsage extension must be present and appropriate for an end-entity certificate. */
    @JsonProperty("KEY_USAGE")
    KEY_USAGE,

    /**
     * As {@link #KEY_USAGE}, plus the ExtendedKeyUsage extension must be present and permit server
     * authentication.
     */
    @JsonProperty("SERVER_AUTH")
    SERVER_AUTH;

    @JsonCreator
    public static @Nullable KeyUsageCheck fromString(final @Nullable String value) {
        return EnumParsing.parse(KeyUsageCheck.class, values(), value);
    }

    @Override
    public @NotNull String toString() {
        return name();
    }
}
