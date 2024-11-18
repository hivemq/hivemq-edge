package com.hivemq.configuration.entity.adapter;

import com.hivemq.configuration.reader.ArbitraryValuesMapAdapter;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings({"FieldMayBeFinal", "unused"})
public class ProtocolAdapterEntity {

    @XmlElement(name = "config")
    @XmlJavaTypeAdapter(ArbitraryValuesMapAdapter.class)
    private @NotNull Map<String, Object> config = new HashMap<>();

    @XmlElement(name = "tags")
    @XmlJavaTypeAdapter(ArbitraryValuesMapAdapter.class)
    private @NotNull Map<String, Object> tags = new HashMap<>();

    @XmlElementWrapper(name = "fieldMappings")
    @XmlElement(name = "fieldMappings")
    private @NotNull List<FieldMappingsEntity> fieldMappingsEntities = new ArrayList<>();

    @XmlElementWrapper(name = "toEdgeMappings")
    @XmlElement(name = "toEdgeMapping")
    private @NotNull List<ToEdgeMappingEntity> toEdgeMappingEntities = new ArrayList<>();

    @XmlElementWrapper(name = "fromEdgeMappings")
    @XmlElement(name = "fromEdgeMapping")
    private @NotNull List<FromEdgeMappingEntity> fromEdgeMappingEntities = new ArrayList<>();

    // no-arg constructor for JaxB
    public ProtocolAdapterEntity() {
    }

    public @NotNull Map<String, Object> getConfig() {
        return config;
    }

    public @NotNull List<FieldMappingsEntity> getFieldMappingsEntities() {
        return fieldMappingsEntities;
    }

    public @NotNull List<FromEdgeMappingEntity> getFromEdgeMappingEntities() {
        return fromEdgeMappingEntities;
    }

    public @NotNull Map<String, Object> getTags() {
        return tags;
    }

    public @NotNull List<ToEdgeMappingEntity> getToEdgeMappingEntities() {
        return toEdgeMappingEntities;
    }
}
