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
import com.hivemq.edge.adapters.plc4x.model.Plc4xAdapterConfig;
import com.hivemq.edge.adapters.plc4x.types.ab.ABAdapterConfig;
import com.hivemq.edge.modules.adapters.annotations.ModuleConfigField;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.math.BigInteger;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;


public class EIPAdapterConfig extends Plc4xAdapterConfig {

    @JsonProperty("backplane")
    @ModuleConfigField(title = "Backplane",
                       description = "Backplane device value",
                       required = true)
    private @NotNull Integer backplane;

    @JsonProperty("slot")
    @ModuleConfigField(title = "Slot",
                       description = "Slot device value",
                       defaultValue = "0",
                       required = true)
    private @NotNull Integer slot;

    @JsonProperty("subscriptions")
    @ModuleConfigField(title = "Subscriptions",
                       description = "Map your sensor data to MQTT Topics")
    private @NotNull List<? extends EIPAdapterConfig.Subscription> subscriptions = new ArrayList<>();

    public enum EIP_DATA_TYPE {
        BOOL((short) 0x01, Boolean.class),
        DWORD((short) 0x04, Integer.class),
        SINT((short) 0x21, Byte.class),
        INT((short) 0x22, Short.class),
        DINT((short) 0x23, Integer.class),
        LINT((short) 0x24, Long.class),
        REAL((short) 0x31, Float.class);

        EIP_DATA_TYPE(short code, Class<?> javaType){
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
        @JsonProperty("eipDataType")
        @ModuleConfigField(title = "Data Type",
                           description = "The expected data type of the tag",
                           enumDisplayValues = {
                                   "Boolean (unit 16)",
                                   "DWord (uint 32)",
                                   "SInt (int 16)",
                                   "Integer (int 16)",
                                   "DInt (int 32)",
                                   "LInt (int 64)",
                                   "Real (float 32)",
                           },
                           required = true)
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
