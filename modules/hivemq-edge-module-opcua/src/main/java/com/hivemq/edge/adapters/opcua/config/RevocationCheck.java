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
 * The {@code revocation} axis: how hard revocation status is enforced.
 *
 * <p>Strictest value is {@link #REQUIRE_CRLS}, which is therefore the default when the axis is
 * omitted.
 *
 * <p>Revocation is only enforceable while a certification path is being built, so any value other
 * than {@link #NONE} requires {@link TrustMode#CHAIN}. The combination is rejected at start-up rather
 * than silently ignored — see {@link TlsChecksProjection}.
 */
public enum RevocationCheck {

    /** Revocation status is not checked. */
    @JsonProperty("NONE")
    NONE,

    /** Revocation is checked where revocation information is available. */
    @JsonProperty("CHECK")
    CHECK,

    /** Revocation is checked and a CRL must be found for every non-end-entity CA in the path. */
    @JsonProperty("REQUIRE_CRLS")
    REQUIRE_CRLS;

    @JsonCreator
    public static @Nullable RevocationCheck fromString(final @Nullable String value) {
        return EnumParsing.parse(RevocationCheck.class, values(), value);
    }

    @Override
    public @NotNull String toString() {
        return name();
    }
}
