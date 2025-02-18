package com.hivemq.combining;

import com.hivemq.configuration.entity.combining.DataCombinerEntity;
import com.hivemq.configuration.entity.combining.DataCombiningEntity;
import com.hivemq.configuration.entity.combining.EntityReferenceEntity;
import com.hivemq.edge.api.model.Combiner;
import com.hivemq.edge.api.model.DataCombiningList;
import com.hivemq.edge.api.model.EntityReferenceList;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record DataCombiner(UUID id, String name, String description, List<EntityReference> entityReferences,
                           List<DataCombining> dataCombinings) {

    public static @NotNull DataCombiner fromModel(final @NotNull Combiner combiner) {
        final List<DataCombining> combining;

        if (combiner.getMappings() != null) {
            combining = combiner.getMappings().getItems().stream().map(DataCombining::fromModel).toList();
        } else {
            combining = new ArrayList<>();
        }

        final List<EntityReference> referenceList =
                combiner.getSources().getItems().stream().map(EntityReference::fromModel).toList();
        return new DataCombiner(combiner.getId(),
                combiner.getName(),
                combiner.getDescription(),
                referenceList,
                combining);
    }

    public @NotNull Combiner toModel() {
        final List<com.hivemq.edge.api.model.DataCombining> combining =
                this.dataCombinings().stream().map(DataCombining::toModel).toList();
        final List<com.hivemq.edge.api.model.EntityReference> sources =
                this.entityReferences().stream().map(EntityReference::toModel).toList();
        return new Combiner().id(id)
                .name(name)
                .description(description)
                .sources(new EntityReferenceList().items(sources))
                .mappings(new DataCombiningList().items(combining));
    }

    public static @NotNull DataCombiner fromPersistence(final @NotNull DataCombinerEntity combiner) {
        final List<DataCombining> combining =
                combiner.getDataCombiningEntities().stream().map(DataCombining::fromPersistence).toList();
        final List<EntityReference> referenceList =
                combiner.getEntityReferenceEntities().stream().map(EntityReference::fromPersistence).toList();
        return new DataCombiner(combiner.getId(),
                combiner.getName(),
                combiner.getDescription(),
                referenceList,
                combining);
    }

    public @NotNull DataCombinerEntity toPersistence() {
        final List<DataCombiningEntity> combining = dataCombinings.stream().map(DataCombining::toPersistence).toList();
        final List<EntityReferenceEntity> sources =
                entityReferences.stream().map(EntityReference::toPersistence).toList();
        return new DataCombinerEntity(id, name, description, sources, combining);
    }

}
