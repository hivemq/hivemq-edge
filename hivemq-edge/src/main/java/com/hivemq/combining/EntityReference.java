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
package com.hivemq.combining;

import com.hivemq.configuration.entity.combining.EntityReferenceEntity;
import org.jetbrains.annotations.NotNull;

public record EntityReference(EntityType type, String id) {

    public static @NotNull EntityReference fromModel(final @NotNull com.hivemq.edge.api.model.EntityReference entityReference) {
        return new EntityReference(EntityType.fromModel(entityReference.getType()),
                entityReference.getId());

    }

    public @NotNull com.hivemq.edge.api.model.EntityReference toModel() {
        return new com.hivemq.edge.api.model.EntityReference().id(id).type(type.toModel());
    }

    public static @NotNull EntityReference fromPersistence(final @NotNull EntityReferenceEntity entityReference) {
        return new EntityReference(entityReference.getType(), entityReference.getId());

    }

    public @NotNull EntityReferenceEntity toPersistence() {
        return new EntityReferenceEntity(type, id);
    }

}
