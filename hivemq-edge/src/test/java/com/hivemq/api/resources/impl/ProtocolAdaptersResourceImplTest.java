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
package com.hivemq.api.resources.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.combining.model.DataIdentifierReference;
import com.hivemq.configuration.entity.adapter.DomainTagOwnerConverter;
import com.hivemq.configuration.entity.adapter.NorthboundMappingEntity;
import com.hivemq.configuration.entity.adapter.ProtocolAdapterEntity;
import com.hivemq.configuration.entity.adapter.QoSConverter;
import com.hivemq.configuration.entity.adapter.SouthboundMappingEntity;
import com.hivemq.configuration.entity.adapter.TagEntity;
import com.hivemq.configuration.entity.adapter.fieldmapping.FieldMappingEntity;
import com.hivemq.configuration.entity.adapter.fieldmapping.InstructionEntity;
import com.hivemq.configuration.entity.combining.DataIdentifierReferenceEntity;
import com.hivemq.configuration.info.SystemInformation;
import com.hivemq.configuration.reader.ProtocolAdapterExtractor;
import com.hivemq.configuration.service.ConfigurationService;
import com.hivemq.edge.HiveMQEdgeRemoteService;
import com.hivemq.edge.VersionProvider;
import com.hivemq.edge.api.model.DomainTagList;
import com.hivemq.edge.api.model.DomainTagOwnerList;
import com.hivemq.edge.api.model.FieldMapping;
import com.hivemq.edge.api.model.NorthboundMappingOwner;
import com.hivemq.edge.api.model.NorthboundMappingOwnerList;
import com.hivemq.edge.api.model.SouthboundMappingOwner;
import com.hivemq.edge.api.model.SouthboundMappingOwnerList;
import com.hivemq.persistence.domain.DomainTag;
import com.hivemq.persistence.domain.DomainTagAddResult;
import com.hivemq.persistence.topicfilter.TopicFilterPersistence;
import com.hivemq.protocols.InternalProtocolAdapterWritingService;
import com.hivemq.protocols.ProtocolAdapterConfigConverter;
import com.hivemq.protocols.ProtocolAdapterManager;
import jakarta.ws.rs.core.Response;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProtocolAdaptersResourceImplTest {

    private final @NotNull HiveMQEdgeRemoteService remoteService = mock();
    private final @NotNull ConfigurationService configurationService = mock();
    private final @NotNull ProtocolAdapterManager protocolAdapterManager = mock();
    private final @NotNull InternalProtocolAdapterWritingService protocolAdapterWritingService = mock();
    private final @NotNull ObjectMapper objectMapper = new ObjectMapper();
    private final @NotNull VersionProvider versionProvider = mock();
    private final @NotNull ProtocolAdapterConfigConverter configConverter = mock();
    private final @NotNull TopicFilterPersistence topicFilterPersistence = mock();
    private final @NotNull SystemInformation systemInformation = mock();
    private final @NotNull ProtocolAdapterExtractor protocolAdapterExtractor = mock();

    private final ProtocolAdaptersResourceImpl protocolAdaptersResource = new ProtocolAdaptersResourceImpl(
            remoteService,
            configurationService,
            protocolAdapterManager,
            protocolAdapterWritingService,
            objectMapper,
            versionProvider,
            topicFilterPersistence,
            systemInformation,
            protocolAdapterExtractor);

    @BeforeEach
    public void setUp() {
        when(systemInformation.isConfigWriteable()).thenReturn(true);
    }

    @Test
    void getDomainTagsForAdapter() {
        when(protocolAdapterWritingService.writingEnabled()).thenReturn(false);

        final ArrayList<DomainTag> domainTags = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            domainTags.add(new DomainTag(
                    "tag" + i, "1", "description", objectMapper.valueToTree(Map.of("address", "addressy"))));
        }

        when(protocolAdapterManager.getTagsForAdapter("adapter")).thenReturn(Optional.of(domainTags));

        final Response response = protocolAdaptersResource.getAdapterDomainTags("adapter");

        final Object entity = response.getEntity();

        assertInstanceOf(DomainTagList.class, entity);
        final DomainTagList domainTagList = (DomainTagList) entity;
        assertEquals(domainTags.size(), domainTagList.getItems().size());
        for (int i = 0; i < domainTags.size(); i++) {
            final DomainTag domainTag = domainTags.get(i);
            assertEquals(domainTag.toModel(), domainTagList.getItems().get(i));
        }
    }

    @Test
    void addAdapterDomainTag_whenAddingSucceeds_thenReturn200() {
        when(protocolAdapterWritingService.writingEnabled()).thenReturn(false);

        var protocolAdapterEntity = mock(ProtocolAdapterEntity.class);
        when(protocolAdapterEntity.getTags())
                .thenReturn(List.of(new TagEntity("tagExists", "description", Map.of("address", "addressy1"))));

        when(protocolAdapterExtractor.getAdapterByAdapterId("adapter")).thenReturn(Optional.of(protocolAdapterEntity));
        when(protocolAdapterExtractor.updateAdapter(any())).thenReturn(true);

        when(protocolAdapterManager.addDomainTag(eq("adapter"), any())).thenReturn(DomainTagAddResult.success());

        final Response response = protocolAdaptersResource.addAdapterDomainTags(
                "adapter",
                (new DomainTag("tag", "1", "description", objectMapper.valueToTree(Map.of("address", "addressy")))
                        .toModel()));

        assertEquals(200, response.getStatus());
    }

    @Test
    void addAdapterDomainTag_whenAlreadyExists_thenReturn403() {
        when(protocolAdapterWritingService.writingEnabled()).thenReturn(false);

        var protocolAdapterEntity = mock(ProtocolAdapterEntity.class);
        when(protocolAdapterEntity.getTags())
                .thenReturn(List.of(new TagEntity("tag", "description", Map.of("address", "addressy1"))));

        when(protocolAdapterExtractor.getAdapterByAdapterId("adapter")).thenReturn(Optional.of(protocolAdapterEntity));
        when(protocolAdapterExtractor.updateAdapter(any())).thenReturn(false);

        final Response response = protocolAdaptersResource.addAdapterDomainTags(
                "adapter",
                new DomainTag("tag", "1", "description", objectMapper.valueToTree(Map.of("address", "addressy")))
                        .toModel());

        assertEquals(403, response.getStatus());
    }

    @Test
    void deleteDomainTag_whenTagExists_thenReturn200() {
        when(protocolAdapterWritingService.writingEnabled()).thenReturn(false);
        when(protocolAdapterExtractor.updateAdapter(any())).thenReturn(true);

        var protocolAdapterEntity = mock(ProtocolAdapterEntity.class);
        when(protocolAdapterEntity.getTags())
                .thenReturn(List.of(new TagEntity("tag", "description", Map.of("address", "addressy1"))));

        when(protocolAdapterExtractor.getAdapterByAdapterId("adapter")).thenReturn(Optional.of(protocolAdapterEntity));

        final Response response = protocolAdaptersResource.deleteAdapterDomainTags(
                "adapter", URLEncoder.encode("tag", StandardCharsets.UTF_8));

        assertEquals(200, response.getStatus());
    }

    @Test
    void deleteDomainTag_whenTagDoesNotExists_thenReturn403() {
        when(protocolAdapterWritingService.writingEnabled()).thenReturn(false);
        when(protocolAdapterExtractor.updateAdapter(any())).thenReturn(false);

        final Response response = protocolAdaptersResource.deleteAdapterDomainTags(
                "adapter", URLEncoder.encode("tag", StandardCharsets.UTF_8));

        assertEquals(404, response.getStatus());
    }

    @Test
    void updateDomainTag_whenTagExists_thenReturn200() {
        when(protocolAdapterWritingService.writingEnabled()).thenReturn(false);

        var protocolAdapterEntity = mock(ProtocolAdapterEntity.class);
        when(protocolAdapterEntity.getTags())
                .thenReturn(List.of(new TagEntity("tag", "description", Map.of("address", "addressy1"))));

        when(protocolAdapterExtractor.getAdapterByAdapterId("adapter")).thenReturn(Optional.of(protocolAdapterEntity));
        when(protocolAdapterExtractor.updateAdapter(any())).thenReturn(true);

        final Response response = protocolAdaptersResource.updateAdapterDomainTag(
                "adapter",
                URLEncoder.encode("tag", StandardCharsets.UTF_8),
                new DomainTag("tag", "1", "description", objectMapper.valueToTree(Map.of("address", "addressy")))
                        .toModel());

        assertEquals(200, response.getStatus());
    }

    @Test
    void updateDomainTag_whenTagDoesNotExists_thenReturn400() {
        when(protocolAdapterWritingService.writingEnabled()).thenReturn(false);
        when(protocolAdapterExtractor.updateAdapter(any())).thenReturn(false);

        final Response response = protocolAdaptersResource.updateAdapterDomainTag(
                "adapter",
                Base64.getEncoder().encodeToString("tag".getBytes(StandardCharsets.UTF_8)),
                new DomainTag("tag", "1", "description", objectMapper.valueToTree(Map.of("address", "addressy")))
                        .toModel());

        assertEquals(403, response.getStatus());
    }

    @Test
    void getDomainTags() {
        when(protocolAdapterWritingService.writingEnabled()).thenReturn(false);
        final ArrayList<DomainTag> domainTags = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            domainTags.add(new DomainTag(
                    "tag" + i, "adapter" + i, "description", objectMapper.valueToTree(Map.of("address", "addressy"))));
        }
        when(protocolAdapterManager.getDomainTags()).thenReturn(domainTags);
        final Response response = protocolAdaptersResource.getDomainTags();
        assertThat(response.getEntity()).isInstanceOfSatisfying(DomainTagOwnerList.class, domainTagOwnerList -> {
            assertThat(domainTagOwnerList.getItems().size()).isEqualTo(domainTags.size());
            IntStream.range(0, domainTagOwnerList.getItems().size())
                    .forEach(i -> assertThat(domainTagOwnerList.getItems().get(i))
                            .isEqualTo(DomainTagOwnerConverter.INSTANCE.toRestEntity(domainTags.get(i))));
        });
    }

    @Test
    void getNorthboundMappings() {
        final int count = 5;
        when(protocolAdapterExtractor.getAllConfigs())
                .thenReturn(IntStream.range(0, count)
                        .mapToObj(i -> new ProtocolAdapterEntity(
                                "adapter" + i,
                                "protocol" + i,
                                i,
                                Map.of("id", i),
                                List.of(
                                        new NorthboundMappingEntity(
                                                "tagName" + i + ".a",
                                                "topic" + i + ".a",
                                                0,
                                                null,
                                                false,
                                                true,
                                                List.of(),
                                                1234L + i),
                                        new NorthboundMappingEntity(
                                                "tagName" + i + ".b",
                                                "topic" + i + ".b",
                                                1,
                                                null,
                                                false,
                                                true,
                                                List.of(),
                                                1234L + i)),
                                List.of(),
                                List.of()))
                        .toList());
        final Response response = protocolAdaptersResource.getNorthboundMappings();
        assertThat(response.getEntity())
                .isInstanceOfSatisfying(NorthboundMappingOwnerList.class, northboundMappingOwnerList -> {
                    assertThat(northboundMappingOwnerList.getItems().size()).isEqualTo(count * 2);
                    IntStream.range(0, count * 2).forEach(i -> {
                        final NorthboundMappingOwner item =
                                northboundMappingOwnerList.getItems().get(i);
                        assertThat(item.getAdapterId()).isEqualTo("adapter" + (i / 2));
                        assertThat(item.getTagName()).isEqualTo("tagName" + (i / 2) + "." + ((i % 2 == 0 ? "a" : "b")));
                        assertThat(item.getTopic()).isEqualTo("topic" + (i / 2) + "." + ((i % 2 == 0 ? "a" : "b")));
                        assertThat(item.getMaxQoS()).isEqualTo(QoSConverter.INSTANCE.toRestEntity(i % 2));
                        assertThat(item.getIncludeTagNames()).isFalse();
                        assertThat(item.getIncludeTimestamp()).isTrue();
                        assertThat(item.getMessageExpiryInterval()).isEqualTo(1234L + i / 2);
                    });
                });
    }

    @Test
    void getSouthboundMappings() {
        final int count = 5;
        when(protocolAdapterExtractor.getAllConfigs())
                .thenReturn(IntStream.range(0, count)
                        .mapToObj(i -> new ProtocolAdapterEntity(
                                "adapter" + i,
                                "protocol" + i,
                                i,
                                Map.of("id", i),
                                List.of(),
                                List.of(
                                        new SouthboundMappingEntity(
                                                "tagName" + i + ".a",
                                                "topicFilter" + i + ".a",
                                                new FieldMappingEntity(List.of(new InstructionEntity(
                                                        "sourceFieldName" + i + ".a",
                                                        "destinationFieldName" + i + ".a",
                                                        new DataIdentifierReferenceEntity(
                                                                "id" + i + ".a",
                                                                DataIdentifierReference.Type.PULSE_ASSET)))),
                                                "fromNorthSchema" + i + ".a"),
                                        new SouthboundMappingEntity(
                                                "tagName" + i + ".b",
                                                "topicFilter" + i + ".b",
                                                new FieldMappingEntity(List.of(new InstructionEntity(
                                                        "sourceFieldName" + i + ".b",
                                                        "destinationFieldName" + i + ".b",
                                                        new DataIdentifierReferenceEntity(
                                                                "id" + i + ".b",
                                                                DataIdentifierReference.Type.PULSE_ASSET)))),
                                                "fromNorthSchema" + i + ".b")),
                                List.of()))
                        .toList());
        final Response response = protocolAdaptersResource.getSouthboundMappings();
        assertThat(response.getEntity())
                .isInstanceOfSatisfying(SouthboundMappingOwnerList.class, southboundMappingOwnerList -> {
                    assertThat(southboundMappingOwnerList.getItems().size()).isEqualTo(count * 2);
                    IntStream.range(0, count * 2).forEach(i -> {
                        final SouthboundMappingOwner item =
                                southboundMappingOwnerList.getItems().get(i);
                        assertThat(item.getAdapterId()).isEqualTo("adapter" + (i / 2));
                        assertThat(item.getTagName()).isEqualTo("tagName" + (i / 2) + "." + ((i % 2 == 0 ? "a" : "b")));
                        assertThat(item.getTopicFilter())
                                .isEqualTo("topicFilter" + (i / 2) + "." + ((i % 2 == 0 ? "a" : "b")));
                        final FieldMapping fieldMapping = item.getFieldMapping();
                        assertThat(fieldMapping.getInstructions()).hasSize(1);
                        assertThat(fieldMapping.getInstructions().getFirst().getSource())
                                .isEqualTo("sourceFieldName" + (i / 2) + "." + ((i % 2 == 0 ? "a" : "b")));
                        assertThat(fieldMapping.getInstructions().getFirst().getDestination())
                                .isEqualTo("destinationFieldName" + (i / 2) + "." + ((i % 2 == 0 ? "a" : "b")));
                        assertThat(fieldMapping.getInstructions().getFirst().getSourceRef())
                                .isNull();
                    });
                });
    }

    @Test
    void testDeleteTagInUseByNorthboundMapping() {
        when(protocolAdapterWritingService.writingEnabled()).thenReturn(false);

        final String adapterId = "test-adapter";
        final String tagName = "temperature";

        final com.hivemq.configuration.entity.adapter.NorthboundMappingEntity northboundMapping =
                new com.hivemq.configuration.entity.adapter.NorthboundMappingEntity(
                        tagName, "test/topic", 1, null, false, true, List.of(), null);

        final com.hivemq.configuration.entity.adapter.TagEntity tagEntity =
                new com.hivemq.configuration.entity.adapter.TagEntity(
                        tagName, "description", Map.of("address", "test"));

        final ProtocolAdapterEntity adapterEntity = new ProtocolAdapterEntity(
                adapterId, "opcua", 1, Map.of(), List.of(northboundMapping), List.of(), List.of(tagEntity));

        when(protocolAdapterExtractor.getAdapterByAdapterId(adapterId)).thenReturn(Optional.of(adapterEntity));

        final Response response = protocolAdaptersResource.deleteAdapterDomainTags(adapterId, tagName);

        assertEquals(409, response.getStatus());
    }

    @Test
    void testDeleteTagInUseBySouthboundMapping() {
        when(protocolAdapterWritingService.writingEnabled()).thenReturn(false);

        final String adapterId = "test-adapter";
        final String tagName = "valve-control";

        final com.hivemq.configuration.entity.adapter.SouthboundMappingEntity southboundMapping =
                new com.hivemq.configuration.entity.adapter.SouthboundMappingEntity(
                        tagName, "commands/valve/+", null, "schema");

        final com.hivemq.configuration.entity.adapter.TagEntity tagEntity =
                new com.hivemq.configuration.entity.adapter.TagEntity(
                        tagName, "description", Map.of("address", "test"));

        final ProtocolAdapterEntity adapterEntity = new ProtocolAdapterEntity(
                adapterId, "modbus", 1, Map.of(), List.of(), List.of(southboundMapping), List.of(tagEntity));

        when(protocolAdapterExtractor.getAdapterByAdapterId(adapterId)).thenReturn(Optional.of(adapterEntity));

        final Response response = protocolAdaptersResource.deleteAdapterDomainTags(adapterId, tagName);

        assertEquals(409, response.getStatus());
    }

    @Test
    void testDeleteTagInUseByBothMappings() {
        when(protocolAdapterWritingService.writingEnabled()).thenReturn(false);

        final String adapterId = "test-adapter";
        final String tagName = "sensor-data";

        final com.hivemq.configuration.entity.adapter.NorthboundMappingEntity northboundMapping =
                new com.hivemq.configuration.entity.adapter.NorthboundMappingEntity(
                        tagName, "sensors/data", 1, null, false, true, List.of(), null);

        final com.hivemq.configuration.entity.adapter.SouthboundMappingEntity southboundMapping =
                new com.hivemq.configuration.entity.adapter.SouthboundMappingEntity(
                        tagName, "commands/sensor/+", null, "schema");

        final com.hivemq.configuration.entity.adapter.TagEntity tagEntity =
                new com.hivemq.configuration.entity.adapter.TagEntity(
                        tagName, "description", Map.of("address", "test"));

        final ProtocolAdapterEntity adapterEntity = new ProtocolAdapterEntity(
                adapterId,
                "opcua",
                1,
                Map.of(),
                List.of(northboundMapping),
                List.of(southboundMapping),
                List.of(tagEntity));

        when(protocolAdapterExtractor.getAdapterByAdapterId(adapterId)).thenReturn(Optional.of(adapterEntity));

        final Response response = protocolAdaptersResource.deleteAdapterDomainTags(adapterId, tagName);

        assertEquals(409, response.getStatus());
    }

    @Test
    void testDeleteTagNotInUse() {
        when(protocolAdapterWritingService.writingEnabled()).thenReturn(false);

        final String adapterId = "test-adapter";
        final String tagName = "unused-tag";

        final com.hivemq.configuration.entity.adapter.TagEntity tagEntity =
                new com.hivemq.configuration.entity.adapter.TagEntity(
                        tagName, "description", Map.of("address", "test"));

        final ProtocolAdapterEntity adapterEntity =
                new ProtocolAdapterEntity(adapterId, "opcua", 1, Map.of(), List.of(), List.of(), List.of(tagEntity));

        when(protocolAdapterExtractor.getAdapterByAdapterId(adapterId)).thenReturn(Optional.of(adapterEntity));
        when(protocolAdapterExtractor.updateAdapter(any())).thenReturn(true);

        final Response response = protocolAdaptersResource.deleteAdapterDomainTags(adapterId, tagName);

        assertEquals(200, response.getStatus());
    }

    @Test
    void testUpdateTagRenameInUse() {
        when(protocolAdapterWritingService.writingEnabled()).thenReturn(false);

        final String adapterId = "test-adapter";
        final String oldTagName = "temperature";
        final String newTagName = "temp-sensor";

        final com.hivemq.configuration.entity.adapter.NorthboundMappingEntity northboundMapping =
                new com.hivemq.configuration.entity.adapter.NorthboundMappingEntity(
                        oldTagName, "test/topic", 1, null, false, true, List.of(), null);

        final com.hivemq.configuration.entity.adapter.TagEntity tagEntity =
                new com.hivemq.configuration.entity.adapter.TagEntity(
                        oldTagName, "description", Map.of("address", "test"));

        final ProtocolAdapterEntity adapterEntity = new ProtocolAdapterEntity(
                adapterId, "opcua", 1, Map.of(), List.of(northboundMapping), List.of(), List.of(tagEntity));

        when(protocolAdapterExtractor.getAdapterByAdapterId(adapterId)).thenReturn(Optional.of(adapterEntity));

        final com.hivemq.edge.api.model.DomainTag updatedTag = new com.hivemq.edge.api.model.DomainTag()
                .name(newTagName)
                .description("description")
                .definition(objectMapper.valueToTree(Map.of("address", "test")));

        final Response response = protocolAdaptersResource.updateAdapterDomainTag(adapterId, oldTagName, updatedTag);

        assertEquals(409, response.getStatus());
    }

    @Test
    void testUpdateTagRenameNotInUse() {
        when(protocolAdapterWritingService.writingEnabled()).thenReturn(false);

        final String adapterId = "test-adapter";
        final String oldTagName = "temperature";
        final String newTagName = "temp-sensor";

        final com.hivemq.configuration.entity.adapter.TagEntity tagEntity =
                new com.hivemq.configuration.entity.adapter.TagEntity(
                        oldTagName, "description", Map.of("address", "test"));

        final ProtocolAdapterEntity adapterEntity =
                new ProtocolAdapterEntity(adapterId, "opcua", 1, Map.of(), List.of(), List.of(), List.of(tagEntity));

        when(protocolAdapterExtractor.getAdapterByAdapterId(adapterId)).thenReturn(Optional.of(adapterEntity));
        when(protocolAdapterExtractor.updateAdapter(any())).thenReturn(true);

        final com.hivemq.edge.api.model.DomainTag updatedTag = new com.hivemq.edge.api.model.DomainTag()
                .name(newTagName)
                .description("description")
                .definition(objectMapper.valueToTree(Map.of("address", "test")));

        final Response response = protocolAdaptersResource.updateAdapterDomainTag(adapterId, oldTagName, updatedTag);

        assertEquals(200, response.getStatus());
    }

    @Test
    void testUpdateTagWithoutRename() {
        when(protocolAdapterWritingService.writingEnabled()).thenReturn(false);

        final String adapterId = "test-adapter";
        final String tagName = "temperature";

        final com.hivemq.configuration.entity.adapter.NorthboundMappingEntity northboundMapping =
                new com.hivemq.configuration.entity.adapter.NorthboundMappingEntity(
                        tagName, "test/topic", 1, null, false, true, List.of(), null);

        final com.hivemq.configuration.entity.adapter.TagEntity tagEntity =
                new com.hivemq.configuration.entity.adapter.TagEntity(
                        tagName, "description", Map.of("address", "test"));

        final ProtocolAdapterEntity adapterEntity = new ProtocolAdapterEntity(
                adapterId, "opcua", 1, Map.of(), List.of(northboundMapping), List.of(), List.of(tagEntity));

        when(protocolAdapterExtractor.getAdapterByAdapterId(adapterId)).thenReturn(Optional.of(adapterEntity));
        when(protocolAdapterExtractor.updateAdapter(any())).thenReturn(true);

        final com.hivemq.edge.api.model.DomainTag updatedTag = new com.hivemq.edge.api.model.DomainTag()
                .name(tagName)
                .description("updated description")
                .definition(objectMapper.valueToTree(Map.of("address", "new-address")));

        final Response response = protocolAdaptersResource.updateAdapterDomainTag(adapterId, tagName, updatedTag);

        assertEquals(200, response.getStatus());
    }

    @Test
    void testBulkUpdateRemovingTagInUse() {
        when(protocolAdapterWritingService.writingEnabled()).thenReturn(false);

        final String adapterId = "test-adapter";
        final String tag1Name = "temperature";
        final String tag2Name = "pressure";

        final com.hivemq.configuration.entity.adapter.NorthboundMappingEntity northboundMapping =
                new com.hivemq.configuration.entity.adapter.NorthboundMappingEntity(
                        tag1Name, "test/topic", 1, null, false, true, List.of(), null);

        final com.hivemq.configuration.entity.adapter.TagEntity tag1Entity =
                new com.hivemq.configuration.entity.adapter.TagEntity(
                        tag1Name, "description", Map.of("address", "test1"));

        final com.hivemq.configuration.entity.adapter.TagEntity tag2Entity =
                new com.hivemq.configuration.entity.adapter.TagEntity(
                        tag2Name, "description", Map.of("address", "test2"));

        final ProtocolAdapterEntity adapterEntity = new ProtocolAdapterEntity(
                adapterId,
                "opcua",
                1,
                Map.of(),
                List.of(northboundMapping),
                List.of(),
                List.of(tag1Entity, tag2Entity));

        when(protocolAdapterExtractor.getAdapterByAdapterId(adapterId)).thenReturn(Optional.of(adapterEntity));

        // Only include tag2 in the new list (removing tag1 which is in use)
        final DomainTagList newTagList = new DomainTagList()
                .items(List.of(new com.hivemq.edge.api.model.DomainTag()
                        .name(tag2Name)
                        .description("description")
                        .definition(objectMapper.valueToTree(Map.of("address", "test2")))));

        final Response response = protocolAdaptersResource.updateAdapterDomainTags(adapterId, newTagList);

        assertEquals(409, response.getStatus());
    }

    @Test
    void testBulkUpdateRemovingTagNotInUse() {
        when(protocolAdapterWritingService.writingEnabled()).thenReturn(false);

        final String adapterId = "test-adapter";
        final String tag1Name = "temperature";
        final String tag2Name = "pressure";

        final com.hivemq.configuration.entity.adapter.TagEntity tag1Entity =
                new com.hivemq.configuration.entity.adapter.TagEntity(
                        tag1Name, "description", Map.of("address", "test1"));

        final com.hivemq.configuration.entity.adapter.TagEntity tag2Entity =
                new com.hivemq.configuration.entity.adapter.TagEntity(
                        tag2Name, "description", Map.of("address", "test2"));

        final ProtocolAdapterEntity adapterEntity = new ProtocolAdapterEntity(
                adapterId, "opcua", 1, Map.of(), List.of(), List.of(), List.of(tag1Entity, tag2Entity));

        when(protocolAdapterExtractor.getAdapterByAdapterId(adapterId)).thenReturn(Optional.of(adapterEntity));
        when(protocolAdapterExtractor.updateAdapter(any())).thenReturn(true);

        // Only include tag2 in the new list (removing tag1 which is NOT in use)
        final DomainTagList newTagList = new DomainTagList()
                .items(List.of(new com.hivemq.edge.api.model.DomainTag()
                        .name(tag2Name)
                        .description("description")
                        .definition(objectMapper.valueToTree(Map.of("address", "test2")))));

        final Response response = protocolAdaptersResource.updateAdapterDomainTags(adapterId, newTagList);

        assertEquals(200, response.getStatus());
    }

    @Test
    void addAdapterDomainTag_whenSameTagNameExistsInDifferentAdapter_thenReturn200() {
        when(protocolAdapterWritingService.writingEnabled()).thenReturn(false);

        // First adapter with tag "temperature"
        final var protocolAdapterEntity1 = mock(ProtocolAdapterEntity.class);
        when(protocolAdapterEntity1.getTags()).thenReturn(List.of(new TagEntity("temperature",
                "description1",
                Map.of("address", "address1"))));

        // Second adapter trying to add the same tag name "temperature"
        final var protocolAdapterEntity2 = mock(ProtocolAdapterEntity.class);
        when(protocolAdapterEntity2.getTags()).thenReturn(List.of(new TagEntity("pressure",
                "description2",
                Map.of("address", "address2"))));

        when(protocolAdapterExtractor.getAdapterByAdapterId("adapter1")).thenReturn(Optional.of(protocolAdapterEntity1));
        when(protocolAdapterExtractor.getAdapterByAdapterId("adapter2")).thenReturn(Optional.of(protocolAdapterEntity2));
        when(protocolAdapterExtractor.updateAdapter(any())).thenReturn(true);

        when(protocolAdapterManager.addDomainTag(eq("adapter2"), any())).thenReturn(DomainTagAddResult.success());

        // Add tag "temperature" to adapter2 (same name as in adapter1)
        final Response response = protocolAdaptersResource.addAdapterDomainTags("adapter2",
                new DomainTag("temperature",
                        "adapter2",
                        "description2",
                        objectMapper.valueToTree(Map.of("address", "address2"))).toModel());

        assertEquals(200, response.getStatus());
    }

    @Test
    void updateDomainTag_whenSameTagNameExistsInDifferentAdapter_thenReturn200() {
        when(protocolAdapterWritingService.writingEnabled()).thenReturn(false);

        // First adapter with tag "temperature"
        final var protocolAdapterEntity1 = mock(ProtocolAdapterEntity.class);
        when(protocolAdapterEntity1.getTags()).thenReturn(List.of(new TagEntity("temperature",
                "description1",
                Map.of("address", "address1"))));

        // Second adapter updating a tag to same name "temperature"
        final var protocolAdapterEntity2 = mock(ProtocolAdapterEntity.class);
        when(protocolAdapterEntity2.getTags()).thenReturn(List.of(new TagEntity("pressure",
                "description2",
                Map.of("address", "address2"))));

        when(protocolAdapterExtractor.getAdapterByAdapterId("adapter1")).thenReturn(Optional.of(protocolAdapterEntity1));
        when(protocolAdapterExtractor.getAdapterByAdapterId("adapter2")).thenReturn(Optional.of(protocolAdapterEntity2));
        when(protocolAdapterExtractor.updateAdapter(any())).thenReturn(true);

        // Update tag "pressure" to "temperature" in adapter2 (same name as in adapter1)
        final Response response = protocolAdaptersResource.updateAdapterDomainTag("adapter2",
                URLEncoder.encode("pressure", StandardCharsets.UTF_8),
                new DomainTag("temperature",
                        "adapter2",
                        "description2",
                        objectMapper.valueToTree(Map.of("address", "address2"))).toModel());

        assertEquals(200, response.getStatus());
    }

    @Test
    void testBulkUpdateWithSameTagNamesAcrossAdapters() {
        when(protocolAdapterWritingService.writingEnabled()).thenReturn(false);

        // First adapter with tags "temperature" and "pressure"
        final String adapter1Id = "adapter1";
        final com.hivemq.configuration.entity.adapter.TagEntity tag1EntityA =
                new com.hivemq.configuration.entity.adapter.TagEntity("temperature",
                        "description1",
                        Map.of("address", "test1"));
        final com.hivemq.configuration.entity.adapter.TagEntity tag2EntityA =
                new com.hivemq.configuration.entity.adapter.TagEntity("pressure",
                        "description2",
                        Map.of("address", "test2"));

        final ProtocolAdapterEntity adapterEntity1 = new ProtocolAdapterEntity(adapter1Id,
                "opcua",
                1,
                Map.of(),
                List.of(),
                List.of(),
                List.of(tag1EntityA, tag2EntityA));

        // Second adapter with same tag names "temperature" and "pressure"
        final String adapter2Id = "adapter2";
        final com.hivemq.configuration.entity.adapter.TagEntity tag1EntityB =
                new com.hivemq.configuration.entity.adapter.TagEntity("humidity",
                        "description3",
                        Map.of("address", "test3"));

        final ProtocolAdapterEntity adapterEntity2 = new ProtocolAdapterEntity(adapter2Id,
                "modbus",
                1,
                Map.of(),
                List.of(),
                List.of(),
                List.of(tag1EntityB));

        when(protocolAdapterExtractor.getAdapterByAdapterId(adapter1Id)).thenReturn(Optional.of(adapterEntity1));
        when(protocolAdapterExtractor.getAdapterByAdapterId(adapter2Id)).thenReturn(Optional.of(adapterEntity2));
        when(protocolAdapterExtractor.updateAdapter(any())).thenReturn(true);

        // Update adapter2 to have the same tag names as adapter1
        final DomainTagList newTagList =
                new DomainTagList().items(List.of(new com.hivemq.edge.api.model.DomainTag().name("temperature")
                                .description("description")
                                .definition(objectMapper.valueToTree(Map.of("address", "test1"))),
                        new com.hivemq.edge.api.model.DomainTag().name("pressure")
                                .description("description")
                                .definition(objectMapper.valueToTree(Map.of("address", "test2")))));

        final Response response = protocolAdaptersResource.updateAdapterDomainTags(adapter2Id, newTagList);

        assertEquals(200, response.getStatus());
    }
}
