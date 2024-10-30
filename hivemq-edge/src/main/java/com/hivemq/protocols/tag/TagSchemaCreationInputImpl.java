package com.hivemq.protocols.tag;

import com.hivemq.adapter.sdk.api.schema.TagSchemaCreationInput;
import com.hivemq.extension.sdk.api.annotations.NotNull;

public class TagSchemaCreationInputImpl implements TagSchemaCreationInput {

    private final @NotNull String tagName;

    public TagSchemaCreationInputImpl(final @NotNull String tagName) {
        this.tagName = tagName;
    }

    @Override
    public @NotNull String getTagName() {
        return tagName;
    }
}
