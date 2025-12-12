/*
 *  Copyright 2019-present HiveMQ GmbH
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.hivemq.configuration.entity.adapter;

import com.hivemq.configuration.entity.EntityConverter;
import com.hivemq.edge.api.model.DomainTagOwner;
import com.hivemq.persistence.domain.DomainTag;
import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.NotNull;

public final class DomainTagOwnerConverter implements EntityConverter<DomainTagOwner, DomainTag> {
    public static final DomainTagOwnerConverter INSTANCE = new DomainTagOwnerConverter();

    private DomainTagOwnerConverter() {
    }

    @Override
    public @NotNull DomainTag toInternalEntity(final @NotNull DomainTagOwner entity) {
        throw new NotImplementedException("DomainTagOwner to DomainTag conversion is not implemented");
    }

    @Override
    public @NotNull DomainTagOwner toRestEntity(final @NotNull DomainTag entity) {
        return DomainTagOwner.builder()
                .adapterId(entity.getAdapterId())
                .name(entity.getTagName())
                .description(entity.getDescription())
                .definition(entity.getDefinition())
                .build();
    }
}
