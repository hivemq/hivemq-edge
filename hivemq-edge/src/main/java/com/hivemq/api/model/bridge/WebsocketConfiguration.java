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
package com.hivemq.api.model.bridge;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;

public class WebsocketConfiguration {

    @JsonProperty(value = "enabled", defaultValue = "false")
    @Schema(description = "If Websockets are used", defaultValue = "false")
    private final boolean enabled;

    @JsonProperty(value = "serverPath", defaultValue = "/mqtt")
    @Schema(description = "The server path used by the bridge client. This must be setup as path at the remote broker", defaultValue = "/mqtt")
    private final @NotNull String serverPath;

    @JsonProperty(value = "subProtocol", defaultValue = "mqtt")
    @Schema(description = "The sub-protocol used by the bridge client. This must be supported by the remote broker", defaultValue = "mqtt")
    private final @NotNull String subProtocol;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public WebsocketConfiguration(@JsonProperty("enabled") final boolean enabled,
                                  @JsonProperty("serverPath") final @NotNull String serverPath,
                                  @JsonProperty("subProtocol") final @NotNull String subProtocol) {
        this.enabled = enabled;
        this.serverPath = serverPath;
        this.subProtocol = subProtocol;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public @NotNull String getServerPath() {
        return serverPath;
    }

    public @NotNull String getSubProtocol() {
        return subProtocol;
    }
}
