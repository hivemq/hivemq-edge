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
package com.hivemq.pulse.converters;

import com.hivemq.configuration.entity.EntityConverter;
import com.hivemq.edge.api.model.PulseStatus;
import com.hivemq.edge.pulse.integration.api.management.PulseAgentStatus;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public class PulseAgentStatusConverter implements EntityConverter<PulseStatus, PulseAgentStatus> {
    public static final PulseAgentStatusConverter INSTANCE = new PulseAgentStatusConverter();

    private PulseAgentStatusConverter() {}

    @Override
    public @NotNull PulseAgentStatus toInternalEntity(final @NotNull PulseStatus pulseStatus) {
        final PulseAgentStatus.Status status = toStatus(pulseStatus.getActivation(), pulseStatus.getRuntime());
        return new PulseAgentStatus() {
            @Override
            public @NotNull Status status() {
                return status;
            }

            @Override
            public @NotNull List<String> errorMessages() {
                return List.of();
            }
        };
    }

    @Override
    public @NotNull PulseStatus toRestEntity(final @NotNull PulseAgentStatus status) {
        return PulseStatus.builder()
                .activation(toActivationEnum(status.status()))
                .runtime(toRuntimeEnum(status.status()))
                .build();
    }

    private static @NotNull PulseAgentStatus.Status toStatus(
            final @NotNull PulseStatus.ActivationEnum activation, final @NotNull PulseStatus.RuntimeEnum runtime) {
        return switch (activation) {
            case ACTIVATED ->
                switch (runtime) {
                    case CONNECTED -> PulseAgentStatus.Status.ACTIVATED_CONNECTED;
                    case DISCONNECTED -> PulseAgentStatus.Status.ACTIVATED_DISCONNECTED;
                    default -> PulseAgentStatus.Status.ERROR;
                };
            case DEACTIVATED -> PulseAgentStatus.Status.DEACTIVATED;
            default -> PulseAgentStatus.Status.ERROR;
        };
    }

    private static @NotNull PulseStatus.ActivationEnum toActivationEnum(final @NotNull PulseAgentStatus.Status status) {
        return switch (status) {
            case ACTIVATED_CONNECTED, ACTIVATED_DISCONNECTED -> PulseStatus.ActivationEnum.ACTIVATED;
            case DEACTIVATED -> PulseStatus.ActivationEnum.DEACTIVATED;
            case ERROR -> PulseStatus.ActivationEnum.ERROR;
        };
    }

    private static @NotNull PulseStatus.RuntimeEnum toRuntimeEnum(final @NotNull PulseAgentStatus.Status status) {
        return switch (status) {
            case ACTIVATED_CONNECTED -> PulseStatus.RuntimeEnum.CONNECTED;
            case ACTIVATED_DISCONNECTED, DEACTIVATED -> PulseStatus.RuntimeEnum.DISCONNECTED;
            case ERROR -> PulseStatus.RuntimeEnum.ERROR;
        };
    }
}
