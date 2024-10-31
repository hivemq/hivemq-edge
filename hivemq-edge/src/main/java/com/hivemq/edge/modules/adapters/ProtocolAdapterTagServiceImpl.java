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
package com.hivemq.edge.modules.adapters;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterTagService;
import com.hivemq.adapter.sdk.api.tag.Tag;
import com.hivemq.persistence.domain.DomainTag;
import com.hivemq.persistence.domain.DomainTagAddResult;
import com.hivemq.persistence.domain.DomainTagPersistence;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ProtocolAdapterTagServiceImpl implements ProtocolAdapterTagService {

    private final @NotNull DomainTagPersistence domainTagPersistence;
    private final @NotNull ObjectMapper objectMapper = new ObjectMapper();

    @Inject
    public ProtocolAdapterTagServiceImpl(final @NotNull DomainTagPersistence domainTagPersistence) {
        this.domainTagPersistence = domainTagPersistence;
    }

    @Override
    public @NotNull AddStatus addTag(
            final @NotNull String adapterId, final @NotNull String protocolId, @NotNull final Tag<?> tag) {
        final JsonNode jsonNode = objectMapper.valueToTree(tag.getTagDefinition());
        final DomainTagAddResult domainTagAddResult =
                domainTagPersistence.addDomainTag(new DomainTag(tag.getTagName(), adapterId, protocolId, ""));

        switch (domainTagAddResult.getDomainTagPutStatus()) {
            case SUCCESS:
                return AddStatus.SUCCESS;
            case ALREADY_EXISTS:
                return AddStatus.ALREADY_PRESENT;
        }
        // shouldnt be able to happen
        throw new RuntimeException();
    }
}
