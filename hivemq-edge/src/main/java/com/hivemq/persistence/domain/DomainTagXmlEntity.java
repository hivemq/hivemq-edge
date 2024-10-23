package com.hivemq.persistence.domain;

import com.hivemq.extension.sdk.api.annotations.NotNull;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;


@XmlRootElement(name = "tag")
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
    private final @NotNull String definition;

    //no-arg for JaxB
    public DomainTagXmlEntity(){
        tagName="";
        adapterId = "";
        protocolId = "";
        description = "";
        definition = "";
    }

    public DomainTagXmlEntity(
            final @NotNull String tagName,
            final @NotNull String adapterId,
            final @NotNull String protocolId,
            final @NotNull String description,
            final @NotNull String definition) {
        this.tagName = tagName;
        this.adapterId = adapterId;
        this.protocolId = protocolId;
        this.description = description;
        this.definition = definition;

    }

    public @NotNull String getAdapterId() {
        return adapterId;
    }

    public @NotNull String getDefinition() {
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
}
