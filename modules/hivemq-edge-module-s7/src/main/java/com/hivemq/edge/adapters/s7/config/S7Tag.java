package com.hivemq.edge.adapters.s7.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import com.hivemq.adapter.sdk.api.tag.Tag;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class S7Tag implements Tag {

    @JsonProperty(value = "name", required = true)
    @ModuleConfigField(title = "name",
                       description = "name of the tag to be used in mappings",
                       required = true)
    private final @NotNull String name;

    @JsonProperty(value = "description", required = true)
    @ModuleConfigField(title = "description",
                       description = "A human readable description of the tag",
                       required = true)
    private final @NotNull String description;

    @JsonProperty(value = "definition", required = true)
    @ModuleConfigField(title = "definition",
                       description = "The actual definition of the tag on the device",
                       required = true)
    private final @NotNull S7TagDefinition definition;


    public S7Tag(
            @JsonProperty(value = "name", required = true) final @NotNull String name,
            @JsonProperty(value = "description", required = true) final @NotNull String description,
            @JsonProperty(value = "definition", required = true) final @NotNull S7TagDefinition definiton) {
        this.name = name;
        this.description = description;
        this.definition = definiton;
    }

    @Override
    public @NotNull S7TagDefinition getDefinition() {
        return definition;
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
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final S7Tag s7Tag = (S7Tag) o;
        return Objects.equals(name, s7Tag.name) &&
                Objects.equals(description, s7Tag.description) &&
                Objects.equals(definition, s7Tag.definition);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description, definition);
    }

    @Override
    public String toString() {
        return "S7Tag{" +
                "name='" +
                name +
                '\'' +
                ", description='" +
                description +
                '\'' +
                ", definition=" +
                definition +
                '}';
    }
}
