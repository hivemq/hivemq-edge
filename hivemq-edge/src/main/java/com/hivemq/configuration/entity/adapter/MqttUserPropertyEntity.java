/*
 * Copyright 2019-present HiveMQ GmbH
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
package com.hivemq.configuration.entity.adapter;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.NotNull;

import jakarta.xml.bind.annotation.XmlElement;
import java.util.Objects;

public class MqttUserPropertyEntity {

    @XmlElement(name = "name", required = true)
    private final @NotNull String name;

    @XmlElement(name = "value", required = true)
    private final @NotNull String value;

    public MqttUserPropertyEntity() {
        name = "";
        value = "";
    }

    public MqttUserPropertyEntity(
            @JsonProperty(value = "name", required = true) final @NotNull String name,
            @JsonProperty(value = "value", required = true) final @NotNull String value) {
        this.name = name;
        this.value = value;
    }

    /**
     * @return the name for this user property.
     */
    public @NotNull String getName() {
        return name;
    }

    /**
     * @return the value of this user property.
     */
    public @NotNull String getValue() {
        return value;
    }

    @Override
    public @NotNull String toString() {
        return "MqttUserProperty{" + "name='" + name + '\'' + ", value='" + value + '\'' + '}';
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final MqttUserPropertyEntity that = (MqttUserPropertyEntity) o;
        return Objects.equals(getName(), that.getName()) && Objects.equals(getValue(), that.getValue());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getValue());
    }
}
