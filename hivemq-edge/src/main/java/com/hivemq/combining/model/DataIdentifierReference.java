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
package com.hivemq.combining.model;

import com.hivemq.configuration.entity.combining.DataIdentifierReferenceEntity;
import org.jetbrains.annotations.NotNull;

public record DataIdentifierReference(String id, Type type) {
    public static @NotNull DataIdentifierReference from(final @NotNull com.hivemq.edge.api.model.DataIdentifierReference model) {
        return new DataIdentifierReference(model.getId(), Type.from(model.getType()));
    }

    public @NotNull com.hivemq.edge.api.model.DataIdentifierReference to() {
        return new com.hivemq.edge.api.model.DataIdentifierReference(this.id(), this.type().to());
    }

    public static DataIdentifierReference fromPersistence(final @NotNull DataIdentifierReferenceEntity entity) {
        return new DataIdentifierReference(entity.getId(), entity.getType());
    }

    public DataIdentifierReferenceEntity toPersistence() {
        return new DataIdentifierReferenceEntity(this.id(), this.type);
    }

    public enum Type {
        TAG,
        TOPIC_FILTER;

        public static @NotNull Type from(final com.hivemq.edge.api.model.DataIdentifierReference.@NotNull TypeEnum type) {
            switch (type) {
                case TAG -> {
                    return TAG;
                }
                case TOPIC_FILTER -> {
                    return TOPIC_FILTER;
                }
            }
            throw new IllegalArgumentException();
        }

        public com.hivemq.edge.api.model.DataIdentifierReference.@NotNull TypeEnum to() {
            switch (this) {
                case TAG -> {
                    return com.hivemq.edge.api.model.DataIdentifierReference.TypeEnum.TAG;
                }
                case TOPIC_FILTER -> {
                    return com.hivemq.edge.api.model.DataIdentifierReference.TypeEnum.TOPIC_FILTER;
                }
            }
            throw new IllegalArgumentException();
        }
    }

}
