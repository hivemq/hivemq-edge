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

import static java.util.Objects.requireNonNull;

import com.hivemq.configuration.entity.EntityValidatable;
import com.hivemq.configuration.reader.ArbitraryValuesMapAdapter;
import jakarta.xml.bind.ValidationEvent;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TagEntity implements EntityValidatable {

    @XmlElement(name = "name", required = true)
    private final @NotNull String name;

    @XmlElement(name = "description")
    private final @Nullable String description;

    @XmlElement(name = "definition")
    @XmlJavaTypeAdapter(ArbitraryValuesMapAdapter.class)
    private final @NotNull Map<String, Object> definition;

    // no-arg constructor for JaxB
    public TagEntity() {
        this("", "", new HashMap<>());
    }

    public TagEntity(
            final @NotNull String name,
            final @Nullable String description,
            final @NotNull Map<String, Object> definition) {
        this.name = requireNonNull(name);
        this.definition = requireNonNull(definition);
        this.description = Objects.requireNonNullElse(description, "");
    }

    public @NotNull Map<String, Object> getDefinition() {
        return definition;
    }

    public @Nullable String getDescription() {
        return description;
    }

    public @NotNull String getName() {
        return name;
    }

    public @NotNull Map<String, Object> toMap() {
        // this is very bad. This means that the field MUST be named name, description and definition
        return Map.of("name", name, "description", description != null ? description : "", "definition", definition);
    }

    @Override
    public boolean equals(final @Nullable Object o) {
        if (o instanceof final TagEntity that) {
            return Objects.equals(name, that.name)
                    && Objects.equals(definition, that.definition)
                    && Objects.equals(description, that.description);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description, definition);
    }

    @Override
    public void validate(final @NotNull List<ValidationEvent> validationEvents) {
        EntityValidatable.notEmpty(validationEvents, name, "tag name");
    }
}
