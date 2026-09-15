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
 * The {@code hostname} axis: whether the endpoint hostname must appear in the certificate.
 *
 * <p>Strictest value is {@link #HOSTNAME}, which is therefore the default when the axis is omitted.
 *
 * <p>A finer distinction (SAN-only versus CN-fallback matching) belongs on this axis conceptually,
 * but Milo exposes a single hostname check today, so no such value is offered. Values that cannot be
 * honoured are deliberately absent rather than shipped as dead branches.
 */
public enum HostnameCheck {

    /** The endpoint hostname is not checked against the certificate. */
    @JsonProperty("NONE")
    NONE,

    /**
     * The endpoint hostname must match a SubjectAlternativeName DNS name or IP address in the
     * certificate.
     */
    @JsonProperty("HOSTNAME")
    HOSTNAME;

    @JsonCreator
    public static @Nullable HostnameCheck fromString(final @Nullable String value) {
        return EnumParsing.parse(HostnameCheck.class, values(), value);
    }

    @Override
    public @NotNull String toString() {
        return name();
    }
}
