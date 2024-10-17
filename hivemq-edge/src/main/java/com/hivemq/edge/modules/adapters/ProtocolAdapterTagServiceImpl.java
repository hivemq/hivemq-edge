package com.hivemq.edge.modules.adapters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterTagService;
import com.hivemq.adapter.sdk.api.tag.Tag;
import com.hivemq.persistence.domain.DomainTag;
import com.hivemq.persistence.domain.DomainTagPersistence;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ProtocolAdapterTagServiceImpl implements ProtocolAdapterTagService {

    private final @NotNull DomainTagPersistence domainTagPersistence;

    @Inject
    public ProtocolAdapterTagServiceImpl(final @NotNull DomainTagPersistence domainTagPersistence) {
        this.domainTagPersistence = domainTagPersistence;
    }

    @Override
    public @NotNull <T> Tag<T> resolveTag(final @NotNull String tagName, final @NotNull Class<T> addressClass) {
        final DomainTag tag = domainTagPersistence.getTag(tagName);
        final ObjectMapper objectMapper = new ObjectMapper();
        try {
            final T address = objectMapper.treeToValue(tag.getTagAddress(), addressClass);
            return new Tag<T>() {
                @Override
                public @NotNull T getTagAddress() {
                    return address;
                }

                @Override
                public @NotNull String getTagName() {
                    return tag.getTag();
                }
            };
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }


    }

    @Override
    public @NotNull AddStatus addTag(@NotNull final Tag<?> tag) {
        return null;
    }
}
