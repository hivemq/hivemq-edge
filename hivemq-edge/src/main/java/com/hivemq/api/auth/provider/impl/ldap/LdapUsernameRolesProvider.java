package com.hivemq.api.auth.provider.impl.ldap;

import com.hivemq.api.auth.provider.IUsernameRolesProvider;
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

    public LdapUsernameRolesProvider(final @NotNull LdapConnectionProperties ldapConnectionProperties) {
        this.ldapClient = new LdapClient(ldapConnectionProperties);
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
            final @NotNull String password) {
        try {
            if(ldapClient.authenticateUser(userName, password)) {
                return Optional.of(new UsernameRoles(userName, Set.of("ADMIN"))); //TODO MAKE CONFIGURABLE!!!!!
            } else {
                return Optional.empty();
            }
        } catch (final LDAPException e) {
            log.error("Error during LDAP authentication for user {}", userName, e);
            return Optional.empty();
        } catch (final IllegalArgumentException e) {
            log.debug("Invalid username or password format for user {}: {}", userName, e.getMessage());
            return Optional.empty();
        }
    }
}
