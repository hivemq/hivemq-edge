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

import com.unboundid.ldap.sdk.DN;
import com.unboundid.util.ByteStringBuffer;
import org.jetbrains.annotations.NotNull;

/**
 * Template-based DN resolver that uses string substitution to construct Distinguished Names.
 * <p>
 * This resolver replaces placeholders in a template string with actual values:
 * <ul>
 *     <li>{@code {username}} - The provided username</li>
 *     <li>{@code {baseDn}} - The base DN of the LDAP directory</li>
 * </ul>
 * <p>
 * Example templates:
 * <pre>
 * OpenLDAP:         "uid={username},ou=people,{baseDn}"
 * Active Directory: "cn={username},cn=Users,{baseDn}"
 * Email-based:      "mail={username},ou=staff,{baseDn}"
 * Custom attribute: "employeeNumber={username},ou=employees,{baseDn}"
 * Multiple OUs:     "uid={username},ou=engineering,ou=staff,{baseDn}"
 * </pre>
 */
public class TemplateDnResolver implements UserDnResolver {

    private final @NotNull String uidAttribute;
    private final @NotNull String baseDn;

    /**
     * Creates a new template-based DN resolver.
     *
     * @param uidAttribute  The uidAttribeut to use for resolution (e.g., "uid")
     * @param baseDn        The base DN of the LDAP directory (e.g., "dc=example,dc=com")
     * @throws IllegalArgumentException if template or baseDn is null or empty
     */
    public TemplateDnResolver(final @NotNull String uidAttribute, final @NotNull String baseDn) {
        if (uidAttribute.isBlank()) {
            throw new IllegalArgumentException("uidAttribute cannot be empty");
        }
        if (baseDn.isBlank()) {
            throw new IllegalArgumentException("Base DN cannot be empty");
        }

        this.uidAttribute = uidAttribute;
        this.baseDn = baseDn;
    }

    @Override
    public @NotNull String resolveDn(final @NotNull String username) {
        if (username.isBlank()) {
            throw new IllegalArgumentException("Username cannot be empty");
        }
        final var escaped = new ByteStringBuffer();
        DN.getDNEscapingStrategy().escape(username, escaped);

        return uidAttribute + "=" + escaped.toString() + "," + baseDn;
    }

    /**
     * Returns the uidAttribute used by this resolver.
     *
     * @return The uidAttribute string
     */
    public @NotNull String getUidAttribute() {
        return uidAttribute;
    }

    /**
     * Returns the base DN used by this resolver.
     *
     * @return The base DN string
     */
    public @NotNull String getBaseDn() {
        return baseDn;
    }
}
