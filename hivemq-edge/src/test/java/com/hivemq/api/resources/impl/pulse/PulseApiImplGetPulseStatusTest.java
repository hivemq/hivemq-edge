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
package com.hivemq.api.resources.impl.pulse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.hivemq.edge.api.model.PulseStatus;
import com.hivemq.pulse.status.PulseAgentStatus;
import jakarta.ws.rs.core.Response;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

public class PulseApiImplGetPulseStatusTest extends AbstractPulseApiImplTest {

    @ParameterizedTest
    @EnumSource(PulseAgentStatus.Status.class)
    public void whenAllKindsOfStatusAreProvided_thenReturnsStatus(final @NotNull PulseAgentStatus.Status status) {
        when(statusProvider.getStatus()).thenReturn(pulseAgentStatus(status));
        try (final Response response = pulseApi.getPulseStatus()) {
            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(response.getEntity()).isInstanceOf(PulseStatus.class);
            final PulseStatus pulseStatus = (PulseStatus) response.getEntity();
            assertThat(pulseStatus).isNotNull();
            assertThat(pulseStatus.getActivation()).isEqualTo(expectedActivation(status));
            assertThat(pulseStatus.getRuntime()).isEqualTo(expectedRuntime(status));
        }
    }

    private static @NotNull PulseStatus.ActivationEnum expectedActivation(final @NotNull PulseAgentStatus.Status status) {
        return switch (status) {
            case ACTIVATED_CONNECTED, ACTIVATED_DISCONNECTED -> PulseStatus.ActivationEnum.ACTIVATED;
            case DEACTIVATED -> PulseStatus.ActivationEnum.DEACTIVATED;
            case ERROR -> PulseStatus.ActivationEnum.ERROR;
        };
    }

    private static @NotNull PulseStatus.RuntimeEnum expectedRuntime(final @NotNull PulseAgentStatus.Status status) {
        return switch (status) {
            case ACTIVATED_CONNECTED -> PulseStatus.RuntimeEnum.CONNECTED;
            case ACTIVATED_DISCONNECTED, DEACTIVATED -> PulseStatus.RuntimeEnum.DISCONNECTED;
            case ERROR -> PulseStatus.RuntimeEnum.ERROR;
        };
    }
}
