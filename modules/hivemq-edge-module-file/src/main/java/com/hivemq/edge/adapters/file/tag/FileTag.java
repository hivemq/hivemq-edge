package com.hivemq.edge.adapters.file.tag;

import com.hivemq.adapter.sdk.api.tag.Tag;
import org.jetbrains.annotations.NotNull;

public class FileTag implements Tag<FileTagDefinition> {

    private final @NotNull String tagName;
    private final @NotNull FileTagDefinition fileTagDefinition;

    public FileTag(final @NotNull String tagName, final @NotNull FileTagDefinition fileTagDefinition) {
        this.tagName = tagName;
        this.fileTagDefinition = fileTagDefinition;
    }


    @Override
    public @NotNull FileTagDefinition getDefinition() {
        return fileTagDefinition;
    }

    @Override
    public @NotNull String getName() {
        return tagName;
    }

    @Override
    public boolean equals(@NotNull final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final FileTag fileTag = (FileTag) o;
        return tagName.equals(fileTag.tagName) && fileTagDefinition.equals(fileTag.fileTagDefinition);
    }

    @Override
    public int hashCode() {
        int result = tagName.hashCode();
        result = 31 * result + fileTagDefinition.hashCode();
        return result;
    }
}
