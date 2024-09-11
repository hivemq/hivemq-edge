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
package com.hivemq.edge.adapters.plc4x.types.siemens.config.legacy;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.edge.adapters.plc4x.config.legacy.LegacyPlc4xAdapterConfig;
import com.hivemq.edge.adapters.plc4x.types.siemens.config.S7AdapterConfig;
import org.jetbrains.annotations.NotNull;

public class LegacyS7AdapterConfig extends LegacyPlc4xAdapterConfig {

    @JsonProperty("port")
    private int port = 102;

    @JsonProperty(value = "controllerType", required = true)
    private @NotNull S7AdapterConfig.ControllerType controllerType = S7AdapterConfig.ControllerType.S7_300;

    @JsonProperty("remoteRack")
    private @NotNull Integer remoteRack = 0;

    @JsonProperty("remoteRack2")
    private @NotNull Integer remoteRack2 = 0;

    @JsonProperty("remoteSlot")
    private @NotNull Integer remoteSlot = 0;

    @JsonProperty("remoteSlot2")
    private @NotNull Integer remoteSlot2 = 0;

    @JsonProperty("remoteTsap")
    private @NotNull Integer remoteTsap = 0;


    public LegacyS7AdapterConfig() {
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

    public S7AdapterConfig.ControllerType getControllerType() {
        return controllerType;
    }

}
