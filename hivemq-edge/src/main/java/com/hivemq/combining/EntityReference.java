package com.hivemq.combining;

import com.hivemq.configuration.entity.combining.EntityReferenceEntity;
import org.jetbrains.annotations.NotNull;

public record EntityReference(EntityType type, String id, boolean isPrimary) {

    public static @NotNull EntityReference fromModel(final @NotNull com.hivemq.edge.api.model.EntityReference entityReference) {
        return new EntityReference(EntityType.fromModel(entityReference.getType()),
                entityReference.getId(),
                entityReference.getIsPrimary());

    }

    public @NotNull com.hivemq.edge.api.model.EntityReference toModel() {
        return new com.hivemq.edge.api.model.EntityReference().id(id).type(type.toModel()).isPrimary(isPrimary);
    }

    public static @NotNull EntityReference fromPersistence(final @NotNull EntityReferenceEntity entityReference) {
        return new EntityReference(entityReference.getType(), entityReference.getId(), entityReference.isPrimary());

    }

    public @NotNull EntityReferenceEntity toPersistence() {
        return new EntityReferenceEntity(type, id, isPrimary);
    }

}
