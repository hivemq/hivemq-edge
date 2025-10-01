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

import com.hivemq.api.errors.InternalServerError;
import com.hivemq.api.errors.pulse.ActivationTokenInvalidError;
import com.hivemq.api.errors.pulse.PulseAgentDeactivatedError;
import com.hivemq.edge.api.model.PulseActivationToken;
import com.hivemq.pulse.status.Status;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

public class PulseApiImplUpdatePulseActivationTokenTest extends AbstractPulseApiImplTest {
    @Test
    public void whenActivatePulseThrowsException_thenReturnsInternalServerError() {
        when(statusProvider.activatePulse(anyString())).thenThrow(new RuntimeException("Test exception"));
        try (final Response response = pulseApi.updatePulseActivationToken(new PulseActivationToken("1234567890"))) {
            assertThat(response.getStatus()).isEqualTo(500);
            assertThat(response.getEntity()).isInstanceOf(InternalServerError.class);
        }
    }

    @Test
    public void whenTokenIsInvalid_thenReturnsActivationTokenInvalidError() {
        when(statusProvider.activatePulse(anyString())).thenReturn(false);
        try (final Response response = pulseApi.updatePulseActivationToken(new PulseActivationToken(""))) {
            assertThat(response.getStatus()).isEqualTo(422);
            assertThat(response.getEntity()).isInstanceOf(ActivationTokenInvalidError.class);
        }
        try (final Response response = pulseApi.updatePulseActivationToken(new PulseActivationToken("123"))) {
            assertThat(response.getStatus()).isEqualTo(422);
            assertThat(response.getEntity()).isInstanceOf(ActivationTokenInvalidError.class);
        }
    }

    @Test
    public void whenTokenIsValidAndStatusIsActivate_thenReturnsOK() {
        when(statusProvider.activatePulse(anyString())).thenReturn(true);
        when(statusProvider.getStatus()).thenReturn(new Status(Status.ActivationStatus.ACTIVATED,
                Status.ConnectionStatus.CONNECTED,
                List.of()));
        try (final Response response = pulseApi.updatePulseActivationToken(new PulseActivationToken("1234567890"))) {
            assertThat(response.getStatus()).isEqualTo(200);
        }
    }

    @Test
    public void whenTokenIsValidAndStatusIsDeactivate_thenReturnsPulseAgentDeactivatedError() {
        when(statusProvider.activatePulse(anyString())).thenReturn(true);
        when(statusProvider.getStatus()).thenReturn(new Status(Status.ActivationStatus.DEACTIVATED,
                Status.ConnectionStatus.DISCONNECTED,
                List.of()));
        try (final Response response = pulseApi.updatePulseActivationToken(new PulseActivationToken("1234567890"))) {
            assertThat(response.getStatus()).isEqualTo(400);
            assertThat(response.getEntity()).isInstanceOf(PulseAgentDeactivatedError.class);
        }
    }

    @Test
    public void whenTokenIsValidAndStatusIsError_thenReturnsInternalServerError() {
        when(statusProvider.activatePulse(anyString())).thenReturn(true);
        when(statusProvider.getStatus()).thenReturn(new Status(Status.ActivationStatus.ERROR,
                Status.ConnectionStatus.ERROR,
                List.of()));
        try (final Response response = pulseApi.updatePulseActivationToken(new PulseActivationToken("1234567890"))) {
            assertThat(response.getStatus()).isEqualTo(500);
            assertThat(response.getEntity()).isInstanceOf(InternalServerError.class);
        }
    }
}
