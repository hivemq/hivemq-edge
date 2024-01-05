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
package com.hivemq.edge.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Objects;
import java.util.UUID;

/**
 * @author Simon L Johnson
 */
public class TypeIdentifier {

    public enum TYPE {
        BRIDGE, ADAPTER, ADAPTER_TYPE, EVENT, USER
    }

    @JsonProperty("type")
    @Schema(description = "The type of the associated object/entity",
            required = true)
    private final @NotNull TYPE type;

    @JsonProperty("identifier")
    @Schema(description = "The identifier associated with the object, a combination of type and identifier is used to uniquely identify an object in the system")
    private final @Nullable String identifier;

    public TypeIdentifier(@NotNull @JsonProperty("type") final TYPE type,
                          @NotNull @JsonProperty("identifier") final String identifier) {
        Preconditions.checkNotNull(type);
        Preconditions.checkNotNull(identifier);
        this.type = type;
        this.identifier = identifier;
    }

    public TYPE getType() {
        return type;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getFullQualifiedIdentifier() {
        return toString();
    }

    public String toString() {
        return String.format("%s:%s", type.toString().toLowerCase(), identifier);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final TypeIdentifier that = (TypeIdentifier) o;
        return type == that.type && identifier.equals(that.identifier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, identifier);
    }

    public static TypeIdentifier create(final @NotNull TYPE type, final @NotNull String identifier){
        Preconditions.checkNotNull(type);
        Preconditions.checkNotNull(identifier);
        return new TypeIdentifier(type,identifier);
    }

    /**
     * Generate a globally unique type identifier in the namespace supplied
     * @return The generated ID
     */
    public static TypeIdentifier generate(final @NotNull TYPE type){
        Preconditions.checkNotNull(type);
        return new TypeIdentifier(type, UUID.randomUUID().toString());
    }
}
