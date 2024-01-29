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
package com.hivemq.edge.adapters.plc4x.types.ads;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.edge.adapters.plc4x.model.Plc4xAdapterConfig;
import com.hivemq.edge.modules.adapters.annotations.ModuleConfigField;
import com.hivemq.extension.sdk.api.annotations.NotNull;


public class ADSAdapterConfig extends Plc4xAdapterConfig {

    @JsonProperty("port")
    @ModuleConfigField(title = "Port",
                       description = "The port number on the device to connect to",
                       required = true,
                       numberMin = PORT_MIN,
                       numberMax = PORT_MAX,
                       defaultValue = "48898")
    private int port = 48898;

    @ModuleConfigField(title = "Target AMS Port",
                       description = "The AMS port number on the device to connect to",
                       required = true,
                       numberMin = PORT_MIN,
                       numberMax = PORT_MAX,
                       defaultValue = "851")
    private int targetAmsPort = 851;

    @ModuleConfigField(title = "Source AMS Port",
                       description = "The local AMS port number used by HiveMQ Edge",
                       required = true,
                       numberMin = PORT_MIN,
                       numberMax = PORT_MAX,
                       defaultValue = "48898")
    private int sourceAmsPort = 48898;

    @JsonProperty("sourceAmsNetId")
    @ModuleConfigField(title = "Source Ams Net Id",
                       required = true,
                       stringPattern = "\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}",
                       description = "The AMS Net ID used by HiveMQ Edge")
    private @NotNull String sourceAmsNetId = "";

    @JsonProperty("targetAmsNetId")
    @ModuleConfigField(title = "Target Ams Net Id",
                       required = true,
                       stringPattern = "\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}",
                       description = "The AMS Net ID of the device to connect to")
    private @NotNull String targetAmsNetId = "";

    public int getSourceAmsPort() {
        return sourceAmsPort;
    }

    public int getTargetAmsPort() {
        return targetAmsPort;
    }

    public String getSourceAmsNetId() {
        return sourceAmsNetId;
    }

    public String getTargetAmsNetId() {
        return targetAmsNetId;
    }

}
