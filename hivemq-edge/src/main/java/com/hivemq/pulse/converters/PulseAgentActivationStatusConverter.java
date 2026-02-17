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
import com.hivemq.pulse.status.Status;
import org.jetbrains.annotations.NotNull;

public final class PulseAgentActivationStatusConverter
        implements EntityConverter<PulseStatus.ActivationEnum, Status.ActivationStatus> {
    public static final PulseAgentActivationStatusConverter INSTANCE = new PulseAgentActivationStatusConverter();

    private PulseAgentActivationStatusConverter() {}

    @Override
    public @NotNull Status.ActivationStatus toInternalEntity(final @NotNull PulseStatus.ActivationEnum status) {
        return switch (status) {
            case ACTIVATED -> Status.ActivationStatus.ACTIVATED;
            case DEACTIVATED -> Status.ActivationStatus.DEACTIVATED;
            default -> Status.ActivationStatus.ERROR;
        };
    }

    @Override
    public @NotNull PulseStatus.ActivationEnum toRestEntity(final @NotNull Status.ActivationStatus status) {
        return switch (status) {
            case ACTIVATED -> PulseStatus.ActivationEnum.ACTIVATED;
            case DEACTIVATED -> PulseStatus.ActivationEnum.DEACTIVATED;
            default -> PulseStatus.ActivationEnum.ERROR;
        };
    }
}
