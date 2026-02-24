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
package com.hivemq.configuration.entity.api.ldap;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

@XmlAccessorType(XmlAccessType.NONE)
@SuppressWarnings("NotNullFieldNotInitialized")
public class LdapServerEntity {

    /*        <ldap-server>
                <host>host</host>
                <port>389</port>
              </ldap-server>
    */
    @XmlElement(name = "host", required = true)
    private @NotNull String host;

    @XmlElement(name = "port", defaultValue = "389")
    private int port = 389;

    public LdapServerEntity() {}

    public LdapServerEntity(final @NotNull String host, final int port) {
        this.host = host;
        this.port = port;
    }

    public @NotNull String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    @Override
    public boolean equals(final Object o) {
        if (!(o instanceof LdapServerEntity)) return false;
        final LdapServerEntity that = (LdapServerEntity) o;
        return getPort() == that.getPort() && Objects.equals(getHost(), that.getHost());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getHost(), getPort());
    }
}
