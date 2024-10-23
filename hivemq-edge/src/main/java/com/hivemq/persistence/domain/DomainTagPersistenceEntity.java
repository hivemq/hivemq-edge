package com.hivemq.persistence.domain;


import com.hivemq.extension.sdk.api.annotations.NotNull;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "tag-persistence")
@XmlAccessorType(XmlAccessType.NONE)
public class DomainTagPersistenceEntity {

    @XmlElementWrapper(name = "tags", required = true)
    @XmlElementRef(required = false)
    private @NotNull List<DomainTagXmlEntity> tags;

    // JaxB needs a default constructor
    public DomainTagPersistenceEntity() {
        this.tags = new ArrayList<>();
    }

    public DomainTagPersistenceEntity(final @NotNull List<DomainTagXmlEntity> tagsAsEntities) {
        this.tags = tagsAsEntities;
    }

    public @NotNull List<DomainTagXmlEntity> getTags() {
        return tags;
    }
}
