package com.hivemq.edge.adapters.file.tag;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FileTagAddress {

    @JsonProperty(value = "filePath", required = true)
    @ModuleConfigField(title = "The file path",
                       description = "The absolute path to the file that should be scraped.",
                       required = true)
    private final @NotNull String filePath;

    @JsonCreator
    public FileTagAddress(@JsonProperty("filePath") final @NotNull String filePath) {
        this.filePath = filePath;
    }

    public @NotNull String getFilePath() {
        return filePath;
    }

    @Override
    public boolean equals(@Nullable final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final FileTagAddress that = (FileTagAddress) o;
        return filePath.equals(that.filePath);
    }

    @Override
    public int hashCode() {
        return filePath.hashCode();
    }
}
