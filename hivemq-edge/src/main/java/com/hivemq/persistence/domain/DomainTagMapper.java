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
package com.hivemq.persistence.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.persistence.domain.xml.DomainTagXmlEntity;

public class DomainTagMapper {

    public static @NotNull DomainTag domainTagFromDomainTagEntity(
            final @NotNull DomainTagXmlEntity domainTagXmlEntity, final @NotNull ObjectMapper objectMapper) {
        return new DomainTag(domainTagXmlEntity.getTagName(),
                domainTagXmlEntity.getAdapterId(),
                domainTagXmlEntity.getProtocolId(),
                domainTagXmlEntity.getDescription());
    }

    public static @NotNull DomainTagXmlEntity domainTagEntityFromDomainTag(
            final @NotNull DomainTag domainTag) {
        return new DomainTagXmlEntity(domainTag.getTagName(),
                domainTag.getAdapterId(),
                domainTag.getProtocolId(),
                domainTag.getDescription());
    }

}
