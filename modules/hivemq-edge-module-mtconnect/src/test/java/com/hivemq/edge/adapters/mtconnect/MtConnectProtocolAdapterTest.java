/*
 * Copyright 2023-present HiveMQ GmbH
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.hivemq.edge.adapters.mtconnect;

import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.config.PollingContext;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartOutput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStopInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStopOutput;
import com.hivemq.adapter.sdk.api.polling.PollingInput;
import com.hivemq.adapter.sdk.api.polling.PollingOutput;
import com.hivemq.adapter.sdk.api.state.ProtocolAdapterState;
import com.hivemq.edge.adapters.mtconnect.config.MtConnectAdapterConfig;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MtConnectProtocolAdapterTest {
    private @NotNull ProtocolAdapterInformation information;
    private @NotNull ProtocolAdapterInput<MtConnectAdapterConfig> adapterInput;
    private @NotNull ProtocolAdapterStartInput startInput;
    private @NotNull ProtocolAdapterStartOutput startOutput;
    private @NotNull ProtocolAdapterStopInput stopInput;
    private @NotNull ProtocolAdapterStopOutput stopOutput;
    private @NotNull ProtocolAdapterState state;
    private @NotNull PollingInput pollingInput;
    private @NotNull PollingOutput pollingOutput;
    private @NotNull MtConnectAdapterConfig config;
    private @NotNull PollingContext pollingContext;

    @BeforeEach
    public void setUp() {
        information = mock(ProtocolAdapterInformation.class);
        adapterInput = (ProtocolAdapterInput<MtConnectAdapterConfig>) mock(ProtocolAdapterInput.class);
        startInput = mock(ProtocolAdapterStartInput.class);
        startOutput = mock(ProtocolAdapterStartOutput.class);
        stopInput = mock(ProtocolAdapterStopInput.class);
        stopOutput = mock(ProtocolAdapterStopOutput.class);
        state = mock(ProtocolAdapterState.class);
        pollingInput = mock(PollingInput.class);
        pollingOutput = mock(PollingOutput.class);
        config = mock(MtConnectAdapterConfig.class);
        pollingContext = mock(PollingContext.class);
    }

    @Test
    public void whenProtocolAdapterStateIsNull_thenStartShouldFail() {
        when(adapterInput.getProtocolAdapterState()).thenReturn(null);
        MtConnectProtocolAdapter adapter = new MtConnectProtocolAdapter(information, adapterInput);
        assertThat(adapter).isNotNull();
        assertThat(adapter.tagMap).isNotNull().isEmpty();
        adapter.start(startInput, startOutput);
        verify(startOutput).failStart(isA(NullPointerException.class), isNull());
    }

    @Test
    public void whenTagIsNotFoundInTagMap_thenPollShouldFail() {
        when(config.isAllowUntrustedCertificates()).thenReturn(false);
        when(config.getHttpConnectTimeoutSeconds()).thenReturn(5);
        when(adapterInput.getAdapterId()).thenReturn("test");
        when(adapterInput.getProtocolAdapterState()).thenReturn(state);
        when(adapterInput.getConfig()).thenReturn(config);
        when(pollingInput.getPollingContext()).thenReturn(pollingContext);
        when(pollingContext.getTagName()).thenReturn("test");
        MtConnectProtocolAdapter adapter = new MtConnectProtocolAdapter(information, adapterInput);
        assertThat(adapter).as("Adapter shouldn't be null").isNotNull();
        assertThat(adapter.tagMap).as("TagMap should be empty").isNotNull().isEmpty();
        assertThat(adapter.getId()).as("ID should be 'test'").isEqualTo("test");
        adapter.start(startInput, startOutput);
        verify(state).setConnectionStatus(ProtocolAdapterState.ConnectionStatus.STATELESS);
        verify(startOutput).startedSuccessfully();
        adapter.poll(pollingInput, pollingOutput);
        verify(pollingOutput).fail(anyString());
        adapter.stop(stopInput, stopOutput);
        verify(stopOutput).stoppedSuccessfully();
        assertThat(adapter.httpClient).as("HttpClient is set to null in stop()").isNull();
    }
}
