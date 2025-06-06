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
package com.hivemq.configuration.entity;

import org.jetbrains.annotations.NotNull;

import jakarta.xml.bind.annotation.*;
import java.util.Objects;

/**
 * @author Lukas Brandl
 */
@XmlRootElement(name = "persistence")
@XmlAccessorType(XmlAccessType.NONE)
@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
public class PersistenceEntity {

    @XmlEnum
    @XmlType(name = "mode")
    public enum PersistenceMode {
        @XmlEnumValue("file-native") FILE_NATIVE("file-native"),
        @XmlEnumValue("file") FILE("file"),
        @XmlEnumValue("in-memory") IN_MEMORY("in-memory");

        private final @NotNull String configRepresentation;

        PersistenceMode(final @NotNull String configRepresentation) {
            this.configRepresentation = configRepresentation;
        }

        public @NotNull String getConfigRepresentation() {
            return configRepresentation;
        }
    }

    @XmlElement(name = "mode", defaultValue = "in-memory")
    private @NotNull PersistenceEntity.PersistenceMode mode = PersistenceMode.IN_MEMORY;

    @NotNull
    public PersistenceMode getMode() {
        return mode;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final PersistenceEntity that = (PersistenceEntity) o;
        return getMode() == that.getMode();
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getMode());
    }
}
