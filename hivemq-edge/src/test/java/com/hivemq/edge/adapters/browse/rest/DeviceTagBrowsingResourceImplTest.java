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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.adapter.sdk.api.ProtocolAdapter;
import com.hivemq.edge.adapters.browse.BrowseException;
import com.hivemq.edge.adapters.browse.BrowsedNode;
import com.hivemq.edge.adapters.browse.BulkTagBrowser;
import com.hivemq.edge.adapters.browse.file.DeviceTagCsvSerializer;
import com.hivemq.edge.adapters.browse.file.DeviceTagJsonSerializer;
import com.hivemq.edge.adapters.browse.file.DeviceTagYamlSerializer;
import com.hivemq.edge.adapters.browse.importer.DeviceTagImporter;
import com.hivemq.edge.adapters.browse.importer.DeviceTagImporterException;
import com.hivemq.edge.adapters.browse.model.ImportMode;
import com.hivemq.edge.adapters.browse.model.ImportResult;
import com.hivemq.edge.adapters.browse.validate.ValidationError;
import com.hivemq.protocols.ProtocolAdapterManager;
import com.hivemq.protocols.ProtocolAdapterWrapper;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Unit tests for {@link DeviceTagBrowsingResourceImpl}.
 */
class DeviceTagBrowsingResourceImplTest {

    private static final @NotNull String ADAPTER_ID = "test-adapter";
    private static final @NotNull String EMPTY_JSON = "{\"rows\":[]}";
    private static final @NotNull String EMPTY_YAML = "rows: []\n";
    private static final @NotNull String EMPTY_CSV = "tag_name,node_id,northbound_topic\n";

