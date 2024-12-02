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

    @XmlElementWrapper(name = "southboundMappings")
    @XmlElement(name = "southboundMapping")
    private @NotNull List<SouthboundMappingEntity> southboundMappingEntities = new ArrayList<>();

    @XmlElementWrapper(name = "northboundMappings")
    @XmlElement(name = "northboundMapping")
    private @NotNull List<NorthboundMappingEntity> northboundMappingEntities = new ArrayList<>();


    // no-arg constructor for JaxB
    public ProtocolAdapterEntity() {
    }

    public ProtocolAdapterEntity(
            final @NotNull String adapterId,
            final @NotNull String protocolId,
            final @NotNull Map<String, Object> config,
            final @NotNull List<NorthboundMappingEntity> northboundMappingEntities,
            final @NotNull List<SouthboundMappingEntity> southboundMappingEntities,
            final @NotNull List<TagEntity> tags) {
        this.adapterId = adapterId;
        this.config = config;
        this.northboundMappingEntities = northboundMappingEntities;
        this.protocolId = protocolId;
        this.tags = tags;
        this.southboundMappingEntities = southboundMappingEntities;
    }

    public @NotNull Map<String, Object> getConfig() {
        return config;
    }

    public @NotNull List<NorthboundMappingEntity> getNorthboundMappingEntities() {
        return northboundMappingEntities;
    }

    public @NotNull List<TagEntity> getTags() {
        return tags;
    }

    public @NotNull List<SouthboundMappingEntity> getSouthboundMappingEntities() {
        return southboundMappingEntities;
    }

    public @NotNull String getProtocolId() {
        return protocolId;
    }

    public @NotNull String getAdapterId() {
        return adapterId;
    }

    public void validate(final @NotNull List<ValidationEvent> validationEvents) {
        if (adapterId == null || adapterId.isEmpty()) {
            validationEvents.add(new ValidationEventImpl(ValidationEvent.FATAL_ERROR, "adapterId is missing", null));
        }
        if (protocolId == null || protocolId.isEmpty()) {
            validationEvents.add(new ValidationEventImpl(ValidationEvent.FATAL_ERROR, "protocolId is missing", null));
        }
        if (northboundMappingEntities != null) {
            northboundMappingEntities.forEach(from -> from.validate(validationEvents));
        }
        if (southboundMappingEntities != null) {
            southboundMappingEntities.forEach(to -> to.validate(validationEvents));
        }
    }

    public static @NotNull ProtocolAdapterEntity from(
            final @NotNull ProtocolAdapterConfig protocolAdapterConfig, final @NotNull ObjectMapper objectMapper) {

        final List<NorthboundMappingEntity> northboundMappings = protocolAdapterConfig.getFromEdgeMappings()
                .stream()
                .map(NorthboundMappingEntity::from)
                .collect(Collectors.toList());

        final List<SouthboundMappingEntity> southboundMappings = protocolAdapterConfig.getToEdgeMappings()
                .stream()
                .map(SouthboundMappingEntity::from)
                .collect(Collectors.toList());

        final List<TagEntity> tagEntities = protocolAdapterConfig.getTags()
                .stream().map(tag -> TagEntity.fromAdapterTag(tag, objectMapper))
                .collect(Collectors.toList());

        final Map<String, Object> configAsMaps =
                objectMapper.convertValue(protocolAdapterConfig.getAdapterConfig(), new TypeReference<>() {
                });

        return new ProtocolAdapterEntity(
                protocolAdapterConfig.getAdapterId(),
                protocolAdapterConfig.getProtocolId(),
                configAsMaps,
                northboundMappings,
                southboundMappings,
                tagEntities);
    }


}
