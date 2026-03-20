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

import static java.util.Objects.requireNonNullElse;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.adapter.sdk.api.tag.Tag;
import com.hivemq.configuration.entity.EntityValidatable;
import com.hivemq.configuration.reader.ArbitraryValuesMapAdapter;
import com.hivemq.protocols.ProtocolAdapterConfig;
import jakarta.xml.bind.ValidationEvent;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings({"FieldMayBeFinal", "unused"})
public class ProtocolAdapterEntity implements EntityValidatable {

    private static final @NotNull TypeReference<Map<String, Object>> AS_MAP_TYPE_REF = new TypeReference<>() {
                // no-op
            };
    private static final @NotNull Integer DEFAULT_CONFIG_VERSION = 1;

    @XmlElement(name = "adapterId", required = true)
    private @NotNull String adapterId;

    @XmlElement(name = "protocolId", required = true)
    private @NotNull String protocolId;

    @XmlElement(name = "configVersion")
    private @Nullable Integer configVersion;

    @XmlElement(name = "config")
    @XmlJavaTypeAdapter(ArbitraryValuesMapAdapter.class)
    private @NotNull Map<String, Object> config;

    @XmlElementWrapper(name = "tags")
    @XmlElement(name = "tag")
    private @NotNull List<TagEntity> tags;

    @XmlElementWrapper(name = "northboundMappings")
    @XmlElement(name = "northboundMapping")
    private @NotNull List<NorthboundMappingEntity> northboundMappings;

    @XmlElementWrapper(name = "southboundMappings")
    @XmlElement(name = "southboundMapping")
    private @NotNull List<SouthboundMappingEntity> southboundMappings;

    // no-arg constructor for JaxB
    public ProtocolAdapterEntity() {
        this("", "", DEFAULT_CONFIG_VERSION, new HashMap<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
    }

    public ProtocolAdapterEntity(
            final @NotNull String adapterId,
            final @NotNull String protocolId,
            final @Nullable Integer configVersion,
            final @Nullable Map<String, Object> config,
            final @Nullable List<NorthboundMappingEntity> northbound,
            final @Nullable List<SouthboundMappingEntity> southbound,
            final @Nullable List<TagEntity> tags) {
        this.adapterId = adapterId;
        this.protocolId = protocolId;
        this.configVersion = configVersion != null
                ? configVersion
                : DEFAULT_CONFIG_VERSION; // if no config version is present, we assume it is the oldest possible
        // version
        this.config = config != null ? config : new HashMap<>();
        this.northboundMappings = northbound != null ? northbound : new ArrayList<>();
        this.southboundMappings = southbound != null ? southbound : new ArrayList<>();
        this.tags = tags != null ? tags : new ArrayList<>();
    }

    public static @NotNull ProtocolAdapterEntity from(
            final @NotNull ProtocolAdapterConfig config, final @NotNull ObjectMapper mapper) {
        return new ProtocolAdapterEntity(
                config.getAdapterId(),
                config.getProtocolId(),
                config.getConfigVersion(),
                mapper.convertValue(config.getAdapterConfig(), AS_MAP_TYPE_REF),
                config.getNorthboundMappings().stream()
                        .map(NorthboundMappingEntity::fromPersistence)
                        .toList(),
                config.getSouthboundMappings().stream()
                        .map(SouthboundMappingEntity::fromPersistence)
                        .toList(),
                config.getTags().stream()
                        .map(tag -> fromAdapterTag(tag, mapper))
                        .toList());
    }

    private static TagEntity fromAdapterTag(final @NotNull Tag tag, final @NotNull ObjectMapper mapper) {
        return new TagEntity(
                tag.getName(), tag.getDescription(), mapper.convertValue(tag.getDefinition(), AS_MAP_TYPE_REF));
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

    @JsonIgnore
    public @NotNull Optional<Set<String>> getDuplicatedTagNameSet() {
        final Set<String> tagNameSet = new HashSet<>();
        final Set<String> duplicatedTagNameSet = new HashSet<>();
        tags.stream().map(TagEntity::getName).forEach(tagName -> {
            if (tagNameSet.contains(tagName)) {
                duplicatedTagNameSet.add(tagName);
            } else {
                tagNameSet.add(tagName);
            }
        });
        if (!duplicatedTagNameSet.isEmpty()) {
            return Optional.of(duplicatedTagNameSet);
        }
        return Optional.empty();
    }

    @Override
    public void validate(final @NotNull List<ValidationEvent> validationEvents) {
        EntityValidatable.notEmpty(validationEvents, adapterId, "adapterId");
        EntityValidatable.notEmpty(validationEvents, protocolId, "protocolId");
        tags.forEach(entity -> entity.validate(validationEvents));

        final boolean northboundAvailable = !northboundMappings.isEmpty();
        final boolean southboundAvailable = !southboundMappings.isEmpty();
        if ((northboundAvailable || southboundAvailable)
                && EntityValidatable.notEmpty(validationEvents, tags, "tags")) {

            final Set<String> tagNames = tags.stream()
                    .map(TagEntity::getName)
                    .filter(name -> !name.isBlank())
                    .collect(Collectors.toSet());

            if (northboundAvailable) {
                northboundMappings.forEach(from -> from.validate(validationEvents));
                northboundMappings.stream()
                        .map(NorthboundMappingEntity::getTagName)
                        .forEach(tagName -> EntityValidatable.notMatch(
                                validationEvents,
                                () -> tagNames.contains(tagName),
                                () -> "Tag name [" + tagName + "] in northbound mapping is not found"));
            }

            if (southboundAvailable) {
                southboundMappings.forEach(to -> to.validate(validationEvents));
                southboundMappings.stream()
                        .map(SouthboundMappingEntity::getTagName)
                        .forEach(tagName -> EntityValidatable.notMatch(
                                validationEvents,
                                () -> tagNames.contains(tagName),
                                () -> "Tag name [" + tagName + "] in southbound mapping is not found"));
            }
        }
    }

    @Override
    public boolean equals(final @Nullable Object o) {
        if (o instanceof final ProtocolAdapterEntity that) {
            return Objects.equals(adapterId, that.adapterId)
                    && Objects.equals(protocolId, that.protocolId)
                    && Objects.equals(configVersion, that.configVersion)
                    && Objects.equals(config, that.config)
                    && Objects.equals(tags, that.tags)
                    && Objects.equals(northboundMappings, that.northboundMappings)
                    && Objects.equals(southboundMappings, that.southboundMappings);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(adapterId, protocolId, configVersion, config, tags, northboundMappings, southboundMappings);
    }
}
