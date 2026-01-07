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
package com.hivemq.api.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.api.TestApiResource;
import com.hivemq.api.TestPermitAllApiResource;
import com.hivemq.api.TestResourceLevelRolesApiResource;
import com.hivemq.api.auth.handler.IAuthenticationHandler;
import com.hivemq.api.auth.provider.impl.ldap.LdapConnectionProperties;
import com.hivemq.api.auth.provider.impl.ldap.LdapUsernameRolesProvider;
import com.hivemq.api.auth.provider.impl.ldap.TlsMode;
import com.hivemq.api.auth.provider.impl.ldap.testcontainer.LdapTestConnection;
import com.hivemq.api.auth.provider.impl.ldap.testcontainer.LldapContainer;
import com.hivemq.bootstrap.ioc.Injector;
import com.hivemq.configuration.service.ApiConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.http.HttpConstants;
import com.hivemq.http.JaxrsHttpServer;
import com.hivemq.http.config.JaxrsHttpServerConfiguration;
import com.hivemq.http.core.HttpUrlConnectionClient;
import com.hivemq.logging.SecurityLog;
import com.unboundid.ldap.sdk.SearchScope;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.hivemq.api.auth.ApiRoles.ADMIN;
import static com.hivemq.api.auth.provider.impl.ldap.testcontainer.LdapTestConnection.TEST_PASSWORD;
import static com.hivemq.api.auth.provider.impl.ldap.testcontainer.LdapTestConnection.TEST_USERNAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Testcontainers
public class LdapAuthenticationTests {

    @Container
    private static final LldapContainer LLDAP_CONTAINER = new LldapContainer();

    protected final Logger logger = LoggerFactory.getLogger(LdapAuthenticationTests.class);

    static final int TEST_HTTP_PORT = 8088;
    static final int CONNECT_TIMEOUT = 1000;
    static final int READ_TIMEOUT = 1000;
    static final String HTTP = "http";

    protected static JaxrsHttpServer server;

    @Mock
    private static Injector injector;
    @Mock
    private static ApiConfigurationService apiConfigurationService;
    @BeforeAll
    static void setUp() throws Exception {
        // Get the dynamically mapped port from the container
        final var host = LLDAP_CONTAINER.getHost();
        final var port = LLDAP_CONTAINER.getLdapPort();

        // Create LdapSimpleBind for LLDAP admin authentication
        // Note: The RDN should be just the user's identifier, the organizational unit
        // will be added from the rdns parameter (getBaseDn() returns "ou=people,{baseDn}")
        final var ldapSimpleBind =
                new LdapConnectionProperties.LdapSimpleBind(
                        "uid=" + LLDAP_CONTAINER.getAdminUsername(),
                        LLDAP_CONTAINER.getAdminPassword());

        // Create connection properties for plain LDAP (no TLS for simplicity)
        // 5 second connect timeout
        // 10 second response timeout
        final var ldapConnectionProperties =
                new LdapConnectionProperties(new LdapConnectionProperties.LdapServers(
                        new String[]{host}, new int[]{port}),
                        TlsMode.NONE,
                        null,
                        5000,  // 5 second connect timeout
                        10000, // 10 second response timeout
                        1,
                        "uid",
                        getBaseDn(),
                        null,
                        SearchScope.SUB,
                        5,
                        ADMIN,
                        false,
                        ldapSimpleBind,
                        null);

        // Create test user in LLDAP
        new LdapTestConnection(ldapConnectionProperties).createTestUser(
                LLDAP_CONTAINER.getAdminDn(),
                LLDAP_CONTAINER.getAdminPassword(),
                LLDAP_CONTAINER.getBaseDn());

        final var config = new JaxrsHttpServerConfiguration();
        config.setPort(TEST_HTTP_PORT);

        final Set<IAuthenticationHandler> authenticationHandlers = new HashSet<>();
        authenticationHandlers.add(new BasicAuthenticationHandler(new LdapUsernameRolesProvider(ldapConnectionProperties, new SecurityLog())));

        apiConfigurationService = mock(ApiConfigurationService.class);
        when(apiConfigurationService.isEnforceApiAuth()).thenReturn(true);

        final ResourceConfig conf = new ResourceConfig(){{
            register(new ApiAuthenticationFeature(authenticationHandlers,apiConfigurationService));
        }
        };
        conf.register(TestApiResource.class);
        conf.register(TestPermitAllApiResource.class);
        conf.register(TestResourceLevelRolesApiResource.class);
        //-- ensure we supplied our own test mapper as this can effect output
        final var mapper = new ObjectMapper();
        config.setObjectMapper(mapper);
        server = new JaxrsHttpServer(mock(), List.of(config), conf);
        server.startServer();
    }

    public static String getBaseDn() {
        return "ou=people," + LLDAP_CONTAINER.getBaseDn();
    }

    @AfterAll
    public static void tearDown(){
        server.stopServer();
    }

    protected static String getTestServerAddress(final @NotNull String protocol, final @NotNull int port, final @NotNull String uri){
        return String.format("%s://%s:%s/%s", protocol, "localhost", port, uri);
    }

    @Test
    public void testGetSecuredResourceWithoutCreds() throws IOException {
        final var response =
                HttpUrlConnectionClient.get(null,
                        getTestServerAddress(HTTP, TEST_HTTP_PORT, "test/get/auth/admin"), CONNECT_TIMEOUT, READ_TIMEOUT);
        assertThat(response.getStatusCode())
                .as("Resource should be denied")
                .isEqualTo(401);
    }

    @Test
    public void testGetSecuredResourceWithInvalidUsername() throws IOException {
        final var headers = Map.of(HttpConstants.AUTH_HEADER,
                BasicAuthenticationHandler.getBasicAuthenticationHeaderValue("testaWRONG", TEST_PASSWORD));
        final var response =
                HttpUrlConnectionClient.get(headers,
                        getTestServerAddress(HTTP, TEST_HTTP_PORT, "test/get/auth/admin"), CONNECT_TIMEOUT, READ_TIMEOUT);
        assertThat(response.getStatusCode())
                .as("Resource should be denied")
                .isEqualTo(401);
    }

    @Test
    public void testGetSecuredResourceWithInvalidPassword() throws IOException {
        final var headers = Map.of(HttpConstants.AUTH_HEADER,
                BasicAuthenticationHandler.getBasicAuthenticationHeaderValue(TEST_USERNAME, "incorrect"));
        final var response =
                HttpUrlConnectionClient.get(headers,
                        getTestServerAddress(HTTP, TEST_HTTP_PORT, "test/get/auth/admin"), CONNECT_TIMEOUT, READ_TIMEOUT);
        assertThat(response.getStatusCode())
                .as("Resource should be denied")
                .isEqualTo(401);
    }

    @Test
    public void testGetSecuredResourceWithValidCreds() throws IOException {
        final var headers = Map.of(HttpConstants.AUTH_HEADER,
                BasicAuthenticationHandler.getBasicAuthenticationHeaderValue(TEST_USERNAME, TEST_PASSWORD));
        final var response =
                HttpUrlConnectionClient.get(headers,
                        getTestServerAddress(HTTP, TEST_HTTP_PORT, "test/get/auth/admin"), CONNECT_TIMEOUT, READ_TIMEOUT);
        assertThat(response.getStatusCode())
                .as("Resource should be accepted")
                .isEqualTo(200);
    }
}
