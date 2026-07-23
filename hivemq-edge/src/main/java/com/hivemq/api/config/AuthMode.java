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
package com.hivemq.api.config;

/**
 * The authentication mechanisms the Admin API accepts, resolved from the {@code <auth-modes>}
 * configuration. When {@code <auth-modes>} is absent the resolved set defaults to
 * {@link #USERNAME_PASSWORD}.
 */
public enum AuthMode {
    /** Local username/password login, backed by the configured users or LDAP. */
    USERNAME_PASSWORD,
    /** OpenID Connect single sign-on. */
    OPEN_ID
}
