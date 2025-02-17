package com.hivemq.configuration.entity.combining;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.bind.ValidationEvent;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import java.util.List;
import java.util.UUID;


public class DataCombinerEntity {
    @JsonProperty("id")
    @XmlElement(name = "id")
    private final @NotNull UUID id;

    @JsonProperty("name")
    @XmlElement(name = "name")
    private final @NotNull String name;

    @JsonProperty("description")
    @XmlElement(name = "description")
    private final @NotNull String description;

    @JsonProperty("entityReferences")
    @XmlElementWrapper(name = "entityReferences", required = true)
    private final @NotNull List<EntityReferenceEntity> entityReferenceEntities;

    @JsonProperty("dataCombinings")
    @XmlElementWrapper(name = "dataCombinings", required = true)
    private final @NotNull List<DataCombiningEntity> dataCombiningEntities;

    // no-arg for jaxb
    public DataCombinerEntity() {
        dataCombiningEntities = List.of();
        entityReferenceEntities = List.of();
        description = "";
        name = "";
        id = UUID.randomUUID();
    }

    public DataCombinerEntity(
            final @NotNull UUID id,
            final @NotNull String name,
            final @NotNull String description,
            final @NotNull List<EntityReferenceEntity> entityReferenceEntities,
            final @NotNull List<DataCombiningEntity> dataCombiningEntities) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.entityReferenceEntities = entityReferenceEntities;
        this.dataCombiningEntities = dataCombiningEntities;
    }

    public void validate(final @NotNull List<ValidationEvent> validationErrors) {
        //TODO
    }

    @Override
    public @NotNull String toString() {
        return "DataCombinerEntity{" +
                "dataCombiningEntities=" +
                dataCombiningEntities +
                ", id=" +
                id +
                ", name='" +
                name +
                '\'' +
                ", description='" +
                description +
                '\'' +
                ", entityReferenceEntities=" +
                entityReferenceEntities +
                '}';
    }

    @Override
    public boolean equals(final @Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final DataCombinerEntity that = (DataCombinerEntity) o;
        return id.equals(that.id) &&
                name.equals(that.name) &&
                description.equals(that.description) &&
                entityReferenceEntities.equals(that.entityReferenceEntities) &&
                dataCombiningEntities.equals(that.dataCombiningEntities);
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + name.hashCode();
        result = 31 * result + description.hashCode();
        result = 31 * result + entityReferenceEntities.hashCode();
        result = 31 * result + dataCombiningEntities.hashCode();
        return result;
    }

    public @NotNull List<DataCombiningEntity> getDataCombiningEntities() {
        return dataCombiningEntities;
    }

    public @NotNull String getDescription() {
        return description;
    }

    public @NotNull List<EntityReferenceEntity> getEntityReferenceEntities() {
        return entityReferenceEntities;
    }

    public @NotNull UUID getId() {
        return id;
    }

    public @NotNull String getName() {
        return name;
    }
}
