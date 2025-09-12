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
@XmlRootElement(name = "pre-login-notice")
@XmlAccessorType(XmlAccessType.NONE)
public class PreLoginNoticeEntity {

    @XmlElement(name = "enabled")
    private boolean enabled;

    @XmlElement(name = "title")
    private @Nullable String title;

    @XmlElement(name = "message")
    private @Nullable String message;

    @XmlElement(name = "consent")
    private @Nullable String consent;

    public PreLoginNoticeEntity() {
        this(false, null, null, null);
    }

    public PreLoginNoticeEntity(
            final boolean enabled,
            final @Nullable String title,
            final @Nullable String message,
            final @Nullable String consent) {
        this.enabled = enabled;
        this.title = title;
        this.message = message;
        this.consent = consent;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public @Nullable String getTitle() {
        return title;
    }

    public @Nullable String getMessage() {
        return message;
    }

    public @Nullable String getConsent() {
        return consent;
    }

    @Override
    public boolean equals(final @Nullable Object o) {
        if (this == o) {
            return true;
        }
        return o instanceof final PreLoginNoticeEntity that &&
                enabled == that.enabled &&
                Objects.equals(title, that.title) &&
                Objects.equals(message, that.message) &&
                Objects.equals(consent, that.consent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(enabled, title, message, consent);
    }
}
