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

/// A directory whose readable entries belong to one piece of TLS material.
///
/// Two elements rather than a bare path, because whether emptiness is acceptable is a question only
/// the operator can answer: a directory a deployment populates later is legitimately empty today,
/// while an empty directory where material was expected means Edge checks less than the
/// configuration says. Written as elements rather than an attribute so the shape survives
/// translation to JSON, YAML or Protobuf, where an attribute has no equivalent.
@XmlRootElement(name = "directory")
@XmlAccessorType(XmlAccessType.NONE)
@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
public class DirectoryEntity {

    @XmlElement(name = "location", required = true)
    private @NotNull String location = "";

    /// Whether this directory may hold no file at all.
    ///
    /// Defaults to false, so the strict reading is what an operator gets without writing anything,
    /// and permitting emptiness is a visible choice in the configuration.
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
        if (!(o instanceof DirectoryEntity that)) {
            return false;
        }
        return mayBeEmpty == that.mayBeEmpty && Objects.equals(location, that.location);
    }

    @Override
    public int hashCode() {
        return Objects.hash(location, mayBeEmpty);
    }
}
