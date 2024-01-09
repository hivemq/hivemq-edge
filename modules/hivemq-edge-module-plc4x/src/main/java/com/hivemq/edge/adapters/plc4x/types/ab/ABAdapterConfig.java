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
package com.hivemq.edge.adapters.plc4x.types.ab;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.hivemq.edge.adapters.plc4x.model.Plc4xAdapterConfig;
import com.hivemq.edge.modules.adapters.annotations.ModuleConfigField;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ABAdapterConfig extends Plc4xAdapterConfig {

    @JsonProperty("station")
    @ModuleConfigField(title = "Station",
                       description = "IP Address or hostname of the device you wish to connect to",
                       required = true)
    private @NotNull String station;

    @JsonProperty("subscriptions")
    @ModuleConfigField(title = "Subscriptions",
                       description = "Map your sensor data to MQTT Topics")
    private @NotNull List<? extends Subscription> subscriptions = new ArrayList<>();

    public String getStation() {
        return station;
    }

    public enum AB_DATA_TYPE {

        WORD((short) 0x03, Short.class),
        DWORD((short) 0x04, Integer.class),
        INTEGER((short) 0x71, Byte.class);

        AB_DATA_TYPE(short code, Class<?> javaType){
            this.code = code;
            this.javaType = javaType;
        }

        private short code;
        private Class<?> javaType;

        public short getCode() {
            return code;
        }

        public Class<?> getJavaType() {
            return javaType;
        }
    }

    @JsonPropertyOrder({"tagName", "tagAddress", "dataType", "destination", "qos"})
    @JsonIgnoreProperties({"dataType"})
    public static class Subscription extends Plc4xAdapterConfig.Subscription {
        @JsonProperty("abDataType")
        @ModuleConfigField(title = "Data Type",
                           description = "The expected data type of the tag",
                           enumDisplayValues = {
                                   "Word (unit 16)",
                                   "DWord (uint 32)",
                                   "Integer (int 16)",
                           },
                           required = true)
        private @NotNull ABAdapterConfig.AB_DATA_TYPE abDataType;

        public ABAdapterConfig.AB_DATA_TYPE getAbDataType() {
            return abDataType;
        }
    }
}
