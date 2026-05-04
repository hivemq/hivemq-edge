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

import org.jetbrains.annotations.NotNull;

/**
 * Lifecycle entry point for the Pulse Agent integration. Implementations are discovered by HiveMQ Edge via
 * {@link java.util.ServiceLoader} and called during edge startup.
 */
public interface PulseAgentBootstrap {

    /**
     * Called during the persistence bootstrap phase. The integration may set up persistence-related state here.
     */
    void bootstrapPulsePersistences(@NotNull PulseAgentPersistenceRuntime runtime);

    /**
     * Called after persistence bootstrap is complete. The integration may register processors, listeners, and
     * runtime components here.
     */
    void afterPersistenceBootstrap(@NotNull PulseAgentRuntime runtime);

    /**
     * Called by HiveMQ Edge during shutdown. Implementations should release any resources
     * acquired during {@link #bootstrapPulsePersistences(PulseAgentPersistenceRuntime)} and
     * {@link #afterPersistenceBootstrap(PulseAgentRuntime)}.
     */
    void shutdown();
}
