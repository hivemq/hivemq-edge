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
package com.hivemq.protocols.v2.northbound;

import com.hivemq.adapter.sdk.api.ProtocolAdapter;
import com.hivemq.adapter.sdk.api.ProtocolAdapterCapability;
import com.hivemq.adapter.sdk.api.ProtocolAdapterCategory;
import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.ProtocolAdapterTag;
import com.hivemq.adapter.sdk.api.config.ProtocolSpecificAdapterConfig;
import com.hivemq.adapter.sdk.api.tag.Tag;
import com.hivemq.adapter.sdk.api.tag.TagDefinition;
import java.util.EnumSet;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * A legacy-SDK identity for v2 northbound publishes and the southbound write bridge.
 * <p>
 * The existing publish and writing services and protocol-adapter interceptors still key their context from the v1
 * {@link ProtocolAdapter} shape. V2 delivery only needs that identity surface — adapter id, protocol id,
 * and display metadata — so this adapter deliberately implements no lifecycle behavior.
 */
public final class ProtocolAdapterPublishIdentity implements ProtocolAdapter {

    private final @NotNull String adapterId;
    private final @NotNull ProtocolAdapterInformation information;

    public ProtocolAdapterPublishIdentity(
            final @NotNull String adapterId,
            final @NotNull com.hivemq.adapter.sdk.api.v2.ProtocolAdapterInformation information) {
        this.adapterId = adapterId;
        this.information = new LegacyInformation(information);
    }

    @Override
    public @NotNull String getId() {
        return adapterId;
    }

    @Override
    public @NotNull ProtocolAdapterInformation getProtocolAdapterInformation() {
        return information;
    }

    private static final class LegacyInformation implements ProtocolAdapterInformation {

        private final @NotNull com.hivemq.adapter.sdk.api.v2.ProtocolAdapterInformation delegate;

        private LegacyInformation(final @NotNull com.hivemq.adapter.sdk.api.v2.ProtocolAdapterInformation delegate) {
            this.delegate = delegate;
        }

        @Override
        public @NotNull String getProtocolName() {
            return delegate.displayName();
        }

        @Override
        public @NotNull String getProtocolId() {
            return delegate.protocolId();
        }

        @Override
        public @NotNull String getDisplayName() {
            return delegate.displayName();
        }

        @Override
        public @NotNull String getDescription() {
            return delegate.description();
        }

        @Override
        public @NotNull String getUrl() {
            return "";
        }

        @Override
        public @NotNull String getVersion() {
            return delegate.version();
        }

        @Override
        public @NotNull String getLogoUrl() {
            return delegate.logoUrl();
        }

        @Override
        public @NotNull String getAuthor() {
            return delegate.author();
        }

        @Override
        public @NotNull ProtocolAdapterCategory getCategory() {
            return delegate.category();
        }

        @Override
        public @NotNull List<ProtocolAdapterTag> getTags() {
            return delegate.tags();
        }

        @Override
        public @NotNull Class<? extends Tag> tagConfigurationClass() {
            return EmptyTag.class;
        }

        @Override
        public @NotNull Class<? extends ProtocolSpecificAdapterConfig> configurationClassNorthbound() {
            return EmptyConfig.class;
        }

        @Override
        public @NotNull Class<? extends ProtocolSpecificAdapterConfig> configurationClassNorthAndSouthbound() {
            return EmptyConfig.class;
        }

        @Override
        public @NotNull EnumSet<ProtocolAdapterCapability> getCapabilities() {
            final EnumSet<ProtocolAdapterCapability> capabilities = EnumSet.of(ProtocolAdapterCapability.READ);
            if (delegate.capabilities().contains(com.hivemq.adapter.sdk.api.v2.ProtocolAdapterCapability.WRITE)) {
                capabilities.add(ProtocolAdapterCapability.WRITE);
            }
            if (delegate.capabilities().contains(com.hivemq.adapter.sdk.api.v2.ProtocolAdapterCapability.BROWSE)) {
                capabilities.add(ProtocolAdapterCapability.DISCOVER);
            }
            return capabilities;
        }

        @Override
        public int getCurrentConfigVersion() {
            return delegate.currentConfigVersion();
        }
    }

    private static final class EmptyConfig implements ProtocolSpecificAdapterConfig {}

    private static final class EmptyTag implements Tag {

        @Override
        public @NotNull TagDefinition getDefinition() {
            return EmptyTagDefinition.INSTANCE;
        }

        @Override
        public @NotNull String getName() {
            return "";
        }

        @Override
        public @NotNull String getDescription() {
            return "";
        }
    }

    private enum EmptyTagDefinition implements TagDefinition {
        INSTANCE
    }
}
