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

import com.hivemq.configuration.entity.EnabledEntity;
import com.hivemq.configuration.entity.api.ldap.LdapAuthenticationEntity;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElementRef;
import jakarta.xml.bind.annotation.XmlElementRefs;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@XmlRootElement(name = "admin-api")
@XmlAccessorType(XmlAccessType.NONE)
@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal", "deprecation"})
public class AdminApiEntity extends EnabledEntity {

    @XmlElementWrapper(name = "listeners")
    @XmlElementRefs({
            @XmlElementRef(required = false, type = HttpListenerEntity.class),
            @XmlElementRef(required = false, type = HttpsListenerEntity.class)})
    private @NotNull List<ApiListenreserEntity> listeners;

    @XmlElementRef(required = false)
    private @NotNull ApiJwsEntity jws;

    @XmlElementWrapper(name = "users")
    @XmlElementRef(required = false)
    private @NotNull List<UserEntity> users;

    @XmlElementRef(required = false)
    private @Nullable LdapAuthenticationEntity ldapAuthentication;

    @XmlElementRef(required = false)
    private @Nullable ApiTlsEntity tls;

    @XmlElementRef(required = false)
    private @NotNull PreLoginNoticeEntity preLoginNotice;

    public AdminApiEntity() {
        this.listeners = new ArrayList<>();
        this.jws = new ApiJwsEntity();
        this.users = new ArrayList<>();
        this.preLoginNotice = new PreLoginNoticeEntity();
    }

    public @NotNull List<ApiListenerEntity> getListeners() {
        return listeners;
    }

    public @NotNull ApiJwsEntity getJws() {
        return jws;
    }

    public @NotNull List<UserEntity> getUsers() {
        return users;
    }

    public @Nullable LdapAuthenticationEntity getLdap() {
        return ldapAuthentication;
    }

    public @Nullable ApiTlsEntity getTls() {
        return tls;
    }

    public @NotNull PreLoginNoticeEntity getPreLoginNotice() {
        return preLoginNotice;
    }

    @Override
    public boolean equals(final @Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof final AdminApiEntity that) {
            if (!super.equals(o)) {
                return false;
            }
            return Objects.equals(listeners, that.listeners) &&
                    Objects.equals(tls, that.tls) &&
                    Objects.equals(jws, that.jws) &&
                    Objects.equals(users, that.users) &&
                    Objects.equals(ldapAuthentication, that.ldapAuthentication) &&
                    Objects.equals(preLoginNotice, that.preLoginNotice);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), listeners, tls, jws, users, ldapAuthentication, preLoginNotice);
    }
}
