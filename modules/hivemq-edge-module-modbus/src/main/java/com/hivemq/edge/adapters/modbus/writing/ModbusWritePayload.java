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
package com.hivemq.edge.adapters.modbus.writing;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import com.hivemq.adapter.sdk.api.writing.WritePayload;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ModbusWritePayload implements WritePayload {
    @JsonProperty("value")
    @ModuleConfigField(title = "Value", description = "The value that should be written", required = true)
    private @NotNull Object value;


    public ModbusWritePayload(
            final @NotNull @JsonProperty("value") Object value) {
        this.value = value;
    }

    public @NotNull Object getValue() {
        return value;
    }

    @Override
    public boolean equals(final @Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ModbusWritePayload payload = (ModbusWritePayload) o;
        return value.equals(payload.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}