package com.hivemq.api.resources.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.api.model.tags.DomainTagModel;
import com.hivemq.api.model.tags.DomainTagModelList;
import com.hivemq.api.model.tags.TagAddress;
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

import static com.hivemq.persistence.domain.DomainTagUpdateResult.DomainTagUpdateStatus.NOT_FOUND;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProtocolAdaptersResourceImplTest {

    private final @NotNull HiveMQEdgeRemoteService remoteService = mock();
    private final @NotNull ConfigurationService configurationService = mock();
    private final @NotNull ProtocolAdapterManager protocolAdapterManager = mock();
    private final @NotNull ObjectMapper objectMapper = new ObjectMapper();
    private final @NotNull VersionProvider versionProvider = mock();
    private final @NotNull DomainTagPersistence domainTagPersistence = mock();

    private final ProtocolAdaptersResourceImpl protocolAdaptersResource =
            new ProtocolAdaptersResourceImpl(remoteService,
                    configurationService,
                    protocolAdapterManager,
                    objectMapper,
                    versionProvider,
                    domainTagPersistence);

    @Test
    void getDomainTagsForAdapter() {
        final ArrayList<DomainTag> domainTags = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            domainTags.add(DomainTag.simpleAddress("address", "tag"));
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
        when(domainTagPersistence.addDomainTag(any(), any())).thenReturn(DomainTagAddResult.success());

        final Response response = protocolAdaptersResource.addAdapterDomainTag("adapter",
                 DomainTagModel.fromDomainTag(DomainTag.simpleAddress("address", "tag")));

        assertEquals(200, response.getStatus());
    }

    @Test
    void addAdapterDomainTag_whenAlreadyExists_thenReturn403() {
        when(domainTagPersistence.addDomainTag(any(),
                any())).thenReturn(DomainTagAddResult.failed(DomainTagAddResult.DomainTagPutStatus.ALREADY_EXISTS));

        final Response response = protocolAdaptersResource.addAdapterDomainTag("adapter",
                DomainTagModel.fromDomainTag(DomainTag.simpleAddress("address", "tag")));

        assertEquals(403, response.getStatus());
    }

    @Test
    void deleteDomainTag_whenTagExists_thenReturn200() {
        when(domainTagPersistence.deleteDomainTag(any(), any())).thenReturn(DomainTagDeleteResult.failed(
                DomainTagDeleteResult.DomainTagDeleteStatus.SUCCESS));

        final Response response = protocolAdaptersResource.deleteDomainTag("adapter",
                Base64.getEncoder().encodeToString("tag".getBytes(StandardCharsets.UTF_8)));

        assertEquals(200, response.getStatus());

    }

    @Test
    void deleteDomainTag_whenTagDoesNotExists_thenReturn403() {
        when(domainTagPersistence.deleteDomainTag(any(), any())).thenReturn(DomainTagDeleteResult.failed(
                DomainTagDeleteResult.DomainTagDeleteStatus.NOT_FOUND));

        final Response response = protocolAdaptersResource.deleteDomainTag("adapter",
                Base64.getEncoder().encodeToString("tag".getBytes(StandardCharsets.UTF_8)));

        assertEquals(403, response.getStatus());

    }

    @Test
    void updateDomainTag_whenTagExists_thenReturn200() {
        when(domainTagPersistence.updateDomainTag(any(),
                any(),
                any())).thenReturn(DomainTagUpdateResult.success());

        final Response response = protocolAdaptersResource.updateDomainTag("adapter",
                Base64.getEncoder().encodeToString("tag".getBytes(StandardCharsets.UTF_8)),
                DomainTagModel.fromDomainTag(DomainTag.simpleAddress("address", "tag")));

        assertEquals(200, response.getStatus());
    }

    @Test
    void updateDomainTag_whenTagDoesNotExists_thenReturn403() {
        when(domainTagPersistence.updateDomainTag(any(),
                any(),
                any())).thenReturn(DomainTagUpdateResult.failed(NOT_FOUND));

        final Response response = protocolAdaptersResource.updateDomainTag("adapter",
                Base64.getEncoder().encodeToString("tag".getBytes(StandardCharsets.UTF_8)),
                DomainTagModel.fromDomainTag(DomainTag.simpleAddress("address", "tag")));

        assertEquals(403, response.getStatus());
    }

    @Test
    void getDomainTags() {
        final ArrayList<DomainTag> domainTags = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            domainTags.add(DomainTag.simpleAddress("address", "tag"));
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
