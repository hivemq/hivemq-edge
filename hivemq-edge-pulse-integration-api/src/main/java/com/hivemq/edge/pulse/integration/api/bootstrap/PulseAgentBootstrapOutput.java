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

import com.hivemq.edge.pulse.integration.api.management.PulseManagement;
import org.jetbrains.annotations.NotNull;

/**
 * Sink supplied by HiveMQ Edge to receive the result of
 * {@link PulseAgentBootstrap#bootstrapPulseAgent(PulseAgentBootstrapInput, PulseAgentBootstrapOutput)}.
 * Implementations must call exactly one of {@link #success(PulseManagement)} or {@link #fatalFailure(Throwable)}.
 */
public interface PulseAgentBootstrapOutput {

    /**
     * Signals that the agent bootstrap completed successfully and hands the constructed {@link PulseManagement}
     * back to Edge.
     */
    void success(@NotNull PulseManagement pulseManagement);

    /**
     * Signals that the agent bootstrap failed with an unrecoverable error. Edge will continue starting up
     * without a Pulse Agent in this case; surface the failure to the operator via the Pulse REST API.
     *
     * @param cause the failure that prevented the agent from starting
     */
    void fatalFailure(@NotNull Throwable cause);
}
