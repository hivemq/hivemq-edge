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
import com.hivemq.edge.adapters.mtconnect.config.MtConnectAdapterHttpHeader;
import com.hivemq.edge.adapters.mtconnect.config.tag.MtConnectAdapterTag;
import com.hivemq.edge.adapters.mtconnect.config.tag.MtConnectAdapterTagDefinition;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MtConnectProtocolAdapterTest {
    private @NotNull ProtocolAdapterInput<MtConnectAdapterConfig> adapterInput;
    private @NotNull MtConnectAdapterConfig config;
    private @NotNull HttpClient httpClient;
    private @NotNull ProtocolAdapterInformation information;
    private @NotNull PollingContext pollingContext;
    private @NotNull PollingInput pollingInput;
    private @NotNull PollingOutput pollingOutput;
    private @NotNull ProtocolAdapterStartInput startInput;
    private @NotNull ProtocolAdapterStartOutput startOutput;
    private @NotNull ProtocolAdapterState state;
    private @NotNull ProtocolAdapterStopInput stopInput;
    private @NotNull ProtocolAdapterStopOutput stopOutput;

    @BeforeEach
    @SuppressWarnings("unchecked")
    public void setUp() {
        adapterInput = (ProtocolAdapterInput<MtConnectAdapterConfig>) mock(ProtocolAdapterInput.class);
        config = mock(MtConnectAdapterConfig.class);
        httpClient = mock(HttpClient.class);
        information = mock(ProtocolAdapterInformation.class);
        pollingContext = mock(PollingContext.class);
        pollingInput = mock(PollingInput.class);
        pollingOutput = mock(PollingOutput.class);
        startInput = mock(ProtocolAdapterStartInput.class);
        startOutput = mock(ProtocolAdapterStartOutput.class);
        state = mock(ProtocolAdapterState.class);
        stopInput = mock(ProtocolAdapterStopInput.class);
        stopOutput = mock(ProtocolAdapterStopOutput.class);
    }

    protected @NotNull String getXml(final @NotNull String fileName) {
        final String filePath = "/smstestbed/" + fileName;
        try {
            return IOUtils.resourceToString(filePath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            fail();
            return "";
        }
    }

    protected @NotNull String getVolatileDataStreamCurrent() {
        return getXml("volatile-data-stream-current.xml");
    }

    protected @NotNull String getVolatileDataStreamTimeSeries() {
        return getXml("volatile-data-stream-time-series.xml");
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
        when(adapterInput.getAdapterId()).thenReturn("smstestbed");
        when(adapterInput.getProtocolAdapterState()).thenReturn(state);
        when(adapterInput.getConfig()).thenReturn(config);
        when(pollingInput.getPollingContext()).thenReturn(pollingContext);
        when(pollingContext.getTagName()).thenReturn("smstestbed");
        MtConnectProtocolAdapter adapter = new MtConnectProtocolAdapter(information, adapterInput);
        assertThat(adapter).as("Adapter shouldn't be null").isNotNull();
        assertThat(adapter.tagMap).as("TagMap should be empty").isNotNull().isEmpty();
        assertThat(adapter.getId()).as("ID should be 'test'").isEqualTo("smstestbed");
        adapter.start(startInput, startOutput);
        verify(state).setConnectionStatus(ProtocolAdapterState.ConnectionStatus.STATELESS);
        verify(startOutput).startedSuccessfully();
        adapter.poll(pollingInput, pollingOutput);
        verify(pollingOutput).fail(anyString());
        adapter.stop(stopInput, stopOutput);
        verify(stopOutput).stoppedSuccessfully();
        assertThat(adapter.httpClient).as("HttpClient is set to null in stop()").isNull();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void whenTagIsFound_thenPollXmlShouldBeCalled() throws IOException {
        when(config.isAllowUntrustedCertificates()).thenReturn(false);
        when(config.getHttpConnectTimeoutSeconds()).thenReturn(5);
        when(adapterInput.getAdapterId()).thenReturn("smstestbed");
        when(adapterInput.getProtocolAdapterState()).thenReturn(state);
        when(adapterInput.getConfig()).thenReturn(config);
        when(adapterInput.getTags()).thenReturn(List.of(new MtConnectAdapterTag("tagName",
                "tagDescription",
                new MtConnectAdapterTagDefinition("http://localhost/vds",
                        5,
                        List.of(new MtConnectAdapterHttpHeader("name", "value"))))));
        when(pollingInput.getPollingContext()).thenReturn(pollingContext);
        when(pollingContext.getTagName()).thenReturn("tagName");
        HttpResponse<String> httpResponse = (HttpResponse<String>) mock(HttpResponse.class);
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(getVolatileDataStreamCurrent());
        CompletableFuture<HttpResponse<String>> completableFuture = CompletableFuture.completedFuture(httpResponse);
        ArgumentCaptor<HttpRequest> argumentCaptorHttpRequest = ArgumentCaptor.forClass(HttpRequest.class);
        when(httpClient.sendAsync(argumentCaptorHttpRequest.capture(), any(HttpResponse.BodyHandler.class))).thenReturn(
                completableFuture);
        MtConnectProtocolAdapter adapter = new MtConnectProtocolAdapter(information, adapterInput);
        adapter.httpClient = httpClient;
        assertThat(adapter).as("Adapter shouldn't be null").isNotNull();
        assertThat(adapter.tagMap).as("TagMap should not be empty").isNotEmpty().containsKey("tagName");
        assertThat(adapter.getId()).as("ID should be 'test'").isEqualTo("smstestbed");
        adapter.start(startInput, startOutput);
        adapter.poll(pollingInput, pollingOutput);
        verify(pollingOutput).finish();
        HttpHeaders httpHeaders = argumentCaptorHttpRequest.getValue().headers();
        assertThat(httpHeaders.firstValue("name").orElse("")).isEqualTo("value");
        assertThat(httpHeaders.firstValue("User-Agent").orElse("")).startsWith("HiveMQ-Edge");
    }
}
