/*
 * Copyright 2019-present HiveMQ GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hivemq.configuration.entity.adapter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.configuration.reader.ArbitraryValuesMapAdapter;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.protocols.ProtocolAdapterConfig;

import javax.xml.bind.ValidationEvent;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.bind.helpers.ValidationEventImpl;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@SuppressWarnings({"FieldMayBeFinal", "unused"})
public class ProtocolAdapterEntity {

    @XmlElement(name = "adapterId", required = true)
    private @NotNull String adapterId;

    @XmlElement(name = "protocolId", required = true)
    private @NotNull String protocolId;

    @XmlElement(name = "config")
    @XmlJavaTypeAdapter(ArbitraryValuesMapAdapter.class)
    private @NotNull Map<String, Object> config = new HashMap<>();

    @XmlElementWrapper(name = "tags")
    @XmlElement(name = "tag")
    private @NotNull List<TagEntity> tags = new ArrayList<>();

    @XmlElementWrapper(name = "toEdgeMappings")
    @XmlElement(name = "toEdgeMapping")
    private @NotNull List<ToEdgeMappingEntity> toEdgeMappingEntities = new ArrayList<>();

    @XmlElementWrapper(name = "fromEdgeMappings")
    @XmlElement(name = "fromEdgeMapping")
    private @NotNull List<FromEdgeMappingEntity> fromEdgeMappingEntities = new ArrayList<>();

    @XmlElementWrapper(name = "field-mappings")
    @XmlElement(name = "field-mapping")
    private @NotNull List<FieldMappingsEntity> fieldMappings = new ArrayList<>();


    // no-arg constructor for JaxB
    public ProtocolAdapterEntity() {
    }

    public ProtocolAdapterEntity(
            @NotNull final String adapterId,
            @NotNull final String protocolId,
            @NotNull final Map<String, Object> config,
            @NotNull final List<FromEdgeMappingEntity> fromEdgeMappingEntities,
            @NotNull final List<ToEdgeMappingEntity> toEdgeMappingEntities, @NotNull final List<TagEntity> tags,
            @NotNull final List<FieldMappingsEntity> fieldMappings) {
        this.adapterId = adapterId;
        this.config = config;
        this.fromEdgeMappingEntities = fromEdgeMappingEntities;
        this.protocolId = protocolId;
        this.tags = tags;
        this.toEdgeMappingEntities = toEdgeMappingEntities;
        this.fieldMappings = fieldMappings;
    }

    public @NotNull Map<String, Object> getConfig() {
        return config;
    }

    public @NotNull List<FromEdgeMappingEntity> getFromEdgeMappingEntities() {
        return fromEdgeMappingEntities;
    }

    public @NotNull List<TagEntity> getTags() {
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

    public @NotNull List<FieldMappingsEntity> getFieldMappings() {
        return fieldMappings;
    }

    public void validate(final @NotNull List<ValidationEvent> validationEvents) {
        if (adapterId == null || adapterId.isEmpty()) {
            validationEvents.add(new ValidationEventImpl(ValidationEvent.FATAL_ERROR, "adapterId is missing", null));
        }
        if (protocolId == null || protocolId.isEmpty()) {
            validationEvents.add(new ValidationEventImpl(ValidationEvent.FATAL_ERROR, "protocolId is missing", null));
        }
        if (fromEdgeMappingEntities != null) {
            fromEdgeMappingEntities.forEach(from -> from.validate(validationEvents));
        }
        if (toEdgeMappingEntities != null) {
            toEdgeMappingEntities.forEach(to -> to.validate(validationEvents));
        }
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

        final List<TagEntity> tagEntities = protocolAdapterConfig.getTags()
                .stream().map(tag -> TagEntity.fromAdapterTag(tag, objectMapper))
                .collect(Collectors.toList());

        final List<FieldMappingsEntity> fieldMappingsEntities = protocolAdapterConfig.getFieldMappings()
                .stream()
                .map(FieldMappingsEntity::from)
                .collect(Collectors.toList());

        final Map<String, Object> configAsMaps =
                objectMapper.convertValue(protocolAdapterConfig.getAdapterConfig(), new TypeReference<>() {
                });

        return new ProtocolAdapterEntity(protocolAdapterConfig.getAdapterId(),
                protocolAdapterConfig.getProtocolId(),
                configAsMaps,
                fromEdgeMappingEntities, toEdgeMappingEntities, tagEntities, fieldMappingsEntities);
    }


}
