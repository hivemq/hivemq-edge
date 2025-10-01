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

package com.hivemq.api.resources.impl.pulse;

import com.hivemq.edge.api.model.PulseStatus;
import com.hivemq.pulse.converters.PulseAgentActivationStatusConverter;
import com.hivemq.pulse.converters.PulseAgentConnectionStatusConverter;
import com.hivemq.pulse.status.Status;
import jakarta.ws.rs.core.Response;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.when;

public class PulseApiImplGetPulseStatusTest extends AbstractPulseApiImplTest {
    protected static @NotNull Stream<Arguments> statusProvider() {
        return Stream.of(Status.ActivationStatus.values())
                .flatMap(color -> Stream.of(Status.ConnectionStatus.values()).map(shape -> arguments(color, shape)));
    }

    @ParameterizedTest
    @MethodSource("statusProvider")
    public void whenAllKindsOfStatusAreProvided_thenReturnsStatus(
            final @NotNull Status.ActivationStatus activationStatus,
            final @NotNull Status.ConnectionStatus connectionStatus) {
        when(statusProvider.getStatus()).thenReturn(new Status(activationStatus, connectionStatus, List.of()));
        try (final Response response = pulseApi.getPulseStatus()) {
            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(response.getEntity()).isInstanceOf(PulseStatus.class);
            final PulseStatus pulseStatus = (PulseStatus) response.getEntity();
            assertThat(pulseStatus).isNotNull();
            assertThat(pulseStatus.getActivation()).isEqualTo(PulseAgentActivationStatusConverter.INSTANCE.toRestEntity(
                    activationStatus));
            assertThat(pulseStatus.getRuntime()).isEqualTo(PulseAgentConnectionStatusConverter.INSTANCE.toRestEntity(
                    connectionStatus));
        }
    }
}
