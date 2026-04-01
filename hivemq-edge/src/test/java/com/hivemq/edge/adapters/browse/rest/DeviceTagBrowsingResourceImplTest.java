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
package com.hivemq.edge.adapters.browse.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.adapter.sdk.api.ProtocolAdapter;
import com.hivemq.edge.adapters.browse.BrowsedNode;
import com.hivemq.edge.adapters.browse.BulkTagBrowser;
import com.hivemq.edge.adapters.browse.file.DeviceTagCsvSerializer;
import com.hivemq.edge.adapters.browse.file.DeviceTagJsonSerializer;
import com.hivemq.edge.adapters.browse.file.DeviceTagYamlSerializer;
import com.hivemq.edge.adapters.browse.importer.DeviceTagImporter;
import com.hivemq.protocols.ProtocolAdapterManager;
import com.hivemq.protocols.ProtocolAdapterWrapper;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import java.lang.reflect.Field;
import java.util.Optional;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Unit tests for {@link DeviceTagBrowsingResourceImpl}, focused on Accept-header content negotiation
 * in {@code resolveFormat()}.
 */
class DeviceTagBrowsingResourceImplTest {

    private static final @NotNull String ADAPTER_ID = "test-adapter";

    private DeviceTagBrowsingResourceImpl resource;
    private HttpHeaders httpHeaders;

    @BeforeEach
    void setUp() throws Exception {
        final ProtocolAdapterManager adapterManager = mock(ProtocolAdapterManager.class);
        final ProtocolAdapterWrapper wrapper = mock(ProtocolAdapterWrapper.class);
        final BrowsableAdapter adapter = mock(BrowsableAdapter.class);

        when(adapterManager.getProtocolAdapterWrapperByAdapterId(ADAPTER_ID)).thenReturn(Optional.of(wrapper));
        when(wrapper.getAdapter()).thenReturn(adapter);
        when(adapter.browse(any(), anyInt())).thenReturn(Stream.of(testNode()));

        resource = new DeviceTagBrowsingResourceImpl(
                adapterManager,
                new DeviceTagCsvSerializer(),
                new DeviceTagJsonSerializer(),
                new DeviceTagYamlSerializer(),
                mock(DeviceTagImporter.class),
                new ObjectMapper());

        httpHeaders = mock(HttpHeaders.class);
        setHeaders(resource, httpHeaders);
    }

