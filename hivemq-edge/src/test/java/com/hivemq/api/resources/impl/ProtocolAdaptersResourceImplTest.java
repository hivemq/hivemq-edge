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
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.adapter.sdk.api.ProtocolAdapter;
import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.config.ProtocolSpecificAdapterConfig;
import com.hivemq.adapter.sdk.api.factories.ProtocolAdapterFactory;
import com.hivemq.adapter.sdk.api.schema.ScalarType;
import com.hivemq.adapter.sdk.api.schema.SchemaBuilder;
import com.hivemq.adapter.sdk.api.schema.TagSchemaCreationOutput;
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
import com.hivemq.edge.api.model.Adapter;
import com.hivemq.edge.api.model.AdapterConfig;
import com.hivemq.edge.api.model.DomainTagList;
import com.hivemq.edge.api.model.DomainTagOwnerList;
import com.hivemq.edge.api.model.FieldMapping;
import com.hivemq.edge.api.model.NorthboundMappingOwner;
import com.hivemq.edge.api.model.NorthboundMappingOwnerList;
import com.hivemq.edge.api.model.SouthboundMappingOwner;
import com.hivemq.edge.api.model.SouthboundMappingOwnerList;
import com.hivemq.http.error.ProblemDetails;
import com.hivemq.persistence.domain.DomainTag;
import com.hivemq.persistence.domain.DomainTagAddResult;
import com.hivemq.persistence.topicfilter.TopicFilterPersistence;
import com.hivemq.protocols.InternalProtocolAdapterWritingService;
import com.hivemq.protocols.ProtocolAdapterConfigConverter;
import com.hivemq.protocols.ProtocolAdapterFactoryManager;
import com.hivemq.protocols.ProtocolAdapterManager;
import com.hivemq.protocols.ProtocolAdapterWrapper;
import jakarta.ws.rs.core.Response;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProtocolAdaptersResourceImplTest {

    private final @NotNull HiveMQEdgeRemoteService remoteService = mock();
    private final @NotNull ConfigurationService configurationService = mock();
    private final @NotNull ProtocolAdapterManager protocolAdapterManager = mock();
    private final @NotNull InternalProtocolAdapterWritingService protocolAdapterWritingService = mock();
    private final @NotNull ObjectMapper objectMapper = new ObjectMapper();
    private final @NotNull VersionProvider versionProvider = mock();
    private final @NotNull TopicFilterPersistence topicFilterPersistence = mock();
    private final @NotNull SystemInformation systemInformation = mock();
    private final @NotNull ProtocolAdapterExtractor protocolAdapterExtractor = mock();
    private final @NotNull ProtocolAdapterFactoryManager protocolAdapterFactoryManager = mock();
    private final @NotNull ProtocolAdapterConfigConverter configConverter =
            new ProtocolAdapterConfigConverter(protocolAdapterFactoryManager, new ObjectMapper());

    private final ProtocolAdaptersResourceImpl protocolAdaptersResource = new ProtocolAdaptersResourceImpl(
            remoteService,
            configurationService,
            protocolAdapterManager,
            protocolAdapterWritingService,
            objectMapper,
            versionProvider,
            topicFilterPersistence,
            systemInformation,
            protocolAdapterExtractor,
            configConverter);

    @BeforeEach
    public void setUp() {
        when(systemInformation.isConfigWriteable()).thenReturn(true);
    }

    /**
     * EDG-891 P5. The duplicate-id guard was reported as unreachable over REST, on the evidence that a
     * second create returned only {@code "Invalid user supplied data"}. The guard does fire — the
     * finding shares its root cause with P2: the friendly text was carried as the error's *detail*, and
     * only the *title* was mapped onto the wire model, so the message was built and then dropped at the
     * boundary. Fixed with P2; pinned here against the resource so the two cannot drift apart again.
     */
    @Test
    void addAdapter_whenTheIdIsAlreadyTaken_thenTheCallerIsToldItMustBeUnique() {
        when(protocolAdapterManager.getAdapterTypeById("opcua"))
                .thenReturn(Optional.of(mock(ProtocolAdapterInformation.class)));
        when(protocolAdapterExtractor.getAdapterByAdapterId("taken"))
                .thenReturn(Optional.of(mock(ProtocolAdapterEntity.class)));

        final Response response = protocolAdaptersResource.addAdapter("opcua", new Adapter("taken"));

        assertEquals(400, response.getStatus());
        final ProblemDetails problem = assertInstanceOf(ProblemDetails.class, response.getEntity());
        assertThat(problem.getErrors())
                .extracting(com.hivemq.http.error.Error::getDetail)
                .as("the caller must be told why the id was refused, not merely that something was invalid")
                .containsExactly("Invalid user supplied data: Adapter ID must be unique in system");
        assertThat(problem.getErrors())
                .extracting(com.hivemq.http.error.Error::getParameter)
                .containsExactly("id");
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
                new DomainTag("tag", "1", "description", objectMapper.valueToTree(Map.of("address", "addressy")))
                        .toModel());

        assertEquals(200, response.getStatus());
    }

    @Test
    void addAdapterDomainTag_whenAlreadyExists_thenReturn409() {
        when(protocolAdapterWritingService.writingEnabled()).thenReturn(false);

        var protocolAdapterEntity = mock(ProtocolAdapterEntity.class);
        when(protocolAdapterEntity.getTags())
                .thenReturn(List.of(new TagEntity("tag", "description", Map.of("address", "addressy1"))));

        when(protocolAdapterExtractor.getAdapterByAdapterId("adapter")).thenReturn(Optional.of(protocolAdapterEntity));

        final Response response = protocolAdaptersResource.addAdapterDomainTags(
                "adapter",
                new DomainTag("tag", "1", "description", objectMapper.valueToTree(Map.of("address", "addressy")))
                        .toModel());

        assertEquals(409, response.getStatus());
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
                                                false,
                                                List.of(),
                                                1234L + i),
                                        new NorthboundMappingEntity(
                                                "tagName" + i + ".b",
                                                "topic" + i + ".b",
                                                1,
                                                null,
                                                false,
                                                true,
                                                false,
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
                        assertThat(item.getTagName()).isEqualTo("tagName" + (i / 2) + "." + (i % 2 == 0 ? "a" : "b"));
                        assertThat(item.getTopic()).isEqualTo("topic" + (i / 2) + "." + (i % 2 == 0 ? "a" : "b"));
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
                        assertThat(item.getTagName()).isEqualTo("tagName" + (i / 2) + "." + (i % 2 == 0 ? "a" : "b"));
                        assertThat(item.getTopicFilter())
                                .isEqualTo("topicFilter" + (i / 2) + "." + (i % 2 == 0 ? "a" : "b"));
                        final FieldMapping fieldMapping = item.getFieldMapping();
                        assertThat(fieldMapping.getInstructions()).hasSize(1);
                        assertThat(fieldMapping.getInstructions().getFirst().getSource())
                                .isEqualTo("sourceFieldName" + (i / 2) + "." + (i % 2 == 0 ? "a" : "b"));
                        assertThat(fieldMapping.getInstructions().getFirst().getDestination())
                                .isEqualTo("destinationFieldName" + (i / 2) + "." + (i % 2 == 0 ? "a" : "b"));
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
                        tagName, "test/topic", 1, null, false, true, false, List.of(), null);

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
                        tagName, "sensors/data", 1, null, false, true, false, List.of(), null);

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
                        oldTagName, "test/topic", 1, null, false, true, false, List.of(), null);

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
                        tagName, "test/topic", 1, null, false, true, false, List.of(), null);

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
                        tag1Name, "test/topic", 1, null, false, true, false, List.of(), null);

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
        when(protocolAdapterEntity1.getTags())
                .thenReturn(List.of(new TagEntity("temperature", "description1", Map.of("address", "address1"))));

        // Second adapter trying to add the same tag name "temperature"
        final var protocolAdapterEntity2 = mock(ProtocolAdapterEntity.class);
        when(protocolAdapterEntity2.getTags())
                .thenReturn(List.of(new TagEntity("pressure", "description2", Map.of("address", "address2"))));

        when(protocolAdapterExtractor.getAdapterByAdapterId("adapter1"))
                .thenReturn(Optional.of(protocolAdapterEntity1));
        when(protocolAdapterExtractor.getAdapterByAdapterId("adapter2"))
                .thenReturn(Optional.of(protocolAdapterEntity2));
        when(protocolAdapterExtractor.updateAdapter(any())).thenReturn(true);

        when(protocolAdapterManager.addDomainTag(eq("adapter2"), any())).thenReturn(DomainTagAddResult.success());

        // Add tag "temperature" to adapter2 (same name as in adapter1)
        final Response response = protocolAdaptersResource.addAdapterDomainTags(
                "adapter2",
                new DomainTag(
                                "temperature",
                                "adapter2",
                                "description2",
                                objectMapper.valueToTree(Map.of("address", "address2")))
                        .toModel());

        assertEquals(200, response.getStatus());
    }

    @Test
    void updateDomainTag_whenSameTagNameExistsInDifferentAdapter_thenReturn200() {
        when(protocolAdapterWritingService.writingEnabled()).thenReturn(false);

        // First adapter with tag "temperature"
        final var protocolAdapterEntity1 = mock(ProtocolAdapterEntity.class);
        when(protocolAdapterEntity1.getTags())
                .thenReturn(List.of(new TagEntity("temperature", "description1", Map.of("address", "address1"))));

        // Second adapter updating a tag to same name "temperature"
        final var protocolAdapterEntity2 = mock(ProtocolAdapterEntity.class);
        when(protocolAdapterEntity2.getTags())
                .thenReturn(List.of(new TagEntity("pressure", "description2", Map.of("address", "address2"))));

        when(protocolAdapterExtractor.getAdapterByAdapterId("adapter1"))
                .thenReturn(Optional.of(protocolAdapterEntity1));
        when(protocolAdapterExtractor.getAdapterByAdapterId("adapter2"))
                .thenReturn(Optional.of(protocolAdapterEntity2));
        when(protocolAdapterExtractor.updateAdapter(any())).thenReturn(true);

        // Update tag "pressure" to "temperature" in adapter2 (same name as in adapter1)
        final Response response = protocolAdaptersResource.updateAdapterDomainTag(
                "adapter2",
                URLEncoder.encode("pressure", StandardCharsets.UTF_8),
                new DomainTag(
                                "temperature",
                                "adapter2",
                                "description2",
                                objectMapper.valueToTree(Map.of("address", "address2")))
                        .toModel());

        assertEquals(200, response.getStatus());
    }

    @Test
    void testBulkUpdateWithSameTagNamesAcrossAdapters() {
        when(protocolAdapterWritingService.writingEnabled()).thenReturn(false);

        // First adapter with tags "temperature" and "pressure"
        final String adapter1Id = "adapter1";
        final com.hivemq.configuration.entity.adapter.TagEntity tag1EntityA =
                new com.hivemq.configuration.entity.adapter.TagEntity(
                        "temperature", "description1", Map.of("address", "test1"));
        final com.hivemq.configuration.entity.adapter.TagEntity tag2EntityA =
                new com.hivemq.configuration.entity.adapter.TagEntity(
                        "pressure", "description2", Map.of("address", "test2"));

        final ProtocolAdapterEntity adapterEntity1 = new ProtocolAdapterEntity(
                adapter1Id, "opcua", 1, Map.of(), List.of(), List.of(), List.of(tag1EntityA, tag2EntityA));

        // Second adapter with same tag names "temperature" and "pressure"
        final String adapter2Id = "adapter2";
        final com.hivemq.configuration.entity.adapter.TagEntity tag1EntityB =
                new com.hivemq.configuration.entity.adapter.TagEntity(
                        "humidity", "description3", Map.of("address", "test3"));

        final ProtocolAdapterEntity adapterEntity2 = new ProtocolAdapterEntity(
                adapter2Id, "modbus", 1, Map.of(), List.of(), List.of(), List.of(tag1EntityB));

        when(protocolAdapterExtractor.getAdapterByAdapterId(adapter1Id)).thenReturn(Optional.of(adapterEntity1));
        when(protocolAdapterExtractor.getAdapterByAdapterId(adapter2Id)).thenReturn(Optional.of(adapterEntity2));
        when(protocolAdapterExtractor.updateAdapter(any())).thenReturn(true);

        // Update adapter2 to have the same tag names as adapter1
        final DomainTagList newTagList = new DomainTagList()
                .items(List.of(
                        new com.hivemq.edge.api.model.DomainTag()
                                .name("temperature")
                                .description("description")
                                .definition(objectMapper.valueToTree(Map.of("address", "test1"))),
                        new com.hivemq.edge.api.model.DomainTag()
                                .name("pressure")
                                .description("description")
                                .definition(objectMapper.valueToTree(Map.of("address", "test2")))));

        final Response response = protocolAdaptersResource.updateAdapterDomainTags(adapter2Id, newTagList);

        assertEquals(200, response.getStatus());
    }

    @Test
    void updateDomainTag_whenRenamingToDuplicateName_thenReturn409() {
        when(protocolAdapterWritingService.writingEnabled()).thenReturn(false);

        final String adapterId = "test-adapter";

        // Adapter has two tags: "tag1" and "tag2"
        final var protocolAdapterEntity = mock(ProtocolAdapterEntity.class);
        when(protocolAdapterEntity.getAdapterId()).thenReturn(adapterId);
        when(protocolAdapterEntity.getTags())
                .thenReturn(List.of(
                        new TagEntity("tag1", "description1", Map.of("address", "address1")),
                        new TagEntity("tag2", "description2", Map.of("address", "address2"))));
        when(protocolAdapterEntity.getNorthboundMappings()).thenReturn(List.of());
        when(protocolAdapterEntity.getSouthboundMappings()).thenReturn(List.of());

        when(protocolAdapterExtractor.getAdapterByAdapterId(adapterId)).thenReturn(Optional.of(protocolAdapterEntity));

        // Try to rename "tag1" to "tag2" (which already exists)
        final Response response = protocolAdaptersResource.updateAdapterDomainTag(
                adapterId,
                URLEncoder.encode("tag1", StandardCharsets.UTF_8),
                new DomainTag(
                                "tag2",
                                adapterId,
                                "new description",
                                objectMapper.valueToTree(Map.of("address", "address1")))
                        .toModel());

        assertEquals(409, response.getStatus());
    }

    @Test
    void updateDomainTags_whenDuplicateTagNamesInList_thenReturn409() {
        when(protocolAdapterWritingService.writingEnabled()).thenReturn(false);

        final String adapterId = "test-adapter";

        final var protocolAdapterEntity = mock(ProtocolAdapterEntity.class);
        when(protocolAdapterEntity.getAdapterId()).thenReturn(adapterId);
        when(protocolAdapterEntity.getTags())
                .thenReturn(List.of(new TagEntity("tag1", "description1", Map.of("address", "address1"))));

        when(protocolAdapterExtractor.getAdapterByAdapterId(adapterId)).thenReturn(Optional.of(protocolAdapterEntity));

        // Try to update with a list containing duplicate tag names
        final DomainTagList duplicateTagList = new DomainTagList()
                .items(List.of(
                        new com.hivemq.edge.api.model.DomainTag()
                                .name("temperature")
                                .description("desc1")
                                .definition(objectMapper.valueToTree(Map.of("address", "addr1"))),
                        new com.hivemq.edge.api.model.DomainTag()
                                .name("temperature") // Duplicate!
                                .description("desc2")
                                .definition(objectMapper.valueToTree(Map.of("address", "addr2")))));

        final Response response = protocolAdaptersResource.updateAdapterDomainTags(adapterId, duplicateTagList);

        assertEquals(409, response.getStatus());
    }

    @Test
    void updateDomainTags_whenNoDuplicateTagNames_thenReturn200() {
        when(protocolAdapterWritingService.writingEnabled()).thenReturn(false);

        final String adapterId = "test-adapter";

        final var protocolAdapterEntity = mock(ProtocolAdapterEntity.class);
        when(protocolAdapterEntity.getAdapterId()).thenReturn(adapterId);
        when(protocolAdapterEntity.getTags())
                .thenReturn(List.of(new TagEntity("tag1", "description1", Map.of("address", "address1"))));
        when(protocolAdapterEntity.getNorthboundMappings()).thenReturn(List.of());
        when(protocolAdapterEntity.getSouthboundMappings()).thenReturn(List.of());

        when(protocolAdapterExtractor.getAdapterByAdapterId(adapterId)).thenReturn(Optional.of(protocolAdapterEntity));
        when(protocolAdapterExtractor.updateAdapter(any())).thenReturn(true);

        // Update with a list containing unique tag names
        final DomainTagList uniqueTagList = new DomainTagList()
                .items(List.of(
                        new com.hivemq.edge.api.model.DomainTag()
                                .name("temperature")
                                .description("desc1")
                                .definition(objectMapper.valueToTree(Map.of("address", "addr1"))),
                        new com.hivemq.edge.api.model.DomainTag()
                                .name("pressure")
                                .description("desc2")
                                .definition(objectMapper.valueToTree(Map.of("address", "addr2")))));

        final Response response = protocolAdaptersResource.updateAdapterDomainTags(adapterId, uniqueTagList);

        assertEquals(200, response.getStatus());
    }

    private void mockAdapterWithTagSchema(final @NotNull String adapterId) {
        final ProtocolAdapterWrapper wrapper = mock();
        final ProtocolAdapter adapter = mock();
        when(wrapper.getAdapter()).thenReturn(adapter);
        doAnswer(invocation -> {
                    final TagSchemaCreationOutput output = invocation.getArgument(1);
                    output.finish(new TagSchemaCreationOutput.DataPointSchema(
                            new SchemaBuilder()
                                    .scalar(ScalarType.LONG)
                                    .title("RPM")
                                    .build(),
                            null,
                            null));
                    return null;
                })
                .when(adapter)
                .createTagSchema(any(), any());
        when(protocolAdapterManager.getProtocolAdapterWrapperByAdapterId(adapterId))
                .thenReturn(Optional.of(wrapper));
    }

    @Test
    void getSchema_whenDirectionOmitted_thenReturnsTheNorthboundSchema() {
        mockAdapterWithTagSchema("adapter");

        final Response response = protocolAdaptersResource.getSchema("adapter", "tag", null);

        assertEquals(200, response.getStatus());
        final JsonNode schema = (JsonNode) response.getEntity();
        assertThat(schema.get("properties").has("tagName")).isTrue();
        assertThat(schema.get("properties").has("value")).isTrue();
    }

    @Test
    void getSchema_whenDirectionIsSouthbound_thenReturnsTheEnvelopeFreeSchema() {
        mockAdapterWithTagSchema("adapter");

        final Response response = protocolAdaptersResource.getSchema("adapter", "tag", "SOUTHBOUND");

        assertEquals(200, response.getStatus());
        final JsonNode schema = (JsonNode) response.getEntity();
        assertThat(schema.get("properties").has("tagName")).isFalse();
        assertThat(schema.get("properties").has("value")).isTrue();
    }

    @Test
    void getSchema_whenDirectionIsUnknown_thenReturns400() {
        // A typo must fail loudly: silently defaulting to NORTHBOUND would hand the caller a
        // differently-shaped document with no signal.
        final Response response = protocolAdaptersResource.getSchema("adapter", "tag", "SOUTBOUND");

        assertEquals(400, response.getStatus());
    }

    @Test
    void getSchema_whenDirectionIsLowercase_thenReturns400() {
        // The OpenAPI enum is uppercase-only; the implementation must not be more lenient than the contract.
        final Response response = protocolAdaptersResource.getSchema("adapter", "tag", "southbound");

        assertEquals(400, response.getStatus());
    }

    @Test
    void getWritingSchema_redirectsWithTheSouthboundDirection() {
        final Response response = protocolAdaptersResource.getWritingSchema("adapter", "tag");

        assertEquals(301, response.getStatus());
        assertThat(response.getLocation().toString())
                .isEqualTo("/api/v1/management/protocol-adapters/schema/adapter/tag?direction=SOUTHBOUND");
    }

    /**
     * EDG-894 P7: when schema generation fails, the 500 must say what the adapter said.
     * <p>
     * {@code TagSchemaCreationOutput.fail(String)} records the adapter's reason on the output and completes the
     * future with a fixed {@code "Json schema creation for tag failed."}. This layer read only the cause, so the
     * entire body of the 500 was that fixed sentence — and every schema failure in the OPC UA adapter takes that
     * route. QA met it as an unexplained 500 on an ordinary VALUE tag and could not tell from the response
     * whether the fault was in schema generation or in the {@code direction} parameter that had just been added,
     * so it was reported as a possible API compatibility break. The adapter had in fact said its connection was
     * not established, and nothing carried that sentence to the caller.
     */
    @Test
    void getSchema_whenTheAdapterGivesAReason_thenTheResponseCarriesIt() {
        mockAdapterFailingSchemaWith("Discovery failed: ClientConnection not connected or not initialized");

        final Response response = protocolAdaptersResource.getSchema("adapter", "tag", null);

        assertEquals(500, response.getStatus());
        assertThat(response.getEntity().toString())
                .as("the operator has to be told which condition stopped the schema being built")
                .contains("ClientConnection not connected")
                .doesNotContain("Json schema creation for tag failed.");
    }

    @Test
    void getSchema_whenTheAdapterGivesNoReason_thenTheCauseIsStillReported() {
        // fail(Throwable, null) leaves no message on the output, and there the throwable is the only account
        // there is. Preferring the adapter's reason must not mean discarding the cause when there isn't one.
        final ProtocolAdapterWrapper wrapper = mock();
        final ProtocolAdapter adapter = mock();
        when(wrapper.getAdapter()).thenReturn(adapter);
        doAnswer(invocation -> {
                    final TagSchemaCreationOutput output = invocation.getArgument(1);
                    output.fail(new IllegalStateException("the node is not readable"), null);
                    return null;
                })
                .when(adapter)
                .createTagSchema(any(), any());
        when(protocolAdapterManager.getProtocolAdapterWrapperByAdapterId("adapter"))
                .thenReturn(Optional.of(wrapper));

        final Response response = protocolAdaptersResource.getSchema("adapter", "tag", null);

        assertEquals(500, response.getStatus());
        assertThat(response.getEntity().toString()).contains("the node is not readable");
    }

    @Test
    void getSchema_withoutDirection_takesTheSamePathAsNorthbound() {
        // The other half of P7, and the half that refutes its stated hypothesis. The finding could not reach the
        // NORTHBOUND call to compare, and offered "if NORTHBOUND succeeds, the defect is the default direction".
        // It cannot: an absent direction resolves to NORTHBOUND, and createTagSchema runs and fails before the
        // direction is consulted at all, so the two answers are identical whether the adapter succeeds or fails.
        mockAdapterFailingSchemaWith("Discovery failed: ClientConnection not connected or not initialized");
        final Response withoutDirection = protocolAdaptersResource.getSchema("adapter", "tag", null);

        mockAdapterFailingSchemaWith("Discovery failed: ClientConnection not connected or not initialized");
        final Response northbound = protocolAdaptersResource.getSchema("adapter", "tag", "NORTHBOUND");

        assertEquals(withoutDirection.getStatus(), northbound.getStatus());
        assertThat(withoutDirection.getEntity().toString())
                .isEqualTo(northbound.getEntity().toString());

        mockAdapterWithTagSchema("adapter");
        final Response okWithout = protocolAdaptersResource.getSchema("adapter", "tag", null);
        mockAdapterWithTagSchema("adapter");
        final Response okNorthbound = protocolAdaptersResource.getSchema("adapter", "tag", "NORTHBOUND");

        assertEquals(200, okWithout.getStatus());
        assertThat((JsonNode) okWithout.getEntity()).isEqualTo((JsonNode) okNorthbound.getEntity());
    }

    private void mockAdapterFailingSchemaWith(final @NotNull String reason) {
        final ProtocolAdapterWrapper wrapper = mock();
        final ProtocolAdapter adapter = mock();
        when(wrapper.getAdapter()).thenReturn(adapter);
        doAnswer(invocation -> {
                    final TagSchemaCreationOutput output = invocation.getArgument(1);
                    output.fail(reason);
                    return null;
                })
                .when(adapter)
                .createTagSchema(any(), any());
        when(protocolAdapterManager.getProtocolAdapterWrapperByAdapterId("adapter"))
                .thenReturn(Optional.of(wrapper));
    }

    // ---------------------------------------------------------------------------------------------
    // A configuration that will not convert must be refused while the caller is still there.
    //
    // The write is answered before the configuration is read: the extractor notifies
    // ProtocolAdapterManager.refresh, which converts on its own executor. A configuration that does not
    // convert used to be answered 200 and then never appear - every later GET a 404, for good, and the
    // reason only in Edge's log and event stream. Anything polling for the adapter burns its whole
    // timeout and then reports that timeout, which names nothing about the configuration.
    // ---------------------------------------------------------------------------------------------

    @Test
    void addAdapter_whenTheConfigurationDoesNotConvert_thenTheCallerIsToldWhyAndNothingIsWritten() {
        mockConvertibleAdapterType("refusing");

        final Response response = protocolAdaptersResource.addAdapter(
                "refusing", adapterModel("new-adapter", "refusing", Map.of("publishChangedDataOnly", true)));

        assertEquals(400, response.getStatus());
        final ProblemDetails problem = assertInstanceOf(ProblemDetails.class, response.getEntity());
        assertThat(problem.getErrors())
                .extracting(com.hivemq.http.error.Error::getDetail)
                .as("the caller must be told which setting was refused, not merely that something was invalid")
                .allSatisfy(detail -> assertThat(detail).contains("'publishChangedDataOnly'"));
        verify(protocolAdapterExtractor, never()).addAdapter(any());
    }

    @Test
    void addAdapter_whenTheConfigurationConverts_thenItIsWritten() {
        mockConvertibleAdapterType("refusing");
        when(protocolAdapterExtractor.addAdapter(any())).thenReturn(true);

        final Response response = protocolAdaptersResource.addAdapter(
                "refusing", adapterModel("new-adapter", "refusing", Map.of("hostname", "machine-1")));

        assertEquals(200, response.getStatus());
        verify(protocolAdapterExtractor).addAdapter(any());
    }

    @Test
    void updateAdapter_whenTheConfigurationDoesNotConvert_thenTheCallerIsToldWhyAndNothingIsWritten() {
        // This path never validated the payload at all, so an unreadable configuration reached the
        // configuration file unopposed and took the running adapter's next reload with it.
        final ProtocolAdapterEntity existing = existingEntity("existing", "refusing");
        when(protocolAdapterExtractor.getAdapterByAdapterId("existing")).thenReturn(Optional.of(existing));

        final Response response = protocolAdaptersResource.updateAdapter(
                "existing", adapterModel("existing", "refusing", Map.of("publishChangedDataOnly", true)));

        assertEquals(400, response.getStatus());
        final ProblemDetails problem = assertInstanceOf(ProblemDetails.class, response.getEntity());
        assertThat(problem.getErrors())
                .extracting(com.hivemq.http.error.Error::getDetail)
                .allSatisfy(detail -> assertThat(detail).contains("'publishChangedDataOnly'"));
        verify(protocolAdapterExtractor, never()).updateAdapter(any());
    }

    @Test
    void updateAdapter_whenTheConfigurationConverts_thenItIsWritten() {
        final ProtocolAdapterEntity existing = existingEntity("existing", "refusing");
        when(protocolAdapterExtractor.getAdapterByAdapterId("existing")).thenReturn(Optional.of(existing));
        when(protocolAdapterExtractor.updateAdapter(any())).thenReturn(true);

        final Response response = protocolAdaptersResource.updateAdapter(
                "existing", adapterModel("existing", "refusing", Map.of("hostname", "machine-1")));

        assertEquals(200, response.getStatus());
        verify(protocolAdapterExtractor).updateAdapter(any());
    }

    @Test
    void createCompleteAdapter_whenTheConfigurationDoesNotConvert_thenTheCallerIsToldWhyAndNothingIsWritten() {
        mockConvertibleAdapterType("refusing");

        final AdapterConfig adapterConfig = new AdapterConfig()
                .config(adapterModel("new-adapter", "refusing", Map.of("publishChangedDataOnly", true)))
                .tags(List.of())
                .northboundMappings(List.of())
                .southboundMappings(List.of());

        final Response response =
                protocolAdaptersResource.createCompleteAdapter("refusing", "new-adapter", adapterConfig);

        assertEquals(400, response.getStatus());
        final ProblemDetails problem = assertInstanceOf(ProblemDetails.class, response.getEntity());
        assertThat(problem.getErrors())
                .extracting(com.hivemq.http.error.Error::getDetail)
                .allSatisfy(detail -> assertThat(detail).contains("'publishChangedDataOnly'"));
        verify(protocolAdapterExtractor, never()).addAdapter(any());
    }

    @Test
    void whenTheFailureIsNested_thenTheReportedFieldIsThePathThroughTheCallersOwnPayload() {
        // The reference chain, not the class that threw: "nested.publishChangedDataOnly" is a path
        // through what the caller wrote, which is the only path they can act on.
        mockConvertibleAdapterType("refusing");

        final Response response = protocolAdaptersResource.addAdapter(
                "refusing",
                adapterModel("new-adapter", "refusing", Map.of("nested", Map.of("publishChangedDataOnly", true))));

        assertEquals(400, response.getStatus());
        final ProblemDetails problem = assertInstanceOf(ProblemDetails.class, response.getEntity());
        assertThat(problem.getErrors())
                .extracting(com.hivemq.http.error.Error::getParameter)
                .containsExactly("nested.publishChangedDataOnly");
    }

    @Test
    void whenTheFailureHasNoPropertyPath_thenItIsReportedAgainstTheConfigurationItself() {
        // No factory for the protocol id: the conversion fails before a single property is reached, so
        // there is no path to name and the complaint belongs to the configuration as a whole.
        mockConvertibleAdapterType("refusing");
        when(protocolAdapterFactoryManager.get("refusing")).thenReturn(Optional.empty());

        final Response response = protocolAdaptersResource.addAdapter(
                "refusing", adapterModel("new-adapter", "refusing", Map.of("hostname", "machine-1")));

        assertEquals(400, response.getStatus());
        final ProblemDetails problem = assertInstanceOf(ProblemDetails.class, response.getEntity());
        assertThat(problem.getErrors())
                .extracting(com.hivemq.http.error.Error::getParameter)
                .containsExactly("config");
        assertThat(problem.getErrors())
                .extracting(com.hivemq.http.error.Error::getDetail)
                .allSatisfy(detail -> assertThat(detail).contains("No Factory was found"));
        verify(protocolAdapterExtractor, never()).addAdapter(any());
    }

    private @NotNull Adapter adapterModel(
            final @NotNull String id, final @NotNull String type, final @NotNull Map<String, Object> config) {
        // The resource casts the config to a LinkedHashMap, which is what Jackson hands it off the wire.
        return new Adapter(id).type(type).config(new LinkedHashMap<>(config));
    }

    private @NotNull ProtocolAdapterEntity existingEntity(
            final @NotNull String adapterId, final @NotNull String protocolId) {
        final ProtocolAdapterEntity entity = mock(ProtocolAdapterEntity.class);
        when(entity.getAdapterId()).thenReturn(adapterId);
        when(entity.getProtocolId()).thenReturn(protocolId);
        when(entity.getTags()).thenReturn(List.of());
        when(entity.getNorthboundMappings()).thenReturn(List.of());
        when(entity.getSouthboundMappings()).thenReturn(List.of());
        mockRefusingFactory(protocolId);
        return entity;
    }

    /**
     * An adapter type whose configuration class refuses a setting it does not have, wired into both the
     * type lookup the resource does and the factory lookup the converter does.
     */
    private void mockConvertibleAdapterType(final @NotNull String protocolId) {
        final ProtocolAdapterInformation information = mock(ProtocolAdapterInformation.class);
        when(protocolAdapterManager.getAdapterTypeById(protocolId)).thenReturn(Optional.of(information));
        when(protocolAdapterManager.getAllAvailableAdapterTypes()).thenReturn(Map.of(protocolId, information));
        //noinspection unchecked
        when((Object) information.configurationClassNorthbound()).thenReturn(RefusingAdapterConfig.class);
        //noinspection unchecked
        when((Object) information.configurationClassNorthAndSouthbound()).thenReturn(RefusingAdapterConfig.class);
        mockRefusingFactory(protocolId);
    }

    private void mockRefusingFactory(final @NotNull String protocolId) {
        final ProtocolAdapterFactory<?> factory = mock(ProtocolAdapterFactory.class);
        when(protocolAdapterFactoryManager.get(protocolId)).thenReturn(Optional.of(factory));
        // Drive the converter's own mapper exactly as a real factory does - the mapper is the point,
        // since it is the one an operator's configuration file goes through.
        when(factory.convertConfigObject(any(), any(), org.mockito.ArgumentMatchers.anyBoolean()))
                .thenAnswer(invocation -> {
                    final ObjectMapper mapper = invocation.getArgument(0);
                    final Map<String, Object> config = invocation.getArgument(1);
                    return mapper.convertValue(config, RefusingAdapterConfig.class);
                });
        when(factory.convertTagDefinitionObjects(any(), any())).thenReturn(List.of());
    }

    /**
     * Stands in for {@code OpcUaSpecificAdapterConfig}, which core cannot reference: the module depends
     * on the adapter SDK, not the other way round. What is pinned here is the mechanism - a config class
     * that refuses a setting it does not have - not OPC UA's particular settings.
     */
    static class RefusingAdapterConfig implements ProtocolSpecificAdapterConfig {

        @JsonProperty("hostname")
        public @Nullable String hostname;

        @JsonProperty("nested")
        public @Nullable RefusingAdapterConfig nested;

        @JsonAnySetter
        void refuseUnknownSetting(final @NotNull String name, final @Nullable Object value) {
            throw new IllegalArgumentException(
                    "The adapter configuration contains '" + name + "', which is not a setting it has.");
        }
    }
}
