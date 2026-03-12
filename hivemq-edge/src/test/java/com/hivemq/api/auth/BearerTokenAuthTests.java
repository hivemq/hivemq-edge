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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.api.AuthTestUtils;
import com.hivemq.api.TestApiResource;
import com.hivemq.api.TestPermitAllApiResource;
import com.hivemq.api.TestResourceLevelRolesApiResource;
import com.hivemq.api.auth.handler.IAuthenticationHandler;
import com.hivemq.api.auth.handler.impl.BearerTokenAuthenticationHandler;
import com.hivemq.api.auth.jwt.JwtAuthenticationProvider;
import com.hivemq.api.auth.provider.IUsernameRolesProvider;
import com.hivemq.api.config.ApiJwtConfiguration;
import com.hivemq.api.resources.impl.AuthenticationResourceImpl;
import com.hivemq.configuration.service.ApiConfigurationService;
import com.hivemq.edge.api.model.ApiBearerToken;
import com.hivemq.edge.api.model.UsernamePasswordCredentials;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.http.HttpConstants;
import com.hivemq.http.JaxrsHttpServer;
import com.hivemq.http.config.JaxrsHttpServerConfiguration;
import com.hivemq.http.core.HttpResponse;
import com.hivemq.http.core.HttpUrlConnectionClient;
import com.hivemq.http.core.HttpUtils;
import com.hivemq.http.error.ProblemDetails;
import jakarta.ws.rs.core.MediaType;
import java.io.ByteArrayInputStream;
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

/**
 * @author Simon L Johnson
 */
public class BearerTokenAuthTests {

    protected final Logger logger = LoggerFactory.getLogger(BearerTokenAuthTests.class);

    static final int TEST_HTTP_PORT = 8088;
    static final int CONNECT_TIMEOUT = 1000;
    static final int READ_TIMEOUT = 1000;
    static final String HTTP = "http";

    protected static JaxrsHttpServer server;
    protected static ObjectMapper objectMapper = new ObjectMapper();

    @BeforeAll
    public static void setUp() throws Exception {

        final JaxrsHttpServerConfiguration config = new JaxrsHttpServerConfiguration();
        config.setPort(TEST_HTTP_PORT);
        // -- ensure we supplied our own test mapper as this can effect output
        config.setObjectMapper(objectMapper);

        final var configuration = new ApiJwtConfiguration(2048, "Test-Issuer", "Test-Audience", 10, 2);
        final var jwtAuthenticationProvider = new JwtAuthenticationProvider(configuration);
        final IUsernameRolesProvider usernamePasswordProvider = AuthTestUtils.createTestUsernamePasswordProvider();
        final Set<IAuthenticationHandler> authenticationHandlers = new HashSet<>();
        authenticationHandlers.add(new BearerTokenAuthenticationHandler(jwtAuthenticationProvider));
        // ChainedAuthTests also includes the usernamePasswordProvider
        // authenticationHandlers.add(new BasicAuthenticationHandler(usernamePasswordProvider));
        final var apiConfigurationService = mock(ApiConfigurationService.class);
        when(apiConfigurationService.isEnforceApiAuth()).thenReturn(true);
        final var apiAuthenticationFeature =
                new ApiAuthenticationFeature(authenticationHandlers, apiConfigurationService);
        final var authenticationResource = new AuthenticationResourceImpl(
                usernamePasswordProvider, jwtAuthenticationProvider, jwtAuthenticationProvider);

        final var resourceConfig = new ResourceConfig();
        resourceConfig.register(apiAuthenticationFeature);
        resourceConfig.register(TestApiResource.class);
        resourceConfig.register(TestPermitAllApiResource.class);
        resourceConfig.register(TestResourceLevelRolesApiResource.class);
        resourceConfig.register(authenticationResource);

        server = new JaxrsHttpServer(mock(), List.of(config), resourceConfig);
        server.startServer();
    }

    @AfterAll
    public static void tearDown() {
        server.stopServer();
    }

