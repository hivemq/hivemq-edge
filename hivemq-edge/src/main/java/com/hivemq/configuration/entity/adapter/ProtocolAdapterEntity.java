package com.hivemq.configuration.entity.adapter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.configuration.reader.ArbitraryValuesMapAdapter;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.protocols.ProtocolAdapterConfig;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@SuppressWarnings({"FieldMayBeFinal", "unused"})
public class ProtocolAdapterEntity {

    @XmlElement(name = "adapterId")
    private @NotNull String adapterId;

    @XmlElement(name = "protocolId")
    private @NotNull String protocolId;

    @XmlElement(name = "config")
    @XmlJavaTypeAdapter(ArbitraryValuesMapAdapter.class)
    private @NotNull Map<String, Object> config = new HashMap<>();

    @XmlElement(name = "tags")
    @XmlJavaTypeAdapter(ArbitraryValuesMapAdapter.class)
    private @NotNull List<Map<String, Object>> tags = new ArrayList<>();

    @XmlElementWrapper(name = "toEdgeMappings")
    @XmlElement(name = "toEdgeMapping")
    private @NotNull List<ToEdgeMappingEntity> toEdgeMappingEntities = new ArrayList<>();

    @XmlElementWrapper(name = "fromEdgeMappings")
    @XmlElement(name = "fromEdgeMapping")
    private @NotNull List<FromEdgeMappingEntity> fromEdgeMappingEntities = new ArrayList<>();

    // no-arg constructor for JaxB
    public ProtocolAdapterEntity() {
    }

    public ProtocolAdapterEntity(
            @NotNull final String adapterId,
            @NotNull final String protocolId,
            @NotNull final Map<String, Object> config,
            @NotNull final List<FromEdgeMappingEntity> fromEdgeMappingEntities,
            @NotNull final List<ToEdgeMappingEntity> toEdgeMappingEntities,
            @NotNull final List<Map<String, Object>> tags) {
        this.adapterId = adapterId;
        this.config = config;
        this.fromEdgeMappingEntities = fromEdgeMappingEntities;
        this.protocolId = protocolId;
        this.tags = tags;
        this.toEdgeMappingEntities = toEdgeMappingEntities;
    }

    public @NotNull Map<String, Object> getConfig() {
        return config;
    }

    public @NotNull List<FromEdgeMappingEntity> getFromEdgeMappingEntities() {
        return fromEdgeMappingEntities;
    }

    public @NotNull List<Map<String, Object>> getTags() {
        return tags;
    }

    public @NotNull List<ToEdgeMappingEntity> getToEdgeMappingEntities() {
        return toEdgeMappingEntities;
    }

    public @NotNull String getProtocolId() {
        return protocolId;
    }

    public @NotNull String getAdapterId() {
        return adapterId;
    }

    public static @NotNull ProtocolAdapterEntity from(
            final @NotNull ProtocolAdapterConfig protocolAdapterConfig, final @NotNull ObjectMapper objectMapper) {

        final List<FromEdgeMappingEntity> fromEdgeMappingEntities = protocolAdapterConfig.getFromEdgeMappings()
                .stream()
                .map(FromEdgeMappingEntity::from)
                .collect(Collectors.toList());

        final List<ToEdgeMappingEntity> toEdgeMappingEntities = protocolAdapterConfig.getToEdgeMappings()
                .stream()
                .map(ToEdgeMappingEntity::from)
                .collect(Collectors.toList());

        final List<Map<String, Object>> tagsAsMaps = protocolAdapterConfig.getTags()
                .stream()
                .map(tag -> objectMapper.convertValue(tag, new TypeReference<Map<String, Object>>() {
                }))
                .collect(Collectors.toList());


        final Map<String, Object> configAsMaps =
                objectMapper.convertValue(protocolAdapterConfig.getAdapterConfig(), new TypeReference<>() {
                });

        return new ProtocolAdapterEntity(protocolAdapterConfig.getAdapterId(),
                protocolAdapterConfig.getProtocolId(),
                configAsMaps,
                fromEdgeMappingEntities,
                toEdgeMappingEntities,
                tagsAsMaps);
    }
}
