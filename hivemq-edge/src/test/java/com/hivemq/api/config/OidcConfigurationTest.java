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
package com.hivemq.api.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hivemq.configuration.entity.api.oidc.OidcAuthenticationEntity;
import com.hivemq.configuration.entity.api.oidc.OidcRoleMappingEntity;
import com.hivemq.configuration.entity.api.oidc.OidcTruststoreEntity;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for {@link OidcConfiguration#fromEntity}.
 */
class OidcConfigurationTest {

    @Test
    void fromEntity_mapsAllFields() {
        final OidcAuthenticationEntity entity = entity(
                "https://idp.example.com",
                "hivemq-edge",
                "the-secret",
                "https://edge.example.com/api/v1/auth/oidc/callback",
                "roles",
                List.of("email", "profile"),
                new OidcRoleMappingEntity("hivemq-admin", "admin"));

        final OidcConfiguration config = OidcConfiguration.fromEntity(entity);

        assertThat(config.getIssuerUri()).isEqualTo(URI.create("https://idp.example.com"));
        assertThat(config.getClientId()).isEqualTo("hivemq-edge");
        assertThat(config.getClientSecret()).isEqualTo("the-secret");
        assertThat(config.getRedirectUri()).isEqualTo(URI.create("https://edge.example.com/api/v1/auth/oidc/callback"));
        assertThat(config.getRoleClaimName()).isEqualTo("roles");
        assertThat(config.getExtraScopes()).containsExactly("email", "profile");
        // No <id-token-signing-algorithms> configured → the full asymmetric allow-list.
        assertThat(config.getIdTokenSigningAlgorithms()).isEqualTo(OidcSigningAlgorithms.DEFAULT);
    }

    @Test
    void fromEntity_configuredSigningAlgorithms_narrowTheAcceptedSet() {
        final OidcAuthenticationEntity entity = entity("https://idp", "client", null, "https://edge/cb", "roles", null);
        set(entity, "idTokenSigningAlgorithms", List.of("ES256"));

        final OidcConfiguration config = OidcConfiguration.fromEntity(entity);

        assertThat(config.getIdTokenSigningAlgorithms()).containsExactly("ES256");
    }

    @Test
    void fromEntity_invalidSigningAlgorithm_throws() {
        final OidcAuthenticationEntity entity = entity("https://idp", "client", null, "https://edge/cb", "roles", null);
        set(entity, "idTokenSigningAlgorithms", List.of("HS256"));

        assertThatThrownBy(() -> OidcConfiguration.fromEntity(entity))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("asymmetric");
    }

    @Test
    void fromEntity_extraScopes_arePassedThrough() {
        // Each scope is its own <extra-scope> element; the list is taken verbatim, with no delimiter parsing.
        final OidcConfiguration config = OidcConfiguration.fromEntity(
                entity("https://idp", "client", null, "https://edge/cb", "roles", List.of("email", "profile")));

        assertThat(config.getExtraScopes()).containsExactly("email", "profile");
    }

    @Test
    void fromEntity_absentExtraScopes_yieldsEmptyList() {
        final OidcConfiguration config =
                OidcConfiguration.fromEntity(entity("https://idp", "client", null, "https://edge/cb", "roles", null));

        assertThat(config.getExtraScopes()).isEmpty();
    }

    @Test
    void fromEntity_roleMappingKeysAreStoredLiterally() {
        // Keys are matched literally (no case-folding), so they are stored exactly as configured.
        final OidcAuthenticationEntity entity = entity(
                "https://idp",
                "client",
                null,
                "https://edge/cb",
                "roles",
                null,
                new OidcRoleMappingEntity("HiveMQ-Admin", "admin"),
                new OidcRoleMappingEntity("HIVEMQ-USER", "user"));

        final OidcConfiguration config = OidcConfiguration.fromEntity(entity);

        assertThat(config.getRoleMappings())
                .containsEntry("HiveMQ-Admin", "admin")
                .containsEntry("HIVEMQ-USER", "user");
    }

    @Test
    void fromEntity_noRoleMappings_isRejected() {
        // Role mappings are required: an absent (or empty) <role-mappings> is rejected rather than granting
        // Edge roles by IdP group name (there is no verbatim mode).
        final OidcAuthenticationEntity entity = entity("https://idp", "client", null, "https://edge/cb", "roles", null);
        set(entity, "roleMappings", List.of());

        assertThatThrownBy(() -> OidcConfiguration.fromEntity(entity))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("role-mappings");
    }

    @Test
    void fromEntity_missingIssuer_throws() {
        assertThatThrownBy(() ->
                        OidcConfiguration.fromEntity(entity(null, "client", null, "https://edge/cb", "roles", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("issuer-uri");
    }

    @Test
    void fromEntity_missingClientId_throws() {
        assertThatThrownBy(() -> OidcConfiguration.fromEntity(
                        entity("https://idp", "  ", null, "https://edge/cb", "roles", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("client-id");
    }

    @Test
    void fromEntity_missingRedirect_throws() {
        assertThatThrownBy(
                        () -> OidcConfiguration.fromEntity(entity("https://idp", "client", null, null, "roles", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("redirect-uri");
    }

    @Test
    void fromEntity_relativeIssuerUri_throws() {
        // URI.create accepts a relative reference; it must not survive into the runtime config, where it
        // would produce a null://null postMessage origin.
        assertThatThrownBy(() -> OidcConfiguration.fromEntity(
                        entity("idp.example.com/auth", "client", null, "https://edge/cb", "roles", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("absolute");
    }

    @Test
    void fromEntity_relativeRedirectUri_throws() {
        assertThatThrownBy(() ->
                        OidcConfiguration.fromEntity(entity("https://idp", "client", null, "callback", "roles", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("absolute");
    }

    @Test
    void fromEntity_nonHttpScheme_throws() {
        assertThatThrownBy(() -> OidcConfiguration.fromEntity(
                        entity("ftp://idp.example.com", "client", null, "https://edge/cb", "roles", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("http or https");
    }

    @Test
    void fromEntity_uriWithoutHost_throws() {
        assertThatThrownBy(() -> OidcConfiguration.fromEntity(
                        entity("https:///path", "client", null, "https://edge/cb", "roles", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("host");
    }

    @Test
    void fromEntity_redirectWithUserInfo_throws() {
        assertThatThrownBy(() -> OidcConfiguration.fromEntity(
                        entity("https://idp", "client", null, "https://user:pw@edge/cb", "roles", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("user information");
    }

    @Test
    void fromEntity_redirectWithFragment_throws() {
        assertThatThrownBy(() -> OidcConfiguration.fromEntity(
                        entity("https://idp", "client", null, "https://edge/cb#frag", "roles", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fragment");
    }

    @Test
    void fromEntity_httpIssuer_throws() {
        // The issuer is the root of trust for discovery and signing keys; plain http lets an on-path
        // attacker rewrite them, so it is rejected outright — including http://localhost.
        assertThatThrownBy(() -> OidcConfiguration.fromEntity(
                        entity("http://idp.example.com", "client", null, "https://edge/cb", "roles", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("https");
        assertThatThrownBy(() -> OidcConfiguration.fromEntity(
                        entity("http://localhost:8080/realms/edge", "client", null, "https://edge/cb", "roles", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("https");
    }

    @Test
    void fromEntity_issuerWithQuery_throws() {
        // The OIDC Discovery spec forbids a query component on the issuer.
        assertThatThrownBy(() -> OidcConfiguration.fromEntity(
                        entity("https://idp.example.com?realm=edge", "client", null, "https://edge/cb", "roles", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("query");
    }

    @Test
    void fromEntity_issuerWithFragment_throws() {
        assertThatThrownBy(() -> OidcConfiguration.fromEntity(
                        entity("https://idp.example.com#frag", "client", null, "https://edge/cb", "roles", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fragment");
    }

    @Test
    void fromEntity_httpsIssuerWithPath_isAccepted() {
        final OidcConfiguration config = OidcConfiguration.fromEntity(
                entity("https://idp.example.com/realms/edge", "client", null, "https://edge/cb", "roles", null));

        assertThat(config.getIssuerUri()).isEqualTo(URI.create("https://idp.example.com/realms/edge"));
    }

    @Test
    void fromEntity_httpLoopbackIssuer_isAcceptedWhenInsecureFlagIsSet() {
        // The dev/test escape hatch: a loopback http issuer is allowed only with the flag on.
        final OidcConfiguration config = OidcConfiguration.fromEntity(
                entity("http://localhost:8080/realms/edge", "client", null, "https://edge/cb", "roles", null), true);

        assertThat(config.getIssuerUri()).isEqualTo(URI.create("http://localhost:8080/realms/edge"));
    }

    @Test
    void fromEntity_httpLoopbackIssuer_isRejectedWhenInsecureFlagIsOff() {
        assertThatThrownBy(() -> OidcConfiguration.fromEntity(
                        entity("http://localhost:8080/realms/edge", "client", null, "https://edge/cb", "roles", null),
                        false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("https");
    }

    @Test
    void fromEntity_httpNonLoopbackIssuer_isRejectedEvenWhenInsecureFlagIsSet() {
        // The flag only relaxes loopback; a real remote http issuer stays rejected.
        assertThatThrownBy(() -> OidcConfiguration.fromEntity(
                        entity("http://idp.example.com/realms/edge", "client", null, "https://edge/cb", "roles", null),
                        true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("https");
    }

    @Test
    void fromEntity_httpLoopbackRedirect_isAccepted() {
        // The redirect carries the auth code and the Edge token back to the browser, so it must be https --
        // except for a literal loopback host, which never leaves the machine (local dev/test).
        final OidcConfiguration config = OidcConfiguration.fromEntity(
                entity("https://idp.example.com", "client", null, "http://localhost:8080/cb", "roles", null));

        assertThat(config.getRedirectUri()).isEqualTo(URI.create("http://localhost:8080/cb"));
    }

    @Test
    void fromEntity_httpLoopbackIpRedirect_isAccepted() {
        // 127.0.0.0/8 is loopback and is matched literally (no DNS), so 127.0.0.2 is accepted too.
        final OidcConfiguration config = OidcConfiguration.fromEntity(
                entity("https://idp.example.com", "client", null, "http://127.0.0.2:8080/cb", "roles", null));

        assertThat(config.getRedirectUri()).isEqualTo(URI.create("http://127.0.0.2:8080/cb"));
    }

    @Test
    void fromEntity_httpNonLoopbackRedirect_isRejected() {
        // A plain-http redirect on a public host exposes the auth code and the Edge token on the wire.
        assertThatThrownBy(() -> OidcConfiguration.fromEntity(entity(
                        "https://idp.example.com",
                        "client",
                        null,
                        "http://edge.public.example.com/api/v1/auth/oidc/callback",
                        "roles",
                        null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("https");
    }

    @Test
    void fromEntity_httpRedirectOnPublicNameResolvingToLoopback_isRejected() {
        // A public hostname such as 127.0.0.1.nip.io resolves to a loopback address, but the loopback check
        // is literal (no DNS), so it is treated as a public host and rejected -- closing the DNS-based bypass.
        assertThatThrownBy(() -> OidcConfiguration.fromEntity(entity(
                        "https://idp.example.com", "client", null, "http://127.0.0.1.nip.io:8080/cb", "roles", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("https");
    }

    @Test
    void fromEntity_blankRoleClaimName_throws() {
        // A blank claim name extracts no roles at all, denying every user for a non-obvious reason.
        assertThatThrownBy(() -> OidcConfiguration.fromEntity(
                        entity("https://idp", "client", null, "https://edge/cb", "  ", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("role-claim-name");
    }

    @Test
    void fromEntity_invalidEdgeRole_throws() {
        assertThatThrownBy(() -> OidcConfiguration.fromEntity(entity(
                        "https://idp",
                        "client",
                        null,
                        "https://edge/cb",
                        "roles",
                        null,
                        new OidcRoleMappingEntity("idp-role", "wizard"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("edge-role");
    }

    @Test
    void fromEntity_acceptsAllValidEdgeRolesCaseInsensitively() {
        final OidcConfiguration config = OidcConfiguration.fromEntity(entity(
                "https://idp",
                "client",
                null,
                "https://edge/cb",
                "roles",
                null,
                new OidcRoleMappingEntity("a", "ADMIN"),
                new OidcRoleMappingEntity("b", "Super"),
                new OidcRoleMappingEntity("c", "user")));

        assertThat(config.getRoleMappings()).containsValues("ADMIN", "Super", "user");
    }

    @Test
    void fromEntity_duplicateIdpRole_throws() {
        assertThatThrownBy(() -> OidcConfiguration.fromEntity(entity(
                        "https://idp",
                        "client",
                        null,
                        "https://edge/cb",
                        "roles",
                        null,
                        new OidcRoleMappingEntity("Team-Admins", "admin"),
                        // the same IdP role literally repeated
                        new OidcRoleMappingEntity("Team-Admins", "user"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("duplicate");
    }

    @Test
    void fromEntity_noTruststore_usesJvmDefault() {
        // Absent <truststore> means the IdP certificate is validated against the JVM default CAs: no factory.
        final OidcConfiguration config =
                OidcConfiguration.fromEntity(entity("https://idp", "client", null, "https://edge/cb", "roles", null));

        assertThat(config.getIdpSslSocketFactory()).isNull();
    }

    @Test
    void fromEntity_configuredTruststore_buildsAnSslSocketFactory(@TempDir final Path tempDir) throws Exception {
        final Path truststorePath = writeEmptyTruststore(tempDir, "truststore.p12", "changeit");
        final OidcAuthenticationEntity entity = entity("https://idp", "client", null, "https://edge/cb", "roles", null);
        set(entity, "truststore", truststore(truststorePath.toString(), "changeit", "PKCS12"));

        final OidcConfiguration config = OidcConfiguration.fromEntity(entity);

        assertThat(config.getIdpSslSocketFactory()).isNotNull();
    }

    @Test
    void fromEntity_missingTruststoreFile_throwsAConfigurationError() {
        final OidcAuthenticationEntity entity = entity("https://idp", "client", null, "https://edge/cb", "roles", null);
        set(entity, "truststore", truststore("/no/such/truststore.p12", "changeit", "PKCS12"));

        // A missing file is a configuration error surfaced at startup, not a silent fallback to the JVM CAs.
        assertThatThrownBy(() -> OidcConfiguration.fromEntity(entity))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("truststore");
    }

    @Test
    void fromEntity_wrongTruststorePassword_throwsAConfigurationError(@TempDir final Path tempDir) throws Exception {
        final Path truststorePath = writeEmptyTruststore(tempDir, "truststore.p12", "changeit");
        final OidcAuthenticationEntity entity = entity("https://idp", "client", null, "https://edge/cb", "roles", null);
        set(entity, "truststore", truststore(truststorePath.toString(), "wrong-password", "PKCS12"));

        assertThatThrownBy(() -> OidcConfiguration.fromEntity(entity))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("truststore");
    }

    @Test
    void fromEntity_truststoreWithPasswordButNoPath_throwsAConfigurationError() {
        // A <truststore> with a password but no path can only be an omitted/mistyped path. Reject it rather
        // than silently validating the IdP against the JVM default CAs, which would broaden the trust anchors
        // the operator meant to pin.
        final OidcAuthenticationEntity entity = entity("https://idp", "client", null, "https://edge/cb", "roles", null);
        set(entity, "truststore", truststore(null, "secret", null));

        assertThatThrownBy(() -> OidcConfiguration.fromEntity(entity))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("truststore-path");
    }

    @Test
    void fromEntity_emptyTruststoreElement_usesJvmDefault() {
        // A completely empty <truststore/> carries no intent to pin, so it falls back to the JVM default CAs.
        final OidcAuthenticationEntity entity = entity("https://idp", "client", null, "https://edge/cb", "roles", null);
        set(entity, "truststore", truststore(null, null, null));

        assertThat(OidcConfiguration.fromEntity(entity).getIdpSslSocketFactory())
                .isNull();
    }

    @Test
    void fromEntity_noConnectionTimeout_usesTheDefault() {
        final OidcConfiguration config =
                OidcConfiguration.fromEntity(entity("https://idp", "client", null, "https://edge/cb", "roles", null));

        assertThat(config.getConnectionTimeoutMillis()).isEqualTo(OidcConfiguration.DEFAULT_CONNECTION_TIMEOUT_MILLIS);
    }

    @Test
    void fromEntity_configuredConnectionTimeout_isCarriedThrough() {
        final OidcAuthenticationEntity entity = entity("https://idp", "client", null, "https://edge/cb", "roles", null);
        set(entity, "connectionTimeoutMillis", 20_000);

        final OidcConfiguration config = OidcConfiguration.fromEntity(entity);

        assertThat(config.getConnectionTimeoutMillis()).isEqualTo(20_000);
    }

    @Test
    void fromEntity_belowMinimumConnectionTimeout_throws() {
        // A sub-100ms timeout (e.g. "1" meaning one second) would make every IdP call time out; it is
        // rejected. The XSD guards this too; the check defends a config path that bypasses the schema.
        final OidcAuthenticationEntity entity = entity("https://idp", "client", null, "https://edge/cb", "roles", null);
        set(entity, "connectionTimeoutMillis", 50);

        assertThatThrownBy(() -> OidcConfiguration.fromEntity(entity))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("connection-timeout-millis");
    }

    @Test
    void fromEntity_minimumConnectionTimeout_isAccepted() {
        final OidcAuthenticationEntity entity = entity("https://idp", "client", null, "https://edge/cb", "roles", null);
        set(entity, "connectionTimeoutMillis", OidcConfiguration.MIN_CONNECTION_TIMEOUT_MILLIS);

        final OidcConfiguration config = OidcConfiguration.fromEntity(entity);

        assertThat(config.getConnectionTimeoutMillis()).isEqualTo(OidcConfiguration.MIN_CONNECTION_TIMEOUT_MILLIS);
    }

    private static @org.jetbrains.annotations.NotNull OidcTruststoreEntity truststore(
            final String path, final String password, final String type) {
        final OidcTruststoreEntity truststore = new OidcTruststoreEntity();
        set(truststore, "truststorePath", path);
        set(truststore, "truststorePassword", password);
        set(truststore, "truststoreType", type);
        return truststore;
    }

    private static @org.jetbrains.annotations.NotNull Path writeEmptyTruststore(
            final Path dir, final String name, final String password) throws Exception {
        final KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null, null);
        final Path path = dir.resolve(name);
        try (final OutputStream out = Files.newOutputStream(path)) {
            keyStore.store(out, password.toCharArray());
        }
        return path;
    }

    @Test
    void validateDiscoveryMetadata_httpsIssuerAndEndpoints_isAccepted() throws Exception {
        // The normal case: discovery issuer matches and every endpoint is https.
        OidcConfiguration.validateDiscoveryMetadata(
                URI.create("https://idp.example.com/realms/edge"),
                metadata(
                        "https://idp.example.com/realms/edge",
                        "https://idp.example.com/realms/edge/protocol/openid-connect/auth",
                        "https://idp.example.com/realms/edge/protocol/openid-connect/token",
                        "https://idp.example.com/realms/edge/protocol/openid-connect/certs"));
    }

    @Test
    void validateDiscoveryMetadata_issuerMismatch_throws() throws Exception {
        // A discovery document whose issuer differs from the configured one is rejected: it means the login
        // would be driven by a provider other than the trusted issuer.
        assertThatThrownBy(() -> OidcConfiguration.validateDiscoveryMetadata(
                        URI.create("https://idp.example.com/realms/edge"),
                        metadata(
                                "https://evil.example.com/realms/edge",
                                "https://idp.example.com/auth",
                                "https://idp.example.com/token",
                                "https://idp.example.com/certs")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not match the configured issuer-uri")
                // A genuine host/realm mismatch gets the plain message, not the cosmetic-difference guidance.
                .hasMessageNotContaining("trailing slash");
    }

    @Test
    void validateDiscoveryMetadata_trailingSlashIssuer_throwsWithAGuidingMessage() throws Exception {
        // The configured issuer differs from the discovery issuer only by a trailing slash (a browser
        // address bar adds one). It is still refused -- the issuer match is exact -- but the message names
        // the cosmetic difference and quotes the value to copy, rather than the opaque "does not match".
        assertThatThrownBy(() -> OidcConfiguration.validateDiscoveryMetadata(
                        URI.create("https://idp.example.com/realms/edge/"),
                        metadata(
                                "https://idp.example.com/realms/edge",
                                "https://idp.example.com/auth",
                                "https://idp.example.com/token",
                                "https://idp.example.com/certs")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("trailing slash")
                .hasMessageContaining("set issuer-uri to 'https://idp.example.com/realms/edge'");
    }

    @Test
    void validateDiscoveryMetadata_caseDifferentIssuer_throwsWithAGuidingMessage() throws Exception {
        // A case-only difference is likewise cosmetic: refused, but with the guiding message.
        assertThatThrownBy(() -> OidcConfiguration.validateDiscoveryMetadata(
                        URI.create("https://IDP.example.com/realms/edge"),
                        metadata(
                                "https://idp.example.com/realms/edge",
                                "https://idp.example.com/auth",
                                "https://idp.example.com/token",
                                "https://idp.example.com/certs")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("letter case");
    }

    @Test
    void validateDiscoveryMetadata_httpEndpoint_throws() throws Exception {
        // An http endpoint (here the token endpoint) is rejected for an https issuer: over plain http the
        // authorization code and PKCE verifier would be exposed to an on-path attacker.
        assertThatThrownBy(() -> OidcConfiguration.validateDiscoveryMetadata(
                        URI.create("https://idp.example.com/realms/edge"),
                        metadata(
                                "https://idp.example.com/realms/edge",
                                "https://idp.example.com/auth",
                                "http://idp.example.com/token",
                                "https://idp.example.com/certs")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("https");
    }

    /**
     * Builds an {@link OIDCProviderMetadata} the way the service does — by parsing a discovery JSON document
     * — so the tests exercise the same code path rather than a hand-constructed object.
     */
    private static @org.jetbrains.annotations.NotNull OIDCProviderMetadata metadata(
            final String issuer, final String authEndpoint, final String tokenEndpoint, final String jwksUri)
            throws Exception {
        final String json = String.format(
                "{\"issuer\":\"%s\",\"authorization_endpoint\":\"%s\",\"token_endpoint\":\"%s\","
                        + "\"jwks_uri\":\"%s\",\"subject_types_supported\":[\"public\"],"
                        + "\"response_types_supported\":[\"code\"]}",
                issuer, authEndpoint, tokenEndpoint, jwksUri);
        return OIDCProviderMetadata.parse(json);
    }

    @Test
    void isLiteralLoopbackHost_matchesLiteralLoopbackFormsOnly() {
        // Literal loopback: accepted.
        assertThat(OidcConfiguration.isLiteralLoopbackHost("localhost")).isTrue();
        assertThat(OidcConfiguration.isLiteralLoopbackHost("LOCALHOST")).isTrue();
        assertThat(OidcConfiguration.isLiteralLoopbackHost("127.0.0.1")).isTrue();
        assertThat(OidcConfiguration.isLiteralLoopbackHost("127.0.0.2")).isTrue();
        assertThat(OidcConfiguration.isLiteralLoopbackHost("127.255.255.255")).isTrue();
        assertThat(OidcConfiguration.isLiteralLoopbackHost("::1")).isTrue();
        assertThat(OidcConfiguration.isLiteralLoopbackHost("[::1]")).isTrue();

        // Not literal loopback: rejected. The nip.io name resolves to 127.0.0.1 but is never resolved here.
        assertThat(OidcConfiguration.isLiteralLoopbackHost("127.0.0.1.nip.io")).isFalse();
        assertThat(OidcConfiguration.isLiteralLoopbackHost("edge.example.com")).isFalse();
        assertThat(OidcConfiguration.isLiteralLoopbackHost("128.0.0.1")).isFalse();
        assertThat(OidcConfiguration.isLiteralLoopbackHost("127.0.0.256")).isFalse();
        assertThat(OidcConfiguration.isLiteralLoopbackHost(null)).isFalse();
    }

    // -- helpers: OidcAuthenticationEntity fields are populated by JAXB, so set them reflectively for tests.

    private static @org.jetbrains.annotations.NotNull OidcAuthenticationEntity entity(
            final String issuer,
            final String clientId,
            final String clientSecret,
            final String redirect,
            final String roleClaim,
            final List<String> extraScopes,
            final OidcRoleMappingEntity... mappings) {
        final OidcAuthenticationEntity entity = new OidcAuthenticationEntity();
        set(entity, "enabled", true);
        set(entity, "issuerUri", issuer);
        set(entity, "clientId", clientId);
        set(entity, "clientSecret", clientSecret);
        set(entity, "redirectUri", redirect);
        set(entity, "roleClaimName", roleClaim);
        set(entity, "extraScopes", extraScopes);
        // Role mappings are required, so default to a valid mapping when a test does not care about them.
        // A test that is about the mappings passes its own; a test of the "no mappings" error clears the
        // field after construction.
        final List<OidcRoleMappingEntity> roleMappings =
                mappings.length == 0 ? List.of(new OidcRoleMappingEntity("idp-admins", "admin")) : List.of(mappings);
        set(entity, "roleMappings", roleMappings);
        return entity;
    }

    private static void set(final Object target, final String fieldName, final Object value) {
        try {
            final Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (final ReflectiveOperationException e) {
            throw new IllegalStateException("could not set test field " + fieldName, e);
        }
    }
}
