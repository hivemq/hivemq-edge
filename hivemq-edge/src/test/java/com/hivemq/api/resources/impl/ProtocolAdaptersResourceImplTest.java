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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.configuration.entity.adapter.ProtocolAdapterEntity;
import com.hivemq.configuration.entity.adapter.TagEntity;
import com.hivemq.configuration.info.SystemInformation;
import com.hivemq.configuration.reader.ProtocolAdapterExtractor;
import com.hivemq.configuration.service.ConfigurationService;
import com.hivemq.edge.HiveMQEdgeRemoteService;
import com.hivemq.edge.VersionProvider;
import com.hivemq.edge.api.model.DomainTagList;
import com.hivemq.persistence.domain.DomainTag;
import com.hivemq.persistence.domain.DomainTagAddResult;
import com.hivemq.persistence.topicfilter.TopicFilterPersistence;
import com.hivemq.protocols.InternalProtocolAdapterWritingService;
import com.hivemq.protocols.ProtocolAdapterConfigConverter;
import com.hivemq.protocols.ProtocolAdapterManager;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.core.Response;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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


    private final ProtocolAdaptersResourceImpl protocolAdaptersResource =
            new ProtocolAdaptersResourceImpl(
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
            domainTags.add(new DomainTag("tag" + i,
                    "1",
                    "description",
                    objectMapper.valueToTree(Map.of("address", "addressy"))));
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
                .thenReturn(List.of(new TagEntity("tagExists","description", Map.of("address", "addressy1"))));

        when(protocolAdapterExtractor.getAdapterByAdapterId("adapter")).thenReturn(Optional.of(protocolAdapterEntity));
        when(protocolAdapterExtractor.updateAdapter(any())).thenReturn(true);

        when(protocolAdapterManager.addDomainTag(eq("adapter"), any())).thenReturn(DomainTagAddResult.success());

        final Response response = protocolAdaptersResource.addAdapterDomainTags("adapter",
                (new DomainTag("tag",
                        "1",
                        "description",
                        objectMapper.valueToTree(Map.of("address", "addressy"))).toModel()));

        assertEquals(200, response.getStatus());
    }

    @Test
    void addAdapterDomainTag_whenAlreadyExists_thenReturn403() {
        when(protocolAdapterWritingService.writingEnabled()).thenReturn(false);

        var protocolAdapterEntity = mock(ProtocolAdapterEntity.class);
        when(protocolAdapterEntity.getTags())
                .thenReturn(List.of(new TagEntity("tag","description", Map.of("address", "addressy1"))));

        when(protocolAdapterExtractor.getAdapterByAdapterId("adapter")).thenReturn(Optional.of(protocolAdapterEntity));
        when(protocolAdapterExtractor.updateAdapter(any())).thenReturn(false);

        final Response response = protocolAdaptersResource.addAdapterDomainTags("adapter",
                new DomainTag("tag",
                        "1",
                        "description",
                        objectMapper.valueToTree(Map.of("address", "addressy"))).toModel());

        assertEquals(403, response.getStatus());
    }

    @Test
    void deleteDomainTag_whenTagExists_thenReturn200() {
        when(protocolAdapterWritingService.writingEnabled()).thenReturn(false);
        when(protocolAdapterExtractor.updateAdapter(any())).thenReturn(true);

        var protocolAdapterEntity = mock(ProtocolAdapterEntity.class);
        when(protocolAdapterEntity.getTags())
                .thenReturn(List.of(new TagEntity("tag","description", Map.of("address", "addressy1"))));

        when(protocolAdapterExtractor.getAdapterByAdapterId("adapter")).thenReturn(Optional.of(protocolAdapterEntity));

        final Response response =
                protocolAdaptersResource.deleteAdapterDomainTags("adapter", URLEncoder.encode("tag", StandardCharsets.UTF_8));

        assertEquals(200, response.getStatus());

    }

    @Test
    void deleteDomainTag_whenTagDoesNotExists_thenReturn403() {
        when(protocolAdapterWritingService.writingEnabled()).thenReturn(false);
        when(protocolAdapterExtractor.updateAdapter(any())).thenReturn(false);

        final Response response =
                protocolAdaptersResource.deleteAdapterDomainTags("adapter", URLEncoder.encode("tag", StandardCharsets.UTF_8));

        assertEquals(404, response.getStatus());

    }

    @Test
    void updateDomainTag_whenTagExists_thenReturn200() {
        when(protocolAdapterWritingService.writingEnabled()).thenReturn(false);

        var protocolAdapterEntity = mock(ProtocolAdapterEntity.class);
        when(protocolAdapterEntity.getTags())
                .thenReturn(List.of(new TagEntity("tag","description", Map.of("address", "addressy1"))));

        when(protocolAdapterExtractor.getAdapterByAdapterId("adapter")).thenReturn(Optional.of(protocolAdapterEntity));
        when(protocolAdapterExtractor.updateAdapter(any())).thenReturn(true);

        final Response response = protocolAdaptersResource.updateAdapterDomainTag("adapter",
                URLEncoder.encode("tag", StandardCharsets.UTF_8),
                new DomainTag("tag",
                        "1",
                        "description",
                        objectMapper.valueToTree(Map.of("address", "addressy"))).toModel());

        assertEquals(200, response.getStatus());
    }

    @Test
    void updateDomainTag_whenTagDoesNotExists_thenReturn400() {
        when(protocolAdapterWritingService.writingEnabled()).thenReturn(false);
        when(protocolAdapterExtractor.updateAdapter(any())).thenReturn(false);

        final Response response = protocolAdaptersResource.updateAdapterDomainTag("adapter",
                Base64.getEncoder().encodeToString("tag".getBytes(StandardCharsets.UTF_8)),
                new DomainTag("tag",
                        "1",
                        "description",
                        objectMapper.valueToTree(Map.of("address", "addressy"))).toModel());

        assertEquals(403, response.getStatus());
    }

    @Test
    void getDomainTags() {
        when(protocolAdapterWritingService.writingEnabled()).thenReturn(false);
        final ArrayList<DomainTag> domainTags = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            domainTags.add(new DomainTag("tag" + i,
                    "1",
                    "description",
                    objectMapper.valueToTree(Map.of("address", "addressy"))));
        }

        when(protocolAdapterManager.getDomainTags()).thenReturn(domainTags);

        final Response response = protocolAdaptersResource.getDomainTags();

        final Object entity = response.getEntity();

        assertInstanceOf(DomainTagList.class, entity);
        final DomainTagList domainTagList = (DomainTagList) entity;
        assertEquals(domainTags.size(), domainTagList.getItems().size());
        for (int i = 0; i < domainTags.size(); i++) {
            final DomainTag domainTag = domainTags.get(i);
            assertEquals(domainTag.toModel(), domainTagList.getItems().get(i));
        }
    }
}
