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

import com.hivemq.configuration.entity.security.AllowEmptyClientIdEntity;
import com.hivemq.configuration.entity.security.PayloadFormatValidationEntity;
import com.hivemq.configuration.entity.security.RequestProblemInformationEntityConfig;
import com.hivemq.configuration.entity.security.UTF8ValidationEntity;
import org.jetbrains.annotations.NotNull;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElementRef;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.Objects;

/**
 * @author Florian Limpöck
 * @since 4.0.0
 */
@XmlRootElement(name = "security")
@XmlAccessorType(XmlAccessType.NONE)
@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
public class SecurityConfigEntity {

    @XmlElementRef(required = false)
    private @NotNull PayloadFormatValidationEntity payloadFormatValidationEntity = new PayloadFormatValidationEntity();

    @XmlElementRef(required = false)
    private @NotNull UTF8ValidationEntity utf8ValidationEntity = new UTF8ValidationEntity();

    @XmlElementRef(required = false)
    private @NotNull AllowEmptyClientIdEntity allowEmptyClientIdEntity = new AllowEmptyClientIdEntity();

    @XmlElementRef(required = false)
    private @NotNull RequestProblemInformationEntityConfig allowRequestProblemInformationEntity = new RequestProblemInformationEntityConfig();

    public @NotNull PayloadFormatValidationEntity getPayloadFormatValidationEntity() {
        return payloadFormatValidationEntity;
    }

    public @NotNull UTF8ValidationEntity getUtf8ValidationEntity() {
        return utf8ValidationEntity;
    }

    public @NotNull AllowEmptyClientIdEntity getAllowEmptyClientIdEntity() {
        return allowEmptyClientIdEntity;
    }

    public @NotNull RequestProblemInformationEntityConfig getAllowRequestProblemInformationEntity() {
        return allowRequestProblemInformationEntity;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final SecurityConfigEntity that = (SecurityConfigEntity) o;
        return Objects.equals(getPayloadFormatValidationEntity(), that.getPayloadFormatValidationEntity()) &&
                Objects.equals(getUtf8ValidationEntity(), that.getUtf8ValidationEntity()) &&
                Objects.equals(getAllowEmptyClientIdEntity(), that.getAllowEmptyClientIdEntity()) &&
                Objects.equals(getAllowRequestProblemInformationEntity(),
                        that.getAllowRequestProblemInformationEntity());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getPayloadFormatValidationEntity(),
                getUtf8ValidationEntity(),
                getAllowEmptyClientIdEntity(),
                getAllowRequestProblemInformationEntity());
    }
}
