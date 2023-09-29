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
        S7_1500
    }

    public enum PduSize {
        b128("128"),
        b256("256"),
        b512("512"),
        b1024("1024"),
        b2048("2048");

        PduSize(String value){
            this.value = value;
        }

        private String value;

        public String toString() {
            return value;
        }
    }

    @JsonProperty("controllerType")
    @ModuleConfigField(title = "S7 Controller Type",
                       description = "Http method associated with the request",
                       defaultValue = "S7_300")
    private @NotNull S7AdapterConfig.ControllerType controllerType = ControllerType.S7_300;

    @JsonProperty("pduSize")
    @ModuleConfigField(title = "Max PDU Size",
                       description = "Maximum size of a data-packet sent to and received from the remote PLC.",
                       defaultValue = "b1024")
    private @NotNull S7AdapterConfig.PduSize pduSize = PduSize.b1024;

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
                       description = "Remote TSAP value.",
                       defaultValue = "0")
    private @NotNull Integer remoteTsap = 0;

    @JsonProperty("maxAmqCaller")
    @ModuleConfigField(title = "Maximum AMQ Caller",
                       description = "Maximum number of unconfirmed requests the PLC will accept in parallel before discarding with errors.",
                       defaultValue = "8")
    private @NotNull Integer maxAmqCaller = 8;

    @JsonProperty("maxAmqCallee")
    @ModuleConfigField(title = "Maximum AMQ Callee",
                       description = "Maximum number of unconfirmed responses or requests accepted in parallel before discarding with errors.",
                       defaultValue = "8")
    private @NotNull Integer maxAmqCallee = 8;

    @JsonProperty("ping")
    @ModuleConfigField(title = "Send ping during periods of inactivity.",
                       defaultValue = "false")
    private @NotNull Boolean ping = false;

    @JsonProperty("pingTime")
    @ModuleConfigField(title = "Ping Time",
                       description = "Time value in seconds at which the execution of the PING will be scheduled. Generally should be the same as (read-timeout / 2).",
                       defaultValue = "-1")
    private @NotNull Integer pingTime = -1;

    @JsonProperty("retryTime")
    @ModuleConfigField(title = "Retry Time",
                       description = "Time for supervision of TCP channels (second).",
                       defaultValue = "4")
    private @NotNull Integer retryTime = 4;

    @JsonProperty("retryTimeout")
    @ModuleConfigField(title = "Retry Timeout",
                       description = "This is the maximum waiting time for reading on the TCP channel.",
                       defaultValue = "8")
    private @NotNull Integer retryTimeout = 8;


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

    public Integer getMaxAmqCaller() {
        return maxAmqCaller;
    }

    public Integer getMaxAmqCallee() {
        return maxAmqCallee;
    }

    public Boolean getPing() {
        return ping;
    }

    public Integer getPingTime() {
        return pingTime;
    }

    public Integer getRetryTime() {
        return retryTime;
    }

    public Integer getRetryTimeout() {
        return retryTimeout;
    }

    public ControllerType getControllerType() {
        return controllerType;
    }

    public PduSize getPduSize() {
        return pduSize;
    }
}
