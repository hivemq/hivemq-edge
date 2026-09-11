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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.api.AuthTestUtils;
import com.hivemq.api.TestApiResource;
import com.hivemq.api.TestPermitAllApiResource;
import com.hivemq.api.TestResourceLevelRolesApiResource;
import com.hivemq.api.auth.handler.IAuthenticationHandler;
import com.hivemq.configuration.service.ApiConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.http.HttpConstants;
import com.hivemq.http.JaxrsHttpServer;
import com.hivemq.http.config.JaxrsHttpServerConfiguration;
import com.hivemq.http.core.HttpResponse;
import com.hivemq.http.core.HttpUrlConnectionClient;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.glassfish.jersey.server.ResourceConfig;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.RandomPortGenerator;

/**
 * @author Simon L Johnson
 */
public class BasicAuthenticationTests {

    protected final Logger logger = LoggerFactory.getLogger(BasicAuthenticationTests.class);

    // A random free port, so that tests running in parallel do not conflict. A conflict would surface
    // as a ProcessingException out of startServer(), logged as "The port ... is already in use".
    private static int testHttpPort;
    // See BearerTokenAuthTests: the first request pays the server bootstrap cost and a 1s budget
    // makes whichever test runs first flaky on a loaded CI agent. Matches JaxrsResourceTests.
    static final int CONNECT_TIMEOUT = 5000;
    static final int READ_TIMEOUT = 5000;
    static final String HTTP = "http";

    protected static JaxrsHttpServer server;

    @BeforeAll
    public static void setUp() throws Exception {
        testHttpPort = RandomPortGenerator.get();
        final var config = new JaxrsHttpServerConfiguration();
        config.setPort(testHttpPort);
        // -- ensure we supplied our own test mapper as this can affect output
        config.setObjectMapper(new ObjectMapper());

        final Set<IAuthenticationHandler> authenticationHandlers = new HashSet<>();
        authenticationHandlers.add(new BasicAuthenticationHandler(AuthTestUtils.createTestUsernamePasswordProvider()));
        final var apiConfigurationService = mock(ApiConfigurationService.class);
        when(apiConfigurationService.isEnforceApiAuth()).thenReturn(true);
        final ApiAuthenticationFeature apiAuthenticationFeature =
                new ApiAuthenticationFeature(authenticationHandlers, apiConfigurationService);

        final var resourceConfig = new ResourceConfig();
        resourceConfig.register(apiAuthenticationFeature);
        resourceConfig.register(TestApiResource.class);
        resourceConfig.register(TestPermitAllApiResource.class);
        resourceConfig.register(TestResourceLevelRolesApiResource.class);

        server = new JaxrsHttpServer(mock(), List.of(config), resourceConfig);
        server.startServer();
    }

    @AfterAll
    public static void tearDown() {
        server.stopServer();
    }

    protected static HttpResponse get(
            final @NotNull String path, final @Nullable String username, final @Nullable String password)
            throws IOException {
        final Map<String, String> headers;
        if (username != null && password != null) {
            headers = Map.of(
                    HttpConstants.AUTH_HEADER,
                    BasicAuthenticationHandler.getBasicAuthenticationHeaderValue(username, password));
        } else {
            headers = null;
        }
        final var serverAddress = String.format("%s://%s:%s/%s", HTTP, "localhost", testHttpPort, path);
        return HttpUrlConnectionClient.get(headers, serverAddress, CONNECT_TIMEOUT, READ_TIMEOUT);
    }

    @Test
    public void testGetSecuredResourceWithoutCreds() throws IOException {
        final var response = get("test/get/auth/admin", null, null);
        assertEquals(401, response.getStatusCode(), "Resource should be denied, without credentials");
    }

    @Test
    public void testGetSecuredResourceWithInvalidUsername() throws IOException {
        final var response = get("test/get/auth/admin", "testaWRONG", "test");
        assertEquals(401, response.getStatusCode(), "Resource should be denied, invalid username");
    }

    @Test
    public void testGetSecuredResourceWithInvalidPassword() throws IOException {
        final var response = get("test/get/auth/admin", "testadmin", "incorrect");
        assertEquals(401, response.getStatusCode(), "Resource should be denied, invalid password");
    }

    @Test
    public void testGetSecuredResourceWithValidCredsInvalidRole() throws IOException {
        final var response = get("test/get/auth/admin", "testuser", "test");
        assertEquals(403, response.getStatusCode(), "Resource should be denied, invalid role");
    }

    @Test
    public void testGetSecuredResourceWithValidCreds() throws IOException {
        final var response = get("test/get/auth/admin", "testadmin", "test");
        assertEquals(200, response.getStatusCode(), "Resource should be allowed");
    }

    @Test
    public void testGetSecuredResourceWithValidCredsMultipleRole() throws IOException {
        final var response = get("test/get/auth/user", "testadmin", "test");
        assertEquals(200, response.getStatusCode(), "Resource should be allowed");
    }

    @Test
    public void testUserInNoRole() throws IOException {
        final var response = get("test/get/auth/user", "testnorole", "test");
        assertEquals(403, response.getStatusCode(), "Resource should be denied, no role");
    }

    @Test
    public void testPermitAllAllowsAnyAuthenticated() throws IOException {
        final var response = get("test/permitall/get", "testnorole", "test");
        assertEquals(200, response.getStatusCode(), "Resource should be allowed, permit all");
    }

    @Test
    public void testPermitAllRejectsNonAuthenticated() throws IOException {
        final var response = get("test/permitall/get", null, null);
        assertEquals(401, response.getStatusCode(), "Resource should be denied, permit all but not authenticated");
    }

    @Test
    public void testResourceLevelRoleRejectsNonAuthenticated() throws IOException {
        final var response = get("test/resource/get", null, null);
        assertEquals(401, response.getStatusCode(), "Resource should be denied, permit all but not authenticated");
    }

    @Test
    public void testResourceLevelRoleAllowsAuthenticated() throws IOException {
        final var response = get("test/resource/get", "testuser", "test");
        assertEquals(200, response.getStatusCode(), "Resource should be allowed");
    }

    @Test
    public void testMethodsLevelOverridesResourceLevelRoleNotAdminRole() throws IOException {
        final var response = get("test/resource/get/onlyadmin", "testuser", "test");
        assertEquals(403, response.getStatusCode(), "Resource should be denied, not admin role");
    }

    @Test
    public void testMethodsLevelOverridesResourceLevelRoleAllowsAuthenticated() throws IOException {
        final var response = get("test/resource/get/onlyadmin", "testadmin", "test");
        assertEquals(200, response.getStatusCode(), "Resource should be allowed");
    }

    @Test
    public void testPermitAllOverriddenByMethodNotAdminRole() throws IOException {
        final var response = get("test/permitall/get/adminonly", "testuser", "test");
        assertEquals(403, response.getStatusCode(), "Resource should be denied, not admin role");
    }

    @Test
    public void testPermitAllOverriddenByMethodAuthenticated() throws IOException {
        final var response = get("test/permitall/get/adminonly", "testadmin", "test");
        assertEquals(200, response.getStatusCode(), "Resource should be allowed");
    }
}
