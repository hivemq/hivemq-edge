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
import org.jetbrains.annotations.NotNull;

import javax.xml.bind.annotation.XmlElement;

public class DataIdentifierReferenceEntity {

    @JsonProperty("id")
    @XmlElement(name = "id")
    private final @NotNull String id;

    @JsonProperty("type")
    @XmlElement(name = "type")
    private final @NotNull DataIdentifierReference.Type type;


    public DataIdentifierReferenceEntity() {
        this.id = "id";
        this.type = null;
    }

    public DataIdentifierReferenceEntity(@NotNull final String id, @NotNull final DataIdentifierReference.Type type) {
        this.id = id;
        this.type = type;
    }

    public @NotNull String getId() {
        return id;
    }

    public @NotNull DataIdentifierReference.Type getType() {
        return type;
    }
}
