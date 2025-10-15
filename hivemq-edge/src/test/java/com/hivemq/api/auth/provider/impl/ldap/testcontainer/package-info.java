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

/**
 * Reusable testcontainer implementations for LDAP servers.
 * <p>
 * This package provides ready-to-use Testcontainers wrappers for different LDAP server implementations,
 * making it easy to write integration tests against real LDAP servers running in Docker containers.
 *
 * <h2>Available Containers</h2>
 * <ul>
 *     <li>{@link com.hivemq.api.auth.provider.impl.ldap.testcontainer.LldapContainer} -
 *         Lightweight LDAP server (LLDAP) container</li>
 *     <li>{@link com.hivemq.api.auth.provider.impl.ldap.testcontainer.OpenLdapContainer} -
 *         Full-featured OpenLDAP server container</li>
 * </ul>
 *
 * <h2>Quick Start</h2>
 *
 * <h3>LLDAP Container (Lightweight, Simple Setup)</h3>
 * <pre>{@code
 * @Testcontainers
 * class MyLdapTest {
 *     @Container
 *     static LldapContainer ldap = new LldapContainer();  // Uses sensible defaults
 *
 *     @Test
 *     void testAuthentication() {
 *         String host = ldap.getHost();
 *         int port = ldap.getLdapPort();
 *         String baseDn = ldap.getBaseDn();        // dc=example,dc=com
 *         String adminDn = ldap.getAdminDn();      // uid=admin,dc=example,dc=com
 *         String adminPw = ldap.getAdminPassword(); // admin_password
 *         // ... connect and test
 *     }
 * }
 * }</pre>
 *
 * <h3>OpenLDAP Container (Full-Featured, LDIF Seeding)</h3>
 * <pre>{@code
 * @Testcontainers
 * class MyOpenLdapTest {
 *     @Container
 *     static OpenLdapContainer ldap = OpenLdapContainer.builder()
 *         .withLdifFile("ldap/users.ldif")    // Load test data from classpath
 *         .withLdifFile("ldap/groups.ldif")   // Multiple files supported
 *         .build();
 *
 *     @Test
 *     void testWithPreloadedData() {
 *         String host = ldap.getHost();
 *         int port = ldap.getLdapPort();
 *         String baseDn = ldap.getBaseDn();        // dc=example,dc=org (from domain)
 *         String adminDn = ldap.getAdminDn();      // cn=admin,dc=example,dc=org
 *         String adminPw = ldap.getAdminPassword(); // admin
 *         // ... test with preloaded users and groups
 *     }
 * }
 * }</pre>
 *
 * <h2>Builder Pattern</h2>
 *
 * Both containers support fluent builder API for custom configuration:
 *
 * <h3>LLDAP Builder Example</h3>
 * <pre>{@code
 * LldapContainer ldap = LldapContainer.builder()
 *     .baseDn("dc=mycompany,dc=org")
 *     .adminUsername("admin")
 *     .adminPassword("secret123")
 *     .ldapPort(3890)
 *     .withLdaps(certFile, keyFile)  // Enable LDAPS
 *     .build();
 * }</pre>
 *
 * <h3>OpenLDAP Builder Example</h3>
 * <pre>{@code
 * OpenLdapContainer ldap = OpenLdapContainer.builder()
 *     .domain("mycompany.org")           // Base DN derived automatically
 *     .organisation("My Company Inc")
 *     .adminPassword("secret123")
 *     .withTls(true)                     // Enable TLS/START_TLS
 *     .withBackend("mdb")
 *     .withRfc2307bisSchema(false)
 *     .withLdifFile("ldap/test-data.ldif")
 *     .build();
 * }</pre>
 *
 * <h2>Comparison: LLDAP vs OpenLDAP</h2>
 *
 * <table border="1">
 *   <tr>
 *     <th>Feature</th>
 *     <th>LLDAP</th>
 *     <th>OpenLDAP</th>
 *   </tr>
 *   <tr>
 *     <td>Container Size</td>
 *     <td>Small (~50MB)</td>
 *     <td>Larger (~200MB)</td>
 *   </tr>
 *   <tr>
 *     <td>Startup Time</td>
 *     <td>Fast (2-5s)</td>
 *     <td>Slower (5-10s)</td>
 *   </tr>
 *   <tr>
 *     <td>Plain LDAP</td>
 *     <td>✓ Yes</td>
 *     <td>✓ Yes</td>
 *   </tr>
 *   <tr>
 *     <td>LDAPS</td>
 *     <td>✓ Yes</td>
 *     <td>✓ Yes</td>
 *   </tr>
 *   <tr>
 *     <td>START_TLS</td>
 *     <td>✗ No</td>
 *     <td>✓ Yes</td>
 *   </tr>
 *   <tr>
 *     <td>LDIF Seeding</td>
 *     <td>Manual (via code)</td>
 *     <td>✓ Automatic</td>
 *   </tr>
 *   <tr>
 *     <td>Schema Support</td>
 *     <td>Basic</td>
 *     <td>Full</td>
 *   </tr>
 *   <tr>
 *     <td>Best For</td>
 *     <td>Simple auth tests</td>
 *     <td>Complex LDAP scenarios</td>
 *   </tr>
 * </table>
 *
 * <h2>When to Use Which</h2>
 *
 * <h3>Use LLDAP ({@link com.hivemq.api.auth.provider.impl.ldap.testcontainer.LldapContainer}) when:</h3>
 * <ul>
 *     <li>Testing basic authentication flows</li>
 *     <li>Need fast test execution</li>
 *     <li>Testing plain LDAP or LDAPS</li>
 *     <li>Creating test users programmatically is acceptable</li>
 *     <li>Don't need advanced LDAP features</li>
 * </ul>
 *
 * <h3>Use OpenLDAP ({@link com.hivemq.api.auth.provider.impl.ldap.testcontainer.OpenLdapContainer}) when:</h3>
 * <ul>
 *     <li>Testing START_TLS connections</li>
 *     <li>Need to seed complex directory structures from LDIF files</li>
 *     <li>Testing group membership and complex queries</li>
 *     <li>Need full LDAP schema support</li>
 *     <li>Testing against production-like LDAP server</li>
 * </ul>
 *
 * <h2>Common Patterns</h2>
 *
 * <h3>Pattern 1: Default Configuration Test</h3>
 * <pre>{@code
 * @Testcontainers
 * class QuickTest {
 *     @Container
 *     static LldapContainer ldap = new LldapContainer();
 *
 *     @Test
 *     void testConnection() {
 *         // All defaults work out of the box
 *         LdapConnectionProperties props = new LdapConnectionProperties(
 *             ldap.getHost(),
 *             ldap.getLdapPort(),
 *             TlsMode.NONE,
 *             null, null, null,
 *             5000, 10000,
 *             "uid={username},ou=people,{baseDn}",
 *             ldap.getBaseDn()
 *         );
 *         LdapClient client = new LdapClient(props);
 *         client.start();
 *         // ... test
 *     }
 * }
 * }</pre>
 *
 * <h3>Pattern 2: LDIF Seeding with OpenLDAP</h3>
 * <pre>{@code
 * @Testcontainers
 * class DataTest {
 *     @Container
 *     static OpenLdapContainer ldap = OpenLdapContainer.builder()
 *         .withLdifFile("ldap/users.ldif")
 *         .withLdifFile("ldap/groups.ldif")
 *         .build();
 *
 *     @Test
 *     void testWithPreloadedUsers() {
 *         // Users and groups from LDIF files are already loaded
 *         try (LDAPConnection conn = createConnection()) {
 *             conn.bind(ldap.getAdminDn(), ldap.getAdminPassword());
 *             SearchResult result = conn.search("ou=people," + ldap.getBaseDn(), ...);
 *             // ... assertions
 *         }
 *     }
 * }
 * }</pre>
 *
 * <h3>Pattern 3: Custom Domain and Credentials</h3>
 * <pre>{@code
 * @Testcontainers
 * class CustomConfigTest {
 *     @Container
 *     static OpenLdapContainer ldap = OpenLdapContainer.builder()
 *         .domain("mycompany.internal")     // Base DN: dc=mycompany,dc=internal
 *         .organisation("MyCompany")
 *         .adminPassword("MySecretPassword")
 *         .build();
 *
 *     @Test
 *     void testCustomConfig() {
 *         assertEquals("dc=mycompany,dc=internal", ldap.getBaseDn());
 *         assertEquals("cn=admin,dc=mycompany,dc=internal", ldap.getAdminDn());
 *         assertEquals("MySecretPassword", ldap.getAdminPassword());
 *     }
 * }
 * }</pre>
 *
 * <h3>Pattern 4: TLS/LDAPS Testing</h3>
 * <pre>{@code
 * @Testcontainers
 * class TlsTest {
 *     static File certFile;
 *     static File keyFile;
 *
 *     static {
 *         // Generate self-signed certificate
 *         // ... (see LdapTlsModesIntegrationTest for example)
 *     }
 *
 *     @Container
 *     static LldapContainer ldap = LldapContainer.builder()
 *         .withLdaps(certFile, keyFile)
 *         .build();
 *
 *     @Test
 *     void testLdaps() {
 *         // Connect with LDAPS
 *         LdapConnectionProperties props = new LdapConnectionProperties(
 *             ldap.getHost(),
 *             ldap.getLdapPort(),
 *             TlsMode.LDAPS,
 *             trustStorePath,
 *             trustStorePassword,
 *             KeyStore.getDefaultType(),
 *             5000, 10000,
 *             dnTemplate,
 *             ldap.getBaseDn()
 *         );
 *         // ... test encrypted connection
 *     }
 * }
 * }</pre>
 *
 * @see com.hivemq.api.auth.provider.impl.ldap.testcontainer.LldapContainer
 * @see com.hivemq.api.auth.provider.impl.ldap.testcontainer.OpenLdapContainer
 * @see com.hivemq.api.auth.provider.impl.ldap.OpenLdapTest
 * @see com.hivemq.api.auth.provider.impl.ldap.LdapTlsModesIntegrationTest
 */
package com.hivemq.api.auth.provider.impl.ldap.testcontainer;
