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
package com.hivemq.pulse.status;

import java.util.List;
import org.jetbrains.annotations.NotNull;

public class PulseAgentStatusImpl implements PulseAgentStatus {

    private final @NotNull ActivationStatus activationStatus;
    private final @NotNull ConnectionStatus connectionStatus;
    private final @NotNull List<String> errorMessages;

    public PulseAgentStatusImpl(
            final @NotNull ActivationStatus activationStatus,
            final @NotNull ConnectionStatus connectionStatus,
            final @NotNull List<String> errorMessages) {
        this.activationStatus = activationStatus;
        this.connectionStatus = connectionStatus;
        this.errorMessages = errorMessages;
    }

    @Override
    public @NotNull ActivationStatus activationStatus() {
        return activationStatus;
    }

    @Override
    public @NotNull ConnectionStatus connectionStatus() {
        return connectionStatus;
    }

    @Override
    public @NotNull List<String> errorMessages() {
        return errorMessages;
    }
}
