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
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPSearchException;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.SearchRequest;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchScope;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Search filter-based DN resolver that performs an LDAP search to find a user's Distinguished Name.
 * <p>
 * Unlike {@link TemplateDnResolver} which constructs the DN using string templates, this resolver
 * queries the LDAP directory to find the user's actual DN. This is useful when:
 * <ul>
 *     <li>User DNs follow complex or unpredictable patterns</li>
 *     <li>Users are scattered across multiple organizational units</li>
 *     <li>The DN structure varies between different user types</li>
 *     <li>Integration with Active Directory where DNs can be complex</li>
 * </ul>
 * <p>
 * Example search filters:
 * <pre>
 * Simple UID search:     "(uid={username})"
 * Email search:          "(mail={username})"
 * Active Directory:      "(sAMAccountName={username})"
 * Multiple attributes:   "(|(uid={username})(mail={username}))"
 * Complex filter:        "(&(objectClass=inetOrgPerson)(uid={username}))"
 * </pre>
 * <p>
 * <strong>Performance Note:</strong> This resolver performs an LDAP search for each DN resolution,
 * which adds latency compared to template-based resolution. Consider using {@link TemplateDnResolver}
 * if your LDAP structure is simple and predictable.
 */
public class SearchFilterDnResolver implements UserDnResolver {

    private static final @NotNull Logger log = LoggerFactory.getLogger(SearchFilterDnResolver.class);
    private static final @NotNull String USERNAME_PLACEHOLDER = "{username}";

    private final @NotNull LDAPConnectionPool connectionPool;
    private final @NotNull String searchBase;
    private final @NotNull String searchFilterTemplate;
    private final @NotNull SearchScope searchScope;
    private final int timeoutSeconds;

    /**
     * Creates a new search filter-based DN resolver.
     *
     * @param connectionPool       The LDAP connection pool to use for searches
     * @param searchBase           The base DN where the search should start (e.g., "ou=people,dc=example,dc=com")
     * @param searchFilterTemplate The LDAP search filter with {username} placeholder (e.g., "(uid={username})")
     * @param searchScope          The search scope (ONE_LEVEL, SUBTREE, or BASE)
     * @param timeoutSeconds       Search timeout in seconds (0 = no timeout)
     * @throws IllegalArgumentException if any parameter is invalid
     */
    public SearchFilterDnResolver(
            final @NotNull LDAPConnectionPool connectionPool,
            final @NotNull String searchBase,
            final @NotNull String searchFilterTemplate,
            final @NotNull SearchScope searchScope,
            final int timeoutSeconds) {
        if (searchBase.isBlank()) {
            throw new IllegalArgumentException("Search base cannot be empty");
        }
        if (searchFilterTemplate.isBlank()) {
            throw new IllegalArgumentException("Search filter template cannot be empty");
        }
        if (!searchFilterTemplate.contains(USERNAME_PLACEHOLDER)) {
            throw new IllegalArgumentException("Search filter template must contain {username} placeholder");
        }
        if (timeoutSeconds < 0) {
            throw new IllegalArgumentException("Timeout cannot be negative: " + timeoutSeconds);
        }

        this.connectionPool = connectionPool;
        this.searchBase = searchBase;
        this.searchFilterTemplate = searchFilterTemplate;
        this.searchScope = searchScope;
        this.timeoutSeconds = timeoutSeconds;
    }

    /**
     * Creates a resolver with SUBTREE scope and 5 second timeout (common defaults).
     *
     * @param connectionPool       The LDAP connection pool
     * @param searchBase           The base DN for searching
     * @param searchFilterTemplate The search filter with {username} placeholder
     */
    public SearchFilterDnResolver(
            final @NotNull LDAPConnectionPool connectionPool,
            final @NotNull String searchBase,
            final @NotNull String searchFilterTemplate) {
        this(connectionPool, searchBase, searchFilterTemplate, SearchScope.SUB, 5);
    }

    @Override
    public @NotNull String resolveDn(final @NotNull String username) {
        if (username.isBlank()) {
            throw new IllegalArgumentException("Username cannot be empty");
        }

        // Replace {username} placeholder with actual username and create LDAP filter
        final String filterString = searchFilterTemplate.replace(USERNAME_PLACEHOLDER, escapeFilterValue(username));

        try {
            final Filter filter = Filter.create(filterString);

            final SearchRequest searchRequest = new SearchRequest(
                    searchBase,
                    searchScope,
                    DereferencePolicy.NEVER,
                    1, // Size limit - we only need one result
                    timeoutSeconds,
                    false, // Types only = false (we want the full entry)
                    filter,
                    "1.1" // Return no attributes, we only need the DN
            );

            log.debug("Searching for user DN with filter: {} in base: {}", filterString, searchBase);

            final SearchResult searchResult = connectionPool.search(searchRequest);

            if (searchResult.getResultCode() != ResultCode.SUCCESS) {
                throw new DnResolutionException(
                        "LDAP search failed with result code: " + searchResult.getResultCode() +
                        ", diagnostic message: " + searchResult.getDiagnosticMessage(),
                        username);
            }

            final int entryCount = searchResult.getEntryCount();
            if (entryCount == 0) {
                log.debug("No LDAP entry found for username: {} with filter: {}", username, filterString);
                throw new DnResolutionException(
                        "No LDAP entry found for username: " + username,
                        username);
            }

            if (entryCount > 1) {
                log.warn("Multiple LDAP entries ({}) found for username: {} with filter: {}. Using first result.",
                        entryCount, username, filterString);
            }

            final SearchResultEntry entry = searchResult.getSearchEntries().getFirst();
            final String dn = entry.getDN();

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
        } catch (final LDAPException e) {
            log.error("Invalid LDAP filter: {}", filterString, e);
            throw new DnResolutionException(
                    "Invalid LDAP filter: " + filterString,
                    username,
                    e);
        }
    }

    /**
     * Escapes special characters in LDAP filter values according to RFC 4515.
     * <p>
     * Escapes: * ( ) \ NUL
     * <p>
     * This prevents LDAP injection attacks when the username contains special characters.
     *
     * @param value The value to escape
     * @return The escaped value safe for use in LDAP filters
     */
    private @NotNull String escapeFilterValue(final @NotNull String value) {
        // UnboundID SDK provides built-in escaping
        return Filter.encodeValue(value);
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
    public @NotNull String getSearchFilterTemplate() {
        return searchFilterTemplate;
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
}
