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

import com.hivemq.pulse.status.Status;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

public class PulseApiImplDeletePulseActivationTokenTest extends AbstractPulseApiImplTest {
    @Test
    public void whenStatusIsDeactivated_thenReturnsActivationTokenAlreadyDeletedError() {
        when(statusProvider.getStatus()).thenReturn(new Status(Status.ActivationStatus.DEACTIVATED,
                Status.ConnectionStatus.DISCONNECTED,
                List.of()));
        try (final Response response = pulseApi.deletePulseActivationToken()) {
            assertThat(response.getStatus()).isEqualTo(409);
        }
    }

    @Test
    public void whenDeactivatePulseThrowsException_thenReturnsInternalServerError() {
        when(statusProvider.getStatus()).thenReturn(new Status(Status.ActivationStatus.ACTIVATED,
                Status.ConnectionStatus.CONNECTED,
                List.of()));
        doThrow(new RuntimeException("Test exception")).when(statusProvider).deactivatePulse();
        try (final Response response = pulseApi.deletePulseActivationToken()) {
            assertThat(response.getStatus()).isEqualTo(500);
        }
    }

    @Test
    public void whenStatusIsActivatedThenActivated_thenReturnsInternalServerError() {
        when(statusProvider.getStatus()).thenReturn(new Status(Status.ActivationStatus.ACTIVATED,
                Status.ConnectionStatus.CONNECTED,
                List.of()));
        try (final Response response = pulseApi.deletePulseActivationToken()) {
            assertThat(response.getStatus()).isEqualTo(500);
        }
    }

    @Test
    public void whenStatusIsActivatedThenDeactivated_thenReturnsOK() {
        when(statusProvider.getStatus()).thenReturn(new Status(Status.ActivationStatus.ACTIVATED,
                        Status.ConnectionStatus.CONNECTED,
                        List.of()),
                new Status(Status.ActivationStatus.DEACTIVATED, Status.ConnectionStatus.DISCONNECTED, List.of()));
        try (final Response response = pulseApi.deletePulseActivationToken()) {
            assertThat(response.getStatus()).isEqualTo(200);
        }
    }

    @Test
    public void whenStatusIsActivatedThenError_thenReturnsActivationTokenNotDeletedError() {
        when(statusProvider.getStatus()).thenReturn(new Status(Status.ActivationStatus.ACTIVATED,
                Status.ConnectionStatus.CONNECTED,
                List.of()), new Status(Status.ActivationStatus.ERROR, Status.ConnectionStatus.ERROR, List.of()));
        try (final Response response = pulseApi.deletePulseActivationToken()) {
            assertThat(response.getStatus()).isEqualTo(503);
        }
    }
}
