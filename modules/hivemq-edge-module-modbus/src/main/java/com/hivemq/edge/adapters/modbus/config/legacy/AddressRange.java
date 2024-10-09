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
package com.hivemq.edge.adapters.modbus.config.legacy;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import com.hivemq.edge.adapters.modbus.config.ModbusAdapterConfig;

@JsonPropertyOrder({"startIdx", "endIdx"})
public class AddressRange {

    @JsonProperty(value = "startIdx", required = true)
    @ModuleConfigField(title = "Start Index",
                       description = "The Starting Index (Incl.) of the Address Range",
                       numberMin = 0,
                       numberMax = ModbusAdapterConfig.PORT_MAX,
                       required = true)
    public final int startIdx;

    @JsonProperty(value = "endIdx", required = true)
    @ModuleConfigField(title = "End Index",
                       description = "The Finishing Index (Excl.) of the Address Range",
                       numberMin = 1,
                       numberMax = ModbusAdapterConfig.PORT_MAX,
                       required = true)
    public final int endIdx;

    public AddressRange(
            @JsonProperty(value = "startIdx", required = true) final int startIdx,
            @JsonProperty(value = "endIdx", required = true) final int endIdx) {
        this.startIdx = startIdx;
        this.endIdx = endIdx;
    }
}
