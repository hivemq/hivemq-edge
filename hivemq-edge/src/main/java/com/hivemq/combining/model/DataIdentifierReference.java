package com.hivemq.combining.model;

import com.hivemq.configuration.entity.combining.DataIdentifierReferenceEntity;
import org.jetbrains.annotations.NotNull;

public record DataIdentifierReference(String id, Type type) {
    public static @NotNull DataIdentifierReference from(final @NotNull com.hivemq.edge.api.model.DataIdentifierReference model) {
        return new DataIdentifierReference(model.getId(), Type.from(model.getType()));
    }

    public @NotNull com.hivemq.edge.api.model.DataIdentifierReference to() {
        return new com.hivemq.edge.api.model.DataIdentifierReference(this.id(), this.type().to());
    }

    public static DataIdentifierReference fromPersistence(final @NotNull DataIdentifierReferenceEntity entity) {
        return new DataIdentifierReference(entity.getId(), entity.getType());
    }

    public DataIdentifierReferenceEntity toPersistence() {
        return new DataIdentifierReferenceEntity(this.id(), this.type);
    }

    public enum Type {
        TAG,
        TOPIC_FILTER;

        public static @NotNull Type from(final com.hivemq.edge.api.model.DataIdentifierReference.@NotNull TypeEnum type) {
            switch (type) {
                case TAG -> {
                    return TAG;
                }
                case TOPIC_FILTER -> {
                    return TOPIC_FILTER;
                }
            }
            throw new IllegalArgumentException();
        }

        public com.hivemq.edge.api.model.DataIdentifierReference.@NotNull TypeEnum to() {
            switch (this) {
                case TAG -> {
                    return com.hivemq.edge.api.model.DataIdentifierReference.TypeEnum.TAG;
                }
                case TOPIC_FILTER -> {
                    return com.hivemq.edge.api.model.DataIdentifierReference.TypeEnum.TOPIC_FILTER;
                }
            }
            throw new IllegalArgumentException();
        }
    }

}
