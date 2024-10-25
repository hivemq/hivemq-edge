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
package com.hivemq.persistence.domain.xml;

import com.fasterxml.jackson.databind.JsonNode;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.NONE)
public class DomainTagXmlEntity {

    @XmlElement(name = "name", required = true)
    private final @NotNull String tagName;

    @XmlElement(name = "adapterId", required = true)
    private final @NotNull String adapterId;

    @XmlElement(name = "protocolId", required = true)
    private final @NotNull String protocolId;

    @XmlElement(name = "description", required = true)
    private final @NotNull String description;

    @XmlElement(name = "definition", required = true)
    private final @NotNull JsonNode definition;

    //no-arg for JaxB
    public DomainTagXmlEntity(){
        this.tagName ="";
        this.adapterId = "";
        this.protocolId = "";
        this.description = "";
        definition = null;
    }

    public DomainTagXmlEntity(
            final @NotNull String tagName,
            final @NotNull String adapterId,
            final @NotNull String protocolId,
            final @NotNull String description,
            final @NotNull JsonNode definition) {
        this.tagName = tagName;
        this.adapterId = adapterId;
        this.protocolId = protocolId;
        this.description = description;
        this.definition = definition;
        /*
        try {
            ObjectMapper xmlMapper = new XmlMapper();
            String xml = xmlMapper.writeValueAsString(definition);
            System.out.println(xml);
            this.definition = xml;
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        */

    }

    public @NotNull String getAdapterId() {
        return adapterId;
    }

    public @NotNull JsonNode getDefinition() {
        return definition;
    }

    public @NotNull String getDescription() {
        return description;
    }

    public @NotNull String getProtocolId() {
        return protocolId;
    }

    public @NotNull String getTagName() {
        return tagName;
    }

    @Override
    public @NotNull String toString() {
        return "DomainTagXmlEntity{" +
                "adapterId='" +
                adapterId +
                '\'' +
                ", tagName='" +
                tagName +
                '\'' +
                ", protocolId='" +
                protocolId +
                '\'' +
                ", description='" +
                description +
                '\'' +
                ", definition=" +
                definition +
                '}';
    }
}
