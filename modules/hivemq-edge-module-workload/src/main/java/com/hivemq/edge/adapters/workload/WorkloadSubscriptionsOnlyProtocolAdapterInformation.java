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
 * A deliberately capability-LESS variant of the workload adapter: protocol-id {@code "workload-subonly"}, declaring
 * ONLY {@link ProtocolAdapterCapability#SUBSCRIPTIONS} (no {@code WRITE}, no {@code BROWSE}). It exists purely so QA can
 * construct the capability-honesty case that is not otherwise buildable on real Edge — an adapter that lacks WRITE must
 * refuse a southbound write mapping. Reuses {@link WorkloadProtocolAdapter} unchanged.
 */
public final class WorkloadSubscriptionsOnlyProtocolAdapterInformation implements ProtocolAdapterInformation {

    public static final @NotNull String PROTOCOL_ID = "workload-subonly";

    @Override
    public @NotNull String protocolId() {
        return PROTOCOL_ID;
    }

    @Override
    public @NotNull String displayName() {
        return "Workload Testing Adapter (subscriptions only)";
    }

    @Override
    public @NotNull String description() {
        return "A capability-less workload variant declaring only SUBSCRIPTIONS (no WRITE, no BROWSE) for QA.";
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
        return EnumSet.of(ProtocolAdapterCapability.SUBSCRIPTIONS); // no WRITE, no BROWSE — the whole point
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
