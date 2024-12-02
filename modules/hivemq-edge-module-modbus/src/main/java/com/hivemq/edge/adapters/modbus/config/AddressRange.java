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
package com.hivemq.edge.adapters.modbus.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import org.jetbrains.annotations.NotNull;

@JsonPropertyOrder({"startIdx", "nrRegistersToRead"})
public class AddressRange {

    @JsonProperty(value = "startIdx", required = true)
    @ModuleConfigField(title = "Start Index",
                       description = "The Starting Index (Incl.) of the Address Range",
                       numberMin = 0,
                       numberMax = ModbusSpecificAdapterConfig.PORT_MAX,
                       required = true)
    public final int startIdx;

    @JsonProperty(value = "readType", required = true)
    @ModuleConfigField(title = "The way the register range should be read",
                       description = "Type of read to performe on the registers",
                       required = true)
    public final @NotNull ModbusAdu readType;

    @JsonProperty(value = "unitId", required = true)
    @ModuleConfigField(title = "The id of the unit to access",
                       description = "Id of the unit to access on the modbus",
                       required = true)
    public final int unitId;

    @JsonProperty(value = "flipRegisters", defaultValue = "false")
    @ModuleConfigField(title = "Indicates if registers should be evaluated in reverse order",
                       description = "Registers and their contents are normally written/read as big endian, some implementations decided to write the content as big endian but to order the actual registers as little endian.",
                       defaultValue = "false")
    public final boolean flipRegisters;

    public AddressRange(
            @JsonProperty(value = "startIdx", required = true) final int startIdx,
            @JsonProperty(value = "readType", required = true) final @NotNull ModbusAdu readType,
            @JsonProperty(value = "unitId", required = true) final int unitId,
            @JsonProperty(value = "flipRegisters", defaultValue = "false") final boolean flipRegisters) {
        this.startIdx = startIdx;
        this.readType = readType;
        this.unitId = unitId;
        this.flipRegisters = flipRegisters;
    }
}
