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
package com.hivemq.edge.adapters.plc4x.config.tag;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import com.hivemq.adapter.sdk.api.tag.TagDefinition;
import com.hivemq.edge.adapters.plc4x.config.Plc4xDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class Plc4xTagDefinition implements TagDefinition {

    @JsonProperty(value = "tagAddress", required = true)
    @ModuleConfigField(title = "Tag Address",
                       description = "The well formed address of the tag to read",
                       required = true)
    private final @NotNull String tagAddress;

    @JsonProperty(value = "dataType", required = true)
    @ModuleConfigField(title = "Data Type", description = "The expected data type of the tag", enumDisplayValues = {
            "Null",
            "Boolean",
            "Byte",
            "Word (unit 16)",
            "DWord (uint 32)",
            "LWord (uint 64)",
            "USint (uint 8)",
            "Uint (uint 16)",
            "UDint (uint 32)",
            "ULint (uint 64)",
            "Sint (int 8)",
            "Int (int 16)",
            "Dint (int 32)",
            "Lint (int 64)",
            "Real (float 32)",
            "LReal (double 64)",
            "Char (1 byte char)",
            "WChar (2 byte char)",
            "String",
            "WString",
            "Timing (Duration ms)",
            "Long Timing (Duration ns)",
            "Date (DateStamp)",
            "Long Date (DateStamp)",
            "Time Of Day (TimeStamp)",
            "Long Time Of Day (TimeStamp)",
            "Date Time (DateTimeStamp)",
            "Long Date Time (DateTimeStamp)",
            "Raw Byte Array"}, required = true)
    private final @NotNull Plc4xDataType.DATA_TYPE dataType;

    @JsonCreator
    public Plc4xTagDefinition(
            @JsonProperty("tagAddress") final @NotNull String tagAddress,
            @JsonProperty(value = "dataType", required = true) final @NotNull Plc4xDataType.DATA_TYPE dataType) {
        this.tagAddress = tagAddress;
        this.dataType = dataType;
    }

    public @NotNull Plc4xDataType.DATA_TYPE getDataType() {
        return dataType;
    }

    public @NotNull String getTagAddress() {
        return tagAddress;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final Plc4xTagDefinition that = (Plc4xTagDefinition) o;
        return Objects.equals(tagAddress, that.tagAddress) && dataType == that.dataType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(tagAddress, dataType);
    }

    @Override
    public String toString() {
        return "Plc4xTagDefinition{" + "tagAddress='" + tagAddress + '\'' + ", dataType=" + dataType + '}';
    }
}
