package com.hivemq.api.resources.impl;

import com.hivemq.api.model.tags.DomainTagModel;
import com.hivemq.api.model.tags.DomainTagModelList;
import com.hivemq.api.resources.DomainTagApi;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.persistence.domain.DomainTag;
import com.hivemq.persistence.domain.DomainTagPersistence;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.stream.Collectors;

@Singleton
public class DomainTagResourceImpl implements DomainTagApi {

    private final @NotNull DomainTagPersistence domainTagPersistence;

    @Inject
    public DomainTagResourceImpl(final @NotNull DomainTagPersistence domainTagPersistence) {
        this.domainTagPersistence = domainTagPersistence;
    }


    @Override
    public @NotNull Response getDomainTagsForAdapter(@NotNull final String adapterId) {
        final List<DomainTag> tagsForAdapter = domainTagPersistence.getTagsForAdapter(adapterId);
        if(tagsForAdapter.isEmpty()){
            // TODO is emoty list 200?
            return Response.ok().build();
        }
        final List<DomainTagModel> domainTagModels =
                tagsForAdapter.stream().map(DomainTagModel::fromDomainTag).collect(Collectors.toList());
        final DomainTagModelList domainTagModelList = new DomainTagModelList(domainTagModels);
        return Response.ok().entity(domainTagModelList).build();
    }

    @Override
    public @NotNull Response addAdapterDomainTag(
            @NotNull final String adapterId,
            @NotNull final DomainTagModel domainTag) {
        domainTagPersistence.addDomainTag(adapterId, DomainTag.fromDomainTagEntity(domainTag));
        return Response.ok().build();
    }

    @Override
    public @NotNull Response deleteDomainTag(@NotNull final String adapterId, @NotNull final String tagId) {
        domainTagPersistence.deleteDomainTag(adapterId, tagId);
        //TODO DELETERESULT
        return Response.ok().build();

    }

    @Override
    public @NotNull Response updateDomainTag(
            final @NotNull String adapterId, @NotNull final String tagId, final @NotNull DomainTagModel domainTag) {
        domainTagPersistence.updateDomainTag(adapterId, tagId, domainTag);
        // TODO update result
        return Response.ok().build();
    }

    @Override
    public @NotNull Response getDomainTags() {
        final List<DomainTag> domainTags = domainTagPersistence.getDomainTags();

        // TODO add to response
        return Response.ok().build();
    }
}
