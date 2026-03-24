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
package com.hivemq.edge.adapters.browse.importer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hivemq.configuration.entity.adapter.MqttUserPropertyEntity;
import com.hivemq.configuration.entity.adapter.NorthboundMappingEntity;
import com.hivemq.configuration.entity.adapter.ProtocolAdapterEntity;
import com.hivemq.configuration.entity.adapter.SouthboundMappingEntity;
import com.hivemq.configuration.entity.adapter.TagEntity;
import com.hivemq.configuration.reader.DataCombiningExtractor;
import com.hivemq.configuration.reader.ProtocolAdapterExtractor;
import com.hivemq.edge.adapters.browse.model.DeviceTagRow;
import com.hivemq.edge.adapters.browse.model.FieldMappingInstruction;
import com.hivemq.edge.adapters.browse.model.ImportMode;
import com.hivemq.edge.adapters.browse.model.ImportResult;
import com.hivemq.edge.adapters.browse.model.TagAction;
import com.hivemq.edge.adapters.browse.validate.DeviceTagValidator;
import com.hivemq.edge.adapters.browse.validate.ValidationError;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class DeviceTagImporterTest {

    private static final String ADAPTER_ID = "test-adapter";

    private final ProtocolAdapterExtractor adapterExtractor = mock(ProtocolAdapterExtractor.class);
    private final DataCombiningExtractor combiningExtractor = mock(DataCombiningExtractor.class);
    private DeviceTagImporter importer;

    @BeforeEach
    void setUp() {
        when(combiningExtractor.getAllCombiners()).thenReturn(List.of());
        when(adapterExtractor.getAllConfigs()).thenReturn(List.of());
        importer =
                new DeviceTagImporter(new DeviceTagValidator(adapterExtractor, combiningExtractor), adapterExtractor);
    }

    private ProtocolAdapterEntity emptyAdapter() {
        return new ProtocolAdapterEntity(ADAPTER_ID, "opcua", 1, Map.of(), List.of(), List.of(), List.of());
    }

    private ProtocolAdapterEntity adapterWithTags(
            final List<TagEntity> tags,
            final List<NorthboundMappingEntity> nb,
            final List<SouthboundMappingEntity> sb) {
        return new ProtocolAdapterEntity(
                ADAPTER_ID, "opcua", 1, Map.of(), new ArrayList<>(nb), new ArrayList<>(sb), new ArrayList<>(tags));
    }

    private void setupAdapter(final ProtocolAdapterEntity adapter) {
        when(adapterExtractor.getAdapterByAdapterId(ADAPTER_ID)).thenReturn(Optional.of(adapter));
        when(adapterExtractor.getAllConfigs()).thenReturn(List.of(adapter));
        when(adapterExtractor.updateAdapter(any())).thenReturn(true);
    }

    private DeviceTagRow tagRow(final String tagName, final String nodeId) {
        return DeviceTagRow.builder().tagName(tagName).nodeId(nodeId).build();
    }

    private DeviceTagRow tagRowWithMappings(
            final String tagName, final String nodeId, final String nbTopic, final String sbTopic) {
        return DeviceTagRow.builder()
                .tagName(tagName)
                .nodeId(nodeId)
                .northboundTopic(nbTopic)
                .southboundTopic(sbTopic)
                .build();
    }

    // --- Multi-row (multiple northbound mappings per tag, EDG-362) ---

    @Test
    void create_multipleNorthboundMappings_sameTag() throws DeviceTagImporterException {
        setupAdapter(emptyAdapter());

        final List<DeviceTagRow> rows = List.of(
                tagRowWithMappings("tag1", "ns=2;i=1", "topic/a", null),
                tagRowWithMappings("tag1", "ns=2;i=1", "topic/b", null));

        final ImportResult result = importer.doImport(rows, ImportMode.CREATE, ADAPTER_ID);

        assertThat(result.tagsCreated()).isEqualTo(1);
        assertThat(result.northboundMappingsCreated()).isEqualTo(2);

        final ArgumentCaptor<ProtocolAdapterEntity> captor = ArgumentCaptor.forClass(ProtocolAdapterEntity.class);
        verify(adapterExtractor).updateAdapter(captor.capture());
        assertThat(captor.getValue().getTags()).hasSize(1);
        assertThat(captor.getValue().getNorthboundMappings()).hasSize(2);
    }

    @Test
    void overwrite_multipleNorthbound_replacesSingle() throws DeviceTagImporterException {
        final ProtocolAdapterEntity adapter = adapterWithTags(
                List.of(new TagEntity("tag1", null, Map.of("node", "ns=2;i=1"))),
                List.of(new NorthboundMappingEntity("tag1", "old/topic", 1, null, false, true, List.of(), null)),
                List.of());
        setupAdapter(adapter);

        // Import 2 NB mappings for the same tag
        final List<DeviceTagRow> rows = List.of(
                tagRowWithMappings("tag1", "ns=2;i=1", "new/topic1", null),
                tagRowWithMappings("tag1", "ns=2;i=1", "new/topic2", null));

        final ImportResult result = importer.doImport(rows, ImportMode.OVERWRITE, ADAPTER_ID);

        assertThat(result.tagsUpdated()).isEqualTo(1);
        assertThat(result.northboundMappingsCreated()).isEqualTo(2);
        assertThat(result.northboundMappingsDeleted()).isEqualTo(1);

        final ArgumentCaptor<ProtocolAdapterEntity> captor = ArgumentCaptor.forClass(ProtocolAdapterEntity.class);
        verify(adapterExtractor).updateAdapter(captor.capture());
        assertThat(captor.getValue().getNorthboundMappings()).hasSize(2);
    }

    @Test
    void multiRow_identicalMappings_noOp() throws DeviceTagImporterException {
        final ProtocolAdapterEntity adapter = adapterWithTags(
                List.of(new TagEntity("tag1", null, Map.of("node", "ns=2;i=1"))),
                List.of(
                        new NorthboundMappingEntity("tag1", "topic/a", 1, null, false, true, List.of(), null),
                        new NorthboundMappingEntity("tag1", "topic/b", 1, null, false, true, List.of(), null)),
                List.of());
        setupAdapter(adapter);

        final List<DeviceTagRow> rows = List.of(
                tagRowWithMappings("tag1", "ns=2;i=1", "topic/a", null),
                tagRowWithMappings("tag1", "ns=2;i=1", "topic/b", null));

        final ImportResult result = importer.doImport(rows, ImportMode.OVERWRITE, ADAPTER_ID);

        assertThat(result.tagsUpdated()).isEqualTo(0);
        assertThat(result.tagsCreated()).isEqualTo(0);
    }

    // --- CREATE mode ---

    @Test
    void create_emptyEdge_fileWithTags() throws DeviceTagImporterException {
        setupAdapter(emptyAdapter());

        final List<DeviceTagRow> rows = List.of(
                tagRowWithMappings("tag1", "ns=2;i=1", "topic/1", "write/1"),
                tagRowWithMappings("tag2", "ns=2;i=2", "topic/2", null));

        final ImportResult result = importer.doImport(rows, ImportMode.CREATE, ADAPTER_ID);

        assertThat(result.tagsCreated()).isEqualTo(2);
        assertThat(result.tagsDeleted()).isEqualTo(0);
        assertThat(result.tagsUpdated()).isEqualTo(0);
        assertThat(result.northboundMappingsCreated()).isEqualTo(2);
        assertThat(result.southboundMappingsCreated()).isEqualTo(1);
        assertThat(result.tagActions()).hasSize(2);
        assertThat(result.tagActions()).allSatisfy(a -> assertThat(a.action()).isEqualTo(TagAction.Action.CREATED));
    }

    @Test
    void create_identicalExistingTag_noop() throws DeviceTagImporterException {
        final ProtocolAdapterEntity adapter = adapterWithTags(
                List.of(new TagEntity("existing", null, Map.of("node", "ns=2;i=1"))), List.of(), List.of());
        setupAdapter(adapter);

        final List<DeviceTagRow> rows = List.of(tagRow("existing", "ns=2;i=1"));

        final ImportResult result = importer.doImport(rows, ImportMode.CREATE, ADAPTER_ID);

        assertThat(result.tagsCreated()).isEqualTo(0);
        assertThat(result.tagsUpdated()).isEqualTo(0);
        assertThat(result.tagsDeleted()).isEqualTo(0);
    }

    @Test
    void create_edgeOnlyTags_fails() {
        final ProtocolAdapterEntity adapter = adapterWithTags(
                List.of(new TagEntity("edge-only", null, Map.of("node", "ns=2;i=99"))), List.of(), List.of());
        setupAdapter(adapter);

        final List<DeviceTagRow> rows = List.of(tagRow("file-only", "ns=2;i=1"));

        assertThatThrownBy(() -> importer.doImport(rows, ImportMode.CREATE, ADAPTER_ID))
                .isInstanceOf(DeviceTagImporterException.class);

        verify(adapterExtractor, never()).updateAdapter(any());
    }

    @Test
    void create_differentDefinition_fails() {
        final ProtocolAdapterEntity adapter = adapterWithTags(
                List.of(new TagEntity("shared", null, Map.of("node", "ns=2;i=99"))), List.of(), List.of());
        setupAdapter(adapter);

        final List<DeviceTagRow> rows = List.of(tagRow("shared", "ns=2;i=1"));

        assertThatThrownBy(() -> importer.doImport(rows, ImportMode.CREATE, ADAPTER_ID))
                .isInstanceOf(DeviceTagImporterException.class);

        verify(adapterExtractor, never()).updateAdapter(any());
    }

    // --- OVERWRITE mode ---

    @Test
    void overwrite_edgeOnly_deleted_fileOnly_created() throws DeviceTagImporterException {
        final ProtocolAdapterEntity adapter = adapterWithTags(
                List.of(new TagEntity("edge-only", null, Map.of("node", "ns=2;i=99"))),
                List.of(new NorthboundMappingEntity("edge-only", "old/topic", 1, null, false, true, List.of(), null)),
                List.of());
        setupAdapter(adapter);

        final List<DeviceTagRow> rows = List.of(tagRowWithMappings("file-only", "ns=2;i=1", "new/topic", null));

        final ImportResult result = importer.doImport(rows, ImportMode.OVERWRITE, ADAPTER_ID);

        assertThat(result.tagsCreated()).isEqualTo(1);
        assertThat(result.tagsDeleted()).isEqualTo(1);
        assertThat(result.northboundMappingsCreated()).isEqualTo(1);
        assertThat(result.northboundMappingsDeleted()).isEqualTo(1);
    }

    @Test
    void overwrite_inBoth_different_updated() throws DeviceTagImporterException {
        final ProtocolAdapterEntity adapter = adapterWithTags(
                List.of(new TagEntity("shared", "old desc", Map.of("node", "ns=2;i=1"))), List.of(), List.of());
        setupAdapter(adapter);

        final List<DeviceTagRow> rows = List.of(DeviceTagRow.builder()
                .tagName("shared")
                .nodeId("ns=2;i=1")
                .tagDescription("new desc")
                .build());

        final ImportResult result = importer.doImport(rows, ImportMode.OVERWRITE, ADAPTER_ID);

        assertThat(result.tagsUpdated()).isEqualTo(1);
        assertThat(result.tagActions()).anySatisfy(a -> {
            assertThat(a.name()).isEqualTo("shared");
            assertThat(a.action()).isEqualTo(TagAction.Action.UPDATED);
        });
    }

    @Test
    void overwrite_inBoth_identical_noOp() throws DeviceTagImporterException {
        final ProtocolAdapterEntity adapter =
                adapterWithTags(List.of(new TagEntity("same", null, Map.of("node", "ns=2;i=1"))), List.of(), List.of());
        setupAdapter(adapter);

        final List<DeviceTagRow> rows = List.of(tagRow("same", "ns=2;i=1"));

        final ImportResult result = importer.doImport(rows, ImportMode.OVERWRITE, ADAPTER_ID);

        assertThat(result.tagsCreated()).isEqualTo(0);
        assertThat(result.tagsUpdated()).isEqualTo(0);
        assertThat(result.tagsDeleted()).isEqualTo(0);
    }

    // --- Mapping field comparison (EDG-360) ---

    @Test
    void overwrite_includeTagNames_differs_updated() throws DeviceTagImporterException {
        final ProtocolAdapterEntity adapter = adapterWithTags(
                List.of(new TagEntity("tag1", null, Map.of("node", "ns=2;i=1"))),
                List.of(new NorthboundMappingEntity("tag1", "topic/1", 1, null, false, true, List.of(), null)),
                List.of());
        setupAdapter(adapter);

        final List<DeviceTagRow> rows = List.of(DeviceTagRow.builder()
                .tagName("tag1")
                .nodeId("ns=2;i=1")
                .northboundTopic("topic/1")
                .maxQos(1)
                .includeTagNames(true)
                .build());

        final ImportResult result = importer.doImport(rows, ImportMode.OVERWRITE, ADAPTER_ID);

        assertThat(result.tagsUpdated()).isEqualTo(1);
    }

    @Test
    void overwrite_includeTimestamp_differs_updated() throws DeviceTagImporterException {
        final ProtocolAdapterEntity adapter = adapterWithTags(
                List.of(new TagEntity("tag1", null, Map.of("node", "ns=2;i=1"))),
                List.of(new NorthboundMappingEntity("tag1", "topic/1", 1, null, false, true, List.of(), null)),
                List.of());
        setupAdapter(adapter);

        final List<DeviceTagRow> rows = List.of(DeviceTagRow.builder()
                .tagName("tag1")
                .nodeId("ns=2;i=1")
                .northboundTopic("topic/1")
                .maxQos(1)
                .includeTimestamp(false)
                .build());

        final ImportResult result = importer.doImport(rows, ImportMode.OVERWRITE, ADAPTER_ID);

        assertThat(result.tagsUpdated()).isEqualTo(1);
    }

    @Test
    void overwrite_messageExpiry_differs_updated() throws DeviceTagImporterException {
        final ProtocolAdapterEntity adapter = adapterWithTags(
                List.of(new TagEntity("tag1", null, Map.of("node", "ns=2;i=1"))),
                List.of(new NorthboundMappingEntity("tag1", "topic/1", 1, null, false, true, List.of(), 3600L)),
                List.of());
        setupAdapter(adapter);

        final List<DeviceTagRow> rows = List.of(DeviceTagRow.builder()
                .tagName("tag1")
                .nodeId("ns=2;i=1")
                .northboundTopic("topic/1")
                .maxQos(1)
                .messageExpiryInterval(7200L)
                .build());

        final ImportResult result = importer.doImport(rows, ImportMode.OVERWRITE, ADAPTER_ID);

        assertThat(result.tagsUpdated()).isEqualTo(1);
    }

    @Test
    void overwrite_userProperties_differ_updated() throws DeviceTagImporterException {
        final ProtocolAdapterEntity adapter = adapterWithTags(
                List.of(new TagEntity("tag1", null, Map.of("node", "ns=2;i=1"))),
                List.of(new NorthboundMappingEntity(
                        "tag1",
                        "topic/1",
                        1,
                        null,
                        false,
                        true,
                        List.of(new MqttUserPropertyEntity("key1", "val1")),
                        null)),
                List.of());
        setupAdapter(adapter);

        final List<DeviceTagRow> rows = List.of(DeviceTagRow.builder()
                .tagName("tag1")
                .nodeId("ns=2;i=1")
                .northboundTopic("topic/1")
                .maxQos(1)
                .mqttUserProperties(Map.of("key1", "val2"))
                .build());

        final ImportResult result = importer.doImport(rows, ImportMode.OVERWRITE, ADAPTER_ID);

        assertThat(result.tagsUpdated()).isEqualTo(1);
    }

    @Test
    void overwrite_allNorthboundFieldsMatch_noOp() throws DeviceTagImporterException {
        final ProtocolAdapterEntity adapter = adapterWithTags(
                List.of(new TagEntity("tag1", null, Map.of("node", "ns=2;i=1"))),
                List.of(new NorthboundMappingEntity(
                        "tag1", "topic/1", 1, null, true, false, List.of(new MqttUserPropertyEntity("k", "v")), 3600L)),
                List.of());
        setupAdapter(adapter);

        final List<DeviceTagRow> rows = List.of(DeviceTagRow.builder()
                .tagName("tag1")
                .nodeId("ns=2;i=1")
                .northboundTopic("topic/1")
                .maxQos(1)
                .includeTagNames(true)
                .includeTimestamp(false)
                .mqttUserProperties(Map.of("k", "v"))
                .messageExpiryInterval(3600L)
                .build());

        final ImportResult result = importer.doImport(rows, ImportMode.OVERWRITE, ADAPTER_ID);

        assertThat(result.tagsUpdated()).isEqualTo(0);
    }

    // --- MERGE_SAFE mode ---

    @Test
    void mergeSafe_edgeOnly_kept() throws DeviceTagImporterException {
        final ProtocolAdapterEntity adapter = adapterWithTags(
                List.of(new TagEntity("edge-only", null, Map.of("node", "ns=2;i=99"))), List.of(), List.of());
        setupAdapter(adapter);

        final List<DeviceTagRow> rows = List.of(tagRow("file-only", "ns=2;i=1"));

        final ImportResult result = importer.doImport(rows, ImportMode.MERGE_SAFE, ADAPTER_ID);

        assertThat(result.tagsCreated()).isEqualTo(1);
        assertThat(result.tagsDeleted()).isEqualTo(0);

        // Verify the final adapter has both tags
        final ArgumentCaptor<ProtocolAdapterEntity> captor = ArgumentCaptor.forClass(ProtocolAdapterEntity.class);
        verify(adapterExtractor).updateAdapter(captor.capture());
        assertThat(captor.getValue().getTags()).hasSize(2);
    }

    @Test
    void mergeSafe_conflict_differentDefinition_fails() {
        final ProtocolAdapterEntity adapter = adapterWithTags(
                List.of(new TagEntity("shared", null, Map.of("node", "ns=2;i=99"))), List.of(), List.of());
        setupAdapter(adapter);

        final List<DeviceTagRow> rows = List.of(tagRow("shared", "ns=2;i=1"));

        assertThatThrownBy(() -> importer.doImport(rows, ImportMode.MERGE_SAFE, ADAPTER_ID))
                .isInstanceOf(DeviceTagImporterException.class);

        verify(adapterExtractor, never()).updateAdapter(any());
    }

    // --- MERGE_OVERWRITE mode ---

    @Test
    void mergeOverwrite_edgeOnly_kept_different_overwritten() throws DeviceTagImporterException {
        final ProtocolAdapterEntity adapter = adapterWithTags(
                List.of(
                        new TagEntity("edge-only", null, Map.of("node", "ns=2;i=99")),
                        new TagEntity("shared", "old", Map.of("node", "ns=2;i=50"))),
                List.of(),
                List.of());
        setupAdapter(adapter);

        final List<DeviceTagRow> rows = List.of(
                tagRow("file-only", "ns=2;i=1"),
                DeviceTagRow.builder()
                        .tagName("shared")
                        .nodeId("ns=2;i=50")
                        .tagDescription("new")
                        .build());

        final ImportResult result = importer.doImport(rows, ImportMode.MERGE_OVERWRITE, ADAPTER_ID);

        assertThat(result.tagsCreated()).isEqualTo(1);
        assertThat(result.tagsUpdated()).isEqualTo(1);
        assertThat(result.tagsDeleted()).isEqualTo(0);

        final ArgumentCaptor<ProtocolAdapterEntity> captor = ArgumentCaptor.forClass(ProtocolAdapterEntity.class);
        verify(adapterExtractor).updateAdapter(captor.capture());
        assertThat(captor.getValue().getTags()).hasSize(3);
    }

    // --- DELETE mode ---

    @Test
    void delete_edgeOnly_deleted() throws DeviceTagImporterException {
        final ProtocolAdapterEntity adapter = adapterWithTags(
                List.of(
                        new TagEntity("to-delete", null, Map.of("node", "ns=2;i=99")),
                        new TagEntity("keep", null, Map.of("node", "ns=2;i=1"))),
                List.of(),
                List.of());
        setupAdapter(adapter);

        // File contains only 'keep' — 'to-delete' should be deleted
        final List<DeviceTagRow> rows = List.of(tagRow("keep", "ns=2;i=1"));

        final ImportResult result = importer.doImport(rows, ImportMode.DELETE, ADAPTER_ID);

        assertThat(result.tagsDeleted()).isEqualTo(1);
        assertThat(result.tagsCreated()).isEqualTo(0);
        assertThat(result.tagActions()).anySatisfy(a -> {
            assertThat(a.name()).isEqualTo("to-delete");
            assertThat(a.action()).isEqualTo(TagAction.Action.DELETED);
        });
    }

    @Test
    void delete_fileOnly_fails() {
        final ProtocolAdapterEntity adapter = adapterWithTags(
                List.of(new TagEntity("edge-tag", null, Map.of("node", "ns=2;i=99"))), List.of(), List.of());
        setupAdapter(adapter);

        final List<DeviceTagRow> rows = List.of(tagRow("not-on-edge", "ns=2;i=1"));

        assertThatThrownBy(() -> importer.doImport(rows, ImportMode.DELETE, ADAPTER_ID))
                .isInstanceOf(DeviceTagImporterException.class);

        verify(adapterExtractor, never()).updateAdapter(any());
    }

    @Test
    void delete_bothDifferent_fails() {
        final ProtocolAdapterEntity adapter = adapterWithTags(
                List.of(new TagEntity("shared", null, Map.of("node", "ns=2;i=99"))), List.of(), List.of());
        setupAdapter(adapter);

        final List<DeviceTagRow> rows = List.of(tagRow("shared", "ns=2;i=1"));

        assertThatThrownBy(() -> importer.doImport(rows, ImportMode.DELETE, ADAPTER_ID))
                .isInstanceOf(DeviceTagImporterException.class);

        verify(adapterExtractor, never()).updateAdapter(any());
    }

    // --- Rename via nodeId correlation ---

    @Test
    void rename_mergeOverwrite_sameNodeId_differentTagName() throws DeviceTagImporterException {
        final ProtocolAdapterEntity adapter = adapterWithTags(
                List.of(new TagEntity("old-name", "desc", Map.of("node", "ns=2;i=1"))),
                List.of(new NorthboundMappingEntity("old-name", "topic/1", 1, null, false, true, List.of(), null)),
                List.of());
        setupAdapter(adapter);

        final List<DeviceTagRow> rows = List.of(DeviceTagRow.builder()
                .tagName("new-name")
                .nodeId("ns=2;i=1")
                .tagDescription("desc")
                .northboundTopic("topic/1")
                .build());

        final ImportResult result = importer.doImport(rows, ImportMode.MERGE_OVERWRITE, ADAPTER_ID);

        assertThat(result.tagsUpdated()).isEqualTo(1);
        assertThat(result.tagsCreated()).isEqualTo(0);
        assertThat(result.tagsDeleted()).isEqualTo(0);
        assertThat(result.tagActions()).anySatisfy(a -> {
            assertThat(a.name()).isEqualTo("new-name");
            assertThat(a.action()).isEqualTo(TagAction.Action.UPDATED);
        });

        final ArgumentCaptor<ProtocolAdapterEntity> captor = ArgumentCaptor.forClass(ProtocolAdapterEntity.class);
        verify(adapterExtractor).updateAdapter(captor.capture());
        final ProtocolAdapterEntity updated = captor.getValue();
        assertThat(updated.getTags()).hasSize(1);
        assertThat(updated.getTags().getFirst().getName()).isEqualTo("new-name");
        assertThat(updated.getNorthboundMappings().getFirst().getTagName()).isEqualTo("new-name");
    }

    @Test
    void rename_mergeSafe_sameNodeId_differentTagName_fails() {
        final ProtocolAdapterEntity adapter = adapterWithTags(
                List.of(new TagEntity("old-name", null, Map.of("node", "ns=2;i=1"))), List.of(), List.of());
        setupAdapter(adapter);

        final List<DeviceTagRow> rows = List.of(tagRow("new-name", "ns=2;i=1"));

        assertThatThrownBy(() -> importer.doImport(rows, ImportMode.MERGE_SAFE, ADAPTER_ID))
                .isInstanceOf(DeviceTagImporterException.class);

        verify(adapterExtractor, never()).updateAdapter(any());
    }

    @Test
    void rename_overwrite_sameNodeId_differentTagName() throws DeviceTagImporterException {
        final ProtocolAdapterEntity adapter = adapterWithTags(
                List.of(new TagEntity("old-name", null, Map.of("node", "ns=2;i=1"))), List.of(), List.of());
        setupAdapter(adapter);

        final List<DeviceTagRow> rows = List.of(tagRow("new-name", "ns=2;i=1"));

        final ImportResult result = importer.doImport(rows, ImportMode.OVERWRITE, ADAPTER_ID);

        assertThat(result.tagsUpdated()).isEqualTo(1);
        assertThat(result.tagsCreated()).isEqualTo(0);
        assertThat(result.tagsDeleted()).isEqualTo(0);
    }

    // --- Wildcard resolution ---

    @Test
    void wildcardResolution_tagName() throws DeviceTagImporterException {
        setupAdapter(emptyAdapter());

        final List<DeviceTagRow> rows = List.of(DeviceTagRow.builder()
                .nodeId("ns=2;i=1")
                .tagName("*")
                .tagNameDefault("auto-tag")
                .build());

        final ImportResult result = importer.doImport(rows, ImportMode.CREATE, ADAPTER_ID);

        assertThat(result.tagsCreated()).isEqualTo(1);
        assertThat(result.tagActions()).anySatisfy(a -> assertThat(a.name()).isEqualTo("auto-tag"));
    }

    @Test
    void wildcardResolution_northboundTopic() throws DeviceTagImporterException {
        setupAdapter(emptyAdapter());

        final List<DeviceTagRow> rows = List.of(DeviceTagRow.builder()
                .nodeId("ns=2;i=1")
                .tagName("tag1")
                .northboundTopic("*")
                .northboundTopicDefault("auto/topic")
                .build());

        final ImportResult result = importer.doImport(rows, ImportMode.CREATE, ADAPTER_ID);

        assertThat(result.northboundMappingsCreated()).isEqualTo(1);
    }

    @Test
    void wildcardResolution_noDefault_producesError() {
        setupAdapter(emptyAdapter());

        final List<DeviceTagRow> rows =
                List.of(DeviceTagRow.builder().nodeId("ns=2;i=1").tagName("*").build());

        assertThatThrownBy(() -> importer.doImport(rows, ImportMode.CREATE, ADAPTER_ID))
                .isInstanceOf(DeviceTagImporterException.class)
                .satisfies(ex -> {
                    final var importEx = (DeviceTagImporterException) ex;
                    assertThat(importEx.getErrors())
                            .anySatisfy(e -> assertThat(e.code()).isEqualTo(ValidationError.Code.WILDCARD_NO_DEFAULT));
                });
    }

    // --- No mutations on validation failure ---

    @Test
    void validationFailure_noMutations() {
        setupAdapter(emptyAdapter());

        final List<DeviceTagRow> rows = List.of(
                DeviceTagRow.builder().tagName("tag with spaces").maxQos(5).build());

        assertThatThrownBy(() -> importer.doImport(rows, ImportMode.CREATE, ADAPTER_ID))
                .isInstanceOf(DeviceTagImporterException.class);

        verify(adapterExtractor, never()).updateAdapter(any());
    }

    // --- Entity construction ---

    @Test
    void entityConstruction_northbound_defaults() throws DeviceTagImporterException {
        setupAdapter(emptyAdapter());

        final List<DeviceTagRow> rows = List.of(DeviceTagRow.builder()
                .nodeId("ns=2;i=1")
                .tagName("tag1")
                .northboundTopic("topic/1")
                .build());

        importer.doImport(rows, ImportMode.CREATE, ADAPTER_ID);

        final ArgumentCaptor<ProtocolAdapterEntity> captor = ArgumentCaptor.forClass(ProtocolAdapterEntity.class);
        verify(adapterExtractor).updateAdapter(captor.capture());

        final NorthboundMappingEntity nb =
                captor.getValue().getNorthboundMappings().getFirst();
        assertThat(nb.getTagName()).isEqualTo("tag1");
        assertThat(nb.getTopic()).isEqualTo("topic/1");
        assertThat(nb.getMaxQoS()).isEqualTo(1); // default
        assertThat(nb.isIncludeTimestamp()).isTrue(); // default
        assertThat(nb.isIncludeTagNames()).isFalse(); // default
    }

    @Test
    void entityConstruction_southbound_defaultFieldMapping() throws DeviceTagImporterException {
        setupAdapter(emptyAdapter());

        final List<DeviceTagRow> rows = List.of(DeviceTagRow.builder()
                .nodeId("ns=2;i=1")
                .tagName("tag1")
                .southboundTopic("write/1")
                .build());

        importer.doImport(rows, ImportMode.CREATE, ADAPTER_ID);

        final ArgumentCaptor<ProtocolAdapterEntity> captor = ArgumentCaptor.forClass(ProtocolAdapterEntity.class);
        verify(adapterExtractor).updateAdapter(captor.capture());

        final SouthboundMappingEntity sb =
                captor.getValue().getSouthboundMappings().getFirst();
        assertThat(sb.getTagName()).isEqualTo("tag1");
        assertThat(sb.getTopicFilter()).isEqualTo("write/1");
        Assertions.assertNotNull(sb.getFieldMapping());
        assertThat(sb.getFieldMapping().getInstructions()).hasSize(1);
        assertThat(sb.getFieldMapping().getInstructions().getFirst().getSourceFieldName())
                .isEqualTo("value");
        assertThat(sb.getFieldMapping().getInstructions().getFirst().getDestinationFieldName())
                .isEqualTo("value");
    }

    @Test
    void entityConstruction_southbound_customFieldMapping() throws DeviceTagImporterException {
        setupAdapter(emptyAdapter());

        final List<DeviceTagRow> rows = List.of(DeviceTagRow.builder()
                .nodeId("ns=2;i=1")
                .tagName("tag1")
                .southboundTopic("write/1")
                .southboundFieldMapping(List.of(
                        new FieldMappingInstruction("src1", "dst1"), new FieldMappingInstruction("src2", "dst2")))
                .build());

        importer.doImport(rows, ImportMode.CREATE, ADAPTER_ID);

        final ArgumentCaptor<ProtocolAdapterEntity> captor = ArgumentCaptor.forClass(ProtocolAdapterEntity.class);
        verify(adapterExtractor).updateAdapter(captor.capture());

        final SouthboundMappingEntity sb =
                captor.getValue().getSouthboundMappings().getFirst();
        Assertions.assertNotNull(sb.getFieldMapping());
        assertThat(sb.getFieldMapping().getInstructions()).hasSize(2);
    }

    // --- Large payload ---

    @Test
    void largeImport_5000Tags() throws DeviceTagImporterException {
        setupAdapter(emptyAdapter());

        final List<DeviceTagRow> rows = new ArrayList<>();
        for (int i = 0; i < 5000; i++) {
            rows.add(tagRowWithMappings("tag-" + i, "ns=2;i=" + (1000 + i), "topic/" + i, null));
        }

        final ImportResult result = importer.doImport(rows, ImportMode.CREATE, ADAPTER_ID);

        assertThat(result.tagsCreated()).isEqualTo(5000);
        assertThat(result.northboundMappingsCreated()).isEqualTo(5000);
        assertThat(result.tagActions()).hasSize(5000);
    }

    // --- Concurrency ---

    @Test
    void concurrentImports_serialized() throws Exception {
        final int threadCount = 5;
        final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch doneLatch = new CountDownLatch(threadCount);
        final AtomicInteger successCount = new AtomicInteger(0);
        final AtomicInteger errorCount = new AtomicInteger(0);

        for (int t = 0; t < threadCount; t++) {
            final int threadIdx = t;
            executor.submit(() -> {
                try {
                    startLatch.await();

                    // Each thread works with its own adapter state to avoid mock conflicts
                    final ProtocolAdapterExtractor localExtractor = mock(ProtocolAdapterExtractor.class);
                    final DataCombiningExtractor localCombining = mock(DataCombiningExtractor.class);
                    when(localCombining.getAllCombiners()).thenReturn(List.of());
                    when(localExtractor.getAllConfigs()).thenReturn(List.of());

                    final String adapterId = "adapter-" + threadIdx;
                    final ProtocolAdapterEntity adapter =
                            new ProtocolAdapterEntity(adapterId, "opcua", 1, Map.of(), List.of(), List.of(), List.of());
                    when(localExtractor.getAdapterByAdapterId(adapterId)).thenReturn(Optional.of(adapter));
                    when(localExtractor.getAllConfigs()).thenReturn(List.of(adapter));
                    when(localExtractor.updateAdapter(any())).thenReturn(true);

                    final DeviceTagValidator localValidator = new DeviceTagValidator(localExtractor, localCombining);
                    final DeviceTagImporter localImporter = new DeviceTagImporter(localValidator, localExtractor);

                    final List<DeviceTagRow> rows = new ArrayList<>();
                    for (int i = 0; i < 20; i++) {
                        rows.add(tagRow("t" + threadIdx + "-tag" + i, "ns=2;i=" + (threadIdx * 100 + i)));
                    }

                    final ImportResult result = localImporter.doImport(rows, ImportMode.CREATE, adapterId);
                    if (result.tagsCreated() == 20) {
                        successCount.incrementAndGet();
                    } else {
                        errorCount.incrementAndGet();
                    }
                } catch (final Exception e) {
                    errorCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();

        assertThat(successCount.get()).isEqualTo(threadCount);
        assertThat(errorCount.get()).isEqualTo(0);
    }

    // --- ImportResult counts ---

    @Test
    void importResult_countsMatchActions() throws DeviceTagImporterException {
        final ProtocolAdapterEntity adapter = adapterWithTags(
                List.of(new TagEntity("delete-me", null, Map.of("node", "ns=2;i=99"))),
                List.of(new NorthboundMappingEntity("delete-me", "old/topic", 1, null, false, true, List.of(), null)),
                List.of());
        setupAdapter(adapter);

        final List<DeviceTagRow> rows = List.of(tagRowWithMappings("new-tag", "ns=2;i=1", "new/topic", "write/1"));

        final ImportResult result = importer.doImport(rows, ImportMode.OVERWRITE, ADAPTER_ID);

        final long createdActions = result.tagActions().stream()
                .filter(a -> a.action() == TagAction.Action.CREATED)
                .count();
        final long deletedActions = result.tagActions().stream()
                .filter(a -> a.action() == TagAction.Action.DELETED)
                .count();

        assertThat(createdActions).isEqualTo(result.tagsCreated());
        assertThat(deletedActions).isEqualTo(result.tagsDeleted());
    }

    // --- Update failure ---

    @Test
    void updateFailed_throwsException() {
        setupAdapter(emptyAdapter());
        when(adapterExtractor.updateAdapter(any())).thenReturn(false);

        final List<DeviceTagRow> rows = List.of(tagRow("tag1", "ns=2;i=1"));

        assertThatThrownBy(() -> importer.doImport(rows, ImportMode.CREATE, ADAPTER_ID))
                .isInstanceOf(DeviceTagImporterException.class)
                .satisfies(ex -> {
                    final var importEx = (DeviceTagImporterException) ex;
                    assertThat(importEx.getErrors())
                            .anySatisfy(e -> assertThat(e.code()).isEqualTo(ValidationError.Code.UPDATE_FAILED));
                });
    }
}
