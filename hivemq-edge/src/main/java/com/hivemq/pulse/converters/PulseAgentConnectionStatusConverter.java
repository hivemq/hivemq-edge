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

public final class PulseAgentConnectionStatusConverter
        implements EntityConverter<PulseStatus.RuntimeEnum, Status.ConnectionStatus> {
    public final static PulseAgentConnectionStatusConverter INSTANCE = new PulseAgentConnectionStatusConverter();

    private PulseAgentConnectionStatusConverter() {
    }

    @Override
    public @NotNull Status.ConnectionStatus toInternalEntity(final @NotNull PulseStatus.RuntimeEnum status) {
        return switch (status) {
            case CONNECTED -> Status.ConnectionStatus.CONNECTED;
            case DISCONNECTED -> Status.ConnectionStatus.DISCONNECTED;
            case ERROR -> Status.ConnectionStatus.ERROR;
            default -> throw new IllegalArgumentException("Unknown pulse connection status " + status);
        };
    }

    @Override
    public @NotNull PulseStatus.RuntimeEnum toRestEntity(final @NotNull Status.ConnectionStatus status) {
        return switch (status) {
            case CONNECTED -> PulseStatus.RuntimeEnum.CONNECTED;
            case DISCONNECTED -> PulseStatus.RuntimeEnum.DISCONNECTED;
            case ERROR -> PulseStatus.RuntimeEnum.ERROR;
            default -> throw new IllegalArgumentException("Unknown pulse connection status " + status);
        };
    }
}
