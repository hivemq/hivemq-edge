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
package com.hivemq.api.model.status;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * @author Simon L Johnson
 */
public class ConnectionDetails {

    @JsonProperty("hostAddress")
    @Schema(description = "A mandatory hostAddress. Either a hostname or IPV4/IPv6 representation of a connection.")
    private final @NotNull String hostAddress;

    @JsonProperty("port")
    @Schema(description = "A mandatory port.")
    private final @NotNull Integer port;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public ConnectionDetails(@NotNull @JsonProperty("hostAddress") final String hostAddress,
                             @NotNull @JsonProperty("port") final Integer port) {
        this.hostAddress = hostAddress;
        this.port = port;
    }

    public Integer getPort() {
        return port;
    }

    public String getHostAddress() {
        return hostAddress;
    }
}
