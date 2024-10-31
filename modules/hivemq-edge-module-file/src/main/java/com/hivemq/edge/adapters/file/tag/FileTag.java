package com.hivemq.edge.adapters.file.tag;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import com.hivemq.adapter.sdk.api.tag.Tag;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class FileTag implements Tag<FileTagDefinition> {

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
    private final @NotNull FileTagDefinition definition;

    public FileTag(@JsonProperty(value = "name", required = true) final @NotNull String name,
                   @JsonProperty(value = "description", required = true) final @NotNull String description,
                   @JsonProperty(value = "definition", required = true) final @NotNull FileTagDefinition definiton) {
        this.name = name;
        this.description = description;
        this.definition = definiton;
    }

    @Override
    public @NotNull FileTagDefinition getDefinition() {
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
    public String toString() {
        return "FileTag{" +
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

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final FileTag fileTag = (FileTag) o;
        return Objects.equals(name, fileTag.name) &&
                Objects.equals(description, fileTag.description) &&
                Objects.equals(definition, fileTag.definition);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description, definition);
    }
}
