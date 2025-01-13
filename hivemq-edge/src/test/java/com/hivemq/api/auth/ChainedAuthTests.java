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
import com.hivemq.api.auth.handler.impl.BasicAuthenticationHandler;
import com.hivemq.api.auth.handler.impl.BearerTokenAuthenticationHandler;
import com.hivemq.api.auth.jwt.JwtAuthenticationProvider;
import com.hivemq.api.auth.provider.IUsernamePasswordProvider;
import com.hivemq.api.config.ApiJwtConfiguration;
import com.hivemq.api.resources.impl.AuthenticationResourceImpl;
import com.hivemq.edge.api.model.ApiBearerToken;
import com.hivemq.edge.api.model.UsernamePasswordCredentials;
import com.hivemq.http.HttpConstants;
import com.hivemq.http.JaxrsHttpServer;
import com.hivemq.http.config.JaxrsHttpServerConfiguration;
import com.hivemq.http.core.HttpResponse;
import com.hivemq.http.core.HttpUrlConnectionClient;
import com.hivemq.http.core.HttpUtils;
import com.hivemq.http.error.ProblemDetails;
import org.glassfish.jersey.server.ResourceConfig;
import org.jetbrains.annotations.NotNull;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author Simon L Johnson
 */
public class ChainedAuthTests {

    protected final Logger logger = LoggerFactory.getLogger(ChainedAuthTests.class);

    static final int TEST_HTTP_PORT = 8088;
    static final int CONNECT_TIMEOUT = 1000;
    static final int READ_TIMEOUT = 1000;
    static final String HTTP = "http";

    protected static @NotNull JaxrsHttpServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        final JaxrsHttpServerConfiguration config = new JaxrsHttpServerConfiguration();
        config.setPort(TEST_HTTP_PORT);

        final ApiJwtConfiguration configuration = new ApiJwtConfiguration(2048, "Test-Issuer", "Test-Audience", 10, 2);
        final JwtAuthenticationProvider jwtAuthenticationProvider = new JwtAuthenticationProvider(configuration);

        final IUsernamePasswordProvider usernamePasswordProvider = AuthTestUtils.createTestUsernamePasswordProvider();
        final Set<IAuthenticationHandler> authenticationHandlers = new HashSet<>();
        authenticationHandlers.add(new BearerTokenAuthenticationHandler(jwtAuthenticationProvider));
        authenticationHandlers.add(new BasicAuthenticationHandler(usernamePasswordProvider));

