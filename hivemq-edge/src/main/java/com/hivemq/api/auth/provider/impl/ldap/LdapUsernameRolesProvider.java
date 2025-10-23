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

import com.hivemq.api.auth.provider.IUsernameRolesProvider;
import com.hivemq.logging.SecurityLog;
import com.unboundid.ldap.sdk.LDAPException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.GeneralSecurityException;
import java.util.Optional;
import java.util.Set;

public class LdapUsernameRolesProvider implements IUsernameRolesProvider {

    private static final @NotNull Logger log = LoggerFactory.getLogger(LdapUsernameRolesProvider.class);

    private final @NotNull LdapClient ldapClient;
    private final @NotNull Set<String> assignedRole;

    public LdapUsernameRolesProvider(final @NotNull LdapConnectionProperties ldapConnectionProperties, final @NotNull SecurityLog securityLog) {
        this.ldapClient = new LdapClient(ldapConnectionProperties, securityLog);
        this.assignedRole = Set.of(ldapConnectionProperties.assignedRole());
        try {
            this.ldapClient.start();
        } catch (final LDAPException | GeneralSecurityException e) {
            log.error("Failed to start LDAP client", e);
            throw new RuntimeException("Failed to initialize LDAP authentication provider", e);
        }
    }

    @Override
    public Optional<UsernameRoles> findByUsernameAndPassword(
            final @NotNull String userName,
            final @NotNull byte @NotNull [] password) {
        try {
            if(ldapClient.authenticateUser(userName, password)) {
                return Optional.of(new UsernameRoles(userName, assignedRole));
            } else {
                return Optional.empty();
            }
        } catch (final LDAPException e) {
            log.error("Error during LDAP authentication for user {}", userName, e);
            return Optional.empty();
        } catch (final SearchFilterDnResolver.DnResolutionException e) {
            // User's DN could not be resolved (user doesn't exist or search failed)
            log.debug("DN resolution failed for user {}: {}", userName, e.getMessage());
            return Optional.empty();
        } catch (final IllegalArgumentException e) {
            log.debug("Invalid username or password format for user {}: {}", userName, e.getMessage());
            return Optional.empty();
        }
    }
}
