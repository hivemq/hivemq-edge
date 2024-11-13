package com.hivemq.edge.adapters.file.tag;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import com.hivemq.adapter.sdk.api.tag.TagDefinition;
import com.hivemq.edge.adapters.file.config.ContentType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class FileTagDefinition implements TagDefinition {

    @JsonProperty(value = "filePath", required = true)
    @ModuleConfigField(title = "The file path",
                       description = "The absolute path to the file that should be scraped.",
                       required = true)
    private final @NotNull String filePath;

    @JsonProperty(value = "contentType", required = true)
    @ModuleConfigField(title = "Content Type",
                       description = "The type of the content within the file.",
                       enumDisplayValues = {
                               "application/octet-stream",
                               "text/plain",
                               "application/json",
                               "application/xml",
                               "text/csv"},
                       required = true)
    private final @NotNull ContentType contentType;

    @JsonCreator
    public FileTagDefinition(@JsonProperty("filePath") final @NotNull String filePath,
                             @JsonProperty(value = "contentType", required = true) final @NotNull ContentType contentType) {
        this.filePath = filePath;
        this.contentType = contentType;
    }

    public @NotNull String getFilePath() {
        return filePath;
    }

    public @NotNull ContentType getContentType() {
        return contentType;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final FileTagDefinition that = (FileTagDefinition) o;
        return Objects.equals(filePath, that.filePath) && contentType == that.contentType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(filePath, contentType);
    }

    @Override
    public String toString() {
        return "FileTagDefinition{" + "filePath='" + filePath + '\'' + ", contentType=" + contentType + '}';
    }
}