    private ProtocolAdapterManager adapterManager;
    private DeviceTagImporter importer;
    private DeviceTagBrowsingResourceImpl resource;
    private HttpHeaders httpHeaders;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws Exception {
        adapterManager = mock(ProtocolAdapterManager.class);
        final ProtocolAdapterWrapper wrapper = mock(ProtocolAdapterWrapper.class);
        final BrowsableAdapter adapter = mock(BrowsableAdapter.class);

        when(adapterManager.getProtocolAdapterWrapperByAdapterId(anyString())).thenReturn(Optional.empty());
        when(adapterManager.getProtocolAdapterWrapperByAdapterId(ADAPTER_ID)).thenReturn(Optional.of(wrapper));
        when(wrapper.getAdapter()).thenReturn(adapter);
        when(adapter.browse(any(), anyInt())).thenReturn(Stream.of(testNode()));

        importer = mock(DeviceTagImporter.class);
        resource = new DeviceTagBrowsingResourceImpl(
                adapterManager,
                new DeviceTagCsvSerializer(),
                new DeviceTagJsonSerializer(),
                new DeviceTagYamlSerializer(),
                importer,
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

    // -- Browse error paths --

    @Test
    void browse_adapterNotFound_returns404() {
        when(httpHeaders.getHeaderString("Accept")).thenReturn("application/json");
        final Response response = resource.browseDeviceTags("non-existent", null, null);
        assertThat(response.getStatus()).isEqualTo(404);
    }

    @Test
    void browse_adapterNotBulkTagBrowser_returns409() {
        final ProtocolAdapterWrapper wrapper = mock(ProtocolAdapterWrapper.class);
        final ProtocolAdapter plainAdapter = mock(ProtocolAdapter.class);
        when(adapterManager.getProtocolAdapterWrapperByAdapterId("plain-adapter"))
                .thenReturn(Optional.of(wrapper));
        when(wrapper.getAdapter()).thenReturn(plainAdapter);

        when(httpHeaders.getHeaderString("Accept")).thenReturn("application/json");
        final Response response = resource.browseDeviceTags("plain-adapter", null, null);
        assertThat(response.getStatus()).isEqualTo(409);
    }

    @Test
    void browse_browseException_returns409() throws BrowseException {
        final ProtocolAdapterWrapper wrapper = mock(ProtocolAdapterWrapper.class);
        final BrowsableAdapter failingAdapter = mock(BrowsableAdapter.class);
        when(adapterManager.getProtocolAdapterWrapperByAdapterId("failing")).thenReturn(Optional.of(wrapper));
        when(wrapper.getAdapter()).thenReturn(failingAdapter);
        when(failingAdapter.browse(any(), anyInt())).thenThrow(new BrowseException("Server error"));

        when(httpHeaders.getHeaderString("Accept")).thenReturn("application/json");
        final Response response = resource.browseDeviceTags("failing", null, null);
        assertThat(response.getStatus()).isEqualTo(409);
    }

    @Test
    void browse_browseExceptionTimeout_returns504() throws BrowseException {
        final ProtocolAdapterWrapper wrapper = mock(ProtocolAdapterWrapper.class);
        final BrowsableAdapter failingAdapter = mock(BrowsableAdapter.class);
        when(adapterManager.getProtocolAdapterWrapperByAdapterId("timeout")).thenReturn(Optional.of(wrapper));
        when(wrapper.getAdapter()).thenReturn(failingAdapter);
        when(failingAdapter.browse(any(), anyInt())).thenThrow(new BrowseException("Operation timed out after 30s"));

        when(httpHeaders.getHeaderString("Accept")).thenReturn("application/json");
        final Response response = resource.browseDeviceTags("timeout", null, null);
        assertThat(response.getStatus()).isEqualTo(504);
    }

    @Test
    void browse_browseExceptionNullMessage_returns409() throws BrowseException {
        final ProtocolAdapterWrapper wrapper = mock(ProtocolAdapterWrapper.class);
        final BrowsableAdapter failingAdapter = mock(BrowsableAdapter.class);
        when(adapterManager.getProtocolAdapterWrapperByAdapterId("null-msg")).thenReturn(Optional.of(wrapper));
        when(wrapper.getAdapter()).thenReturn(failingAdapter);
        when(failingAdapter.browse(any(), anyInt())).thenThrow(new BrowseException("Browse failed", null));

        when(httpHeaders.getHeaderString("Accept")).thenReturn("application/json");
        final Response response = resource.browseDeviceTags("null-msg", null, null);
        assertThat(response.getStatus()).isEqualTo(409);
    }

    // -- Browse Content-Disposition header --

    @Test
    void browse_csvContentDisposition_containsAdapterIdAndExtension() {
        when(httpHeaders.getHeaderString("Accept")).thenReturn("text/csv");
        final Response response = resource.browseDeviceTags(ADAPTER_ID, null, null);
        assertThat(response.getHeaderString("Content-Disposition"))
                .contains(ADAPTER_ID)
                .contains(".csv");
    }

    @Test
    void browse_jsonContentDisposition_hasJsonExtension() {
        when(httpHeaders.getHeaderString("Accept")).thenReturn("application/json");
        final Response response = resource.browseDeviceTags(ADAPTER_ID, null, null);
        assertThat(response.getHeaderString("Content-Disposition")).contains(".json");
    }

    @Test
    void browse_yamlContentDisposition_hasYamlExtension() {
        when(httpHeaders.getHeaderString("Accept")).thenReturn("application/yaml");
        final Response response = resource.browseDeviceTags(ADAPTER_ID, null, null);
        assertThat(response.getHeaderString("Content-Disposition")).contains(".yaml");
    }

    // -- Import: adapter lookup --

    @Test
    void import_adapterNotFound_returns404() {
        final Response response = resource.importDeviceTags("non-existent", tempFile(EMPTY_JSON), null, null);
        assertThat(response.getStatus()).isEqualTo(404);
    }

    // -- Import: mode validation --

    @Test
    void import_invalidMode_returns400() {
        when(httpHeaders.getHeaderString("Content-Type")).thenReturn("application/json");
        final Response response = resource.importDeviceTags(ADAPTER_ID, tempFile(EMPTY_JSON), "INVALID_MODE", null);
        assertThat(response.getStatus()).isEqualTo(400);
    }

    @Test
    void import_nullMode_defaultsToMergeSafe() throws DeviceTagImporterException {
        when(httpHeaders.getHeaderString("Content-Type")).thenReturn("application/json");
        when(importer.doImport(anyList(), eq(ImportMode.MERGE_SAFE), eq(ADAPTER_ID), any()))
                .thenReturn(emptyResult());
        final Response response = resource.importDeviceTags(ADAPTER_ID, tempFile(EMPTY_JSON), null, null);
        assertThat(response.getStatus()).isEqualTo(200);
        verify(importer).doImport(anyList(), eq(ImportMode.MERGE_SAFE), eq(ADAPTER_ID), any());
    }

    // -- Import: content type dispatch --

    @Test
    void import_jsonContentType_returns200() throws DeviceTagImporterException {
        when(httpHeaders.getHeaderString("Content-Type")).thenReturn("application/json");
        when(importer.doImport(anyList(), any(), eq(ADAPTER_ID), any())).thenReturn(emptyResult());
        final Response response = resource.importDeviceTags(ADAPTER_ID, tempFile(EMPTY_JSON), "CREATE", null);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void import_csvContentType_returns200() throws DeviceTagImporterException {
        when(httpHeaders.getHeaderString("Content-Type")).thenReturn("text/csv");
        when(importer.doImport(anyList(), any(), eq(ADAPTER_ID), any())).thenReturn(emptyResult());
        final Response response = resource.importDeviceTags(ADAPTER_ID, tempFile(EMPTY_CSV), null, null);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void import_yamlContentType_returns200() throws DeviceTagImporterException {
        when(httpHeaders.getHeaderString("Content-Type")).thenReturn("application/yaml");
        when(importer.doImport(anyList(), any(), eq(ADAPTER_ID), any())).thenReturn(emptyResult());
        final Response response = resource.importDeviceTags(ADAPTER_ID, tempFile(EMPTY_YAML), null, null);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void import_unsupportedContentType_returns415() {
        when(httpHeaders.getHeaderString("Content-Type")).thenReturn("application/xml");
        final Response response = resource.importDeviceTags(ADAPTER_ID, tempFile("<xml/>"), null, null);
        assertThat(response.getStatus()).isEqualTo(415);
    }

    @Test
    void import_nullContentType_returns415() {
        when(httpHeaders.getHeaderString("Content-Type")).thenReturn(null);
        final Response response = resource.importDeviceTags(ADAPTER_ID, tempFile(EMPTY_JSON), null, null);
        assertThat(response.getStatus()).isEqualTo(415);
    }

    // -- Import: charset handling --

    @Test
    void import_nonUtf8Charset_returns415() {
        when(httpHeaders.getHeaderString("Content-Type")).thenReturn("text/csv; charset=iso-8859-1");
        final Response response = resource.importDeviceTags(ADAPTER_ID, tempFile("data"), null, null);
        assertThat(response.getStatus()).isEqualTo(415);
    }

    @Test
    void import_utf8CharsetExplicit_accepted() throws DeviceTagImporterException {
        when(httpHeaders.getHeaderString("Content-Type")).thenReturn("application/json; charset=utf-8");
        when(importer.doImport(anyList(), any(), eq(ADAPTER_ID), any())).thenReturn(emptyResult());
        final Response response = resource.importDeviceTags(ADAPTER_ID, tempFile(EMPTY_JSON), null, null);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    // -- Import: parse errors --

    @Test
    void import_malformedJson_returns400() {
        when(httpHeaders.getHeaderString("Content-Type")).thenReturn("application/json");
        final Response response = resource.importDeviceTags(ADAPTER_ID, tempFile("{not-json"), null, null);
        assertThat(response.getStatus()).isEqualTo(400);
    }

    // -- Import: validation and runtime errors --

    @Test
    void import_validationErrors_returns400() throws DeviceTagImporterException {
        when(httpHeaders.getHeaderString("Content-Type")).thenReturn("application/json");
        final List<ValidationError> errors = List.of(new ValidationError(
                1, "tag_name", "dup", ValidationError.Code.DUPLICATE_TAG_NAME, "Duplicate tag name"));
        when(importer.doImport(anyList(), any(), eq(ADAPTER_ID), any()))
                .thenThrow(new DeviceTagImporterException(errors));
        final Response response = resource.importDeviceTags(ADAPTER_ID, tempFile(EMPTY_JSON), null, null);
        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getEntity().toString()).contains("Validation Failed");
    }

    @Test
    void import_runtimeException_returns500() throws DeviceTagImporterException {
        when(httpHeaders.getHeaderString("Content-Type")).thenReturn("application/json");
        when(importer.doImport(anyList(), any(), eq(ADAPTER_ID), any()))
                .thenThrow(new RuntimeException("Unexpected error"));
        final Response response = resource.importDeviceTags(ADAPTER_ID, tempFile(EMPTY_JSON), null, null);
        assertThat(response.getStatus()).isEqualTo(500);
    }

    // -- Helpers --

    private @NotNull File tempFile(final @NotNull String content) {
        try {
            final Path file = tempDir.resolve("import-" + System.nanoTime() + ".tmp");
            Files.writeString(file, content);
            return file.toFile();
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static @NotNull ImportResult emptyResult() {
        return new ImportResult(0, 0, 0, 0, 0, 0, 0, List.of());
    }

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