    private static void setHeaders(final @NotNull DeviceTagBrowsingResourceImpl resource, final HttpHeaders headers) {
        try {
            final Field field = resource.getClass().getSuperclass().getDeclaredField("headers");
            field.setAccessible(true);
            field.set(resource, headers);
        } catch (final ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    // -- Single-value Accept headers --

    @Test
    void browse_acceptCsv_returnsCsv() {
        when(httpHeaders.getHeaderString("Accept")).thenReturn("text/csv");
        final Response response = resource.browseDeviceTags(ADAPTER_ID, null, null);
        assertThat(response.getMediaType().toString()).contains("text/csv");
    }

    @Test
    void browse_acceptJson_returnsJson() {
        when(httpHeaders.getHeaderString("Accept")).thenReturn("application/json");
        final Response response = resource.browseDeviceTags(ADAPTER_ID, null, null);
        assertThat(response.getMediaType().toString()).contains("application/json");
    }

    @Test
    void browse_acceptYaml_returnsYaml() {
        when(httpHeaders.getHeaderString("Accept")).thenReturn("application/yaml");
        final Response response = resource.browseDeviceTags(ADAPTER_ID, null, null);
        assertThat(response.getMediaType().toString()).contains("application/yaml");
    }

    // -- Multi-value Accept headers (the bug that Finding 5 reported) --

    @Test
    void browse_acceptCsvFirst_commaJoined_returnsCsv() {
        when(httpHeaders.getHeaderString("Accept")).thenReturn("text/csv, application/json, application/yaml");
        final Response response = resource.browseDeviceTags(ADAPTER_ID, null, null);
        assertThat(response.getMediaType().toString()).contains("text/csv");
    }

    @Test
    void browse_acceptCsvFirst_noSpaces_returnsCsv() {
        when(httpHeaders.getHeaderString("Accept")).thenReturn("text/csv,application/json,application/yaml");
        final Response response = resource.browseDeviceTags(ADAPTER_ID, null, null);
        assertThat(response.getMediaType().toString()).contains("text/csv");
    }

    @Test
    void browse_acceptYamlFirst_commaJoined_returnsYaml() {
        when(httpHeaders.getHeaderString("Accept")).thenReturn("application/yaml, text/csv, application/json");
        final Response response = resource.browseDeviceTags(ADAPTER_ID, null, null);
        assertThat(response.getMediaType().toString()).contains("application/yaml");
    }

    @Test
    void browse_acceptJsonFirst_commaJoined_returnsJson() {
        when(httpHeaders.getHeaderString("Accept")).thenReturn("application/json, text/csv, application/yaml");
        final Response response = resource.browseDeviceTags(ADAPTER_ID, null, null);
        assertThat(response.getMediaType().toString()).contains("application/json");
    }

    // -- Defaults and edge cases --

    @Test
    void browse_noAcceptHeader_defaultsToCsv() {
        when(httpHeaders.getHeaderString("Accept")).thenReturn(null);
        final Response response = resource.browseDeviceTags(ADAPTER_ID, null, null);
        assertThat(response.getMediaType().toString()).contains("text/csv");
    }

    @Test
    void browse_emptyAcceptHeader_defaultsToCsv() {
        when(httpHeaders.getHeaderString("Accept")).thenReturn("");
        final Response response = resource.browseDeviceTags(ADAPTER_ID, null, null);
        assertThat(response.getMediaType().toString()).contains("text/csv");
    }

    @Test
    void browse_wildcardAccept_defaultsToCsv() {
        when(httpHeaders.getHeaderString("Accept")).thenReturn("*/*");
        final Response response = resource.browseDeviceTags(ADAPTER_ID, null, null);
        assertThat(response.getMediaType().toString()).contains("text/csv");
    }

    @Test
    void browse_nullHeaders_defaultsToCsv() {
        setHeaders(resource, null);
        final Response response = resource.browseDeviceTags(ADAPTER_ID, null, null);
        assertThat(response.getMediaType().toString()).contains("text/csv");
    }

    @ParameterizedTest
    @CsvSource({
        "text/csv,                                          text/csv",
        "application/json,                                  application/json",
        "application/yaml,                                  application/yaml",
        "'text/csv, application/json',                      text/csv",
        "'application/yaml, application/json',              application/yaml",
        "'application/json, application/yaml, text/csv',    application/json",
        "'text/csv,application/json,application/yaml',      text/csv",
    })
    void browse_parameterized_acceptHeaderContentNegotiation(
            final @NotNull String acceptHeader, final @NotNull String expectedMediaType) {
        when(httpHeaders.getHeaderString("Accept")).thenReturn(acceptHeader);
        final Response response = resource.browseDeviceTags(ADAPTER_ID, null, null);
        assertThat(response.getMediaType().toString()).contains(expectedMediaType);
    }

    // -- Negative maxDepth ---

    @Test
    void browse_negativeMaxDepth_returns400() {
        when(httpHeaders.getHeaderString("Accept")).thenReturn("application/json");
        final Response response = resource.browseDeviceTags(ADAPTER_ID, null, -1);
        assertThat(response.getStatus()).isEqualTo(400);
    }

    @Test
    void browse_zeroMaxDepth_succeeds() {
        when(httpHeaders.getHeaderString("Accept")).thenReturn("application/json");
        final Response response = resource.browseDeviceTags(ADAPTER_ID, null, 0);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    // -- Helpers --

    private static @NotNull BrowsedNode testNode() {
        return new BrowsedNode(
                "/Test/Node",
                "urn:test",
                2,
                "ns=2;i=1001",
                "Int32",
                "READ",
                "A test node",
                "test-node",
                "Test node description",
                "test-adapter/test-node",
                "test-adapter/w/test-node");
    }

    /** Combine ProtocolAdapter + BulkTagBrowser so Mockito can mock both in one object. */
    interface BrowsableAdapter extends ProtocolAdapter, BulkTagBrowser {}
}
