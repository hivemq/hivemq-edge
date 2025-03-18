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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.exceptions.ProtocolAdapterException;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartOutput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStopInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStopOutput;
import com.hivemq.adapter.sdk.api.polling.batch.BatchPollingInput;
import com.hivemq.adapter.sdk.api.polling.batch.BatchPollingOutput;
import com.hivemq.adapter.sdk.api.state.ProtocolAdapterState;
import com.hivemq.edge.adapters.mtconnect.config.MtConnectAdapterConfig;
import com.hivemq.edge.adapters.mtconnect.config.MtConnectAdapterHttpHeader;
import com.hivemq.edge.adapters.mtconnect.config.tag.MtConnectAdapterTag;
import com.hivemq.edge.adapters.mtconnect.config.tag.MtConnectAdapterTagDefinition;
import com.hivemq.edge.adapters.mtconnect.schemas.MtConnectSchema;
import com.hivemq.edge.modules.adapters.impl.factories.AdapterFactoriesImpl;
import jakarta.xml.bind.JAXBException;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;

import javax.management.modelmbean.XMLParseException;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MtConnectProtocolAdapterTest {
    private static final @NotNull ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private @NotNull ProtocolAdapterInput<MtConnectAdapterConfig> adapterInput;
    private @NotNull MtConnectAdapterConfig config;
    private @NotNull HttpClient httpClient;
    private @NotNull ProtocolAdapterInformation information;
    private @NotNull BatchPollingInput pollingInput;
    private @NotNull BatchPollingOutput pollingOutput;
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
        pollingInput = mock(BatchPollingInput.class);
        pollingOutput = mock(BatchPollingOutput.class);
        startInput = mock(ProtocolAdapterStartInput.class);
        startOutput = mock(ProtocolAdapterStartOutput.class);
        state = mock(ProtocolAdapterState.class);
        stopInput = mock(ProtocolAdapterStopInput.class);
        stopOutput = mock(ProtocolAdapterStopOutput.class);
    }

    @Test
    public void whenProtocolAdapterStateIsNull_thenStartShouldFail() {
        when(adapterInput.getProtocolAdapterState()).thenReturn(null);
        MtConnectProtocolAdapter adapter = new MtConnectProtocolAdapter(information, adapterInput);
        assertThat(adapter).isNotNull();
        assertThat(adapter.tags).isNotNull().isEmpty();
        adapter.start(startInput, startOutput);
        verify(startOutput).failStart(isA(NullPointerException.class), isNull());
    }

    @Test
    public void whenTagsIsEmpty_thenPollShouldFail() {
        when(config.isAllowUntrustedCertificates()).thenReturn(false);
        when(config.getHttpConnectTimeoutSeconds()).thenReturn(5);
        when(adapterInput.getAdapterId()).thenReturn("streams");
        when(adapterInput.getProtocolAdapterState()).thenReturn(state);
        when(adapterInput.getConfig()).thenReturn(config);
        MtConnectProtocolAdapter adapter = new MtConnectProtocolAdapter(information, adapterInput);
        assertThat(adapter).as("Adapter shouldn't be null").isNotNull();
        assertThat(adapter.tags).as("Tags should be empty").isNotNull().isEmpty();
        assertThat(adapter.getId()).as("ID should be 'test'").isEqualTo("streams");
        adapter.start(startInput, startOutput);
        verify(state).setConnectionStatus(ProtocolAdapterState.ConnectionStatus.STATELESS);
        verify(startOutput).startedSuccessfully();
        adapter.poll(pollingInput, pollingOutput);
        verify(pollingOutput).fail(any(ProtocolAdapterException.class), anyString());
        adapter.stop(stopInput, stopOutput);
        verify(stopOutput).stoppedSuccessfully();
        assertThat(adapter.httpClient).as("HttpClient is set to null in stop()").isNull();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void whenTagsAreNotEmptyAndResponsesAreValid_thenPollShouldBeCalledSuccessfully() throws IOException {
        when(config.isAllowUntrustedCertificates()).thenReturn(false);
        when(config.getHttpConnectTimeoutSeconds()).thenReturn(5);
        when(adapterInput.getAdapterId()).thenReturn("streams");
        when(adapterInput.getProtocolAdapterState()).thenReturn(state);
        when(adapterInput.adapterFactories()).thenReturn(new AdapterFactoriesImpl());
        when(adapterInput.getConfig()).thenReturn(config);
        when(adapterInput.getTags()).thenReturn(List.of(new MtConnectAdapterTag("tagName",
                "tagDescription",
                new MtConnectAdapterTagDefinition("http://localhost/vds",
                        false,
                        true,
                        5,
                        List.of(new MtConnectAdapterHttpHeader("name", "value"))))));
        final ArgumentCaptor<ProtocolAdapterState.ConnectionStatus> argumentCaptorConnectionStatus =
                ArgumentCaptor.forClass(ProtocolAdapterState.ConnectionStatus.class);
        final ArgumentCaptor<DataPoint> argumentCaptorDataPoint = ArgumentCaptor.forClass(DataPoint.class);
        final HttpResponse<String> httpResponse = (HttpResponse<String>) mock(HttpResponse.class);
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(IOUtils.resourceToString("/streams/streams-1-3-smstestbed-current.xml",
                StandardCharsets.UTF_8));
        when(httpResponse.headers()).thenReturn(HttpHeaders.of(Map.of("Content-Type", List.of("application/xml")),
                (name, value) -> true));
        final CompletableFuture<HttpResponse<String>> completableFuture =
                CompletableFuture.completedFuture(httpResponse);
        final ArgumentCaptor<HttpRequest> argumentCaptorHttpRequest = ArgumentCaptor.forClass(HttpRequest.class);
        when(httpClient.sendAsync(argumentCaptorHttpRequest.capture(), any(HttpResponse.BodyHandler.class))).thenReturn(
                completableFuture);
        final MtConnectProtocolAdapter adapter = new MtConnectProtocolAdapter(information, adapterInput);
        adapter.httpClient = httpClient;
        assertThat(adapter).as("Adapter shouldn't be null").isNotNull();
        assertThat(adapter.tags).as("Tags should not be empty").isNotEmpty();
        assertThat(adapter.tags.stream().anyMatch(tag -> "tagName".equals(tag.getName()))).isTrue();
        assertThat(adapter.getId()).as("ID should be 'test'").isEqualTo("streams");
        adapter.start(startInput, startOutput);
        adapter.poll(pollingInput, pollingOutput);
        verify(pollingOutput).finish();
        verify(state, times(2)).setConnectionStatus(argumentCaptorConnectionStatus.capture());
        assertThat(argumentCaptorConnectionStatus.getAllValues()).isEqualTo(List.of(ProtocolAdapterState.ConnectionStatus.STATELESS,
                ProtocolAdapterState.ConnectionStatus.STATELESS));
        verify(pollingOutput).addDataPoint(argumentCaptorDataPoint.capture());
        final HttpHeaders httpHeaders = argumentCaptorHttpRequest.getValue().headers();
        assertThat(httpHeaders.firstValue("name").orElse("")).isEqualTo("value");
        assertThat(httpHeaders.firstValue("User-Agent").orElse("")).startsWith("HiveMQ-Edge");
        assertThat(argumentCaptorDataPoint.getValue().getTagName()).isEqualTo("tagName");
        final String jsonString = (String) argumentCaptorDataPoint.getValue().getTagValue();
        final JsonNode jsonNode = OBJECT_MAPPER.readTree(jsonString);
        assertThat(jsonNode.get(MtConnectProtocolAdapter.NODE_SCHEMA_LOCATION).asText()).isEqualTo(
                "urn:nist.gov:NistStreams:1.3 /schemas/NistStreams_1.3.xsd");
        assertThat(jsonNode.get("Streams")).isNotNull();
        final JsonNode jsonNodeDeviceStreams = jsonNode.get("Streams").get("DeviceStream");
        assertThat(jsonNodeDeviceStreams).isNotNull();
        assertThat(jsonNodeDeviceStreams.isArray()).isTrue();
        assertThat(jsonNodeDeviceStreams.size()).isEqualTo(8);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void whenTagsAreNotEmptyAndResponsesAreInvalid_thenPollShouldFail() throws IOException {
        when(config.isAllowUntrustedCertificates()).thenReturn(false);
        when(config.getHttpConnectTimeoutSeconds()).thenReturn(5);
        when(adapterInput.getAdapterId()).thenReturn("streams");
        when(adapterInput.getProtocolAdapterState()).thenReturn(state);
        when(adapterInput.adapterFactories()).thenReturn(new AdapterFactoriesImpl());
        when(adapterInput.getConfig()).thenReturn(config);
        when(adapterInput.getTags()).thenReturn(List.of(new MtConnectAdapterTag("tagName",
                "tagDescription",
                new MtConnectAdapterTagDefinition("http://localhost/vds",
                        false,
                        true,
                        5,
                        List.of(new MtConnectAdapterHttpHeader("name", "value"))))));
        final ArgumentCaptor<ProtocolAdapterState.ConnectionStatus> argumentCaptorConnectionStatus =
                ArgumentCaptor.forClass(ProtocolAdapterState.ConnectionStatus.class);
        final ArgumentCaptor<Throwable> argumentCaptorThrowable = ArgumentCaptor.forClass(Throwable.class);
        final ArgumentCaptor<String> argumentCaptorString = ArgumentCaptor.forClass(String.class);
        final HttpResponse<String> httpResponse = (HttpResponse<String>) mock(HttpResponse.class);
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn("I'm not a valid XML.");
        when(httpResponse.headers()).thenReturn(HttpHeaders.of(Map.of("Content-Type", List.of("application/xml")),
                (name, value) -> true));
        final CompletableFuture<HttpResponse<String>> completableFuture =
                CompletableFuture.completedFuture(httpResponse);
        final ArgumentCaptor<HttpRequest> argumentCaptorHttpRequest = ArgumentCaptor.forClass(HttpRequest.class);
        when(httpClient.sendAsync(argumentCaptorHttpRequest.capture(), any(HttpResponse.BodyHandler.class))).thenReturn(
                completableFuture);
        final MtConnectProtocolAdapter adapter = new MtConnectProtocolAdapter(information, adapterInput);
        adapter.httpClient = httpClient;
        assertThat(adapter).as("Adapter shouldn't be null").isNotNull();
        assertThat(adapter.tags).as("Tags should not be empty").isNotEmpty();
        assertThat(adapter.tags.stream().anyMatch(tag -> "tagName".equals(tag.getName()))).isTrue();
        assertThat(adapter.getId()).as("ID should be 'test'").isEqualTo("streams");
        adapter.start(startInput, startOutput);
        adapter.poll(pollingInput, pollingOutput);
        verify(pollingOutput).fail(argumentCaptorThrowable.capture(), argumentCaptorString.capture());
        verify(state, times(2)).setConnectionStatus(argumentCaptorConnectionStatus.capture());
        assertThat(argumentCaptorConnectionStatus.getAllValues()).isEqualTo(List.of(ProtocolAdapterState.ConnectionStatus.STATELESS,
                ProtocolAdapterState.ConnectionStatus.ERROR));
    }

    @Test
    public void whenSchemaValidationIsDisabled_thenCustomSchemaShouldPass()
            throws IOException, XMLParseException, JAXBException {
        when(adapterInput.adapterFactories()).thenReturn(new AdapterFactoriesImpl());
        final MtConnectProtocolAdapter adapter = new MtConnectProtocolAdapter(information, adapterInput);
        final String jsonString = adapter.processXml(IOUtils.resourceToString(
                "/streams/streams-1-3-smstestbed-time-series.xml",
                StandardCharsets.UTF_8), new MtConnectAdapterTagDefinition("", false, true, 10, List.of()));
        assertThat(jsonString).isNotNull();
        final JsonNode jsonNode = OBJECT_MAPPER.readTree(jsonString);
        assertThat(jsonNode.get(MtConnectProtocolAdapter.NODE_SCHEMA_LOCATION).asText()).isEqualTo(
                "urn:nist.gov:NistStreams:1.3 /schemas/NistStreams_1.3.xsd");
    }

    @ParameterizedTest
    @CsvSource({"true,true", "true,false", "false,true", "false,false"})
    public void whenSchemaValidationIsEnabledOrDisabled_thenStandardSchemaShouldPass(
            boolean enableSchemaValidation,
            boolean includeNull)
            throws IOException, XMLParseException, JAXBException {
        when(adapterInput.adapterFactories()).thenReturn(new AdapterFactoriesImpl());
        final MtConnectProtocolAdapter adapter = new MtConnectProtocolAdapter(information, adapterInput);
        final String jsonString = adapter.processXml(IOUtils.resourceToString("/devices/devices-1-3-smstestbed.xml",
                        StandardCharsets.UTF_8),
                new MtConnectAdapterTagDefinition("", enableSchemaValidation, includeNull, 10, List.of()));
        assertThat(jsonString).isNotNull();
        final JsonNode jsonNode = OBJECT_MAPPER.readTree(jsonString);
        if (!enableSchemaValidation) {
            assertThat(jsonNode.get(MtConnectProtocolAdapter.NODE_SCHEMA_LOCATION)
                    .asText()).isEqualTo(MtConnectSchema.Devices_1_3.getLocation());
        }
        assertThat(jsonNode.get("Header")).isNotNull();
        assertThat(jsonNode.get("Devices")).isNotNull();
        assertThat(jsonNode.get("Streams")).isNull();
        if (includeNull) {
            if (enableSchemaValidation) {
                assertThat(jsonNode.get("Header").has("AssetCounts")).isTrue();
                assertThat(jsonNode.get("Header").get("AssetCounts")).isInstanceOf(NullNode.class);
            } else {
                assertThat(jsonNode.get("Header").has("AssetCounts")).isFalse();
            }
        } else {
            assertThat(jsonNode.get("Header").has("AssetCounts")).isFalse();
        }
    }

    @Test
    public void whenSchemaValidationIsEnabled_thenCustomSchemaShouldFail() {
        when(adapterInput.adapterFactories()).thenReturn(new AdapterFactoriesImpl());
        final MtConnectProtocolAdapter adapter = new MtConnectProtocolAdapter(information, adapterInput);
        assertThatThrownBy(() -> adapter.processXml(IOUtils.resourceToString(
                        "/streams/streams-1-3-smstestbed-current.xml",
                        StandardCharsets.UTF_8),
                new MtConnectAdapterTagDefinition("", true, true, 10, List.of()))).isInstanceOf(XMLParseException.class)
                .hasMessage(
                        "XML Parse Exception: Schema urn:nist.gov:NistStreams:1.3 /schemas/NistStreams_1.3.xsd is not support");
    }
}
