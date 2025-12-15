/*
 *  Copyright 2019-present HiveMQ GmbH
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.hivemq.pulse.converters;

import com.hivemq.configuration.entity.EntityConverter;
import com.hivemq.edge.api.model.PulseStatus;
import com.hivemq.pulse.status.Status;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class PulseAgentStatusConverter implements EntityConverter<PulseStatus, Status> {
    public final static PulseAgentStatusConverter INSTANCE = new PulseAgentStatusConverter();

    private PulseAgentStatusConverter() {
    }

    @Override
    public @NotNull Status toInternalEntity(final @NotNull PulseStatus pulseStatus) {
        return new Status(PulseAgentActivationStatusConverter.INSTANCE.toInternalEntity(pulseStatus.getActivation()),
                PulseAgentConnectionStatusConverter.INSTANCE.toInternalEntity(pulseStatus.getRuntime()),
                List.of());
    }

    @Override
    public @NotNull PulseStatus toRestEntity(final @NotNull Status status) {
        return PulseStatus.builder()
                .activation(PulseAgentActivationStatusConverter.INSTANCE.toRestEntity(status.activationStatus()))
                .runtime(PulseAgentConnectionStatusConverter.INSTANCE.toRestEntity(status.connectionStatus()))
                .build();
    }
}
