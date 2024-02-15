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
package com.hivemq.configuration.entity.uns;

import com.hivemq.configuration.entity.DisabledEntity;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.uns.NamespaceUtils;
import com.hivemq.uns.config.NamespaceProfile;
import com.hivemq.uns.config.NamespaceSegment;
import com.hivemq.uns.config.impl.NamespaceProfileImpl;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 * @author Simon L Johnson
 */
@XmlRootElement(name = "namespace")
@XmlAccessorType(XmlAccessType.NONE)
public class NamespaceEntity extends DisabledEntity {

    @XmlElement(name = "description")
    private @Nullable String description;

    @XmlElement(name = "name")
    private @Nullable String name;

    @XmlElement(name = "prefixAllTopics")
    private @Nullable Boolean prefixAllTopics;

    @XmlElementWrapper(name = "segments", required = true)
    @XmlElementRef(required = false)
    private @Nullable List<NamespaceSegment> segments;

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public List<NamespaceSegment> getSegments() {
        return segments;
    }

    public void setSegments(final List<NamespaceSegment> segments) {
        this.segments = segments;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public Boolean getPrefixAllTopics() {
        return prefixAllTopics;
    }

    public void setPrefixAllTopics(final Boolean prefixAllTopics) {
        this.prefixAllTopics = prefixAllTopics;
    }

    public static NamespaceEntity convert(NamespaceProfile profile){
        NamespaceEntity entity = new NamespaceEntity();
        entity.setName(profile.getName());
        entity.setPrefixAllTopics(profile.getPrefixAllTopics());
        entity.setDescription(profile.getDescription());
//        entity.setType(NamespaceUtils.getNamespaceProfileType(profile));
        entity.setSegments(profile.getSegments());
        entity.setEnabled(profile.getEnabled());
        return entity;
    }

    public static NamespaceProfile unconvert(NamespaceEntity entity){
        NamespaceProfileImpl impl = new NamespaceProfileImpl(entity.getName(), entity.getDescription(), entity.getSegments());
        impl.setEnabled(entity.isEnabled());
        impl.setPrefixAllTopics(entity.getPrefixAllTopics());
        return impl;
    }
}
