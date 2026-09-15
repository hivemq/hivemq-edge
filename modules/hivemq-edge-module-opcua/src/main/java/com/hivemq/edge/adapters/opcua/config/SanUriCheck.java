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
 * The {@code sanUri} axis: which identity, if any, is asserted from the certificate's
 * SubjectAlternativeName URI.
 *
 * <p>Strictest value is {@link #APPLICATION_URI}, which is therefore the default when the axis is
 * omitted. Further SAN-URI identities may be added here as the model outgrows OPC UA.
 */
public enum SanUriCheck {

    /** The SubjectAlternativeName URI is not checked. */
    @JsonProperty("NONE")
    NONE,

    /**
     * The OPC UA {@code ApplicationUri} announced by the server must match the SubjectAlternativeName
     * URI in its certificate.
     */
    @JsonProperty("APPLICATION_URI")
    APPLICATION_URI;

    @JsonCreator
    public static @Nullable SanUriCheck fromString(final @Nullable String value) {
        return EnumParsing.parse(SanUriCheck.class, values(), value);
    }

    @Override
    public @NotNull String toString() {
        return name();
    }
}
