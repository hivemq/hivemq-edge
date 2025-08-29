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
import com.hivemq.configuration.entity.EntityValidatable;
import com.hivemq.configuration.reader.ArbitraryValuesMapAdapter;
import com.hivemq.protocols.ProtocolAdapterConfig;
import jakarta.xml.bind.ValidationEvent;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNullElse;

@SuppressWarnings({"FieldMayBeFinal", "unused"})
public class ProtocolAdapterEntity implements EntityValidatable {

    private static final @NotNull String ADAPTER_ID = "adapterId";
    private static final @NotNull String PROTOCOL_ID = "protocolId";
    private static final @NotNull String CONFIG_VERSION = "configVersion";
    private static final @NotNull String CONFIG = "config";
    private static final @NotNull String TAGS = "tags";
    private static final @NotNull String TAG = "tag";
    private static final @NotNull String SOUTHBOUND_MAPPINGS = "southboundMappings";
    private static final @NotNull String SOUTHBOUND_MAPPING = "southboundMapping";
    private static final @NotNull String NORTHBOUND_MAPPINGS = "northboundMappings";
    private static final @NotNull String NORTHBOUND_MAPPING = "northboundMapping";
    private static final @NotNull TypeReference<Map<String, Object>> AS_MAP_TYPE_REF =
            new TypeReference<>() {
                // no-op
            };
    private static final @NotNull Integer DEFAULT_CONFIG_VERSION = 1;

    @XmlElement(name = ADAPTER_ID, required = true)
    private @NotNull String adapterId;

    @XmlElement(name = PROTOCOL_ID, required = true)
    private @NotNull String protocolId;

    @XmlElement(name = CONFIG_VERSION)
    private @Nullable Integer configVersion;

    @XmlElement(name = CONFIG)
    @XmlJavaTypeAdapter(ArbitraryValuesMapAdapter.class)
    private @NotNull Map<String, Object> config;

    @XmlElementWrapper(name = TAGS)
    @XmlElement(name = TAG)
    private @NotNull List<TagEntity> tags;

    @XmlElementWrapper(name = NORTHBOUND_MAPPINGS)
    @XmlElement(name = NORTHBOUND_MAPPING)
    private @NotNull List<NorthboundMappingEntity> northboundMappings;

    @XmlElementWrapper(name = SOUTHBOUND_MAPPINGS)
    @XmlElement(name = SOUTHBOUND_MAPPING)
    private @NotNull List<SouthboundMappingEntity> southboundMappings;

    // no-arg constructor for JaxB
    public ProtocolAdapterEntity() {
        this("", "", DEFAULT_CONFIG_VERSION, new HashMap<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
    }

    public ProtocolAdapterEntity(
            final @NotNull String adapterId,
            final @NotNull String protocolId,
            final @Nullable Integer configVersion,
            final @NotNull Map<String, Object> config,
            final @NotNull List<NorthboundMappingEntity> northbound,
            final @NotNull List<SouthboundMappingEntity> southbound,
            final @NotNull List<TagEntity> tags) {
        this.adapterId = adapterId;
        this.protocolId = protocolId;
        this.configVersion = configVersion != null ?
                configVersion :
                DEFAULT_CONFIG_VERSION; // if no config version is present, we assume it is the oldest possible version
        this.config = config != null ? config : new HashMap<>();
        this.northboundMappings = northbound != null ? northbound : new ArrayList<>();
        this.southboundMappings = southbound != null ? southbound : new ArrayList<>();
        this.tags = tags != null ? tags : new ArrayList<>();
    }

    public static @NotNull ProtocolAdapterEntity from(
            final @NotNull ProtocolAdapterConfig config,
            final @NotNull ObjectMapper mapper) {
        return new ProtocolAdapterEntity(config.getAdapterId(),
                config.getProtocolId(),
                config.getConfigVersion(),
                mapper.convertValue(config.getAdapterConfig(), AS_MAP_TYPE_REF),
                config.getNorthboundMappings().stream().map(NorthboundMappingEntity::fromPersistence).toList(),
                config.getSouthboundMappings().stream().map(SouthboundMappingEntity::fromPersistence).toList(),
                config.getTags().stream().map(tag -> TagEntity.fromAdapterTag(tag, mapper)).toList());
    }

    public @NotNull String getProtocolId() {
        return protocolId;
    }

    public @NotNull String getAdapterId() {
        return adapterId;
    }

    public @NotNull Integer getConfigVersion() {
        return requireNonNullElse(configVersion, DEFAULT_CONFIG_VERSION);
    }

    public @NotNull Map<String, Object> getConfig() {
        return config;
    }

    public @NotNull List<NorthboundMappingEntity> getNorthboundMappings() {
        return northboundMappings;
    }

    public @NotNull List<SouthboundMappingEntity> getSouthboundMappings() {
        return southboundMappings;
    }

    public @NotNull List<TagEntity> getTags() {
        return tags;
    }

    @Override
    public void validate(final @NotNull List<ValidationEvent> validationEvents) {
        EntityValidatable.notEmpty(validationEvents, adapterId, "adapterId");
        EntityValidatable.notEmpty(validationEvents, protocolId, "protocolId");
        if (tags != null) {
            tags.forEach(entity -> entity.validate(validationEvents));
        }

        final boolean northboundAvailable = northboundMappings != null && !northboundMappings.isEmpty();
        final boolean southboundAvailable = southboundMappings != null && !southboundMappings.isEmpty();

        if ((northboundAvailable || southboundAvailable) &&
                EntityValidatable.notEmpty(validationEvents, tags, "tags")) {

            final Set<String> tagNameSet = tags.stream()
                    .map(TagEntity::getName)
                    .filter(name -> name != null && !name.isBlank())
                    .collect(Collectors.toSet());

            if (northboundAvailable) {
                northboundMappings.forEach(from -> from.validate(validationEvents));
                northboundMappings.stream()
                        .map(NorthboundMappingEntity::getTagName)
                        .forEach(tagName -> EntityValidatable.notMatch(validationEvents,
                                () -> tagNameSet.contains(tagName),
                                () -> "Tag name [" + tagName + "] in northbound mapping is not found"));
            }

            if (southboundAvailable) {
                southboundMappings.forEach(to -> to.validate(validationEvents));
                southboundMappings.stream()
                        .map(SouthboundMappingEntity::getTagName)
                        .forEach(tagName -> EntityValidatable.notMatch(validationEvents,
                                () -> tagNameSet.contains(tagName),
                                () -> "Tag name [" + tagName + "] in southbound mapping is not found"));
            }
        }
    }

    @Override
    public boolean equals(final @Nullable Object o) {
        if (o instanceof final ProtocolAdapterEntity that) {
            return Objects.equals(adapterId, that.adapterId) &&
                    Objects.equals(protocolId, that.protocolId) &&
                    Objects.equals(configVersion, that.configVersion) &&
                    Objects.equals(config, that.config) &&
                    Objects.equals(tags, that.tags) &&
                    Objects.equals(northboundMappings, that.northboundMappings) &&
                    Objects.equals(southboundMappings, that.southboundMappings);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(adapterId, protocolId, configVersion, config, tags, northboundMappings, southboundMappings);
    }
}
