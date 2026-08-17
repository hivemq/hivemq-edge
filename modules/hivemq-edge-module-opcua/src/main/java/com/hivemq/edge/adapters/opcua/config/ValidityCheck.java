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
 * The {@code validity} axis: whether the certificate's validity period is enforced.
 *
 * <p>Strictest value is {@link #NOT_BEFORE_OR_AFTER}, which is therefore the default when the axis is
 * omitted.
 */
public enum ValidityCheck {

    /** The validity period is not checked; expired certificates are accepted. */
    @JsonProperty("NONE")
    NONE,

    /** The current time must lie within the certificate's {@code notBefore}/{@code notAfter} window. */
    @JsonProperty("NOT_BEFORE_OR_AFTER")
    NOT_BEFORE_OR_AFTER;

    @JsonCreator
    public static @Nullable ValidityCheck fromString(final @Nullable String value) {
        return EnumParsing.parse(ValidityCheck.class, values(), value);
    }

    @Override
    public @NotNull String toString() {
        return name();
    }
}
