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
package com.hivemq.edge.adapters.plc4x.types.siemens;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.edge.adapters.plc4x.model.Plc4xAdapterConfig;
import com.hivemq.edge.modules.adapters.annotations.ModuleConfigField;
import com.hivemq.extension.sdk.api.annotations.NotNull;

public class S7AdapterConfig extends Plc4xAdapterConfig {

    public enum ControllerType {
        S7_300,
        S7_400,
        S7_1200,
        S7_1500,
        LOGO
    }

    @JsonProperty("port")
    @ModuleConfigField(title = "Port",
                       description = "The port number on the device you wish to connect to",
                       required = true,
                       numberMin = PORT_MIN,
                       numberMax = PORT_MAX,
                       defaultValue = "102")
    private int port = 102;

    @JsonProperty(value = "controllerType", required = true)
    @ModuleConfigField(title = "S7 Controller Type",
                       description = "Http method associated with the request",
                       required = true)
    private @NotNull S7AdapterConfig.ControllerType controllerType = ControllerType.S7_300;

    @JsonProperty("remoteRack")
    @ModuleConfigField(title = "Remote Rack",
                       description = "Rack value for the remote main CPU (PLC).",
                       defaultValue = "0")
    private @NotNull Integer remoteRack = 0;

    @JsonProperty("remoteRack2")
    @ModuleConfigField(title = "Remote Rack 2",
                       description = "Rack value for the remote secondary CPU (PLC).",
                       defaultValue = "0")
    private @NotNull Integer remoteRack2 = 0;

    @JsonProperty("remoteSlot")
    @ModuleConfigField(title = "Remote Slot",
                       description = "Slot value for the remote main CPU (PLC).",
                       defaultValue = "0")
    private @NotNull Integer remoteSlot = 0;

    @JsonProperty("remoteSlot2")
    @ModuleConfigField(title = "Remote Slot 2",
                       description = "Slot value for the remote secondary CPU (PLC).",
                       defaultValue = "0")
    private @NotNull Integer remoteSlot2 = 0;

    @JsonProperty("remoteTsap")
    @ModuleConfigField(title = "Remote TSAP",
                       description = "Remote TSAP value. The TSAP (Transport Services Access Point) mechanism is used as a further addressing level in the S7 PLC network. Usually only required for PLC from the LOGO series.",
                       defaultValue = "0")
    private @NotNull Integer remoteTsap = 0;


    public S7AdapterConfig() {
    }

    public int getPort() {
        return port;
    }

    public Integer getRemoteRack() {
        return remoteRack;
    }

    public Integer getRemoteRack2() {
        return remoteRack2;
    }

    public Integer getRemoteSlot() {
        return remoteSlot;
    }

    public Integer getRemoteSlot2() {
        return remoteSlot2;
    }

    public Integer getRemoteTsap() {
        return remoteTsap;
    }

    public ControllerType getControllerType() {
        return controllerType;
    }

}
