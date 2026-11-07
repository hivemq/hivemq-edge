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
package com.hivemq.configuration.entity.tls;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

/// A glob pattern whose matches belong to one piece of TLS material.
///
/// Same two elements and same default as a directory, for the same reason: whether matching nothing
/// is acceptable is the operator's call, and the strict reading needs nothing written down.
@XmlRootElement(name = "pattern")
@XmlAccessorType(XmlAccessType.NONE)
@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
public class PatternEntity {

    @XmlElement(name = "location", required = true)
    private @NotNull String location = "";

    @XmlElement(name = "may-be-empty", defaultValue = "false")
    private boolean mayBeEmpty = false;

    public @NotNull String getLocation() {
        return location;
    }

    public boolean isMayBeEmpty() {
        return mayBeEmpty;
    }

    public void setLocation(final @NotNull String location) {
        this.location = location;
    }

    public void setMayBeEmpty(final boolean mayBeEmpty) {
        this.mayBeEmpty = mayBeEmpty;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PatternEntity that)) {
            return false;
        }
        return mayBeEmpty == that.mayBeEmpty && Objects.equals(location, that.location);
    }

    @Override
    public int hashCode() {
        return Objects.hash(location, mayBeEmpty);
    }
}
