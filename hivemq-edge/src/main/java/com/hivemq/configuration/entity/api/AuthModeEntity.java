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

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;

/**
 * The authentication mechanisms the Admin API accepts, configured via {@code <auth-modes>}.
 * <p>
 * When {@code <auth-modes>} is absent the gateway defaults to {@link #USERNAME_PASSWORD}. When it is
 * present it must list at least one value; multiple values may be combined (for example an
 * installation that accepts both LDAP-backed local users and OIDC single sign-on).
 */
@XmlEnum
@XmlType(name = "authMode")
public enum AuthModeEntity {
    /** Local username/password login, backed by the {@code <users>} list or {@code <ldap>} (LDAP takes precedence). */
    USERNAME_PASSWORD,
    /** OpenID Connect single sign-on, configured by {@code <oidc-authentication>}. */
    OPEN_ID
}
