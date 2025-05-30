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

import org.jetbrains.annotations.NotNull;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElementRef;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.Objects;

/**
 * @author Simon L Johnson
 */
@XmlRootElement(name = "uns")
@XmlAccessorType(XmlAccessType.NONE)
@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
public class UnsConfigEntity {

    @XmlElementRef(required = false)
    private @NotNull ISA95Entity isa95 = new ISA95Entity();

    public @NotNull ISA95Entity getIsa95() { return isa95; }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final UnsConfigEntity that = (UnsConfigEntity) o;
        return Objects.equals(getIsa95(), that.getIsa95());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getIsa95());
    }
}
