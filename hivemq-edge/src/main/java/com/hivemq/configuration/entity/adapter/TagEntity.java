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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.adapter.sdk.api.tag.Tag;
import com.hivemq.configuration.entity.EntityValidatable;
import com.hivemq.configuration.reader.ArbitraryValuesMapAdapter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import jakarta.xml.bind.ValidationEvent;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
        name = "";
        description = "";
        definition = new HashMap<>();
    }

    public TagEntity(
            final @NotNull String name,
            final @Nullable String description,
            final @NotNull Map<String, Object> definition) {
        this.name = name;
        this.description = description;
        this.definition = definition;
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

    public static TagEntity fromAdapterTag(final @NotNull Tag tag, final @NotNull ObjectMapper objectMapper) {
        final Map<String, Object> definitionAsMap =
                objectMapper.convertValue(tag.getDefinition(), new TypeReference<>() {
                });
        return new TagEntity(tag.getName(), tag.getDescription(), definitionAsMap);
    }


    // this is very bad. This means that the field MUST be named name, description and definition
    public @NotNull Map<String, Object> toMap() {
        final HashMap<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("description", description);
        map.put("definition", definition);
        return map;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final TagEntity tagEntity = (TagEntity) o;
        return Objects.equals(getName(), tagEntity.getName()) &&
                Objects.equals(getDescription(), tagEntity.getDescription()) &&
                Objects.equals(getDefinition(), tagEntity.getDefinition());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getDescription(), getDefinition());
    }

    @Override
    public void validate(final @NotNull List<ValidationEvent> validationEvents) {
        EntityValidatable.notEmpty(validationEvents, name, "tag name");
    }
}
