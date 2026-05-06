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
package com.hivemq.edge.pulse.integration.api.bootstrap;

import com.codahale.metrics.MetricRegistry;
import com.hivemq.edge.pulse.integration.api.asset.AssetProviderRegistry;
import com.hivemq.edge.pulse.integration.api.message.PulseMessageProcessor;
import com.hivemq.edge.pulse.integration.api.message.PulseMessagePublisher;
import java.io.File;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Context handed to {@link PulseAgentBootstrap#bootstrapPulseAgent(PulseAgentBootstrapInput, PulseAgentBootstrapOutput)}.
 * Exposes the services the Pulse Agent integration needs from HiveMQ Edge after persistence bootstrap is complete.
 */
public interface PulseAgentBootstrapInput {

    @NotNull
    MetricRegistry metricRegistry();

    @Nullable
    File configFolder();

    @Nullable
    String hiveMQVersion();

    @NotNull
    AssetProviderRegistry assetProviderRegistry();

    @NotNull
    PulseMessagePublisher pulseMessagePublisher();

    void supplyPulseMessageProcessor(int prio, @NotNull PulseMessageProcessor processor);

    void registerRestComponent(@NotNull Object component);

    void registerModuleInformation(@NotNull String id, @NotNull String name, @NotNull String version);
}
