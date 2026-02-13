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
import org.jetbrains.annotations.Nullable;

/// Used exclusively within the combiner.
public record DataIdentifierReference(
        String id, Type type, @Nullable String scope) {

    public DataIdentifierReference(final String id, final Type type) {
        this(id, type, null);
    }

    public static @Nullable DataIdentifierReference from(
            final @Nullable com.hivemq.edge.api.model.DataIdentifierReference model) {
        if (model == null) {
            return null;
        }

        return new DataIdentifierReference(model.getId(), Type.from(model.getType()), model.getScope());
    }

    public static DataIdentifierReference fromPersistence(final @NotNull DataIdentifierReferenceEntity entity) {
        return new DataIdentifierReference(entity.getId(), entity.getType(), entity.getScope());
    }

    public @NotNull com.hivemq.edge.api.model.DataIdentifierReference to() {
        final com.hivemq.edge.api.model.DataIdentifierReference ref =
                new com.hivemq.edge.api.model.DataIdentifierReference(id(), type().to());
        ref.setScope(scope());
        return ref;
    }

    public DataIdentifierReferenceEntity toPersistence() {
        return new DataIdentifierReferenceEntity(this.id(), this.type, this.scope);
    }

    public boolean isIdEmpty() {
        return id == null || id.isBlank();
    }

    /**
     * Returns {@code true} if the scope is valid for the given type:
     * TAG requires a non-blank scope; all other types require null scope.
     */
    public boolean isScopeValid() {
        return type == Type.TAG ? scope != null && !scope.isBlank() : scope == null;
    }

    /**
     * Returns a fully qualified name for this reference, suitable for use as a JSON key.
     * Format: {@code [scope/]TYPE:id} where dots in id are replaced with slashes.
     * Examples: {@code TOPIC_FILTER:topic/a}, {@code adapter1/TAG:temperature}
     */
    public @NotNull String toFullyQualifiedName() {
        final String baseKey = type + ":" + id.replaceAll("\\.", "/");
        if (scope != null) {
            return scope + "/" + baseKey;
        }
        return baseKey;
    }

    public enum Type {
        PULSE_ASSET,
        TAG,
        TOPIC_FILTER;

        public static @NotNull Type from(
                final com.hivemq.edge.api.model.DataIdentifierReference.@NotNull TypeEnum type) {
            return switch (type) {
                case PULSE_ASSET -> PULSE_ASSET;
                case TAG -> TAG;
                case TOPIC_FILTER -> TOPIC_FILTER;
            };
        }

        public com.hivemq.edge.api.model.DataIdentifierReference.@NotNull TypeEnum to() {
            return switch (this) {
                case PULSE_ASSET -> com.hivemq.edge.api.model.DataIdentifierReference.TypeEnum.PULSE_ASSET;
                case TAG -> com.hivemq.edge.api.model.DataIdentifierReference.TypeEnum.TAG;
                case TOPIC_FILTER -> com.hivemq.edge.api.model.DataIdentifierReference.TypeEnum.TOPIC_FILTER;
            };
        }
    }
}
