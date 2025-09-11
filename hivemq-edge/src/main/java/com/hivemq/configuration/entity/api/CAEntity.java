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
package com.hivemq.configuration.entity.api;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;


@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
@XmlRootElement(name = "confidentiality-agreement")
@XmlAccessorType(XmlAccessType.NONE)
public class CAEntity {

    @XmlElement(name = "enabled")
    private boolean enabled;

    @XmlElement(name = "content")
    private @Nullable String content;

    public CAEntity() {
        this(false, null);
    }

    public CAEntity(final boolean enabled, final @Nullable String content) {
        this.enabled = enabled;
        this.content = content;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public @Nullable String getContent() {
        return content;
    }

    @Override
    public boolean equals(final @Nullable Object o) {
        return o instanceof final CAEntity that &&
                Objects.equals(enabled, that.enabled) &&
                Objects.equals(content, that.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(enabled, content);
    }
}
