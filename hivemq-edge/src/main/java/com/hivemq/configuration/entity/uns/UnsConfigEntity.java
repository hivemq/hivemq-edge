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

import com.hivemq.configuration.entity.listener.ListenerEntity;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Simon L Johnson
 */
@XmlRootElement(name = "uns")
@XmlAccessorType(XmlAccessType.NONE)
@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
public class UnsConfigEntity {

    @XmlElementRef(required = false)
    private @NotNull ISA95Entity isa95 = new ISA95Entity();

    @XmlElementWrapper(name = "profiles")
    @XmlElementRef(required = false)
    private @NotNull List<NamespaceEntity> profiles = new ArrayList<>();

    public @NotNull List<NamespaceEntity> getProfiles() {
        return profiles;
    }

    public void setProfiles(List<NamespaceEntity> profiles){
        this.profiles = profiles;
    }

    public @NotNull ISA95Entity getIsa95() { return isa95; }
}
