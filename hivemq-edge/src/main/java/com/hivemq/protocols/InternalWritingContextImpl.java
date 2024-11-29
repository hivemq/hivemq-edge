package com.hivemq.protocols;

import com.hivemq.persistence.mappings.SoutboundMapping;
import com.hivemq.persistence.mappings.fieldmapping.FieldMapping;
import org.jetbrains.annotations.NotNull;

public class InternalWritingContextImpl implements InternalWritingContext {

    private final @NotNull SoutboundMapping soutboundMapping;


    public InternalWritingContextImpl(@NotNull final SoutboundMapping soutboundMapping) {
        this.soutboundMapping = soutboundMapping;
    }

    @Override
    public FieldMapping getFieldMapping() {
        return soutboundMapping.getFieldMapping();
    }

    @Override
    public @NotNull String getTagName() {
        return soutboundMapping.getTagName();
    }

    @Override
    public @NotNull String getTopicFilter() {
        return soutboundMapping.getTopicFilter();
    }

    @Override
    public int getMaxQoS() {
        return soutboundMapping.getMaxQoS();
    }
}
