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
package com.hivemq.edge.adapters.snmp.config.tag;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import com.hivemq.adapter.sdk.api.tag.TagDefinition;
import com.hivemq.edge.adapters.snmp.config.SnmpDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

import static java.util.Objects.requireNonNullElse;

/**
 * Defines an SNMP tag using an Object Identifier (OID).
 */
public class SnmpTagDefinition implements TagDefinition {

    private static final @NotNull String OID_REGEX = "^[0-9]+(\\.[0-9]+)*$";

    @JsonProperty(value = "oid", required = true)
    @ModuleConfigField(title = "OID",
                       description = "SNMP Object Identifier (e.g., 1.3.6.1.2.1.1.5.0 for sysName)",
                       required = true,
                       stringPattern = OID_REGEX)
    private final @NotNull String oid;

    @JsonProperty(value = "dataType")
    @ModuleConfigField(title = "Data Type",
                       description = "Expected SNMP data type. Use AUTO for automatic detection.",
                       defaultValue = "AUTO")
    private final @NotNull SnmpDataType dataType;

    @JsonCreator
    public SnmpTagDefinition(
            @JsonProperty(value = "oid", required = true) final @NotNull String oid,
            @JsonProperty(value = "dataType") final @Nullable SnmpDataType dataType) {
        this.oid = oid;
        this.dataType = requireNonNullElse(dataType, SnmpDataType.AUTO);
    }

    public @NotNull String getOid() {
        return oid;
    }

    public @NotNull SnmpDataType getDataType() {
        return dataType;
    }

    @Override
    public String toString() {
        return "SnmpTagDefinition{" +
                "oid='" + oid + '\'' +
                ", dataType=" + dataType +
                '}';
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof SnmpTagDefinition that)) return false;
        return Objects.equals(oid, that.oid) && dataType == that.dataType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(oid, dataType);
    }
}
