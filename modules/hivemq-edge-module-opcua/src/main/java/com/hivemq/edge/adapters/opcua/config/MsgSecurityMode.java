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
import org.eclipse.milo.opcua.stack.core.types.enumerated.MessageSecurityMode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * OPC UA Message Security Mode configuration enum.
 * Maps to Eclipse Milo's MessageSecurityMode.
 */
public enum MsgSecurityMode {
    @JsonProperty("IGNORED")
    IGNORED(null),

    @JsonProperty("NONE")
    NONE(MessageSecurityMode.None),

    @JsonProperty("SIGN")
    SIGN(MessageSecurityMode.Sign),

    @JsonProperty("SIGN_AND_ENCRYPT")
    SIGN_AND_ENCRYPT(MessageSecurityMode.SignAndEncrypt);

    private final @Nullable MessageSecurityMode miloMode;

    MsgSecurityMode(final @Nullable MessageSecurityMode miloMode) {
        this.miloMode = miloMode;
    }

    /**
     * Gets the corresponding Eclipse Milo MessageSecurityMode.
     *
     * @return the corresponding Eclipse Milo MessageSecurityMode
     */
    public @Nullable MessageSecurityMode getMiloMode() {
        return miloMode;
    }

    /**
     * Find the MsgSecurityMode enum for a given Milo MessageSecurityMode.
     *
     * @param mode the Milo MessageSecurityMode
     * @return the corresponding MsgSecurityMode, or null if not found
     */
    public static @Nullable MsgSecurityMode forMiloMode(final @NotNull MessageSecurityMode mode) {
        for (final var value : values()) {
            if (value.miloMode == mode) {
                return value;
            }
        }
        return null;
    }

    /**
     * Jackson creator. Blank or absent yields {@code null}, which {@link Security} resolves to
     * {@link #IGNORED}: the mode is unset and the security policy decides it.
     *
     * <p>An unrecognized value rejects the configuration rather than resolving to {@link #IGNORED} —
     * see {@link EnumParsing}. Returning {@code null} for a misspelling, which this used to do, made
     * {@code SING} indistinguishable from "not configured", and under {@code policy=NONE} that
     * selects message security {@code None} where {@code SIGN} would have matched no endpoint at all.
     *
     * @param value the string value from JSON
     * @return the corresponding MsgSecurityMode, or {@code null} when the value is absent or blank
     */
    @JsonCreator
    public static @Nullable MsgSecurityMode fromString(final @Nullable String value) {
        return EnumParsing.parse(MsgSecurityMode.class, values(), value);
    }
}
