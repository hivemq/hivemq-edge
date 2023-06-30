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
import com.hivemq.bootstrap.ioc.Injector;
import com.hivemq.http.JaxrsHttpServer;
import com.hivemq.http.config.JaxrsHttpServerConfiguration;
import com.hivemq.http.core.HttpConstants;
import com.hivemq.http.core.HttpResponse;
import com.hivemq.http.core.HttpUrlConnectionClient;
import com.hivemq.http.core.HttpUtils;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Simon L Johnson
 */
public class BasicAuthenticationTests {

    protected final Logger logger = LoggerFactory.getLogger(BasicAuthenticationTests.class);

    static final int TEST_HTTP_PORT = 8088;
    static final int CONNECT_TIMEOUT = 1000;
    static final int READ_TIMEOUT = 1000;
    static final String HTTP = "http";

    protected static JaxrsHttpServer server;

    @Mock
    private static Injector injector;

    @BeforeClass
    public static void setUp() throws Exception {
        JaxrsHttpServerConfiguration config = new JaxrsHttpServerConfiguration();
        config.setPort(TEST_HTTP_PORT);

        final Set<IAuthenticationHandler > authenticationHandlers = new HashSet<>();
        authenticationHandlers.add(new BasicAuthenticationHandler(AuthTestUtils.createTestUsernamePasswordProvider()));
        ResourceConfig conf = new ResourceConfig(){{
                register(new ApiAuthenticationFeature(authenticationHandlers));
            }
        };
        conf.register(TestApiResource.class);
        conf.register(TestPermitAllApiResource.class);
        conf.register(TestResourceLevelRolesApiResource.class);
        //-- ensure we supplied our own test mapper as this can effect output
        ObjectMapper mapper = new ObjectMapper();
        config.setObjectMapper(mapper);
        server = new JaxrsHttpServer(List.of(config), conf);
        server.startServer();
    }

    @AfterClass
    public static void tearDown(){
        server.stopServer();
    }

    protected static String getTestServerAddress(String protocol, int port, String uri){
        String url = String.format("%s://%s:%s/%s", protocol, "localhost", port, uri);
        return url;
    }

    @Test
    public void testGetSecuredResourceWithoutCreds() throws IOException {
        HttpResponse response =
                HttpUrlConnectionClient.get(null,
                        getTestServerAddress(HTTP, TEST_HTTP_PORT, "test/get/auth/admin"), CONNECT_TIMEOUT, READ_TIMEOUT);
        Assert.assertEquals("Resource should be denied", 401, response.getStatusCode());
    }

    @Test
    public void testGetSecuredResourceWithInvalidUsername() throws IOException {
        Map<String, String> headers = Map.of(HttpConstants.AUTH_HEADER,
                HttpUtils.getBasicAuthenticationHeaderValue("testaWRONG", "test"));
        HttpResponse response =
                HttpUrlConnectionClient.get(headers,
                        getTestServerAddress(HTTP, TEST_HTTP_PORT, "test/get/auth/admin"), CONNECT_TIMEOUT, READ_TIMEOUT);
        Assert.assertEquals("Resource should be denied", 401, response.getStatusCode());
    }

    @Test
    public void testGetSecuredResourceWithInvalidPassword() throws IOException {
        Map<String, String> headers = Map.of(HttpConstants.AUTH_HEADER,
                HttpUtils.getBasicAuthenticationHeaderValue("testadmin", "incorrect"));
        HttpResponse response =
                HttpUrlConnectionClient.get(headers,
                        getTestServerAddress(HTTP, TEST_HTTP_PORT, "test/get/auth/admin"), CONNECT_TIMEOUT, READ_TIMEOUT);
        Assert.assertEquals("Resource should be denied", 401, response.getStatusCode());
    }

    @Test
    public void testGetSecuredResourceWithValidCredsInvalidRole() throws IOException {
        Map<String, String> headers = Map.of(HttpConstants.AUTH_HEADER,
                HttpUtils.getBasicAuthenticationHeaderValue("testuser", "test"));
        HttpResponse response =
                HttpUrlConnectionClient.get(headers,
                        getTestServerAddress(HTTP, TEST_HTTP_PORT, "test/get/auth/admin"), CONNECT_TIMEOUT, READ_TIMEOUT);
        Assert.assertEquals("Resource should be denied", 403, response.getStatusCode());
    }

    @Test
    public void testGetSecuredResourceWithValidCreds() throws IOException {
        Map<String, String> headers = Map.of(HttpConstants.AUTH_HEADER,
                HttpUtils.getBasicAuthenticationHeaderValue("testadmin", "test"));
        HttpResponse response =
                HttpUrlConnectionClient.get(headers,
                        getTestServerAddress(HTTP, TEST_HTTP_PORT, "test/get/auth/admin"), CONNECT_TIMEOUT, READ_TIMEOUT);
        Assert.assertEquals("Resource should be accepted", 200, response.getStatusCode());
    }

    @Test
    public void testGetSecuredResourceWithValidCredsMultipleRole() throws IOException {
        Map<String, String> headers = Map.of(HttpConstants.AUTH_HEADER,
                HttpUtils.getBasicAuthenticationHeaderValue("testadmin", "test"));
        HttpResponse response =
                HttpUrlConnectionClient.get(headers,
                        getTestServerAddress(HTTP, TEST_HTTP_PORT, "test/get/auth/user"), CONNECT_TIMEOUT, READ_TIMEOUT);
        Assert.assertEquals("Resource should be accepted", 200, response.getStatusCode());
    }

