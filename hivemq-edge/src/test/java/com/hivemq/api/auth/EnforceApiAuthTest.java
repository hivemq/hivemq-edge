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
import com.hivemq.api.AuthTestUtils;
import com.hivemq.api.TestApiResource;
import com.hivemq.api.TestPermitAllApiResource;
import com.hivemq.api.TestResourceLevelRolesApiResource;
import com.hivemq.api.auth.handler.IAuthenticationHandler;
import com.hivemq.bootstrap.ioc.Injector;
import com.hivemq.configuration.service.ApiConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.http.HttpConstants;
import com.hivemq.http.JaxrsHttpServer;
import com.hivemq.http.config.JaxrsHttpServerConfiguration;
import com.hivemq.http.core.HttpUrlConnectionClient;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author martin Schoenert
 *
 * This is basically a copy of the BasicAuthenticationTests,
 * but this time we do NOT enforce the user roles,
 * so the tests that expect role enforcement now return success!
 * these tests have a comment
 * // succeeds because api auth is not enforced
 */
public class EnforceApiAuthTest {

    protected final Logger logger = LoggerFactory.getLogger(EnforceApiAuthTest.class);

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
    public static void setUp() throws Exception {
        final var config = new JaxrsHttpServerConfiguration();
        config.setPort(TEST_HTTP_PORT);

        final Set<IAuthenticationHandler > authenticationHandlers = new HashSet<>();
        authenticationHandlers.add(new BasicAuthenticationHandler(AuthTestUtils.createTestUsernamePasswordProvider()));

        apiConfigurationService = mock(ApiConfigurationService.class);
        when(apiConfigurationService.isEnforceApiAuth()).thenReturn(false);

        final var conf = new ResourceConfig(){{
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
    @AfterAll
    public static void tearDown(){
        server.stopServer();
    }

    protected static String getTestServerAddress(final @NotNull String protocol, final @NotNull int port, final @NotNull String uri){
        return String.format("%s://%s:%s/%s", protocol, "localhost", port, uri);
    }

    //TODO there are a number of tests where the message is the wrong way around (says allowed when it should say denied)

    @Test
    public void testGetSecuredResourceWithoutCreds() throws IOException {
        final var response =
                HttpUrlConnectionClient.get(null,
                        getTestServerAddress(HTTP, TEST_HTTP_PORT, "test/get/auth/admin"), CONNECT_TIMEOUT, READ_TIMEOUT);
        assertEquals(401,response.getStatusCode(),"Resource should be denied");
    }

    @Test
    public void testGetSecuredResourceWithInvalidUsername() throws IOException {
        final var headers = Map.of(HttpConstants.AUTH_HEADER,
                BasicAuthenticationHandler.getBasicAuthenticationHeaderValue("testaWRONG", "test"));
        final var response =
                HttpUrlConnectionClient.get(headers,
                        getTestServerAddress(HTTP, TEST_HTTP_PORT, "test/get/auth/admin"), CONNECT_TIMEOUT, READ_TIMEOUT);
        assertEquals(401,response.getStatusCode(),"Resource should be denied");
    }

    @Test
    public void testGetSecuredResourceWithInvalidPassword() throws IOException {
        final var headers = Map.of(HttpConstants.AUTH_HEADER,
                BasicAuthenticationHandler.getBasicAuthenticationHeaderValue("testadmin", "incorrect"));
        final var response =
                HttpUrlConnectionClient.get(headers,
                        getTestServerAddress(HTTP, TEST_HTTP_PORT, "test/get/auth/admin"), CONNECT_TIMEOUT, READ_TIMEOUT);
        assertEquals(401,response.getStatusCode(),"Resource should be denied");
    }

    @Test
    public void testGetSecuredResourceWithValidCredsInvalidRole() throws IOException {
        final var headers = Map.of(HttpConstants.AUTH_HEADER,
                BasicAuthenticationHandler.getBasicAuthenticationHeaderValue("testuser", "test"));
        final var response =
                HttpUrlConnectionClient.get(headers,
                        getTestServerAddress(HTTP, TEST_HTTP_PORT, "test/get/auth/admin"), CONNECT_TIMEOUT, READ_TIMEOUT);
        // succeeds because user roles are not enforced
        assertEquals(200,response.getStatusCode(),"Resource should be allowed (because user roles are not enforced)");
    }

    @Test
    public void testGetSecuredResourceWithValidCreds() throws IOException {
        final var headers = Map.of(HttpConstants.AUTH_HEADER,
                BasicAuthenticationHandler.getBasicAuthenticationHeaderValue("testadmin", "test"));
        final var response =
                HttpUrlConnectionClient.get(headers,
                        getTestServerAddress(HTTP, TEST_HTTP_PORT, "test/get/auth/admin"), CONNECT_TIMEOUT, READ_TIMEOUT);
        assertEquals(200,response.getStatusCode(),"Resource should be accepted");
    }

    @Test
    public void testGetSecuredResourceWithValidCredsMultipleRole() throws IOException {
        final var headers = Map.of(HttpConstants.AUTH_HEADER,
                BasicAuthenticationHandler.getBasicAuthenticationHeaderValue("testadmin", "test"));
        final var response =
                HttpUrlConnectionClient.get(headers,
                        getTestServerAddress(HTTP, TEST_HTTP_PORT, "test/get/auth/user"), CONNECT_TIMEOUT, READ_TIMEOUT);
        assertEquals(200,response.getStatusCode(),"Resource should be accepted");
    }

    @Test
    public void testUserInNoRole() throws IOException {
        final var headers = Map.of(HttpConstants.AUTH_HEADER,
                BasicAuthenticationHandler.getBasicAuthenticationHeaderValue("testnorole", "test"));
        final var response =
                HttpUrlConnectionClient.get(headers,
                        getTestServerAddress(HTTP, TEST_HTTP_PORT, "test/get/auth/user"), CONNECT_TIMEOUT, READ_TIMEOUT);
        // succeeds because user roles are not enforced
        assertEquals(200,response.getStatusCode(),"Resource should be allowed (because user roles are not enforced)");
    }

    @Test
    public void testPermitAllAllowsAnyAuthenticated() throws IOException {
        final var headers = Map.of(HttpConstants.AUTH_HEADER,
                BasicAuthenticationHandler.getBasicAuthenticationHeaderValue("testnorole", "test"));
        final var response =
                HttpUrlConnectionClient.get(headers,
                        getTestServerAddress(HTTP, TEST_HTTP_PORT, "test/permitall/get"), CONNECT_TIMEOUT, READ_TIMEOUT);
        assertEquals(200,response.getStatusCode(),"Resource should be allowed");
    }

    @Test
    public void testPermitAllRejectsNonAuthenticated() throws IOException {
        final var response =
                HttpUrlConnectionClient.get(null,
                        getTestServerAddress(HTTP, TEST_HTTP_PORT, "test/permitall/get"), CONNECT_TIMEOUT, READ_TIMEOUT);
        assertEquals(401,response.getStatusCode(),"Resource should be allowed");
    }

    @Test
    public void testResourceLevelRoleRejectsNonAuthenticated() throws IOException {
        final var response =
                HttpUrlConnectionClient.get(null,
                        getTestServerAddress(HTTP, TEST_HTTP_PORT, "test/resource/get"), CONNECT_TIMEOUT, READ_TIMEOUT);
        assertEquals(401,response.getStatusCode(),"Resource should not be allowed");
    }

    @Test
    public void testResourceLevelRoleAllowsAuthenticated() throws IOException {
        final var headers = Map.of(HttpConstants.AUTH_HEADER,
                BasicAuthenticationHandler.getBasicAuthenticationHeaderValue("testuser", "test"));
        final var response =
                HttpUrlConnectionClient.get(headers,
                        getTestServerAddress(HTTP, TEST_HTTP_PORT, "test/resource/get"), CONNECT_TIMEOUT, READ_TIMEOUT);
        assertEquals(200,response.getStatusCode(),"Resource should be allowed");
    }

    //TODO this test is misnomed: the testuser is authenticated, they just don't have the required admin role
    @Test
    public void testMethodsLevelOverridesResourceLevelRoleAllowsNonAuthenticated() throws IOException {
        final var headers = Map.of(HttpConstants.AUTH_HEADER,
                BasicAuthenticationHandler.getBasicAuthenticationHeaderValue("testuser", "test"));
        final var response =
                HttpUrlConnectionClient.get(headers,
                        getTestServerAddress(HTTP, TEST_HTTP_PORT, "test/resource/get/onlyadmin"), CONNECT_TIMEOUT, READ_TIMEOUT);
        // succeeds because user roles are not enforced
        assertEquals(200,response.getStatusCode(),"Resource should be allowed (because user roles are not enforced)");
    }

    @Test
    public void testMethodsLevelOverridesResourceLevelRoleAllowsAuthenticated() throws IOException {
        final var headers = Map.of(HttpConstants.AUTH_HEADER,
                BasicAuthenticationHandler.getBasicAuthenticationHeaderValue("testadmin", "test"));
        final var response =
                HttpUrlConnectionClient.get(headers,
                        getTestServerAddress(HTTP, TEST_HTTP_PORT, "test/resource/get/onlyadmin"), CONNECT_TIMEOUT, READ_TIMEOUT);
        assertEquals(200,response.getStatusCode(),"Resource should be allowed");
    }


    @Test
    public void testPermitAllOverriddenByMethodNonAuthenticated() throws IOException {
        final var headers = Map.of(HttpConstants.AUTH_HEADER,
                BasicAuthenticationHandler.getBasicAuthenticationHeaderValue("testuser", "test"));
        final var response =
                HttpUrlConnectionClient.get(headers,
                        getTestServerAddress(HTTP, TEST_HTTP_PORT, "test/permitall/get/adminonly"), CONNECT_TIMEOUT, READ_TIMEOUT);
        // succeeds because user roles are not enforced
        assertEquals(200,response.getStatusCode(),"Resource should be allowed (because user roles are not enforced)");
    }

    @Test
    public void testPermitAllOverriddenByMethodAuthenticated() throws IOException {
        final var headers = Map.of(HttpConstants.AUTH_HEADER,
                BasicAuthenticationHandler.getBasicAuthenticationHeaderValue("testadmin", "test"));
        final var response =
                HttpUrlConnectionClient.get(headers,
                        getTestServerAddress(HTTP, TEST_HTTP_PORT, "test/permitall/get/adminonly"), CONNECT_TIMEOUT, READ_TIMEOUT);
        assertEquals(200,response.getStatusCode(),"Resource should not be allowed");
    }

}
