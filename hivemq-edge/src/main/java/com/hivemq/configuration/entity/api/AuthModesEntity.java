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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

/**
 * XML entity for {@code <auth-modes>} under {@code <admin-api>}.
 * <p>
 * Lists the authentication mechanisms the Admin API accepts. When this element is present it must
 * contain at least one {@code <auth-mode>}; that constraint is enforced by the XSD.
 * <p>
 * Example configuration:
 * <pre>{@code
 * <auth-modes>
 *     <auth-mode>USERNAME_PASSWORD</auth-mode>
 *     <auth-mode>OPEN_ID</auth-mode>
 * </auth-modes>
 * }</pre>
 */
@XmlRootElement(name = "auth-modes")
@XmlAccessorType(XmlAccessType.NONE)
@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
public class AuthModesEntity {

    @XmlElement(name = "auth-mode", required = true)
    private @NotNull List<AuthModeEntity> authModes = new ArrayList<>();

    public @NotNull List<AuthModeEntity> getAuthModes() {
        return authModes;
    }

    @Override
    public boolean equals(final Object o) {
        if (!(o instanceof AuthModesEntity that)) {
            return false;
        }
        return Objects.equals(getAuthModes(), that.getAuthModes());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getAuthModes());
    }
}
