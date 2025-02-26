package com.hivemq.configuration.entity.combining;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.combining.model.DataIdentifierReference;
import org.jetbrains.annotations.NotNull;

import javax.xml.bind.annotation.XmlElement;

public class DataIdentifierReferenceEntity {

    @JsonProperty("id")
    @XmlElement(name = "id")
    private final @NotNull String id;

    @JsonProperty("type")
    @XmlElement(name = "type")
    private final @NotNull DataIdentifierReference.Type type;


    public DataIdentifierReferenceEntity() {
        this.id = "id";
        this.type = null;
    }

    public DataIdentifierReferenceEntity(@NotNull final String id, @NotNull final DataIdentifierReference.Type type) {
        this.id = id;
        this.type = type;
    }

    public @NotNull String getId() {
        return id;
    }

    public @NotNull DataIdentifierReference.Type getType() {
        return type;
    }
}
