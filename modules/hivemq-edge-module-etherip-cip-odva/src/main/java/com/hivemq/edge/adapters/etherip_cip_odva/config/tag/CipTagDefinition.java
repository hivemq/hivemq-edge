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
package com.hivemq.edge.adapters.etherip_cip_odva.config.tag;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.google.common.base.Objects;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import com.hivemq.adapter.sdk.api.tag.TagDefinition;
import com.hivemq.edge.adapters.etherip_cip_odva.config.CipDataType;
import java.io.Serial;
import java.io.Serializable;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@JsonPropertyOrder({
    "address",
    "numberOfElements",
    "dataType",
    "hysteresis",
    "minUpdateIntervalMs",
    "batchSize",
    "batchByteIndex",
    "batchBitIndex"
})
public class CipTagDefinition implements TagDefinition, Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private static final String TAG_ADDRESS_PATTERN = "@[0-9]+/[0-9]+/[0-9]+";

    @JsonProperty(value = "address", required = true)
    @ModuleConfigField(
            title = "address",
            description = "Address of the tag on the device. Format: @{class}/{instance}/{attribute}, ie: @4/100/1",
            stringPattern = TAG_ADDRESS_PATTERN,
            required = true)
    private final @NotNull String address;

    @JsonProperty(value = "numberOfElements", required = true)
    @ModuleConfigField(
            title = "Number of elements",
            description = "Number of elements to read. Default 1. If >1 reads an array starting at batchByteIndex",
            defaultValue = "1",
            numberMin = 1,
            numberMax = 1500,
            required = true)
    private final @NotNull Integer numberOfElements;

    @JsonProperty(value = "dataType", required = true)
    @ModuleConfigField(
            title = "Data Type",
            description = "The expected data type of the tag",
            defaultValue = "SINT",
            required = true)
    private final @NotNull CipDataType dataType;

    @JsonProperty(value = "hysteresis", required = true)
    @ModuleConfigField(
            title = "Hysteresis",
            description = "Minimal change/difference to send update (ignored for boolean and string)",
            defaultValue = "0",
            required = true)
    private final @NotNull Double hysteresis;

    @JsonProperty(value = "minUpdateIntervalMs")
    @ModuleConfigField(
            title = "minUpdateIntervalMs",
            description =
                    "Report value at minimum every X milliseconds (even if not changed). empty or 0 disables scheduled updates",
            defaultValue = "0",
            numberMin = 0,
            numberMax = 24 * 3600 * 1000)
    private final @NotNull Integer minUpdateIntervalMs;

    @JsonProperty(value = "batchByteIndex", required = true)
    @ModuleConfigField(
            title = "Byte index in batch",
            description = "Byte index of this tag in a batch (0 based)",
            defaultValue = "0",
            required = true)
    private final @NotNull Integer batchByteIndex;

    @JsonProperty(value = "batchBitIndex")
    @ModuleConfigField(
            title = "Bit index in batch",
            description =
                    "Bit index of this tag in a byte. Relevant only for a BOOL/FLAG data type), ignored for the rest. 'empty' checks whole SINT to be <> than 0",
            numberMin = 0,
            numberMax = 7)
    private final @Nullable Integer batchBitIndex;

    @JsonCreator
    public CipTagDefinition(
            @JsonProperty(value = "address", required = true) @NotNull final String address,
            @JsonProperty(value = "numberOfElements", required = true) @NotNull final Integer numberOfElements,
            @JsonProperty(value = "dataType", required = true) @NotNull final CipDataType dataType,
            @JsonProperty(value = "hysteresis") @NotNull final Double hysteresis,
            @JsonProperty(value = "minUpdateIntervalMs") final Integer minUpdateIntervalMs,
            @JsonProperty(value = "batchByteIndex") @NotNull final Integer batchByteIndex,
            @JsonProperty(value = "batchBitIndex") @Nullable final Integer batchBitIndex) {
        this.address = address;
        this.numberOfElements = numberOfElements;
        this.dataType = dataType;
        this.hysteresis = hysteresis;
        this.minUpdateIntervalMs = minUpdateIntervalMs == null ? 0 : minUpdateIntervalMs;
        this.batchByteIndex = batchByteIndex;
        this.batchBitIndex = batchBitIndex;
    }

    public @NotNull String getAddress() {
        return address;
    }

    public @NotNull Integer getNumberOfElements() {
        return numberOfElements;
    }

    public @NotNull CipDataType getDataType() {
        return dataType;
    }

    public @NotNull Double getHysteresis() {
        return hysteresis;
    }

    public @NotNull Integer getMinUpdateIntervalMs() {
        return minUpdateIntervalMs;
    }

    public @NotNull Integer getBatchByteIndex() {
        return batchByteIndex;
    }

    public @Nullable Integer getBatchBitIndex() {
        return batchBitIndex;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof final CipTagDefinition that)) {
            return false;
        }
        return Objects.equal(address, that.address)
                && Objects.equal(numberOfElements, that.numberOfElements)
                && dataType == that.dataType
                && Objects.equal(hysteresis, that.hysteresis)
                && Objects.equal(minUpdateIntervalMs, that.minUpdateIntervalMs)
                && Objects.equal(batchByteIndex, that.batchByteIndex)
                && Objects.equal(batchBitIndex, that.batchBitIndex);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(
                address, numberOfElements, dataType, hysteresis, minUpdateIntervalMs, batchByteIndex, batchBitIndex);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("address", address)
                .append("numberOfElements", numberOfElements)
                .append("dataType", dataType)
                .append("hysteresis", hysteresis)
                .append("minUpdateIntervalMs", minUpdateIntervalMs)
                .append("batchByteIndex", batchByteIndex)
                .append("batchBitIndex", batchBitIndex)
                .toString();
    }
}
