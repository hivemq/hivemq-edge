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

import com.hivemq.configuration.service.ApiConfigurationService;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Simon L Johnson
 */
public class EnforceUserRolesTest {

    protected final Logger logger = LoggerFactory.getLogger(EnforceUserRolesTest.class);

    static final int TEST_HTTP_PORT = 8089; // Different port to avoid conflict if any
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

        // MOCK CONFIGURATION TO RETURN FALSE
        apiConfigurationService = mock(ApiConfigurationService.class);
        when(apiConfigurationService.isEnforceUserRoles()).thenReturn(false);

        final Set<IAuthenticationHandler> authenticationHandlers = new HashSet<>();
        authenticationHandlers.add(
                new BasicAuthenticationHandler(AuthTestUtils.createTestUsernamePasswordProvider()));
        final var conf = new ResourceConfig() {
            {
                register(new ApiAuthenticationFeature(authenticationHandlers, apiConfigurationService));
            }
        };
        conf.register(TestApiResource.class);
        conf.register(TestPermitAllApiResource.class);
        conf.register(TestResourceLevelRolesApiResource.class);
        // -- ensure we supplied our own test mapper as this can effect output
        final var mapper = new ObjectMapper();
        config.setObjectMapper(mapper);
        server = new JaxrsHttpServer(mock(), List.of(config), conf);
        server.startServer();
    }

    @AfterAll
    public static void tearDown() {
        server.stopServer();
    }

    protected static String getTestServerAddress(final @NotNull String protocol, final @NotNull int port,
            final @NotNull String uri) {
        return String.format("%s://%s:%s/%s", protocol, "localhost", port, uri);
    }

    @Test
    public void testGetSecuredResourceWithValidCredsInvalidRoleButEnforcementDisabled() throws IOException {
        // User 'testuser' has role 'test', but resource requires 'admin' (mapped from
        // class/method)
        // Since enforcement is disabled, this should return 200 instead of 403
        final var headers = Map.of(HttpConstants.AUTH_HEADER,
                BasicAuthenticationHandler.getBasicAuthenticationHeaderValue("testuser", "test"));
        final var response = HttpUrlConnectionClient.get(headers,
                getTestServerAddress(HTTP, TEST_HTTP_PORT, "test/get/auth/admin"), CONNECT_TIMEOUT,
                READ_TIMEOUT);
        assertEquals(200, response.getStatusCode(), "Resource should be accepted because role enforcement is disabled");
    }

    @Test
    public void testGetSecuredResourceWithoutCreds() throws IOException {
        // Authentication should still be required!
        final var response = HttpUrlConnectionClient.get(null,
                getTestServerAddress(HTTP, TEST_HTTP_PORT, "test/get/auth/admin"), CONNECT_TIMEOUT,
                READ_TIMEOUT);
        assertEquals(401, response.getStatusCode(), "Resource should be denied (Authentication still required)");
    }
}