    @Test
    public void testUserInNoRole() throws IOException {
        Map<String, String> headers = Map.of(HttpConstants.AUTH_HEADER,
                HttpUtils.getBasicAuthenticationHeaderValue("testnorole", "test"));
        HttpResponse response =
                HttpUrlConnectionClient.get(headers,
                        getTestServerAddress(HTTP, TEST_HTTP_PORT, "test/get/auth/user"), CONNECT_TIMEOUT, READ_TIMEOUT);
        Assert.assertEquals("Resource should be denied", 403, response.getStatusCode());
    }

    @Test
    public void testPermitAllAllowsAnyAuthenticated() throws IOException {
        Map<String, String> headers = Map.of(HttpConstants.AUTH_HEADER,
                HttpUtils.getBasicAuthenticationHeaderValue("testnorole", "test"));
        HttpResponse response =
                HttpUrlConnectionClient.get(headers,
                        getTestServerAddress(HTTP, TEST_HTTP_PORT, "test/permitall/get"), CONNECT_TIMEOUT, READ_TIMEOUT);
        Assert.assertEquals("Resource should be allowed", 200, response.getStatusCode());
    }

    @Test
    public void testPermitAllRejectsNonAuthenticated() throws IOException {
        HttpResponse response =
                HttpUrlConnectionClient.get(null,
                        getTestServerAddress(HTTP, TEST_HTTP_PORT, "test/permitall/get"), CONNECT_TIMEOUT, READ_TIMEOUT);
        Assert.assertEquals("Resource should be allowed", 401, response.getStatusCode());
    }

    @Test
    public void testResourceLevelRoleRejectsNonAuthenticated() throws IOException {
        HttpResponse response =
                HttpUrlConnectionClient.get(null,
                        getTestServerAddress(HTTP, TEST_HTTP_PORT, "test/resource/get"), CONNECT_TIMEOUT, READ_TIMEOUT);
        Assert.assertEquals("Resource should not be allowed", 401, response.getStatusCode());
    }

    @Test
    public void testResourceLevelRoleAllowsAuthenticated() throws IOException {
        Map<String, String> headers = Map.of(HttpConstants.AUTH_HEADER,
                HttpUtils.getBasicAuthenticationHeaderValue("testuser", "test"));
        HttpResponse response =
                HttpUrlConnectionClient.get(headers,
                        getTestServerAddress(HTTP, TEST_HTTP_PORT, "test/resource/get"), CONNECT_TIMEOUT, READ_TIMEOUT);
        Assert.assertEquals("Resource should be allowed", 200, response.getStatusCode());
    }

    @Test
    public void testMethodsLevelOverridesResourceLevelRoleAllowsNonAuthenticated() throws IOException {
        Map<String, String> headers = Map.of(HttpConstants.AUTH_HEADER,
                HttpUtils.getBasicAuthenticationHeaderValue("testuser", "test"));
        HttpResponse response =
                HttpUrlConnectionClient.get(headers,
                        getTestServerAddress(HTTP, TEST_HTTP_PORT, "test/resource/get/onlyadmin"), CONNECT_TIMEOUT, READ_TIMEOUT);
        Assert.assertEquals("Resource should be allowed", 403, response.getStatusCode());
    }

    @Test
    public void testMethodsLevelOverridesResourceLevelRoleAllowsAuthenticated() throws IOException {
        Map<String, String> headers = Map.of(HttpConstants.AUTH_HEADER,
                HttpUtils.getBasicAuthenticationHeaderValue("testadmin", "test"));
        HttpResponse response =
                HttpUrlConnectionClient.get(headers,
                        getTestServerAddress(HTTP, TEST_HTTP_PORT, "test/resource/get/onlyadmin"), CONNECT_TIMEOUT, READ_TIMEOUT);
        Assert.assertEquals("Resource should be allowed", 200, response.getStatusCode());
    }


    @Test
    public void testPermitAllOverriddenByMethodNonAuthenticated() throws IOException {
        Map<String, String> headers = Map.of(HttpConstants.AUTH_HEADER,
                HttpUtils.getBasicAuthenticationHeaderValue("testuser", "test"));
        HttpResponse response =
                HttpUrlConnectionClient.get(headers,
                        getTestServerAddress(HTTP, TEST_HTTP_PORT, "test/permitall/get/adminonly"), CONNECT_TIMEOUT, READ_TIMEOUT);
        Assert.assertEquals("Resource should not be allowed", 403, response.getStatusCode());
    }

    @Test
    public void testPermitAllOverriddenByMethodAuthenticated() throws IOException {
        Map<String, String> headers = Map.of(HttpConstants.AUTH_HEADER,
                HttpUtils.getBasicAuthenticationHeaderValue("testadmin", "test"));
        HttpResponse response =
                HttpUrlConnectionClient.get(headers,
                        getTestServerAddress(HTTP, TEST_HTTP_PORT, "test/permitall/get/adminonly"), CONNECT_TIMEOUT, READ_TIMEOUT);
        Assert.assertEquals("Resource should not be allowed", 200, response.getStatusCode());
    }

}
