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
package com.hivemq.configuration.entity.combining;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.combining.model.DataIdentifierReference;
import com.hivemq.configuration.entity.EntityValidatable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import jakarta.xml.bind.ValidationEvent;
import jakarta.xml.bind.annotation.XmlElement;
import java.util.List;
import java.util.Objects;

public class DataIdentifierReferenceEntity implements EntityValidatable {

    @JsonProperty("id")
    @XmlElement(name = "id")
    private @NotNull String id;

    @JsonProperty("type")
    @XmlElement(name = "type")
    private @NotNull DataIdentifierReference.Type type;

    @JsonProperty("scope")
    @XmlElement(name = "scope")
    private @Nullable String scope;

    // no-arg for jaxb
    public DataIdentifierReferenceEntity() {
    }

    public DataIdentifierReferenceEntity(@NotNull final String id, @NotNull final DataIdentifierReference.Type type) {
        this(id, type, null);
    }

    public DataIdentifierReferenceEntity(
            @NotNull final String id,
            @NotNull final DataIdentifierReference.Type type,
            @Nullable final String scope) {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(id, "id must not be null");
        this.id = id;
        this.type = type;
        this.scope = scope;
    }

    public @NotNull String getId() {
        return id;
    }

    public @NotNull DataIdentifierReference.Type getType() {
        return type;
    }

    public @Nullable String getScope() {
        return scope;
    }

    public static DataIdentifierReferenceEntity from(com.hivemq.edge.api.model.DataIdentifierReference ref) {
        return new DataIdentifierReferenceEntity(ref.getId(),
                DataIdentifierReference.Type.from(ref.getType()),
                ref.getScope());
    }

    @Override
    public void validate(final @NotNull List<ValidationEvent> validationEvents) {
         // The following check is disabled because it will break existing configurations during upgrade.
//        if (type == DataIdentifierReference.Type.TAG) {
//            EntityValidatable.notMatch(validationEvents,
//                    () -> scope != null && !scope.isBlank(),
//                    () -> "scope is required for TAG type");
//        } else {
//            EntityValidatable.notMatch(validationEvents,
//                    () -> scope == null,
//                    () -> "scope must be null for " + type + " type");
//        }
    }

    @Override
    public String toString() {
        return "DataIdentifierReferenceEntity{" + "id='" + id + '\'' + ", type=" + type + ", scope='" + scope + '\'' + '}';
    }
}
