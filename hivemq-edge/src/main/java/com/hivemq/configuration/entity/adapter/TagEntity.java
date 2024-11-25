package com.hivemq.configuration.entity.adapter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.adapter.sdk.api.tag.Tag;
import com.hivemq.configuration.reader.ArbitraryValuesMapAdapter;
import org.jetbrains.annotations.NotNull;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.HashMap;
import java.util.Map;

public class TagEntity {


    @XmlElement(name = "name", required = true)
    private final @NotNull String name;

    @XmlElement(name = "description", required = true)
    private final @NotNull String description;

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
            final @NotNull String description,
            final @NotNull Map<String, Object> definition) {
        this.name = name;
        this.description = description;
        this.definition = definition;
    }

    public @NotNull Map<String, Object> getDefinition() {
        return definition;
    }

    public @NotNull String getDescription() {
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
}
