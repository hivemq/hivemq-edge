/*
 * Copyright 2019-present HiveMQ GmbH
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
package com.hivemq.api.model.components;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Bean to transport module details across the API
 *
 * @author Simon L Johnson
 */
public class Listener {

    public enum TRANSPORT {
        TCP, UDP, DCCP, SCTP, RSVP, QUIC
    }

    @JsonProperty("hostName")
    @Schema(description = "A mandatory ID hostName with the Listener")
    private final @NotNull String hostName;

    @JsonProperty("port")
    @Schema(description = "The listener port")
    private final @NotNull Integer port;

    @JsonProperty("name")
    @Schema(description = "The listener name")
    private final @NotNull String name;

    @JsonProperty("description")
    @Schema(description = "The extension description", nullable = true)
    private final @Nullable String description;

    @JsonProperty("externalHostname")
    @Schema(description = "The external hostname", nullable = true)
    private final @Nullable String externalHostname;

    @JsonProperty("transport")
    @Schema(description = "The underlying transport that this listener uses", nullable = true)
    private final @Nullable TRANSPORT transport;

    @JsonProperty("protocol")
    @Schema(description = "A protocol that this listener services", nullable = true)
    private final @Nullable String protocol;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public Listener(
            final @NotNull @JsonProperty("name") String name,
            final @NotNull @JsonProperty("hostName") String hostName,
            final @NotNull @JsonProperty("port") Integer port,
            final @Nullable @JsonProperty("description") String description,
            final @Nullable @JsonProperty("externalHostname") String externalHostname,
            final @Nullable @JsonProperty("transport") TRANSPORT transport,
            final @Nullable @JsonProperty("protocol") String protocol) {
        this.name = name;
        this.description = description;
        this.hostName = hostName;
        this.port = port;
        this.externalHostname = externalHostname;
        this.transport = transport;
        this.protocol = protocol;
    }

    public @NotNull String getHostName() {
        return hostName;
    }

    public @NotNull Integer getPort() {
        return port;
    }

    public @NotNull String getName() {
        return name;
    }

    public @Nullable String getDescription() {
        return description;
    }

    public @Nullable String getExternalHostname() {
        return externalHostname;
    }

    public @Nullable String getProtocol() {
        return protocol;
    }

    public @Nullable TRANSPORT getTransport() {
        return transport;
    }
}
