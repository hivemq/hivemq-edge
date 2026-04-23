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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import com.hivemq.adapter.sdk.api.tag.Tag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Represents an SNMP tag that maps an OID to a named data point.
 */
public class SnmpTag implements Tag {

    @JsonProperty(value = "name", required = true)
    @ModuleConfigField(title = "Name",
                       description = "Name of the tag to be used in mappings",
                       format = ModuleConfigField.FieldType.MQTT_TAG,
                       required = true)
    private final @NotNull String name;

    @JsonProperty(value = "description")
    @ModuleConfigField(title = "Description",
                       description = "A human-readable description of the tag")
    private final @NotNull String description;

    @JsonProperty(value = "definition", required = true)
    @ModuleConfigField(title = "Definition",
                       description = "The OID definition for this tag")
    private final @NotNull SnmpTagDefinition definition;

    public SnmpTag(
            @JsonProperty(value = "name", required = true) final @NotNull String name,
            @JsonProperty(value = "description") final @Nullable String description,
            @JsonProperty(value = "definition", required = true) final @NotNull SnmpTagDefinition definition) {
        this.name = name;
        this.description = Objects.requireNonNullElse(description, "");
        this.definition = definition;
    }

    @Override
    public @NotNull String getName() {
        return name;
    }

    @Override
    public @NotNull String getDescription() {
        return description;
    }

    @Override
    public @NotNull SnmpTagDefinition getDefinition() {
        return definition;
    }

    @Override
    public String toString() {
        return "SnmpTag{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", definition=" + definition +
                '}';
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof SnmpTag snmpTag)) return false;
        return Objects.equals(name, snmpTag.name) &&
                Objects.equals(description, snmpTag.description) &&
                Objects.equals(definition, snmpTag.definition);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description, definition);
    }
}
