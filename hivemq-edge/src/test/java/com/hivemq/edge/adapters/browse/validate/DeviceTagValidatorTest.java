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
package com.hivemq.edge.adapters.browse.validate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.hivemq.configuration.entity.adapter.ProtocolAdapterEntity;
import com.hivemq.configuration.entity.adapter.TagEntity;
import com.hivemq.configuration.reader.DataCombiningExtractor;
import com.hivemq.configuration.reader.ProtocolAdapterExtractor;
import com.hivemq.edge.adapters.browse.model.DeviceTagRow;
import com.hivemq.edge.adapters.browse.model.FieldMappingInstruction;
import com.hivemq.edge.adapters.browse.model.ImportMode;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DeviceTagValidatorTest {

    private final ProtocolAdapterExtractor adapterExtractor = mock(ProtocolAdapterExtractor.class);
    private final DataCombiningExtractor combiningExtractor = mock(DataCombiningExtractor.class);
    private DeviceTagValidator validator;

    @BeforeEach
    void setUp() {
        validator = new DeviceTagValidator(adapterExtractor, combiningExtractor);
        when(combiningExtractor.getAllCombiners()).thenReturn(List.of());
        when(adapterExtractor.getAllConfigs()).thenReturn(List.of());
    }

    // --- Duplicate validations ---

    @Test
    void validate_duplicateNodeId() {
        final List<DeviceTagRow> rows = List.of(
                DeviceTagRow.builder().nodeId("ns=2;i=1").tagName("tag1").build(),
                DeviceTagRow.builder().nodeId("ns=2;i=1").tagName("tag2").build());

        final List<ValidationError> errors = validator.validate(rows, ImportMode.CREATE, "adapter1");

        assertThat(errors).anySatisfy(e -> {
            assertThat(e.code()).isEqualTo(ValidationError.Code.DUPLICATE_NODE);
            assertThat(e.value()).isEqualTo("ns=2;i=1");
        });
    }

    @Test
    void validate_noDuplicateNodeId() {
        final List<DeviceTagRow> rows = List.of(
                DeviceTagRow.builder().nodeId("ns=2;i=1").tagName("tag1").build(),
                DeviceTagRow.builder().nodeId("ns=2;i=2").tagName("tag2").build());

        final List<ValidationError> errors = validator.validate(rows, ImportMode.CREATE, "adapter1");

        assertThat(errors).noneMatch(e -> e.code().equals("DUPLICATE_NODE"));
    }

    @Test
    void validate_duplicateTagName() {
        final List<DeviceTagRow> rows = List.of(
                DeviceTagRow.builder().nodeId("ns=2;i=1").tagName("same-tag").build(),
                DeviceTagRow.builder().nodeId("ns=2;i=2").tagName("same-tag").build());

        final List<ValidationError> errors = validator.validate(rows, ImportMode.CREATE, "adapter1");

        assertThat(errors).anySatisfy(e -> {
            assertThat(e.code()).isEqualTo(ValidationError.Code.DUPLICATE_TAG_NAME);
            assertThat(e.value()).isEqualTo("same-tag");
        });
    }

    @Test
    void validate_duplicateTagName_sameDefinition_allowed() {
        // Multiple rows with same tagName, same nodeId, same description → multi-mapping, allowed
        final List<DeviceTagRow> rows = List.of(
                DeviceTagRow.builder()
                        .nodeId("ns=2;i=1")
                        .tagName("same-tag")
                        .northboundTopic("topic/a")
                        .build(),
                DeviceTagRow.builder()
                        .nodeId("ns=2;i=1")
                        .tagName("same-tag")
                        .northboundTopic("topic/b")
                        .build());

        final List<ValidationError> errors = validator.validate(rows, ImportMode.CREATE, "adapter1");

        assertThat(errors).noneMatch(e -> e.code() == ValidationError.Code.DUPLICATE_TAG_NAME);
        assertThat(errors).noneMatch(e -> e.code() == ValidationError.Code.DUPLICATE_NODE);
    }

    @Test
    void validate_duplicateTagName_differentDescription_rejected() {
        final List<DeviceTagRow> rows = List.of(
                DeviceTagRow.builder()
                        .nodeId("ns=2;i=1")
                        .tagName("same-tag")
                        .tagDescription("desc-a")
                        .build(),
                DeviceTagRow.builder()
                        .nodeId("ns=2;i=1")
                        .tagName("same-tag")
                        .tagDescription("desc-b")
                        .build());

        final List<ValidationError> errors = validator.validate(rows, ImportMode.CREATE, "adapter1");

        assertThat(errors).anySatisfy(e -> assertThat(e.code()).isEqualTo(ValidationError.Code.DUPLICATE_TAG_NAME));
    }

    // --- Tag name validation ---

    @Test
    void validate_validTagName() {
        final List<DeviceTagRow> rows = List.of(DeviceTagRow.builder()
                .nodeId("ns=2;i=1")
                .tagName("my-tag.name_1")
                .build());

        final List<ValidationError> errors = validator.validate(rows, ImportMode.CREATE, "adapter1");

        assertThat(errors).noneMatch(e -> e.code().equals("INVALID_TAG_NAME"));
    }

    @Test
    void validate_tagName_startsWithDash_nowValid() {
        final List<DeviceTagRow> rows = List.of(
                DeviceTagRow.builder().nodeId("ns=2;i=1").tagName("-dashed").build());

        final List<ValidationError> errors = validator.validate(rows, ImportMode.CREATE, "adapter1");

        assertThat(errors).noneMatch(e -> e.code() == ValidationError.Code.INVALID_TAG_NAME);
    }

    @Test
    void validate_tagName_specialChars_nowValid() {
        final List<DeviceTagRow> rows = List.of(DeviceTagRow.builder()
                .nodeId("ns=2;i=1")
                .tagName("tag@special#chars!")
                .build());

        final List<ValidationError> errors = validator.validate(rows, ImportMode.CREATE, "adapter1");

        assertThat(errors).noneMatch(e -> e.code() == ValidationError.Code.INVALID_TAG_NAME);
    }

    @Test
    void validate_invalidTagName_withWhitespace() {
        final List<DeviceTagRow> rows = List.of(DeviceTagRow.builder()
                .nodeId("ns=2;i=1")
                .tagName("tag with spaces")
                .build());

        final List<ValidationError> errors = validator.validate(rows, ImportMode.CREATE, "adapter1");

        assertThat(errors).anySatisfy(e -> assertThat(e.code()).isEqualTo(ValidationError.Code.INVALID_TAG_NAME));
    }

    @Test
    void validate_tagNameTooLong() {
        final String longName = "a".repeat(257);
        final List<DeviceTagRow> rows = List.of(
                DeviceTagRow.builder().nodeId("ns=2;i=1").tagName(longName).build());

        final List<ValidationError> errors = validator.validate(rows, ImportMode.CREATE, "adapter1");

        assertThat(errors).anySatisfy(e -> {
            assertThat(e.code()).isEqualTo(ValidationError.Code.INVALID_TAG_NAME);
            assertThat(e.message()).contains("maximum length");
        });
    }

    // --- QoS validation ---

    @Test
    void validate_validQos() {
        final List<DeviceTagRow> rows = List.of(
                DeviceTagRow.builder()
                        .nodeId("ns=2;i=1")
                        .tagName("tag1")
                        .maxQos(0)
                        .build(),
                DeviceTagRow.builder()
                        .nodeId("ns=2;i=2")
                        .tagName("tag2")
                        .maxQos(1)
                        .build(),
                DeviceTagRow.builder()
                        .nodeId("ns=2;i=3")
                        .tagName("tag3")
                        .maxQos(2)
                        .build());

        final List<ValidationError> errors = validator.validate(rows, ImportMode.CREATE, "adapter1");

        assertThat(errors).noneMatch(e -> e.code().equals("INVALID_QOS"));
    }

    @Test
    void validate_invalidQos_tooHigh() {
        final List<DeviceTagRow> rows = List.of(DeviceTagRow.builder()
                .nodeId("ns=2;i=1")
                .tagName("tag1")
                .maxQos(3)
                .build());

        final List<ValidationError> errors = validator.validate(rows, ImportMode.CREATE, "adapter1");

        assertThat(errors).anySatisfy(e -> assertThat(e.code()).isEqualTo(ValidationError.Code.INVALID_QOS));
    }

    @Test
    void validate_invalidQos_negative() {
        final List<DeviceTagRow> rows = List.of(DeviceTagRow.builder()
                .nodeId("ns=2;i=1")
                .tagName("tag1")
                .maxQos(-1)
                .build());

        final List<ValidationError> errors = validator.validate(rows, ImportMode.CREATE, "adapter1");

        assertThat(errors).anySatisfy(e -> assertThat(e.code()).isEqualTo(ValidationError.Code.INVALID_QOS));
    }

    // --- Topic validation ---

    @Test
    void validate_northboundTopic_noWildcards() {
        final List<DeviceTagRow> rows = List.of(DeviceTagRow.builder()
                .nodeId("ns=2;i=1")
                .tagName("tag1")
                .northboundTopic("adapter/data/int32")
                .build());

        final List<ValidationError> errors = validator.validate(rows, ImportMode.CREATE, "adapter1");

        assertThat(errors).noneMatch(e -> e.code().equals("INVALID_TOPIC"));
    }

    @Test
    void validate_northboundTopic_withPlusWildcard() {
        final List<DeviceTagRow> rows = List.of(DeviceTagRow.builder()
                .nodeId("ns=2;i=1")
                .tagName("tag1")
                .northboundTopic("adapter/+/int32")
                .build());

        final List<ValidationError> errors = validator.validate(rows, ImportMode.CREATE, "adapter1");

        assertThat(errors).anySatisfy(e -> {
            assertThat(e.code()).isEqualTo(ValidationError.Code.INVALID_TOPIC);
            assertThat(e.column()).isEqualTo("northbound_topic");
        });
    }

    @Test
    void validate_northboundTopic_withHashWildcard() {
        final List<DeviceTagRow> rows = List.of(DeviceTagRow.builder()
                .nodeId("ns=2;i=1")
                .tagName("tag1")
                .northboundTopic("adapter/#")
                .build());

        final List<ValidationError> errors = validator.validate(rows, ImportMode.CREATE, "adapter1");

        assertThat(errors).anySatisfy(e -> assertThat(e.code()).isEqualTo(ValidationError.Code.INVALID_TOPIC));
    }

    @Test
    void validate_southboundTopic_wildcardAllowed() {
        final List<DeviceTagRow> rows = List.of(DeviceTagRow.builder()
                .nodeId("ns=2;i=1")
                .tagName("tag1")
                .southboundTopic("adapter/+/write/#")
                .build());

        final List<ValidationError> errors = validator.validate(rows, ImportMode.CREATE, "adapter1");

        assertThat(errors)
                .noneMatch(e -> e.code().equals("INVALID_TOPIC") && e.column().equals("southbound_topic"));
    }

    // --- Expiry validation ---

    @Test
    void validate_validExpiryInterval() {
        final List<DeviceTagRow> rows = List.of(DeviceTagRow.builder()
                .nodeId("ns=2;i=1")
                .tagName("tag1")
                .messageExpiryInterval(3600L)
                .build());

        final List<ValidationError> errors = validator.validate(rows, ImportMode.CREATE, "adapter1");

        assertThat(errors).noneMatch(e -> e.code().equals("INVALID_EXPIRY"));
    }

    @Test
    void validate_invalidExpiryInterval_negative() {
        final List<DeviceTagRow> rows = List.of(DeviceTagRow.builder()
                .nodeId("ns=2;i=1")
                .tagName("tag1")
                .messageExpiryInterval(-1L)
                .build());

        final List<ValidationError> errors = validator.validate(rows, ImportMode.CREATE, "adapter1");

        assertThat(errors).anySatisfy(e -> assertThat(e.code()).isEqualTo(ValidationError.Code.INVALID_EXPIRY));
    }

    @Test
    void validate_invalidExpiryInterval_zero() {
        final List<DeviceTagRow> rows = List.of(DeviceTagRow.builder()
                .nodeId("ns=2;i=1")
                .tagName("tag1")
                .messageExpiryInterval(0L)
                .build());

        final List<ValidationError> errors = validator.validate(rows, ImportMode.CREATE, "adapter1");

        assertThat(errors).anySatisfy(e -> assertThat(e.code()).isEqualTo(ValidationError.Code.INVALID_EXPIRY));
    }

    // --- Field mapping validation ---

    @Test
    void validate_validFieldMapping() {
        final List<DeviceTagRow> rows = List.of(DeviceTagRow.builder()
                .nodeId("ns=2;i=1")
                .tagName("tag1")
                .southboundFieldMapping(List.of(new FieldMappingInstruction("source", "destination")))
                .build());

        final List<ValidationError> errors = validator.validate(rows, ImportMode.CREATE, "adapter1");

        assertThat(errors).noneMatch(e -> e.code().equals("INVALID_FIELD_MAPPING"));
    }

    @Test
    void validate_fieldMapping_emptySource() {
        final List<DeviceTagRow> rows = List.of(DeviceTagRow.builder()
                .nodeId("ns=2;i=1")
                .tagName("tag1")
                .southboundFieldMapping(List.of(new FieldMappingInstruction("", "destination")))
                .build());

        final List<ValidationError> errors = validator.validate(rows, ImportMode.CREATE, "adapter1");

        assertThat(errors).anySatisfy(e -> {
            assertThat(e.code()).isEqualTo(ValidationError.Code.INVALID_FIELD_MAPPING);
            assertThat(e.message()).contains("source");
        });
    }

    @Test
    void validate_fieldMapping_emptyDestination() {
        final List<DeviceTagRow> rows = List.of(DeviceTagRow.builder()
                .nodeId("ns=2;i=1")
                .tagName("tag1")
                .southboundFieldMapping(List.of(new FieldMappingInstruction("source", "")))
                .build());

        final List<ValidationError> errors = validator.validate(rows, ImportMode.CREATE, "adapter1");

        assertThat(errors).anySatisfy(e -> {
            assertThat(e.code()).isEqualTo(ValidationError.Code.INVALID_FIELD_MAPPING);
            assertThat(e.message()).contains("destination");
        });
    }

    // --- User properties validation ---

    @Test
    void validate_userProperties_emptyKey() {
        final List<DeviceTagRow> rows = List.of(DeviceTagRow.builder()
                .nodeId("ns=2;i=1")
                .tagName("tag1")
                .mqttUserProperties(Map.of("", "value"))
                .build());

        final List<ValidationError> errors = validator.validate(rows, ImportMode.CREATE, "adapter1");

        assertThat(errors)
                .anySatisfy(e -> assertThat(e.code()).isEqualTo(ValidationError.Code.INVALID_USER_PROPERTIES));
    }

    // --- Mapping without tag ---

    @Test
    void validate_northboundMappingWithoutTag() {
        final List<DeviceTagRow> rows = List.of(DeviceTagRow.builder()
                .nodeId("ns=2;i=1")
                .northboundTopic("adapter/data/int32")
                .build());

        final List<ValidationError> errors = validator.validate(rows, ImportMode.CREATE, "adapter1");

        assertThat(errors).anySatisfy(e -> {
            assertThat(e.code()).isEqualTo(ValidationError.Code.MAPPING_WITHOUT_TAG);
            assertThat(e.column()).isEqualTo("northbound_topic");
        });
    }

    @Test
    void validate_southboundMappingWithoutTag() {
        final List<DeviceTagRow> rows = List.of(DeviceTagRow.builder()
                .nodeId("ns=2;i=1")
                .southboundTopic("adapter/write/int32")
                .build());

        final List<ValidationError> errors = validator.validate(rows, ImportMode.CREATE, "adapter1");

        assertThat(errors).anySatisfy(e -> {
            assertThat(e.code()).isEqualTo(ValidationError.Code.MAPPING_WITHOUT_TAG);
            assertThat(e.column()).isEqualTo("southbound_topic");
        });
    }

    // --- Node ID required ---

    @Test
    void validate_missingNodeId_withTag() {
        final List<DeviceTagRow> rows =
                List.of(DeviceTagRow.builder().tagName("tag1").build());

        final List<ValidationError> errors = validator.validate(rows, ImportMode.CREATE, "adapter1");

        assertThat(errors).anySatisfy(e -> assertThat(e.code()).isEqualTo(ValidationError.Code.INVALID_NODE_ID));
    }

    @Test
    void validate_missingNodeId_withoutTag_noError() {
        final List<DeviceTagRow> rows =
                List.of(DeviceTagRow.builder().nodePath("/Objects/Data").build());

        final List<ValidationError> errors = validator.validate(rows, ImportMode.CREATE, "adapter1");

        assertThat(errors).noneMatch(e -> e.code().equals("INVALID_NODE_ID"));
    }

    // --- Multiple errors collected ---

    @Test
    void validate_multipleErrors_allCollected() {
        final List<DeviceTagRow> rows = List.of(DeviceTagRow.builder()
                .nodeId("ns=2;i=1")
                .tagName("tag with spaces")
                .maxQos(5)
                .messageExpiryInterval(-1L)
                .northboundTopic("topic/+/wildcard")
                .build());

        final List<ValidationError> errors = validator.validate(rows, ImportMode.CREATE, "adapter1");

        assertThat(errors.size()).isGreaterThanOrEqualTo(3);
        assertThat(errors).anySatisfy(e -> assertThat(e.code()).isEqualTo(ValidationError.Code.INVALID_TAG_NAME));
        assertThat(errors).anySatisfy(e -> assertThat(e.code()).isEqualTo(ValidationError.Code.INVALID_QOS));
        assertThat(errors).anySatisfy(e -> assertThat(e.code()).isEqualTo(ValidationError.Code.INVALID_EXPIRY));
    }

    // --- Cross-reference validations ---

    @Test
    void validate_createMode_edgeOnly_fails() {
        final ProtocolAdapterEntity adapter = new ProtocolAdapterEntity(
                "adapter1",
                "opcua",
                1,
                Map.of(),
                List.of(),
                List.of(),
                List.of(new TagEntity("edge-only-tag", "desc", Map.of("node", "ns=2;i=99"))));
        when(adapterExtractor.getAdapterByAdapterId("adapter1")).thenReturn(Optional.of(adapter));
        when(adapterExtractor.getAllConfigs()).thenReturn(List.of(adapter));

        final List<DeviceTagRow> rows = List.of(DeviceTagRow.builder()
                .nodeId("ns=2;i=1")
                .tagName("file-only-tag")
                .build());

        final List<ValidationError> errors = validator.validate(rows, ImportMode.CREATE, "adapter1");

        assertThat(errors).anySatisfy(e -> {
            assertThat(e.code()).isEqualTo(ValidationError.Code.TAG_CONFLICT);
            assertThat(e.value()).isEqualTo("edge-only-tag");
        });
    }

    @Test
    void validate_createMode_identicalTag_noError() {
        final ProtocolAdapterEntity adapter = new ProtocolAdapterEntity(
                "adapter1",
                "opcua",
                1,
                Map.of(),
                List.of(),
                List.of(),
                List.of(new TagEntity("existing-tag", null, Map.of("node", "ns=2;i=1"))));
        when(adapterExtractor.getAdapterByAdapterId("adapter1")).thenReturn(Optional.of(adapter));
        when(adapterExtractor.getAllConfigs()).thenReturn(List.of(adapter));

        final List<DeviceTagRow> rows = List.of(DeviceTagRow.builder()
                .nodeId("ns=2;i=1")
                .tagName("existing-tag")
                .build());

        final List<ValidationError> errors = validator.validate(rows, ImportMode.CREATE, "adapter1");

        assertThat(errors).noneMatch(e -> e.code() == ValidationError.Code.TAG_CONFLICT);
    }

    @Test
    void validate_createMode_differentDefinition_fails() {
        final ProtocolAdapterEntity adapter = new ProtocolAdapterEntity(
                "adapter1",
                "opcua",
                1,
                Map.of(),
                List.of(),
                List.of(),
                List.of(new TagEntity("existing-tag", "desc", Map.of("node", "ns=2;i=99"))));
        when(adapterExtractor.getAdapterByAdapterId("adapter1")).thenReturn(Optional.of(adapter));
        when(adapterExtractor.getAllConfigs()).thenReturn(List.of(adapter));

        final List<DeviceTagRow> rows = List.of(DeviceTagRow.builder()
                .nodeId("ns=2;i=1")
                .tagName("existing-tag")
                .build());

        final List<ValidationError> errors = validator.validate(rows, ImportMode.CREATE, "adapter1");

        assertThat(errors).anySatisfy(e -> assertThat(e.code()).isEqualTo(ValidationError.Code.TAG_CONFLICT));
    }

    // --- DELETE mode validation ---

    @Test
    void validate_deleteMode_fileOnly_fails() {
        final ProtocolAdapterEntity adapter = new ProtocolAdapterEntity(
                "adapter1",
                "opcua",
                1,
                Map.of(),
                List.of(),
                List.of(),
                List.of(new TagEntity("edge-tag", null, Map.of("node", "ns=2;i=99"))));
        when(adapterExtractor.getAdapterByAdapterId("adapter1")).thenReturn(Optional.of(adapter));
        when(adapterExtractor.getAllConfigs()).thenReturn(List.of(adapter));

        final List<DeviceTagRow> rows = List.of(
                DeviceTagRow.builder().nodeId("ns=2;i=1").tagName("not-on-edge").build());

        final List<ValidationError> errors = validator.validate(rows, ImportMode.DELETE, "adapter1");

        assertThat(errors).anySatisfy(e -> {
            assertThat(e.code()).isEqualTo(ValidationError.Code.TAG_CONFLICT);
            assertThat(e.value()).isEqualTo("not-on-edge");
        });
    }

    @Test
    void validate_deleteMode_bothDifferent_fails() {
        final ProtocolAdapterEntity adapter = new ProtocolAdapterEntity(
                "adapter1",
                "opcua",
                1,
                Map.of(),
                List.of(),
                List.of(),
                List.of(new TagEntity("shared", null, Map.of("node", "ns=2;i=99"))));
        when(adapterExtractor.getAdapterByAdapterId("adapter1")).thenReturn(Optional.of(adapter));
        when(adapterExtractor.getAllConfigs()).thenReturn(List.of(adapter));

        final List<DeviceTagRow> rows = List.of(
                DeviceTagRow.builder().nodeId("ns=2;i=1").tagName("shared").build());

        final List<ValidationError> errors = validator.validate(rows, ImportMode.DELETE, "adapter1");

        assertThat(errors).anySatisfy(e -> assertThat(e.code()).isEqualTo(ValidationError.Code.TAG_CONFLICT));
    }

    @Test
    void validate_deleteMode_identical_noError() {
        final ProtocolAdapterEntity adapter = new ProtocolAdapterEntity(
                "adapter1",
                "opcua",
                1,
                Map.of(),
                List.of(),
                List.of(),
                List.of(new TagEntity("shared", null, Map.of("node", "ns=2;i=1"))));
        when(adapterExtractor.getAdapterByAdapterId("adapter1")).thenReturn(Optional.of(adapter));
        when(adapterExtractor.getAllConfigs()).thenReturn(List.of(adapter));

        final List<DeviceTagRow> rows = List.of(
                DeviceTagRow.builder().nodeId("ns=2;i=1").tagName("shared").build());

        final List<ValidationError> errors = validator.validate(rows, ImportMode.DELETE, "adapter1");

        assertThat(errors).noneMatch(e -> e.code() == ValidationError.Code.TAG_CONFLICT);
    }

    @Test
    void validate_tagConflict_mergeSafe_sameNodeId_differentDefinition() {
        final ProtocolAdapterEntity adapter = new ProtocolAdapterEntity(
                "adapter1",
                "opcua",
                1,
                Map.of(),
                List.of(),
                List.of(),
                List.of(new TagEntity("my-tag", "old-desc", Map.of("node", "ns=2;i=1"))));
        when(adapterExtractor.getAdapterByAdapterId("adapter1")).thenReturn(Optional.of(adapter));
        when(adapterExtractor.getAllConfigs()).thenReturn(List.of(adapter));

        // Same nodeId, different description → inBoth with different definition
        final List<DeviceTagRow> rows = List.of(DeviceTagRow.builder()
                .nodeId("ns=2;i=1")
                .tagName("my-tag")
                .tagDescription("new-desc")
                .build());

        final List<ValidationError> errors = validator.validate(rows, ImportMode.MERGE_SAFE, "adapter1");

        assertThat(errors).anySatisfy(e -> {
            assertThat(e.code()).isEqualTo(ValidationError.Code.TAG_CONFLICT);
            assertThat(e.message()).contains("MERGE_OVERWRITE");
        });
    }

    @Test
    void validate_tagConflict_mergeSafe_sameNodeId_rename() {
        final ProtocolAdapterEntity adapter = new ProtocolAdapterEntity(
                "adapter1",
                "opcua",
                1,
                Map.of(),
                List.of(),
                List.of(),
                List.of(new TagEntity("old-name", null, Map.of("node", "ns=2;i=1"))));
        when(adapterExtractor.getAdapterByAdapterId("adapter1")).thenReturn(Optional.of(adapter));
        when(adapterExtractor.getAllConfigs()).thenReturn(List.of(adapter));

        // Same nodeId, different tagName → rename is a definition change
        final List<DeviceTagRow> rows = List.of(
                DeviceTagRow.builder().nodeId("ns=2;i=1").tagName("new-name").build());

        final List<ValidationError> errors = validator.validate(rows, ImportMode.MERGE_SAFE, "adapter1");

        assertThat(errors).anySatisfy(e -> {
            assertThat(e.code()).isEqualTo(ValidationError.Code.TAG_CONFLICT);
            assertThat(e.message()).contains("MERGE_OVERWRITE");
        });
    }

    @Test
    void validate_tagNameCollision_mergeSafe_differentNodes_sameTagName() {
        final ProtocolAdapterEntity adapter = new ProtocolAdapterEntity(
                "adapter1",
                "opcua",
                1,
                Map.of(),
                List.of(),
                List.of(),
                List.of(new TagEntity("colliding-name", null, Map.of("node", "ns=2;i=99"))));
        when(adapterExtractor.getAdapterByAdapterId("adapter1")).thenReturn(Optional.of(adapter));
        when(adapterExtractor.getAllConfigs()).thenReturn(List.of(adapter));

        // Different nodeId, same tagName → collision with surviving edge-only tag
        final List<DeviceTagRow> rows = List.of(DeviceTagRow.builder()
                .nodeId("ns=2;i=1")
                .tagName("colliding-name")
                .build());

        final List<ValidationError> errors = validator.validate(rows, ImportMode.MERGE_SAFE, "adapter1");

        assertThat(errors).anySatisfy(e -> {
            assertThat(e.code()).isEqualTo(ValidationError.Code.TAG_CONFLICT);
            assertThat(e.message()).contains("collides");
        });
    }

    @Test
    void validate_tagNameCollision_mergeOverwrite_differentNodes_sameTagName() {
        final ProtocolAdapterEntity adapter = new ProtocolAdapterEntity(
                "adapter1",
                "opcua",
                1,
                Map.of(),
                List.of(),
                List.of(),
                List.of(new TagEntity("colliding-name", null, Map.of("node", "ns=2;i=99"))));
        when(adapterExtractor.getAdapterByAdapterId("adapter1")).thenReturn(Optional.of(adapter));
        when(adapterExtractor.getAllConfigs()).thenReturn(List.of(adapter));

        // Different nodeId, same tagName → collision with surviving edge-only tag in MERGE_OVERWRITE too
        final List<DeviceTagRow> rows = List.of(DeviceTagRow.builder()
                .nodeId("ns=2;i=1")
                .tagName("colliding-name")
                .build());

        final List<ValidationError> errors = validator.validate(rows, ImportMode.MERGE_OVERWRITE, "adapter1");

        assertThat(errors).anySatisfy(e -> {
            assertThat(e.code()).isEqualTo(ValidationError.Code.TAG_CONFLICT);
            assertThat(e.message()).contains("collides");
        });
    }

    @Test
    void validate_rename_mergeOverwrite_noConflict() {
        final ProtocolAdapterEntity adapter = new ProtocolAdapterEntity(
                "adapter1",
                "opcua",
                1,
                Map.of(),
                List.of(),
                List.of(),
                List.of(new TagEntity("old-name", null, Map.of("node", "ns=2;i=1"))));
        when(adapterExtractor.getAdapterByAdapterId("adapter1")).thenReturn(Optional.of(adapter));
        when(adapterExtractor.getAllConfigs()).thenReturn(List.of(adapter));

        // Same nodeId, different tagName, no edge-only survivors to collide with
        final List<DeviceTagRow> rows = List.of(
                DeviceTagRow.builder().nodeId("ns=2;i=1").tagName("new-name").build());

        final List<ValidationError> errors = validator.validate(rows, ImportMode.MERGE_OVERWRITE, "adapter1");

        assertThat(errors).noneMatch(e -> e.code() == ValidationError.Code.TAG_CONFLICT);
    }

    @Test
    void validate_tagConflict_mergeSafe_sameDefinition_noError() {
        final ProtocolAdapterEntity adapter = new ProtocolAdapterEntity(
                "adapter1",
                "opcua",
                1,
                Map.of(),
                List.of(),
                List.of(),
                List.of(new TagEntity("my-tag", null, Map.of("node", "ns=2;i=1"))));
        when(adapterExtractor.getAdapterByAdapterId("adapter1")).thenReturn(Optional.of(adapter));
        when(adapterExtractor.getAllConfigs()).thenReturn(List.of(adapter));

        final List<DeviceTagRow> rows = List.of(
                DeviceTagRow.builder().nodeId("ns=2;i=1").tagName("my-tag").build());

        final List<ValidationError> errors = validator.validate(rows, ImportMode.MERGE_SAFE, "adapter1");

        assertThat(errors).noneMatch(e -> e.code().equals("TAG_CONFLICT"));
    }

    @Test
    void validate_sameTagNameOnOtherAdapter_allowed() {
        final ProtocolAdapterEntity myAdapter =
                new ProtocolAdapterEntity("adapter1", "opcua", 1, Map.of(), List.of(), List.of(), List.of());
        final ProtocolAdapterEntity otherAdapter = new ProtocolAdapterEntity(
                "adapter2",
                "opcua",
                1,
                Map.of(),
                List.of(),
                List.of(),
                List.of(new TagEntity("shared-tag", null, Map.of("node", "ns=2;i=99"))));

        when(adapterExtractor.getAdapterByAdapterId("adapter1")).thenReturn(Optional.of(myAdapter));
        when(adapterExtractor.getAllConfigs()).thenReturn(List.of(myAdapter, otherAdapter));

        final List<DeviceTagRow> rows = List.of(
                DeviceTagRow.builder().nodeId("ns=2;i=1").tagName("shared-tag").build());

        final List<ValidationError> errors = validator.validate(rows, ImportMode.CREATE, "adapter1");

        // Tag names are scoped to their adapter — same name on another adapter is not a conflict
        assertThat(errors).noneMatch(e -> e.value() != null && e.value().equals("shared-tag"));
    }

    // --- Row numbers are 1-indexed ---

    @Test
    void validate_rowNumbersAre1Indexed() {
        final List<DeviceTagRow> rows = List.of(
                DeviceTagRow.builder().nodeId("ns=2;i=1").tagName("valid-tag").build(),
                DeviceTagRow.builder()
                        .nodeId("ns=2;i=2")
                        .tagName("tag with spaces")
                        .build());

        final List<ValidationError> errors = validator.validate(rows, ImportMode.CREATE, "adapter1");

        assertThat(errors).anySatisfy(e -> {
            assertThat(e.code()).isEqualTo(ValidationError.Code.INVALID_TAG_NAME);
            assertThat(e.row()).isEqualTo(2);
        });
    }

    // --- Rows without tag are skipped ---

    @Test
    void validate_rowsWithoutTag_skippedForRowValidation() {
        final List<DeviceTagRow> rows =
                List.of(DeviceTagRow.builder().nodePath("/Objects/Data").build());

        final List<ValidationError> errors = validator.validate(rows, ImportMode.CREATE, "adapter1");

        // No row-level validation errors for rows without tags
        assertThat(errors).noneMatch(e -> e.code().equals("INVALID_TAG_NAME"));
        assertThat(errors).noneMatch(e -> e.code().equals("INVALID_NODE_ID"));
    }

    // --- Expiry upper bound (MQTT uint32 max) ---

    @Test
    void validate_expiryInterval_atMqttMax_valid() {
        final List<DeviceTagRow> rows = List.of(DeviceTagRow.builder()
                .nodeId("ns=2;i=1")
                .tagName("tag1")
                .messageExpiryInterval(4_294_967_295L)
                .build());

        final List<ValidationError> errors = validator.validate(rows, ImportMode.CREATE, "adapter1");

        assertThat(errors).noneMatch(e -> e.code() == ValidationError.Code.INVALID_EXPIRY);
    }

    @Test
    void validate_expiryInterval_exceedsMqttMax_rejected() {
        final List<DeviceTagRow> rows = List.of(DeviceTagRow.builder()
                .nodeId("ns=2;i=1")
                .tagName("tag1")
                .messageExpiryInterval(4_294_967_296L)
                .build());

        final List<ValidationError> errors = validator.validate(rows, ImportMode.CREATE, "adapter1");

        assertThat(errors).anySatisfy(e -> {
            assertThat(e.code()).isEqualTo(ValidationError.Code.INVALID_EXPIRY);
            assertThat(e.message()).contains("4294967295");
        });
    }

    // --- Topic length limits ---

    @Test
    void validate_northboundTopic_exceedsMaxLength_rejected() {
        final String longTopic = "a/".repeat(32768);
        final List<DeviceTagRow> rows = List.of(DeviceTagRow.builder()
                .nodeId("ns=2;i=1")
                .tagName("tag1")
                .northboundTopic(longTopic)
                .build());

        final List<ValidationError> errors = validator.validate(rows, ImportMode.CREATE, "adapter1");

        assertThat(errors).anySatisfy(e -> {
            assertThat(e.code()).isEqualTo(ValidationError.Code.INVALID_TOPIC);
            assertThat(e.message()).contains("maximum length");
        });
    }

    @Test
    void validate_southboundTopic_exceedsMaxLength_rejected() {
        final String longTopic = "a/".repeat(32768);
        final List<DeviceTagRow> rows = List.of(DeviceTagRow.builder()
                .nodeId("ns=2;i=1")
                .tagName("tag1")
                .southboundTopic(longTopic)
                .build());

        final List<ValidationError> errors = validator.validate(rows, ImportMode.CREATE, "adapter1");

        assertThat(errors).anySatisfy(e -> {
            assertThat(e.code()).isEqualTo(ValidationError.Code.INVALID_TOPIC);
            assertThat(e.message()).contains("maximum length");
        });
    }

    // --- Tag description length limit ---

    @Test
    void validate_tagDescription_exceedsMaxLength_rejected() {
        final String longDesc = "x".repeat(1025);
        final List<DeviceTagRow> rows = List.of(DeviceTagRow.builder()
                .nodeId("ns=2;i=1")
                .tagName("tag1")
                .tagDescription(longDesc)
                .build());

        final List<ValidationError> errors = validator.validate(rows, ImportMode.CREATE, "adapter1");

        assertThat(errors).anySatisfy(e -> assertThat(e.message()).contains("maximum length"));
    }

    @Test
    void validate_tagDescription_atMaxLength_valid() {
        final String desc = "x".repeat(1024);
        final List<DeviceTagRow> rows = List.of(DeviceTagRow.builder()
                .nodeId("ns=2;i=1")
                .tagName("tag1")
                .tagDescription(desc)
                .build());

        final List<ValidationError> errors = validator.validate(rows, ImportMode.CREATE, "adapter1");

        assertThat(errors).noneMatch(e -> e.message() != null && e.message().contains("description"));
    }

    // --- User property length limits ---

    @Test
    void validate_userPropertyKey_exceedsMaxLength_rejected() {
        final String longKey = "k".repeat(257);
        final List<DeviceTagRow> rows = List.of(DeviceTagRow.builder()
                .nodeId("ns=2;i=1")
                .tagName("tag1")
                .mqttUserProperties(Map.of(longKey, "value"))
                .build());

        final List<ValidationError> errors = validator.validate(rows, ImportMode.CREATE, "adapter1");

        assertThat(errors).anySatisfy(e -> {
            assertThat(e.code()).isEqualTo(ValidationError.Code.INVALID_USER_PROPERTIES);
            assertThat(e.message()).contains("key");
        });
    }

    @Test
    void validate_userPropertyValue_exceedsMaxLength_rejected() {
        final String longValue = "v".repeat(257);
        final List<DeviceTagRow> rows = List.of(DeviceTagRow.builder()
                .nodeId("ns=2;i=1")
                .tagName("tag1")
                .mqttUserProperties(Map.of("key", longValue))
                .build());

        final List<ValidationError> errors = validator.validate(rows, ImportMode.CREATE, "adapter1");

        assertThat(errors).anySatisfy(e -> {
            assertThat(e.code()).isEqualTo(ValidationError.Code.INVALID_USER_PROPERTIES);
            assertThat(e.message()).contains("value");
        });
    }
}
