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
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterWritingService;
import com.hivemq.api.model.tags.DomainTagModel;
import com.hivemq.api.model.tags.DomainTagModelList;
import com.hivemq.configuration.service.ConfigurationService;
import com.hivemq.edge.HiveMQEdgeRemoteService;
import com.hivemq.edge.VersionProvider;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.persistence.domain.DomainTag;
import com.hivemq.persistence.domain.DomainTagAddResult;
import com.hivemq.persistence.domain.DomainTagDeleteResult;
import com.hivemq.persistence.domain.DomainTagPersistence;
import com.hivemq.persistence.domain.DomainTagUpdateResult;
import com.hivemq.protocols.ProtocolAdapterManager;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.Response;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Map;

import static com.hivemq.persistence.domain.DomainTagUpdateResult.DomainTagUpdateStatus.ADAPTER_NOT_FOUND;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProtocolAdaptersResourceImplTest {

    private final @NotNull HiveMQEdgeRemoteService remoteService = mock();
    private final @NotNull ConfigurationService configurationService = mock();
    private final @NotNull ProtocolAdapterManager protocolAdapterManager = mock();
    private final @NotNull ProtocolAdapterWritingService protocolAdapterWritingService = mock();
    private final @NotNull ObjectMapper objectMapper = new ObjectMapper();
    private final @NotNull VersionProvider versionProvider = mock();
    private final @NotNull DomainTagPersistence domainTagPersistence = mock();

    private final ProtocolAdaptersResourceImpl protocolAdaptersResource =
            new ProtocolAdaptersResourceImpl(remoteService,
                    configurationService,
                    protocolAdapterManager,
                    protocolAdapterWritingService,
                    objectMapper,
                    versionProvider,
                    domainTagPersistence);

    @Test
    void getDomainTagsForAdapter() {
        when(protocolAdapterWritingService.writingEnabled()).thenReturn(false);

        final ArrayList<DomainTag> domainTags = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            domainTags.add(new DomainTag("tag"+i, "1", "s7", "description", Map.of("address", "addressy")));
        }

        when(domainTagPersistence.getTagsForAdapter(any())).thenReturn(domainTags);

        final Response response = protocolAdaptersResource.getDomainTagsForAdapter("adapter");

        final Object entity = response.getEntity();
        assertInstanceOf(DomainTagModelList.class, entity);
        final DomainTagModelList domainTagModelList = (DomainTagModelList) entity;
        assertEquals(domainTags.size(), domainTagModelList.getItems().size());
        for (final DomainTag domainTag : domainTags) {
            assertEquals(DomainTagModel.fromDomainTag(domainTag), domainTagModelList.getItems().get(0));
        }
    }

    @Test
    void addAdapterDomainTag_whenAddingSucceeds_thenReturn200() {
        when(protocolAdapterWritingService.writingEnabled()).thenReturn(false);
        when(domainTagPersistence.addDomainTag(any())).thenReturn(DomainTagAddResult.success());

        final Response response = protocolAdaptersResource.addAdapterDomainTag("adapter",
                 DomainTagModel.fromDomainTag(new DomainTag("tag", "1", "s7", "description", Map.of("address", "addressy"))));

        assertEquals(200, response.getStatus());
    }

    @Test
    void addAdapterDomainTag_whenAlreadyExists_thenReturn403() {
        when(protocolAdapterWritingService.writingEnabled()).thenReturn(false);
        when(domainTagPersistence.addDomainTag(
                any())).thenReturn(DomainTagAddResult.failed(DomainTagAddResult.DomainTagPutStatus.ALREADY_EXISTS, "it exists"));

        final Response response = protocolAdaptersResource.addAdapterDomainTag("adapter",
                DomainTagModel.fromDomainTag(new DomainTag("tag", "1", "s7", "description", Map.of("address", "addressy"))));

        assertEquals(403, response.getStatus());
    }

    @Test
    void deleteDomainTag_whenTagExists_thenReturn200() {
        when(protocolAdapterWritingService.writingEnabled()).thenReturn(false);
        when(domainTagPersistence.deleteDomainTag(any(), any())).thenReturn(DomainTagDeleteResult.failed(
                DomainTagDeleteResult.DomainTagDeleteStatus.SUCCESS));

        final Response response = protocolAdaptersResource.deleteDomainTag("adapter",
                Base64.getEncoder().encodeToString("tag".getBytes(StandardCharsets.UTF_8)));

        assertEquals(200, response.getStatus());

    }

    @Test
    void deleteDomainTag_whenTagDoesNotExists_thenReturn403() {
        when(protocolAdapterWritingService.writingEnabled()).thenReturn(false);
        when(domainTagPersistence.deleteDomainTag(any(), any())).thenReturn(DomainTagDeleteResult.failed(
                DomainTagDeleteResult.DomainTagDeleteStatus.NOT_FOUND));

        final Response response = protocolAdaptersResource.deleteDomainTag("adapter",
                Base64.getEncoder().encodeToString("tag".getBytes(StandardCharsets.UTF_8)));

        assertEquals(404, response.getStatus());

    }

    @Test
    void updateDomainTag_whenTagExists_thenReturn200() {
        when(protocolAdapterWritingService.writingEnabled()).thenReturn(false);
        when(domainTagPersistence.updateDomainTag(any(),
                any())).thenReturn(DomainTagUpdateResult.success());

        final Response response = protocolAdaptersResource.updateDomainTag("adapter",
                Base64.getEncoder().encodeToString("tag".getBytes(StandardCharsets.UTF_8)),
                DomainTagModel.fromDomainTag(new DomainTag("tag", "1", "s7", "description", Map.of("address", "addressy"))));

        assertEquals(200, response.getStatus());
    }

    @Test
    void updateDomainTag_whenTagDoesNotExists_thenReturn403() {
        when(protocolAdapterWritingService.writingEnabled()).thenReturn(false);
        when(domainTagPersistence.updateDomainTag(any(),
                any())).thenReturn(DomainTagUpdateResult.failed(ADAPTER_NOT_FOUND));

        final Response response = protocolAdaptersResource.updateDomainTag("adapter",
                Base64.getEncoder().encodeToString("tag".getBytes(StandardCharsets.UTF_8)),
                DomainTagModel.fromDomainTag(new DomainTag("tag", "1", "s7", "description", Map.of("address", "addressy"))));

        assertEquals(403, response.getStatus());
    }

    @Test
    void getDomainTags() {
        when(protocolAdapterWritingService.writingEnabled()).thenReturn(false);
        final ArrayList<DomainTag> domainTags = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            domainTags.add(new DomainTag("tag"+i, "1", "s7", "description", Map.of("address", "addressy")));
        }

        when(domainTagPersistence.getDomainTags()).thenReturn(domainTags);

        final Response response = protocolAdaptersResource.getDomainTags();

        final Object entity = response.getEntity();
        assertInstanceOf(DomainTagModelList.class, entity);
        final DomainTagModelList domainTagModelList = (DomainTagModelList) entity;
        assertEquals(domainTags.size(), domainTagModelList.getItems().size());
        for (final DomainTag domainTag : domainTags) {
            assertEquals(DomainTagModel.fromDomainTag(domainTag), domainTagModelList.getItems().get(0));
        }
    }
}
