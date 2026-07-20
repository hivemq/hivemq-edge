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
package com.hivemq.edge.adapters.workload;

import com.hivemq.adapter.sdk.api.ProtocolAdapterCategory;
import com.hivemq.adapter.sdk.api.ProtocolAdapterTag;
import com.hivemq.adapter.sdk.api.v2.ProtocolAdapterCapability;
import com.hivemq.adapter.sdk.api.v2.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.v2.node.Node;
import java.util.EnumSet;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * The {@link ProtocolAdapterInformation} of the {@link WorkloadProtocolAdapter} type: protocol-id {@code "workload"},
 * SIMULATION category, all v2 capabilities, node class {@link WorkloadNode}, config-version 2.
 */
public final class WorkloadProtocolAdapterInformation implements ProtocolAdapterInformation {

    public static final @NotNull String PROTOCOL_ID = "workload";

    @Override
    public @NotNull String protocolId() {
        return PROTOCOL_ID;
    }

    @Override
    public @NotNull String displayName() {
        return "Workload Testing Adapter";
    }

    @Override
    public @NotNull String description() {
        return "A self-driving QA device simulator: realistic data streams plus an autonomous fault timeline.";
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
        return "HiveMQ QA";
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
        return EnumSet.allOf(ProtocolAdapterCapability.class);
    }

    @Override
    public @NotNull Class<? extends Node> nodeClass() {
        return WorkloadNode.class;
    }

    @Override
    public int currentConfigVersion() {
        return 2;
    }
}
