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
 * LDAP client library for HiveMQ Edge authentication.
 * <p>
 * This package provides a comprehensive LDAP client implementation with support for:
 * <ul>
 *     <li>TLS/SSL encryption (LDAPS, StartTLS, or plain LDAP)</li>
 *     <li>Connection pooling for high performance</li>
 *     <li>Multiple DN resolution strategies (template-based and search-based)</li>
 *     <li>Flexible authentication options</li>
 * </ul>
 *
 * <h2>Quick Start</h2>
 *
 * <h3>Basic Usage with Template DN Resolution</h3>
 * <pre>{@code
 * // 1. Configure connection properties
 * LdapConnectionProperties props = new LdapConnectionProperties(
 *     "ldap.example.com",                      // LDAP server host
 *     636,                                      // Port (636 for LDAPS)
 *     TlsMode.LDAPS,                           // TLS mode
 *     "/path/to/truststore.jks",              // Truststore (or null for system CAs)
 *     "changeit".toCharArray(),               // Truststore password
 *     "JKS",                                   // Truststore type
 *     10000,                                   // Connect timeout (ms)
 *     30000,                                   // Response timeout (ms)
 *     "uid={username},ou=people,{baseDn}",    // DN template
 *     "dc=example,dc=com"                     // Base DN
 * );
 *
 * // 2. Create and start LDAP client
 * LdapClient client = new LdapClient(props);
 * client.start();
 *
 * try {
 *     // 3. Authenticate a user
 *     boolean authenticated = client.authenticateUser("jdoe", "password123");
 *     if (authenticated) {
 *         System.out.println("Authentication successful!");
 *     }
 * } finally {
 *     // 4. Always stop the client when done
 *     client.stop();
 * }
 * }</pre>
 *
 * <h3>Advanced Usage with Search Filter DN Resolution</h3>
 * <pre>{@code
 * // Configure and start client (same as above)
 * LdapClient client = new LdapClient(props);
 * client.start();
 *
 * try {
 *     // Create a search-based DN resolver
 *     SearchFilterDnResolver resolver = new SearchFilterDnResolver(
 *         client.getConnectionPool(),
 *         "dc=example,dc=com",                // Search base
 *         "(|(uid={username})(mail={username}))", // Search filter (uid OR mail)
 *         SearchScope.SUB,                     // Search entire subtree
 *         5                                    // Timeout (seconds)
 *     );
 *
 *     // Resolve the DN and authenticate
 *     String userDn = resolver.resolveDn("jdoe@example.com");
 *     boolean authenticated = client.bindUser(userDn, "password123");
 *
 * } catch (SearchFilterDnResolver.DnResolutionException e) {
 *     System.err.println("User not found: " + e.getUsername());
 * } finally {
 *     client.stop();
 * }
 * }</pre>
 *
 * <h2>DN Resolution Strategies</h2>
 *
 * <h3>Template-Based Resolution ({@link TemplateDnResolver})</h3>
 * <p>
 * <strong>Best for:</strong> Simple, predictable LDAP structures
 * <br><strong>Performance:</strong> Fast (no LDAP query)
 * <br><strong>Flexibility:</strong> Limited
 * <p>
 * Constructs DNs using string templates with placeholders:
 * <ul>
 *     <li><strong>OpenLDAP:</strong> {@code uid={username},ou=people,{baseDn}}</li>
 *     <li><strong>Active Directory:</strong> {@code cn={username},cn=Users,{baseDn}}</li>
 *     <li><strong>Email-based:</strong> {@code mail={username},ou=staff,{baseDn}}</li>
 * </ul>
 *
 * <h3>Search Filter-Based Resolution ({@link SearchFilterDnResolver})</h3>
 * <p>
 * <strong>Best for:</strong> Complex LDAP structures, scattered users
 * <br><strong>Performance:</strong> Slower (requires LDAP query)
 * <br><strong>Flexibility:</strong> High
 * <p>
 * Searches the LDAP directory to find the user's DN using filters:
 * <ul>
 *     <li><strong>Simple:</strong> {@code (uid={username})}</li>
 *     <li><strong>Email:</strong> {@code (mail={username})}</li>
 *     <li><strong>Multiple attributes:</strong> {@code (|(uid={username})(mail={username}))}</li>
 *     <li><strong>Complex:</strong> {@code (&(objectClass=inetOrgPerson)(uid={username}))}</li>
 * </ul>
 *
 * <h2>TLS Configuration</h2>
 *
 * <h3>LDAPS (Recommended)</h3>
 * <pre>{@code
 * TlsMode.LDAPS  // TLS from connection start, usually port 636
 * }</pre>
 *
 * <h3>StartTLS</h3>
 * <pre>{@code
 * TlsMode.START_TLS  // Upgrade plain connection to TLS, usually port 389
 * }</pre>
 *
 * <h3>Plain LDAP (Not Recommended for Production)</h3>
 * <pre>{@code
 * TlsMode.NONE  // No encryption, credentials sent in clear text
 * }</pre>
 *
 * <h3>System CA Certificates</h3>
 * <p>
 * Pass {@code null} for truststore path to use system's default CA certificates:
 * <pre>{@code
 * new LdapConnectionProperties(
 *     "ldap.example.com",
 *     636,
 *     TlsMode.LDAPS,
 *     null,  // Use system CAs (Let's Encrypt, DigiCert, etc.)
 *     null,
 *     null,
 *     "uid={username},ou=people,{baseDn}",
 *     "dc=example,dc=com"
 * );
 * }</pre>
 *
 * <h2>Error Handling</h2>
 *
 * <pre>{@code
 * try {
 *     client.start();
 *     boolean authenticated = client.authenticateUser("user", "pass");
 *
 * } catch (LDAPException e) {
 *     // LDAP protocol errors (connection issues, invalid credentials, etc.)
 *     if (e.getResultCode() == ResultCode.INVALID_CREDENTIALS) {
 *         // Wrong password
 *     } else if (e.getResultCode() == ResultCode.CONNECT_ERROR) {
 *         // Cannot connect to LDAP server
 *     }
 * } catch (GeneralSecurityException e) {
 *     // SSL/TLS errors (certificate issues, etc.)
 * } catch (SearchFilterDnResolver.DnResolutionException e) {
 *     // User DN could not be resolved (user not found, timeout, etc.)
 *     String username = e.getUsername();
 * }
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * All classes in this package are thread-safe once initialized. The {@link LdapClient} uses
 * connection pooling internally to handle concurrent authentication requests efficiently.
 *
 * @see LdapClient Main client for LDAP authentication
 * @see LdapConnectionProperties Connection configuration
 * @see TemplateDnResolver Fast, template-based DN resolution
 * @see SearchFilterDnResolver Flexible, search-based DN resolution
 * @see TlsMode TLS/SSL encryption modes
 */
package com.hivemq.api.auth.provider.impl.ldap;
