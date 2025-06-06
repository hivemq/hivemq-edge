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
import org.jetbrains.annotations.NotNull;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElementRef;
import jakarta.xml.bind.annotation.XmlElementRefs;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author Simon L Johnson
 */
@XmlRootElement(name = "admin-api")
@XmlAccessorType(XmlAccessType.NONE)
@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
public class AdminApiEntity extends EnabledEntity {

    @XmlElementWrapper(name = "listeners")
    @XmlElementRefs({
            @XmlElementRef(required = false, type = HttpListenerEntity.class),
            @XmlElementRef(required = false, type = HttpsListenerEntity.class)})
    private @NotNull List<ApiListenerEntity> listeners = new ArrayList<>();

    @XmlElementRef(required = false)
    private @NotNull ApiTlsEntity tls;

    @XmlElementRef(required = false)
    private @NotNull ApiJwsEntity jws = new ApiJwsEntity();

    @XmlElementWrapper(name = "users")
    @XmlElementRef(required = false)
    private @NotNull List<UserEntity> users = new ArrayList<>();

    public @NotNull List<ApiListenerEntity> getListeners() {
        return listeners;
    }

    public ApiJwsEntity getJws() {
        return jws;
    }

    public List<UserEntity> getUsers() {
        return users;
    }

    public ApiTlsEntity getTls() {
        return tls;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        final AdminApiEntity that = (AdminApiEntity) o;
        return Objects.equals(getListeners(), that.getListeners()) &&
                Objects.equals(getTls(), that.getTls()) &&
                Objects.equals(getJws(), that.getJws()) &&
                Objects.equals(getUsers(), that.getUsers());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getListeners(), getTls(), getJws(), getUsers());
    }
}
