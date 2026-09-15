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
import java.util.Objects;

/**
 * XML entity for {@code <username-authentication>} under {@code <admin-api>}.
 * <p>
 * Switches local username/password authentication (the {@code <users>} and {@code <ldap>} sources)
 * on or off. When the stanza is present, {@code <enabled>} is required; if the whole stanza is
 * absent, local authentication defaults to enabled, preserving the behavior of a configuration that
 * predates this element.
 * <p>
 * When disabled, no default administrator is installed, no local users or LDAP are wired, and
 * {@code POST /api/v1/auth/authenticate} is rejected.
 * <p>
 * Example configuration:
 * <pre>{@code
 * <username-authentication>
 *     <enabled>false</enabled>
 * </username-authentication>
 * }</pre>
 */
@XmlRootElement(name = "username-authentication")
@XmlAccessorType(XmlAccessType.NONE)
@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
public class UsernameAuthenticationEntity {

    @XmlElement(name = "enabled", required = true)
    private boolean enabled = true;

    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public boolean equals(final Object o) {
        if (!(o instanceof UsernameAuthenticationEntity that)) {
            return false;
        }
        return enabled == that.enabled;
    }

    @Override
    public int hashCode() {
        return Objects.hash(enabled);
    }
}
