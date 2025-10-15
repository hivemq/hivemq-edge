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
import com.hivemq.api.auth.handler.impl.BasicAuthenticationHandler;
import com.hivemq.api.auth.provider.impl.ldap.LdapConnectionProperties;
import com.hivemq.api.auth.provider.impl.ldap.LdapUsernameRolesProvider;
import com.hivemq.api.auth.provider.impl.ldap.TlsMode;
import com.hivemq.api.auth.provider.impl.ldap.testcontainer.LldapContainer;
import com.hivemq.bootstrap.ioc.Injector;
import com.hivemq.http.HttpConstants;
import com.hivemq.http.JaxrsHttpServer;
import com.hivemq.http.config.JaxrsHttpServerConfiguration;
import com.hivemq.http.core.HttpResponse;
import com.hivemq.http.core.HttpUrlConnectionClient;
import com.hivemq.http.core.HttpUtils;
import com.unboundid.ldap.sdk.AddRequest;
import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.BindRequest;
import com.unboundid.ldap.sdk.BindResult;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.Modification;
import com.unboundid.ldap.sdk.ModificationType;
import com.unboundid.ldap.sdk.ModifyRequest;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.SimpleBindRequest;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Assert;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@Testcontainers
public class LdapAuthenticationTests {

    @Container
    private static final LldapContainer LLDAP_CONTAINER = new LldapContainer();

    private static final String TEST_USERNAME = "testadmin";
    private static final String TEST_PASSWORD = "test";
    private static final String LDAP_DN_TEMPLATE = "uid={username},ou=people,{baseDn}";

    protected final Logger logger = LoggerFactory.getLogger(LdapAuthenticationTests.class);

    static final int TEST_HTTP_PORT = 8088;
    static final int CONNECT_TIMEOUT = 1000;
    static final int READ_TIMEOUT = 1000;
    static final String HTTP = "http";

    protected static JaxrsHttpServer server;

    @Mock
    private static Injector injector;

    private static LdapConnectionProperties ldapConnectionProperties;

    @BeforeAll
    static void setUp() throws Exception {
        // Get the dynamically mapped port from the container
        final String host = LLDAP_CONTAINER.getHost();
        final int port = LLDAP_CONTAINER.getLdapPort();

        // Create connection properties for plain LDAP (no TLS for simplicity)
        ldapConnectionProperties = new LdapConnectionProperties(
                host,
                port,
                TlsMode.NONE,
                null,
                null,
                null,
                5000,  // 5 second connect timeout
                10000, // 10 second response timeout
                LDAP_DN_TEMPLATE,
                LLDAP_CONTAINER.getBaseDn());

        // Create test user in LLDAP
        createTestUser();

        JaxrsHttpServerConfiguration config = new JaxrsHttpServerConfiguration();
        config.setPort(TEST_HTTP_PORT);

        final Set<IAuthenticationHandler > authenticationHandlers = new HashSet<>();
        authenticationHandlers.add(new BasicAuthenticationHandler(new LdapUsernameRolesProvider(ldapConnectionProperties)));
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
        server = new JaxrsHttpServer(mock(), List.of(config), conf);
        server.startServer();
    }

    @AfterAll
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
    public void testGetSecuredResourceWithValidCreds() throws IOException {
        Map<String, String> headers = Map.of(HttpConstants.AUTH_HEADER,
                HttpUtils.getBasicAuthenticationHeaderValue("testadmin", "test"));
        HttpResponse response =
                HttpUrlConnectionClient.get(headers,
                        getTestServerAddress(HTTP, TEST_HTTP_PORT, "test/get/auth/admin"), CONNECT_TIMEOUT, READ_TIMEOUT);
        Assert.assertEquals("Resource should be accepted", 200, response.getStatusCode());
    }

    /**
     * Creates a test user in LLDAP using the admin account.
     */
    private static void createTestUser() throws LDAPException, GeneralSecurityException {
        try (LDAPConnection adminConnection = ldapConnectionProperties.createConnection()) {
            // Bind as admin
            final String adminUserDn = "uid=" + LLDAP_CONTAINER.getAdminUsername() + ",ou=people," + LLDAP_CONTAINER.getBaseDn();
            final BindRequest bindRequest = new SimpleBindRequest(adminUserDn, LLDAP_CONTAINER.getAdminPassword());
            final BindResult bindResult = adminConnection.bind(bindRequest);

            assertThat(bindResult.getResultCode()).isEqualTo(ResultCode.SUCCESS);

            // Add test user
            final String testUserDnString = "uid=" + TEST_USERNAME + ",ou=people," + LLDAP_CONTAINER.getBaseDn();

            final AddRequest addRequest = new AddRequest(testUserDnString,
                    new Attribute("objectClass", "inetOrgPerson", "posixAccount"),
                    new Attribute("uid", TEST_USERNAME),
                    new Attribute("cn", TEST_USERNAME),
                    new Attribute("sn", "User"),
                    new Attribute("mail", TEST_USERNAME + "@example.com"),
                    new Attribute("uidNumber", "2000"),
                    new Attribute("gidNumber", "2000"),
                    new Attribute("homeDirectory", "/home/" + TEST_USERNAME)
            );

            adminConnection.add(addRequest);

            // Set password
            final ModifyRequest modifyRequest = new ModifyRequest(testUserDnString,
                    new Modification(ModificationType.REPLACE, "userPassword", TEST_PASSWORD));

            adminConnection.modify(modifyRequest);
        }
    }
}
