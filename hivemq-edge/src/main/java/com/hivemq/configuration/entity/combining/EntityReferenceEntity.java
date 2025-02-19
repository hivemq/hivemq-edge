package com.hivemq.configuration.entity.combining;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.combining.EntityType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.bind.annotation.XmlElement;

public class EntityReferenceEntity {

    @JsonProperty("type")
    @XmlElement(name = "type")
    private final @NotNull EntityType type;

    @JsonProperty("id")
    @XmlElement(name = "id")
    private final @NotNull String id;

    @JsonProperty("isPrimary")
    @XmlElement(name = "isPrimary")
    private final boolean isPrimary;

    // no-arg for jaxb
    public EntityReferenceEntity() {
        isPrimary = false;
        id = "";
        type = EntityType.EDGE_BROKER;
    }

    public EntityReferenceEntity(@NotNull final EntityType type, @NotNull final String id, final boolean isPrimary) {
        this.type = type;
        this.id = id;
        this.isPrimary = isPrimary;
    }

    @Override
    public @NotNull String toString() {
        return "EntityReferenceEntity{" + "id='" + id + '\'' + ", type=" + type + ", isPrimary=" + isPrimary + '}';
    }

    @Override
    public boolean equals(final @Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final EntityReferenceEntity that = (EntityReferenceEntity) o;
        return isPrimary == that.isPrimary && type == that.type && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + id.hashCode();
        result = 31 * result + Boolean.hashCode(isPrimary);
        return result;
    }

    public @NotNull String getId() {
        return id;
    }

    public boolean isPrimary() {
        return isPrimary;
    }

    public @NotNull EntityType getType() {
        return type;
    }



}
