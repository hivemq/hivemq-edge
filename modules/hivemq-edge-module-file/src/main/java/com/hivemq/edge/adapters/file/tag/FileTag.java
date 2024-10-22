package com.hivemq.edge.adapters.file.tag;

import com.hivemq.adapter.sdk.api.tag.Tag;
import org.jetbrains.annotations.NotNull;

public class FileTag implements Tag<FileTagAddress> {

    private final @NotNull String tagName;
    private final @NotNull FileTagAddress fileTagAddress;

    public FileTag(final @NotNull String tagName, final @NotNull FileTagAddress fileTagAddress) {
        this.tagName = tagName;
        this.fileTagAddress = fileTagAddress;
    }


    @Override
    public @NotNull FileTagAddress getTagDefinition() {
        return fileTagAddress;
    }

    @Override
    public @NotNull String getTagName() {
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
        return tagName.equals(fileTag.tagName) && fileTagAddress.equals(fileTag.fileTagAddress);
    }

    @Override
    public int hashCode() {
        int result = tagName.hashCode();
        result = 31 * result + fileTagAddress.hashCode();
        return result;
    }
}
