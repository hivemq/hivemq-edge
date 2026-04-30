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
package com.hivemq.edge.pulse.integration.api;

import com.codahale.metrics.MetricRegistry;
import com.hivemq.configuration.info.SystemInformation;
import com.hivemq.pulse.asset.AssetFactory;
import com.hivemq.pulse.asset.AssetProviderRegistry;
import com.hivemq.pulse.status.StatusFactory;
import com.hivemq.pulse.status.StatusProviderRegistry;
import org.jetbrains.annotations.NotNull;

/**
 * Context handed to {@link PulseAgentBootstrap#afterPersistenceBootstrap(PulseAgentRuntime)}. Exposes the services
 * the Pulse Agent integration needs from HiveMQ Edge after persistence bootstrap is complete.
 */
public interface PulseAgentRuntime {

    @NotNull
    MetricRegistry metricRegistry();

    @NotNull
    SystemInformation systemInformation();

    @NotNull
    AssetProviderRegistry assetProviderRegistry();

    @NotNull
    StatusProviderRegistry statusProviderRegistry();

    @NotNull
    StatusFactory statusFactory();

    @NotNull
    AssetFactory assetFactory();

    @NotNull
    PulseDatapointPublisher pulseDatapointPublisher();

    void supplyPulseDatapointProcessor(int prio, @NotNull PulseDatapointProcessor processor);

    void registerRestComponent(@NotNull Object component);

    void registerModuleInformation(@NotNull String id, @NotNull String name, @NotNull String version);
}
