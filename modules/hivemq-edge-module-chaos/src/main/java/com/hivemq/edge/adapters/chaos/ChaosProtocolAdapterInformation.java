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
package com.hivemq.edge.adapters.chaos;

import com.hivemq.adapter.sdk.api.ProtocolAdapterCategory;
import com.hivemq.adapter.sdk.api.ProtocolAdapterTag;
import com.hivemq.adapter.sdk.api.v2.ProtocolAdapterCapability;
import com.hivemq.adapter.sdk.api.v2.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.v2.node.Node;
import java.util.EnumSet;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * The {@link ProtocolAdapterInformation} of the {@link ChaosProtocolAdapter} type. It declares the
 * scriptable capabilities the simulator should advertise — the framework gates subscription, write, and browse
 * behavior on this set — and reports a {@code config-version} of {@code 2}, marking it a v2 type.
 */
public final class ChaosProtocolAdapterInformation implements ProtocolAdapterInformation {

    private final @NotNull String protocolId;
    private final @NotNull EnumSet<ProtocolAdapterCapability> capabilities;

    /**
     * @param protocolId   the {@code protocol-id} this type is registered under.
     * @param capabilities the capabilities the simulator advertises.
     */
    public ChaosProtocolAdapterInformation(
            final @NotNull String protocolId, final @NotNull EnumSet<ProtocolAdapterCapability> capabilities) {
        this.protocolId = protocolId;
        this.capabilities = EnumSet.copyOf(capabilities);
    }

    @Override
    public @NotNull String protocolId() {
        return protocolId;
    }

    @Override
    public @NotNull String displayName() {
        return "Chaos";
    }

    @Override
    public @NotNull String description() {
        return "A scriptable test simulator for the v2 protocol-adapter framework.";
    }

    @Override
    public @NotNull String version() {
        return "1.0.0";
    }

    @Override
    public @NotNull String logoUrl() {
        return "";
    }

    @Override
    public @NotNull String author() {
        return "HiveMQ";
    }

    @Override
    public @NotNull ProtocolAdapterCategory category() {
        return ProtocolAdapterCategory.SIMULATION;
    }

    @Override
    public @NotNull List<ProtocolAdapterTag> tags() {
        return List.of();
    }

    @Override
    public @NotNull EnumSet<ProtocolAdapterCapability> capabilities() {
        return EnumSet.copyOf(capabilities);
    }

    @Override
    public @NotNull Class<? extends Node> nodeClass() {
        return ChaosNode.class;
    }

    @Override
    public int currentConfigVersion() {
        return 2;
    }
}
