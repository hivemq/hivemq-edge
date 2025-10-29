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
package com.hivemq.protocols;

import com.hivemq.adapter.sdk.api.config.ProtocolSpecificAdapterConfig;
import com.hivemq.adapter.sdk.api.tag.Tag;
import com.hivemq.persistence.mappings.NorthboundMapping;
import com.hivemq.persistence.mappings.SouthboundMapping;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class ProtocolAdapterConfig {

    private final @NotNull ProtocolSpecificAdapterConfig adapterConfig;
    private @NotNull List<? extends Tag> tags;
    private final @NotNull String adapterId;
    private final @NotNull String protocolId;
    private final int configVersion;
    private @NotNull List<SouthboundMapping> southboundMappings;
    private @NotNull List<NorthboundMapping> northboundMappings;

    public ProtocolAdapterConfig(
            final @NotNull String adapterId,
            final @NotNull String protocolId,
            final int configVersion,
            final @NotNull ProtocolSpecificAdapterConfig protocolSpecificConfig,
            final @NotNull List<SouthboundMapping> southboundMappings,
            final @NotNull List<NorthboundMapping> northboundMappings,
            final @NotNull List<? extends Tag> tags) {
        this.adapterId = adapterId;
        this.protocolId = protocolId;
        this.configVersion = configVersion;
        this.southboundMappings = southboundMappings;
        this.northboundMappings = northboundMappings;
        this.adapterConfig = protocolSpecificConfig;
        this.tags = tags;
    }

    public @NotNull Optional<Set<String>> missingTags() {
        if (protocolId.equals("simulation")) {
            return Optional.empty();
        }

        final Set<String> names = new HashSet<>();
        southboundMappings.forEach(mapping -> names.add(mapping.getTagName()));
        northboundMappings.forEach(mapping -> names.add(mapping.getTagName()));

        this.tags.forEach(tag -> names.remove(tag.getName()));
        if (names.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(names);
        }
    }

    public @NotNull String getAdapterId() {
        return adapterId;
    }

    public @NotNull String getProtocolId() {
        return protocolId;
    }

    public @NotNull ProtocolSpecificAdapterConfig getAdapterConfig() {
        return adapterConfig;
    }

    public @NotNull List<? extends Tag> getTags() {
        return tags;
    }

    public @NotNull List<NorthboundMapping> getNorthboundMappings() {
        return northboundMappings;
    }

    public @NotNull List<SouthboundMapping> getSouthboundMappings() {
        return southboundMappings;
    }

    public int getConfigVersion() {
        return configVersion;
    }

    /**
     * Updates the tags for hot-reload support.
     * This method is used to update tags without restarting the adapter.
     *
     * @param tags the new tags
     */
    public void setTags(final @NotNull List<? extends Tag> tags) {
        this.tags = tags;
    }

    /**
     * Updates the northbound mappings for hot-reload support.
     * This method is used to update northbound mappings without restarting the adapter.
     *
     * @param northboundMappings the new northbound mappings
     */
    public void setNorthboundMappings(final @NotNull List<NorthboundMapping> northboundMappings) {
        this.northboundMappings = northboundMappings;
    }

    /**
     * Updates the southbound mappings for hot-reload support.
     * This method is used to update southbound mappings without restarting the adapter.
     *
     * @param southboundMappings the new southbound mappings
     */
    public void setSouthboundMappings(final @NotNull List<SouthboundMapping> southboundMappings) {
        this.southboundMappings = southboundMappings;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        final ProtocolAdapterConfig that = (ProtocolAdapterConfig) o;
        return getConfigVersion() == that.getConfigVersion() &&
                Objects.equals(getAdapterConfig(), that.getAdapterConfig()) &&
                Objects.equals(getTags(), that.getTags()) &&
                Objects.equals(getAdapterId(), that.getAdapterId()) &&
                Objects.equals(getProtocolId(), that.getProtocolId()) &&
                Objects.equals(getSouthboundMappings(), that.getSouthboundMappings()) &&
                Objects.equals(getNorthboundMappings(), that.getNorthboundMappings());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getAdapterConfig(),
                getTags(),
                getAdapterId(),
                getProtocolId(),
                getConfigVersion(),
                getSouthboundMappings(),
                getNorthboundMappings());
    }

    @Override
    public String toString() {
        return "ProtocolAdapterConfig{" +
                "adapterConfig=" +
                adapterConfig +
                ", tags=" +
                tags +
                ", adapterId='" +
                adapterId +
                '\'' +
                ", protocolId='" +
                protocolId +
                '\'' +
                ", configVersion=" +
                configVersion +
                ", southboundMappings=" +
                southboundMappings +
                ", northboundMappings=" +
                northboundMappings +
                '}';
    }
}
