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
package com.hivemq.api.auth.provider.impl.ldap;

import org.jetbrains.annotations.NotNull;

/**
 * Interface for resolving usernames to Distinguished Names (DNs) in LDAP.
 * <p>
 * Different LDAP servers use different DN structures and attribute names.
 * This interface provides a strategy for converting a username to a full DN
 * that can be used for LDAP bind operations.
 * <p>
 * Example DN formats:
 * <ul>
 *     <li>OpenLDAP: {@code uid=jdoe,ou=people,dc=example,dc=com}</li>
 *     <li>Active Directory: {@code cn=John Doe,cn=Users,dc=company,dc=com}</li>
 *     <li>Email-based: {@code mail=jdoe@company.com,ou=staff,dc=company,dc=com}</li>
 * </ul>
 */
@FunctionalInterface
public interface UserDnResolver {

    /**
     * Resolves a username to a Distinguished Name.
     *
     * @param username The username to resolve (e.g., "jdoe", "john.doe@company.com")
     * @return The full Distinguished Name (e.g., "uid=jdoe,ou=people,dc=example,dc=com")
     */
    @NotNull String resolveDn(final @NotNull String username);
}