    protected static HttpResponse get(final @NotNull String path, final @Nullable Map<String, String> headers)
            throws IOException {
        final var serverAddress = String.format("%s://%s:%s/%s", HTTP, "localhost", TEST_HTTP_PORT, path);
        return HttpUrlConnectionClient.get(headers, serverAddress, CONNECT_TIMEOUT, READ_TIMEOUT);
    }

    protected static HttpResponse post(final @NotNull String path, final ByteArrayInputStream body) throws IOException {
        final var headers = HttpUrlConnectionClient.JSON_HEADERS;
        final var serverAddress = String.format("%s://%s:%s/%s", HTTP, "localhost", TEST_HTTP_PORT, path);
        return HttpUrlConnectionClient.post(headers, serverAddress, body, CONNECT_TIMEOUT, READ_TIMEOUT);
    }

    protected ByteArrayInputStream bodyCredentials(final @NotNull String username, final @NotNull String password)
            throws JsonProcessingException {
        final var credentials =
                new UsernamePasswordCredentials().userName(username).password(password);
        return new ByteArrayInputStream(objectMapper.writeValueAsBytes(credentials));
    }

    @Test
    public void testAuthenticateValidUser() throws IOException {
        HttpResponse response;

        response = post("api/v1/auth/authenticate", bodyCredentials("testuser", "test"));

        assertEquals(200, response.getStatusCode(), "Resource should be accepted");
        assertEquals(MediaType.APPLICATION_JSON, response.getContentType(), "API authenticate response should be json");
        final ApiBearerToken token = objectMapper.readValue(response.getResponseBody(), ApiBearerToken.class);
        assertNotNull(token.getToken(), "Response should contain a bearer token");
    }

    @Test
    public void testAuthenticateInvalidUser() throws IOException {
        HttpResponse response;

        response = post("api/v1/auth/authenticate", bodyCredentials("testuser", "invalidpassword"));

        assertEquals(401, response.getStatusCode(), "Resource should be denied");
        assertEquals(
                HttpConstants.APPLICATION_PROBLEM_JSON_CHARSET_UTF_8,
                response.getContentType(),
                "API authenticate response should be json");
        assertThat(objectMapper
                        .readValue(response.getResponseBody(), ProblemDetails.class)
                        .getErrors()
                        .getFirst()
                        .getDetail())
                .as("Response should indicate correct failure message")
                .isEqualTo("Invalid username and/or password");
    }

    @Test
    public void testAuthenticatedTokenAllowsApiAccess() throws IOException {
        HttpResponse response;

        response = post("api/v1/auth/authenticate", bodyCredentials("testuser", "test"));

        assertEquals(200, response.getStatusCode(), "Resource should be accepted");
        assertEquals(MediaType.APPLICATION_JSON, response.getContentType(), "API authenticate response should be json");

        final ApiBearerToken token = objectMapper.readValue(response.getResponseBody(), ApiBearerToken.class);
        assertNotNull(token.getToken(), "Response should contain a bearer token");
        final var bodyToken = new ByteArrayInputStream(objectMapper.writeValueAsBytes(token));

        // -- now validate the token against the UNSECURE API which returns whether its valid
        response = post("api/v1/auth/validate-token", bodyToken);

        assertEquals(200, response.getStatusCode(), "Resource should be accepted");

        // -- finally use it as a bearer token header against a secure endpoint
        final Map<String, String> headers = Map.of(
                HttpConstants.AUTH_HEADER,
                HttpUtils.getBearerTokenAuthenticationHeaderValue(token.getToken()),
                "Content-Type",
                "application/json",
                "Accept",
                "application/json");

        response = get("test/get/auth/user", headers);

        assertEquals(200, response.getStatusCode(), "Resource should be accepted");
        final ApiPrincipal user = objectMapper.readValue(response.getResponseBody(), ApiPrincipal.class);
        assertEquals("testuser", user.getName(), "Username should match that supplied at point of auth");
    }
}