        final ResourceConfig conf = new ResourceConfig() {
            {
                register(new ApiAuthenticationFeature(authenticationHandlers));
            }
        };
        conf.register(TestApiResource.class);
        conf.register(TestPermitAllApiResource.class);
        conf.register(TestResourceLevelRolesApiResource.class);
        conf.register(new AuthenticationResourceImpl(usernamePasswordProvider,
                jwtAuthenticationProvider,
                jwtAuthenticationProvider));
        //-- ensure we supplied our own test mapper as this can effect output
        final ObjectMapper mapper = new ObjectMapper();
        config.setObjectMapper(mapper);
        server = new JaxrsHttpServer(mock(), List.of(config), conf);
        server.startServer();
    }

    @AfterClass
    public static void tearDown() {
        server.stopServer();
    }

    protected static String getTestServerAddress(final @NotNull String protocol, final int port, final String uri) {
        final String url = String.format("%s://%s:%s/%s", protocol, "localhost", port, uri);
        return url;
    }

    @Test
    public void testAuthenticateValidUser() throws IOException {

        final ObjectMapper mapper = new ObjectMapper();
        final UsernamePasswordCredentials creds = new UsernamePasswordCredentials().userName("testuser").password("test");
        final HttpResponse response = HttpUrlConnectionClient.post(HttpUrlConnectionClient.JSON_HEADERS,
                getTestServerAddress(HTTP, TEST_HTTP_PORT, "api/v1/auth/authenticate"),
                new ByteArrayInputStream(mapper.writeValueAsBytes(creds)),
                CONNECT_TIMEOUT,
                READ_TIMEOUT);
        Assert.assertEquals("Resource should be accepted", 200, response.getStatusCode());
        Assert.assertEquals("API authenticate response should be json",
                MediaType.APPLICATION_JSON,
                response.getContentType());
        final ApiBearerToken token = mapper.readValue(response.getResponseBody(), ApiBearerToken.class);
        Assert.assertNotNull("Response should contain a bearer token", token.getToken());
    }

    @Test
    public void testAuthenticateInvalidUser() throws IOException {

        final ObjectMapper mapper = new ObjectMapper();
        final UsernamePasswordCredentials creds =
                new UsernamePasswordCredentials().userName("testuser").password("invalidpassword");
        final HttpResponse response = HttpUrlConnectionClient.post(HttpUrlConnectionClient.JSON_HEADERS,
                getTestServerAddress(HTTP, TEST_HTTP_PORT, "api/v1/auth/authenticate"),
                new ByteArrayInputStream(mapper.writeValueAsBytes(creds)),
                CONNECT_TIMEOUT,
                READ_TIMEOUT);

        assertThat(response.getStatusCode()).as("Resource should NOT be accepted").isEqualTo(401);
        assertThat(response.getContentType()).as("API authenticate response should be json")
                .startsWith(MediaType.APPLICATION_JSON);
        assertThat(mapper.readValue(response.getResponseBody(), ProblemDetails.class)
                .getErrors()
                .get(0)
                .getDetail()).as("Response should indicate correct failure message")
                .isEqualTo("Invalid username and/or password");
    }

    @Test
    public void testAuthenticatedTokenAllowsApiAccess() throws IOException {

        final ObjectMapper mapper = new ObjectMapper();
        final UsernamePasswordCredentials creds = new UsernamePasswordCredentials().userName("testuser").password("test");
        HttpResponse response = HttpUrlConnectionClient.post(HttpUrlConnectionClient.JSON_HEADERS,
                getTestServerAddress(HTTP, TEST_HTTP_PORT, "api/v1/auth/authenticate"),
                new ByteArrayInputStream(mapper.writeValueAsBytes(creds)),
                CONNECT_TIMEOUT,
                READ_TIMEOUT);
        Assert.assertEquals("Resource should be accepted", 200, response.getStatusCode());
        Assert.assertEquals("API authenticate response should be json",
                MediaType.APPLICATION_JSON,
                response.getContentType());
        final ApiBearerToken token = mapper.readValue(response.getResponseBody(), ApiBearerToken.class);
        Assert.assertNotNull("Response should contain a bearer token", token.getToken());

        //-- now validate the token against the UNSECURE API which returns whether its valid
        response = HttpUrlConnectionClient.post(HttpUrlConnectionClient.JSON_HEADERS,
                getTestServerAddress(HTTP, TEST_HTTP_PORT, "api/v1/auth/validate-token"),
                new ByteArrayInputStream(mapper.writeValueAsBytes(token)),
                CONNECT_TIMEOUT,
                READ_TIMEOUT);
        Assert.assertEquals("Resource should be accepted", 200, response.getStatusCode());

        //-- finally use it as a bear token header against a secure endpoint

        final Map<String, String> headers = Map.of(HttpConstants.AUTH_HEADER,
                HttpUtils.getBearerTokenAuthenticationHeaderValue(token.getToken()),
                "Content-Type",
                "application/json",
                "Accept",
                "application/json");

        response = HttpUrlConnectionClient.get(headers,
                getTestServerAddress(HTTP, TEST_HTTP_PORT, "test/get/auth/user"),
                CONNECT_TIMEOUT,
                READ_TIMEOUT);
        Assert.assertEquals("Resource should be accepted", 200, response.getStatusCode());
        final ApiPrincipal user = mapper.readValue(response.getResponseBody(), ApiPrincipal.class);
        Assert.assertEquals("Username should match that supplied at point of auth", "testuser", user.getName());

    }

    @Test
    public void testGetSecuredResourceWithBasicAuthHeader() throws IOException {

        final ObjectMapper mapper = new ObjectMapper();
        final Map<String, String> headers =
                Map.of(HttpConstants.AUTH_HEADER, HttpUtils.getBasicAuthenticationHeaderValue("testadmin", "test"));
        final HttpResponse response = HttpUrlConnectionClient.get(headers,
                getTestServerAddress(HTTP, TEST_HTTP_PORT, "test/get/auth/admin"),
                CONNECT_TIMEOUT,
                READ_TIMEOUT);
        Assert.assertEquals("Resource should be accepted", 200, response.getStatusCode());
        final ApiPrincipal user = mapper.readValue(response.getResponseBody(), ApiPrincipal.class);
        Assert.assertEquals("Username should match that supplied at point of auth", "testadmin", user.getName());
    }

}
