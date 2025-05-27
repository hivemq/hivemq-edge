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
package com.hivemq.edge.adapters.etherip.config.tag;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import com.hivemq.adapter.sdk.api.tag.TagDefinition;
import com.hivemq.edge.adapters.etherip.config.EipDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class EipTagDefinition implements TagDefinition {

    @JsonProperty(value = "address", required = true)
    @ModuleConfigField(title = "address",
                       description = "Address of the tag on the device",
                       required = true)
    private final @NotNull String address;

    @JsonProperty(value = "dataType", required = true)
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
    private final @NotNull EipDataType dataType;

    @JsonCreator
    public EipTagDefinition(@JsonProperty(value = "address", required = true) final @NotNull String address,
                            @JsonProperty(value = "dataType", required = true) final @NotNull EipDataType dataType) {
        this.address = address;
        this.dataType = dataType;
    }

    public @NotNull String getAddress() {
        return address;
    }

    public @NotNull EipDataType getDataType() {
        return dataType;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final EipTagDefinition that = (EipTagDefinition) o;
        return Objects.equals(address, that.address) && dataType == that.dataType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(address, dataType);
    }

    @Override
    public String toString() {
        return "EipTagDefinition{" + "address='" + address + '\'' + ", dataType=" + dataType + '}';
    }
}
