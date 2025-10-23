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

import com.unboundid.ldap.sdk.DereferencePolicy;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPConnectionPool;
import com.unboundid.ldap.sdk.LDAPSearchException;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.SearchRequest;
import com.unboundid.ldap.sdk.SearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Search filter-based DN resolver that performs an LDAP search to find a user's Distinguished Name.
 * <p>
 * <strong>Performance Note:</strong> This resolver performs an LDAP search for each DN resolution,
 * which adds latency compared to template-based resolution. Consider using {@link TemplateDnResolver}
 * if your LDAP structure is simple and predictable.
 */
public class SearchFilterDnResolver implements UserDnResolver {

    private static final @NotNull Logger log = LoggerFactory.getLogger(SearchFilterDnResolver.class);

    private final @NotNull LDAPConnectionPool connectionPool;
    private final @NotNull String searchBase;
    private final @NotNull String uidAttribute;
    private final @NotNull SearchScope searchScope;
    private final @Nullable String requiredObjectClass;
    private final int timeoutSeconds;

    /**
     * Creates a new search filter-based DN resolver.
     *
     * @param connectionPool       The LDAP connection pool to use for searches
     * @param searchBase           The base DN where the search should start (e.g., "ou=people,dc=example,dc=com")
     * @param uidAttribute         The attribute to be used for the query (e.g., "uid")
     * @param searchScope          The search scope (ONE_LEVEL, SUBTREE, or BASE)
     * @param timeoutSeconds       Search timeout in seconds (0 = no timeout)
     * @throws IllegalArgumentException if any parameter is invalid
     */
    public SearchFilterDnResolver(
            final @NotNull LDAPConnectionPool connectionPool,
            final @NotNull String searchBase,
            final @NotNull String uidAttribute,
            final @Nullable String requiredObjectClass,
            final @NotNull SearchScope searchScope,
            final int timeoutSeconds) {
        if (searchBase.isBlank()) {
            throw new IllegalArgumentException("Search base cannot be empty");
        }
        if (uidAttribute.isBlank()) {
            throw new IllegalArgumentException("Search filter template cannot be empty");
        }
        if (timeoutSeconds < 0) {
            throw new IllegalArgumentException("Timeout cannot be negative: " + timeoutSeconds);
        }

        this.connectionPool = connectionPool;
        this.searchBase = searchBase;
        this.uidAttribute = uidAttribute;
        this.searchScope = searchScope;
        this.timeoutSeconds = timeoutSeconds;
        this.requiredObjectClass = requiredObjectClass;
    }

    @Override
    public @NotNull String resolveDn(final @NotNull String username) {
        if (username.isBlank()) {
            throw new IllegalArgumentException("Username cannot be empty");
        }

        try {
            final var filter = userFilter(username);

            final var searchRequest = new SearchRequest(
                    searchBase,
                    searchScope,
                    DereferencePolicy.NEVER,
                    1, // Size limit - we only need one result
                    timeoutSeconds,
                    false, // Types only = false (we want the full entry)
                    filter,
                    "1.1" // Return no attributes, we only need the DN
            );

            log.debug("Searching for user DN with uidAttribute: {} in base: {}", uidAttribute, searchBase);

            final var searchResult = connectionPool.search(searchRequest);

            if (searchResult.getResultCode() != ResultCode.SUCCESS) {
                throw new DnResolutionException(
                        "LDAP search failed with result code: " + searchResult.getResultCode() +
                        ", diagnostic message: " + searchResult.getDiagnosticMessage(),
                        username);
            }

            final var entryCount = searchResult.getEntryCount();
            if (entryCount == 0) {
                log.debug("No LDAP entry found for username: {} with uidAttribute: {}", username, uidAttribute);
                throw new DnResolutionException(
                        "No LDAP entry found for username: " + username,
                        username);
            }

            if (entryCount > 1) {
                log.warn("Multiple LDAP entries ({}) found for username: {} with uidAttribute: {}. Using first result.",
                        entryCount, username, uidAttribute);
            }

            final var entry = searchResult.getSearchEntries().getFirst();
            final var dn = entry.getDN();

            log.debug("Resolved username '{}' to DN: {}", username, dn);
            return dn;

        } catch (final LDAPSearchException e) {
            if (e.getResultCode() == ResultCode.TIME_LIMIT_EXCEEDED) {
                log.error("LDAP search timed out after {} seconds for username: {}", timeoutSeconds, username);
                throw new DnResolutionException(
                        "LDAP search timed out after " + timeoutSeconds + " seconds",
                        username,
                        e);
            }
            log.error("LDAP search failed for username: {}, error: {}", username, e.getMessage());
            throw new DnResolutionException(
                    "LDAP search failed: " + e.getMessage(),
                    username,
                    e);
        }
    }

    /**
     * Returns the search base used by this resolver.
     *
     * @return The search base DN
     */
    public @NotNull String getSearchBase() {
        return searchBase;
    }

    /**
     * Returns the search filter template used by this resolver.
     *
     * @return The search filter template with {username} placeholder
     */
    public @NotNull String getUidAttribute() {
        return uidAttribute;
    }

    /**
     * Returns the search scope used by this resolver.
     *
     * @return The search scope
     */
    public @NotNull SearchScope getSearchScope() {
        return searchScope;
    }

    /**
     * Returns the timeout in seconds used by this resolver.
     *
     * @return The timeout in seconds
     */
    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    /**
     * Exception thrown when a user's DN cannot be resolved through LDAP search.
     */
    public static class DnResolutionException extends RuntimeException {
        private final @NotNull String username;

        public DnResolutionException(final @NotNull String message, final @NotNull String username) {
            super(message);
            this.username = username;
        }

        public DnResolutionException(
                final @NotNull String message,
                final @NotNull String username,
                final @NotNull Throwable cause) {
            super(message, cause);
            this.username = username;
        }

        public @NotNull String getUsername() {
            return username;
        }
    }

    private @NotNull Filter userFilter(final @NotNull String key) {
        final Filter keyFilter = Filter.createEqualityFilter(uidAttribute, key);

        if (requiredObjectClass == null) {
            return keyFilter;
        }
        return Filter.createANDFilter(keyFilter,
                Filter.createEqualityFilter("objectClass", requiredObjectClass));
    }
}
