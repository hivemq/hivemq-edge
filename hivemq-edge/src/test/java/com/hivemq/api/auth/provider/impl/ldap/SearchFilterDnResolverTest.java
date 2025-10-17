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

import com.unboundid.ldap.sdk.LDAPConnectionPool;
import com.unboundid.ldap.sdk.LDAPSearchException;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.SearchRequest;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchScope;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SearchFilterDnResolver}.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SearchFilterDnResolverTest {

    @Mock
    private LDAPConnectionPool connectionPool;

    @Test
    void testConstructorValidation_emptySearchBase() {
        assertThatThrownBy(() -> new SearchFilterDnResolver(
                connectionPool,
                "",
                "(uid={username})",
                SearchScope.SUB,
                5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Search base cannot be empty");
    }

    @Test
    void testConstructorValidation_blankSearchBase() {
        assertThatThrownBy(() -> new SearchFilterDnResolver(
                connectionPool,
                "   ",
                "(uid={username})",
                SearchScope.SUB,
                5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Search base cannot be empty");
    }

    @Test
    void testConstructorValidation_emptySearchFilter() {
        assertThatThrownBy(() -> new SearchFilterDnResolver(
                connectionPool,
                "ou=people,dc=example,dc=com",
                "",
                SearchScope.SUB,
                5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Search filter template cannot be empty");
    }

    @Test
    void testConstructorValidation_searchFilterWithoutPlaceholder() {
        assertThatThrownBy(() -> new SearchFilterDnResolver(
                connectionPool,
                "ou=people,dc=example,dc=com",
                "(uid=hardcoded)",
                SearchScope.SUB,
                5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Search filter template must contain {username} placeholder");
    }

    @Test
    void testConstructorValidation_negativeTimeout() {
        assertThatThrownBy(() -> new SearchFilterDnResolver(
                connectionPool,
                "ou=people,dc=example,dc=com",
                "(uid={username})",
                SearchScope.SUB,
                -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Timeout cannot be negative");
    }

    @Test
    void testSimpleConstructor_usesDefaults() {
        final SearchFilterDnResolver resolver = new SearchFilterDnResolver(
                connectionPool,
                "ou=people,dc=example,dc=com",
                "(uid={username})"
        );

        assertThat(resolver.getSearchBase()).isEqualTo("ou=people,dc=example,dc=com");
        assertThat(resolver.getSearchFilterTemplate()).isEqualTo("(uid={username})");
        assertThat(resolver.getSearchScope()).isEqualTo(SearchScope.SUB);
        assertThat(resolver.getTimeoutSeconds()).isEqualTo(5);
    }

    @Test
    void testResolveDn_emptyUsername() {
        final SearchFilterDnResolver resolver = new SearchFilterDnResolver(
                connectionPool,
                "ou=people,dc=example,dc=com",
                "(uid={username})"
        );

        assertThatThrownBy(() -> resolver.resolveDn(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Username cannot be empty");
    }

    @Test
    void testResolveDn_blankUsername() {
        final SearchFilterDnResolver resolver = new SearchFilterDnResolver(
                connectionPool,
                "ou=people,dc=example,dc=com",
                "(uid={username})"
        );

        assertThatThrownBy(() -> resolver.resolveDn("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Username cannot be empty");
    }

    @Test
    void testResolveDn_successfulSearch() throws Exception {
        // Arrange
        final SearchFilterDnResolver resolver = new SearchFilterDnResolver(
                connectionPool,
                "ou=people,dc=example,dc=com",
                "(uid={username})",
                SearchScope.SUB,
                10
        );

        final SearchResultEntry mockEntry = mock(SearchResultEntry.class);
        when(mockEntry.getDN()).thenReturn("uid=jdoe,ou=people,dc=example,dc=com");

        final SearchResult mockResult = mock(SearchResult.class);
        when(mockResult.getResultCode()).thenReturn(ResultCode.SUCCESS);
        when(mockResult.getEntryCount()).thenReturn(1);
        when(mockResult.getSearchEntries()).thenReturn(Arrays.asList(mockEntry));

        when(connectionPool.search(any(SearchRequest.class))).thenReturn(mockResult);

        // Act
        final String dn = resolver.resolveDn("jdoe");

        // Assert
        assertThat(dn).isEqualTo("uid=jdoe,ou=people,dc=example,dc=com");

        // Verify search request parameters
        final ArgumentCaptor<SearchRequest> requestCaptor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(connectionPool).search(requestCaptor.capture());

        final SearchRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.getBaseDN()).isEqualTo("ou=people,dc=example,dc=com");
        assertThat(capturedRequest.getScope()).isEqualTo(SearchScope.SUB);
        assertThat(capturedRequest.getTimeLimitSeconds()).isEqualTo(10);
        assertThat(capturedRequest.getSizeLimit()).isEqualTo(1);
        assertThat(capturedRequest.getFilter().toString()).isEqualTo("(uid=jdoe)");
    }

    @Test
    void testResolveDn_noResultsFound() throws Exception {
        // Arrange
        final SearchFilterDnResolver resolver = new SearchFilterDnResolver(
                connectionPool,
                "ou=people,dc=example,dc=com",
                "(uid={username})"
        );

        final SearchResult mockResult = mock(SearchResult.class);
        when(mockResult.getResultCode()).thenReturn(ResultCode.SUCCESS);
        when(mockResult.getEntryCount()).thenReturn(0);

        when(connectionPool.search(any(SearchRequest.class))).thenReturn(mockResult);

        // Act & Assert
        assertThatThrownBy(() -> resolver.resolveDn("nonexistent"))
                .isInstanceOf(SearchFilterDnResolver.DnResolutionException.class)
                .hasMessageContaining("No LDAP entry found for username: nonexistent");
    }

    @Test
    void testResolveDn_multipleResultsFound_usesFirst() throws Exception {
        // Arrange
        final SearchFilterDnResolver resolver = new SearchFilterDnResolver(
                connectionPool,
                "dc=example,dc=com",
                "(uid={username})"
        );

        final SearchResultEntry mockEntry1 = mock(SearchResultEntry.class);
        when(mockEntry1.getDN()).thenReturn("uid=jdoe,ou=engineering,dc=example,dc=com");

        final SearchResultEntry mockEntry2 = mock(SearchResultEntry.class);
        when(mockEntry2.getDN()).thenReturn("uid=jdoe,ou=sales,dc=example,dc=com");

        final SearchResult mockResult = mock(SearchResult.class);
        when(mockResult.getResultCode()).thenReturn(ResultCode.SUCCESS);
        when(mockResult.getEntryCount()).thenReturn(2);
        when(mockResult.getSearchEntries()).thenReturn(Arrays.asList(mockEntry1, mockEntry2));

        when(connectionPool.search(any(SearchRequest.class))).thenReturn(mockResult);

        // Act
        final String dn = resolver.resolveDn("jdoe");

        // Assert - should use first result
        assertThat(dn).isEqualTo("uid=jdoe,ou=engineering,dc=example,dc=com");
    }

    @Test
    void testResolveDn_searchFailureResultCode() throws Exception {
        // Arrange
        final SearchFilterDnResolver resolver = new SearchFilterDnResolver(
                connectionPool,
                "ou=people,dc=example,dc=com",
                "(uid={username})"
        );

        final SearchResult mockResult = mock(SearchResult.class);
        when(mockResult.getResultCode()).thenReturn(ResultCode.NO_SUCH_OBJECT);
        when(mockResult.getDiagnosticMessage()).thenReturn("Search base does not exist");

        when(connectionPool.search(any(SearchRequest.class))).thenReturn(mockResult);

        // Act & Assert
        assertThatThrownBy(() -> resolver.resolveDn("jdoe"))
                .isInstanceOf(SearchFilterDnResolver.DnResolutionException.class)
                .hasMessageContaining("LDAP search failed with result code")
                .hasMessageContaining("no such object");
    }

    @Test
    void testResolveDn_searchTimeout() throws Exception {
        // Arrange
        final SearchFilterDnResolver resolver = new SearchFilterDnResolver(
                connectionPool,
                "ou=people,dc=example,dc=com",
                "(uid={username})",
                SearchScope.SUB,
                1
        );

        final LDAPSearchException timeoutException = new LDAPSearchException(
                ResultCode.TIME_LIMIT_EXCEEDED,
                "Search time limit exceeded"
        );

        when(connectionPool.search(any(SearchRequest.class))).thenThrow(timeoutException);

        // Act & Assert
        assertThatThrownBy(() -> resolver.resolveDn("jdoe"))
                .isInstanceOf(SearchFilterDnResolver.DnResolutionException.class)
                .hasMessageContaining("LDAP search timed out after 1 seconds")
                .hasCause(timeoutException);
    }

    @Test
    void testResolveDn_ldapException() throws Exception {
        // Arrange
        final SearchFilterDnResolver resolver = new SearchFilterDnResolver(
                connectionPool,
                "ou=people,dc=example,dc=com",
                "(uid={username})"
        );

        final LDAPSearchException ldapException = new LDAPSearchException(
                ResultCode.OPERATIONS_ERROR,
                "LDAP server error"
        );

        when(connectionPool.search(any(SearchRequest.class))).thenThrow(ldapException);

        // Act & Assert
        assertThatThrownBy(() -> resolver.resolveDn("jdoe"))
                .isInstanceOf(SearchFilterDnResolver.DnResolutionException.class)
                .hasMessageContaining("LDAP search failed")
                .hasCause(ldapException);
    }

    @Test
    void testResolveDn_specialCharactersInUsername() throws Exception {
        // Arrange
        final SearchFilterDnResolver resolver = new SearchFilterDnResolver(
                connectionPool,
                "ou=people,dc=example,dc=com",
                "(uid={username})"
        );

        final SearchResultEntry mockEntry = mock(SearchResultEntry.class);
        when(mockEntry.getDN()).thenReturn("uid=user\\(special\\),ou=people,dc=example,dc=com");

        final SearchResult mockResult = mock(SearchResult.class);
        when(mockResult.getResultCode()).thenReturn(ResultCode.SUCCESS);
        when(mockResult.getEntryCount()).thenReturn(1);
        when(mockResult.getSearchEntries()).thenReturn(Arrays.asList(mockEntry));

        when(connectionPool.search(any(SearchRequest.class))).thenReturn(mockResult);

        // Act
        final String dn = resolver.resolveDn("user(special)");

        // Assert
        assertThat(dn).isNotNull();

        // Verify that special characters were escaped in the filter
        final ArgumentCaptor<SearchRequest> requestCaptor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(connectionPool).search(requestCaptor.capture());

        final SearchRequest capturedRequest = requestCaptor.getValue();
        // The filter should have escaped the parentheses
        assertThat(capturedRequest.getFilter().toString()).contains("\\28").contains("\\29");
    }

    @Test
    void testResolveDn_complexFilter() throws Exception {
        // Arrange
        final SearchFilterDnResolver resolver = new SearchFilterDnResolver(
                connectionPool,
                "dc=example,dc=com",
                "(&(objectClass=inetOrgPerson)(uid={username}))",
                SearchScope.SUB,
                10
        );

        final SearchResultEntry mockEntry = mock(SearchResultEntry.class);
        when(mockEntry.getDN()).thenReturn("uid=jdoe,ou=people,dc=example,dc=com");

        final SearchResult mockResult = mock(SearchResult.class);
        when(mockResult.getResultCode()).thenReturn(ResultCode.SUCCESS);
        when(mockResult.getEntryCount()).thenReturn(1);
        when(mockResult.getSearchEntries()).thenReturn(Arrays.asList(mockEntry));

        when(connectionPool.search(any(SearchRequest.class))).thenReturn(mockResult);

        // Act
        final String dn = resolver.resolveDn("jdoe");

        // Assert
        assertThat(dn).isEqualTo("uid=jdoe,ou=people,dc=example,dc=com");

        final ArgumentCaptor<SearchRequest> requestCaptor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(connectionPool).search(requestCaptor.capture());

        final SearchRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.getFilter().toString()).isEqualTo("(&(objectClass=inetOrgPerson)(uid=jdoe))");
    }

    @Test
    void testResolveDn_orFilter() throws Exception {
        // Arrange - search by either uid or mail
        final SearchFilterDnResolver resolver = new SearchFilterDnResolver(
                connectionPool,
                "ou=people,dc=example,dc=com",
                "(|(uid={username})(mail={username}))"
        );

        final SearchResultEntry mockEntry = mock(SearchResultEntry.class);
        when(mockEntry.getDN()).thenReturn("uid=jdoe,ou=people,dc=example,dc=com");

        final SearchResult mockResult = mock(SearchResult.class);
        when(mockResult.getResultCode()).thenReturn(ResultCode.SUCCESS);
        when(mockResult.getEntryCount()).thenReturn(1);
        when(mockResult.getSearchEntries()).thenReturn(Arrays.asList(mockEntry));

        when(connectionPool.search(any(SearchRequest.class))).thenReturn(mockResult);

        // Act
        final String dn = resolver.resolveDn("jdoe@example.com");

        // Assert
        assertThat(dn).isNotNull();

        final ArgumentCaptor<SearchRequest> requestCaptor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(connectionPool).search(requestCaptor.capture());

        final SearchRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.getFilter().toString())
                .isEqualTo("(|(uid=jdoe@example.com)(mail=jdoe@example.com))");
    }

    @Test
    void testGetters() {
        final SearchFilterDnResolver resolver = new SearchFilterDnResolver(
                connectionPool,
                "ou=people,dc=example,dc=com",
                "(uid={username})",
                SearchScope.ONE,
                15
        );

        assertThat(resolver.getSearchBase()).isEqualTo("ou=people,dc=example,dc=com");
        assertThat(resolver.getSearchFilterTemplate()).isEqualTo("(uid={username})");
        assertThat(resolver.getSearchScope()).isEqualTo(SearchScope.ONE);
        assertThat(resolver.getTimeoutSeconds()).isEqualTo(15);
    }

    @Test
    void testDnResolutionException_withCause() {
        final Exception cause = new RuntimeException("test cause");
        final SearchFilterDnResolver.DnResolutionException exception =
                new SearchFilterDnResolver.DnResolutionException("test message", "testuser", cause);

        assertThat(exception.getMessage()).isEqualTo("test message");
        assertThat(exception.getUsername()).isEqualTo("testuser");
        assertThat(exception.getCause()).isEqualTo(cause);
    }

    @Test
    void testDnResolutionException_withoutCause() {
        final SearchFilterDnResolver.DnResolutionException exception =
                new SearchFilterDnResolver.DnResolutionException("test message", "testuser");

        assertThat(exception.getMessage()).isEqualTo("test message");
        assertThat(exception.getUsername()).isEqualTo("testuser");
        assertThat(exception.getCause()).isNull();
    }
}
