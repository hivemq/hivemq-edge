package com.hivemq.protocols;

import com.hivemq.persistence.mappings.ToEdgeMapping;
import com.hivemq.persistence.mappings.fieldmapping.FieldMapping;
import org.jetbrains.annotations.NotNull;

public class InternalWritingContextImpl implements InternalWritingContext {

    private final @NotNull ToEdgeMapping toEdgeMapping;


    public InternalWritingContextImpl(@NotNull final ToEdgeMapping toEdgeMapping) {
        this.toEdgeMapping = toEdgeMapping;
    }

    @Override
    public FieldMapping getFieldMapping() {
        return toEdgeMapping.getFieldMapping();
    }

    @Override
    public @NotNull String getTagName() {
        return toEdgeMapping.getTagName();
    }

    @Override
    public @NotNull String getTopicFilter() {
        return toEdgeMapping.getTopicFilter();
    }

    @Override
    public int getMaxQoS() {
        return toEdgeMapping.getMaxQoS();
    }
}
