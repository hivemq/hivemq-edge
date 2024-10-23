package com.hivemq.persistence.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.extension.sdk.api.annotations.NotNull;

public class DomainTagMapper {

    public static @NotNull DomainTag domainTagFromDomainTagEntity(
            final @NotNull DomainTagXmlEntity domainTagXmlEntity, final @NotNull ObjectMapper objectMapper) {
        final JsonNode jsonNode = objectMapper.valueToTree(domainTagXmlEntity.getDefinition());
        return new DomainTag(domainTagXmlEntity.getTagName(),
                domainTagXmlEntity.getAdapterId(),
                domainTagXmlEntity.getProtocolId(),
                domainTagXmlEntity.getDescription(),
                jsonNode);
    }

    public static @NotNull DomainTagXmlEntity domainTagEntityFromDomainTag(
            final @NotNull DomainTag domainTag) {
        return new DomainTagXmlEntity(domainTag.getTagName(),
                domainTag.getAdapterId(),
                domainTag.getProtocolId(),
                domainTag.getDescription(),
                domainTag.getTagDefinition().toString());
    }

}
