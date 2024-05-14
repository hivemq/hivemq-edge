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
package com.hivemq.edge.adapters.plc4x.types.eip;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import com.hivemq.edge.adapters.plc4x.model.Plc4xAdapterConfig;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;


public class EIPAdapterConfig extends Plc4xAdapterConfig {

    @JsonProperty("port")
    @ModuleConfigField(title = "Port",
                       description = "The port number on the device you wish to connect to",
                       required = true,
                       numberMin = PORT_MIN,
                       numberMax = PORT_MAX,
                       defaultValue = "44818")
    private int port = 44818;

    @JsonProperty("backplane")
    @ModuleConfigField(title = "Backplane",
                       description = "Backplane device value",
                       defaultValue = "1",
                       required = false)
    private @NotNull Integer backplane;

    @JsonProperty("slot")
    @ModuleConfigField(title = "Slot", description = "Slot device value", defaultValue = "0", required = false)
    private @NotNull Integer slot;

    @JsonProperty("subscriptions")
    @ModuleConfigField(title = "Subscriptions", description = "Map your sensor data to MQTT Topics")
    private @NotNull List<AdapterSubscription> subscriptions = new ArrayList<>();

    @Override
    public int getPort() {
        return port;
    }

    @NotNull
    @Override
    public List<AdapterSubscription> getSubscriptions() {
        return subscriptions;
    }

    public enum EIP_DATA_TYPE {
        BOOL,
        DINT,
        INT,
        LINT,
        LREAL,
        LTIME,
        REAL,
        SINT,
        STRING,
        TIME,
        UDINT,
        UINT,
        ULINT,
        USINT;
    }

    @JsonPropertyOrder({"tagName", "tagAddress", "dataType", "destination", "qos"})
    @JsonIgnoreProperties({"dataType"})
    public static class AdapterSubscription extends AdapterSubscriptionImpl {
        @JsonProperty("eipDataType")
        @ModuleConfigField(title = "Data Type", description = "The expected data type of the tag", enumDisplayValues = {
                "Bool",
                "DInt",
                "Int",
                "LInt",
                "LReal",
                "LTime",
                "Real",
                "SInt",
                "String",
                "Time",
                "UDInt",
                "UInt",
                "ULInt",
                "USInt"}, required = true)
        private @NotNull EIPAdapterConfig.EIP_DATA_TYPE eipDataType;

        public EIPAdapterConfig.EIP_DATA_TYPE getEipDataType() {
            return eipDataType;
        }
    }

    public Integer getBackplane() {
        return backplane;
    }

    public Integer getSlot() {
        return slot;
    }
}
