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

import com.google.common.base.Preconditions;
import com.hivemq.api.auth.ApiRoles;
import com.hivemq.configuration.entity.api.oidc.OidcAuthenticationEntity;
import com.hivemq.configuration.entity.api.oidc.OidcRoleMappingEntity;
import com.hivemq.configuration.entity.api.oidc.OidcTruststoreEntity;
import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runtime configuration for OpenID Connect (OIDC) authentication.
 * <p>
 * Immutable, derived from an {@link OidcAuthenticationEntity} via {@link #fromEntity}.
 * The IdP endpoints (authorization / token / JWKS) are not held here — they are resolved
 * at runtime from {@code issuerUri}'s discovery document.
 * <p>
 * {@code roleMappings} maps an IdP role name onto an Edge role string. It is required and non-empty — only
 * an explicitly mapped IdP role produces an Edge role. Keys are matched literally — see
 * {@code OidcServiceImpl#resolveEdgeRoles}.
 */
public class OidcConfiguration {

    private static final @NotNull Logger log = LoggerFactory.getLogger(OidcConfiguration.class);

    private final @NotNull URI issuerUri;
    private final @NotNull String clientId;
    private final @Nullable String clientSecret;
    private final @NotNull URI redirectUri;
    private final @NotNull String roleClaimName;
    private final @NotNull List<String> extraScopes;
    // IdP-role → Edge-role mappings. Required and non-empty: only an explicitly mapped IdP role produces an
    // Edge role; an unmapped value grants nothing.
    private final @NotNull Map<String, String> roleMappings;
    private final @NotNull Set<String> idTokenSigningAlgorithms;
    // null => no <truststore> configured: the IdP TLS certificate is validated against the JVM default CAs.
    private final @Nullable SSLSocketFactory idpSslSocketFactory;
    // Connect/read timeout in milliseconds for each IdP call. Defaults to DEFAULT_CONNECTION_TIMEOUT_MILLIS.
    private final int connectionTimeoutMillis;

    public static final int DEFAULT_CONNECTION_TIMEOUT_MILLIS = 5000;

    public OidcConfiguration(
            final @NotNull URI issuerUri,
            final @NotNull String clientId,
            final @Nullable String clientSecret,
            final @NotNull URI redirectUri,
            final @NotNull String roleClaimName,
            final @NotNull List<String> extraScopes,
            final @NotNull Map<String, String> roleMappings,
            final @NotNull Set<String> idTokenSigningAlgorithms) {
        this(
                issuerUri,
                clientId,
                clientSecret,
                redirectUri,
                roleClaimName,
                extraScopes,
                roleMappings,
                idTokenSigningAlgorithms,
                null,
                DEFAULT_CONNECTION_TIMEOUT_MILLIS);
    }

    public OidcConfiguration(
            final @NotNull URI issuerUri,
            final @NotNull String clientId,
            final @Nullable String clientSecret,
            final @NotNull URI redirectUri,
            final @NotNull String roleClaimName,
            final @NotNull List<String> extraScopes,
            final @NotNull Map<String, String> roleMappings,
            final @NotNull Set<String> idTokenSigningAlgorithms,
            final @Nullable SSLSocketFactory idpSslSocketFactory,
            final int connectionTimeoutMillis) {
        this.issuerUri = Preconditions.checkNotNull(issuerUri);
        this.clientId = Preconditions.checkNotNull(clientId);
        this.clientSecret = clientSecret;
        this.redirectUri = Preconditions.checkNotNull(redirectUri);
        this.roleClaimName = Preconditions.checkNotNull(roleClaimName);
        this.idTokenSigningAlgorithms = Set.copyOf(idTokenSigningAlgorithms);
        this.extraScopes = List.copyOf(extraScopes);
        this.roleMappings = Map.copyOf(roleMappings);
        this.idpSslSocketFactory = idpSslSocketFactory;
        this.connectionTimeoutMillis = connectionTimeoutMillis;
    }

    /**
     * Environment variable (or system property, checked as a fallback) that relaxes the HTTPS
     * requirement on the issuer for a loopback host. It exists only to run the flow against a local,
     * plain-HTTP Identity Provider during development and testing. It defaults to off, is restricted to
     * loopback issuers, and logs a warning on every use — it must never be set in production.
     */
    public static final @NotNull String ALLOW_INSECURE_LOCAL_IDP_PROPERTY =
            "HIVEMQ_EDGE_ENABLE_INSECURE_LOCAL_CONNECTION_FOR_IDENTITY_PROVIDER";

    /**
     * Builds an {@link OidcConfiguration} from its XML entity.
     * <p>
     * Precondition: the entity is enabled and its required fields ({@code issuer-uri},
     * {@code client-id}, {@code redirect-uri}) are populated — the caller
     * ({@code ApiConfigurator}) is responsible for validating and skipping a disabled or
     * incomplete entity.
     *
     * @throws IllegalArgumentException if a required URI field is missing or malformed
     */
    public static @NotNull OidcConfiguration fromEntity(final @NotNull OidcAuthenticationEntity entity) {
        return fromEntity(entity, isInsecureLocalIdpAllowed());
    }

    static @NotNull OidcConfiguration fromEntity(
            final @NotNull OidcAuthenticationEntity entity, final boolean allowInsecureLoopbackIssuer) {
        final String issuer = entity.getIssuerUri();
        final String clientId = entity.getClientId();
        final String redirect = entity.getRedirectUri();
        Preconditions.checkArgument(issuer != null && !issuer.isBlank(), "OIDC issuer-uri must be configured");
        Preconditions.checkArgument(clientId != null && !clientId.isBlank(), "OIDC client-id must be configured");
        Preconditions.checkArgument(redirect != null && !redirect.isBlank(), "OIDC redirect-uri must be configured");

        final URI issuerUri = parseIssuerUri(issuer, allowInsecureLoopbackIssuer);
        final URI redirectUri = parseRedirectUri(redirect);

        // A blank claim name would extract no roles at all, denying every user with no obvious cause.
        final String roleClaimName = entity.getRoleClaimName();
        Preconditions.checkArgument(
                roleClaimName != null && !roleClaimName.isBlank(), "OIDC role-claim-name must not be blank");
        warnOnSurroundingWhitespace("role-claim-name", roleClaimName);

        final List<String> scopes = parseScopes(entity.getExtraScopes());

        // Role mappings are required: only an explicitly mapped IdP role produces an Edge role, and an
        // unmapped value grants nothing. The <role-mappings> element must be present with at least one
        // <role-mapping> (enforced by the XSD; re-checked here so a programmatic entity cannot bypass it).
        // Keys are stored and matched literally — no trimming or case-folding — so a stray space or wrong
        // case is an honest mismatch the operator can see in the logs, not a silent, surprising match.
        final List<OidcRoleMappingEntity> mappingEntities = entity.getRoleMappings();
        Preconditions.checkArgument(
                mappingEntities != null && !mappingEntities.isEmpty(),
                "OIDC requires a <role-mappings> element with at least one <role-mapping>");
        final Map<String, String> mappings = new LinkedHashMap<>();
        for (final OidcRoleMappingEntity mapping : mappingEntities) {
            final String idpRole = mapping.getIdpRole();
            final String edgeRole = mapping.getEdgeRole();
            Preconditions.checkArgument(
                    idpRole != null && !idpRole.isBlank(), "OIDC role mapping is missing an <idp-role>");
            Preconditions.checkArgument(
                    edgeRole != null && !edgeRole.isBlank(), "OIDC role mapping is missing an <edge-role>");
            Preconditions.checkArgument(
                    isValidEdgeRole(edgeRole),
                    "OIDC role mapping has an invalid <edge-role> '%s'; must be one of admin, super, user",
                    edgeRole);
            warnOnSurroundingWhitespace("idp-role", idpRole);
            warnOnSurroundingWhitespace("edge-role", edgeRole);
            Preconditions.checkArgument(
                    !mappings.containsKey(idpRole), "OIDC role mapping has a duplicate <idp-role> '%s'", idpRole);
            mappings.put(idpRole, edgeRole);
        }

        // Accepted ID-token signing algorithms. An absent/empty list defaults to the full asymmetric set;
        // a configured list is validated against it (the XSD requires at least one entry when present).
        final List<String> configuredAlgorithms = entity.getIdTokenSigningAlgorithms();
        final Set<String> signingAlgorithms = configuredAlgorithms == null || configuredAlgorithms.isEmpty()
                ? OidcSigningAlgorithms.DEFAULT
                : OidcSigningAlgorithms.validate(configuredAlgorithms);

        // Optional truststore for the IdP TLS connection. Absent, or present with no path, means the IdP
        // certificate is validated against the JVM default CA certificates.
        final SSLSocketFactory idpSslSocketFactory = buildSslSocketFactory(entity.getTruststore());

        // Connect/read timeout for the IdP calls. The XSD (positiveInteger, default 5000) guarantees a
        // positive value; this check guards a config path that bypasses schema validation.
        final int connectionTimeoutMillis = entity.getConnectionTimeoutMillis();
        Preconditions.checkArgument(
                connectionTimeoutMillis > 0,
                "OIDC connection-timeout must be a positive number of milliseconds, but was %s",
                connectionTimeoutMillis);

        return new OidcConfiguration(
                issuerUri,
                clientId.trim(),
                entity.getClientSecret(),
                redirectUri,
                roleClaimName,
                scopes,
                mappings,
                signingAlgorithms,
                idpSslSocketFactory,
                connectionTimeoutMillis);
    }

    /**
     * Builds the {@link SSLSocketFactory} used for the IdP TLS connection from the configured truststore,
     * mirroring the LDAP truststore behavior. Returns {@code null} when no {@code <truststore>} is
     * configured at all, so the caller falls back to the JVM default CA certificates. A missing file or
     * wrong password is a configuration error surfaced at startup, not a silent fallback.
     * <p>
     * A {@code <truststore>} element that is present but carries a password or type without a usable path
     * is rejected rather than silently downgraded to the JVM default CAs: it can only be an omitted or
     * mistyped path, and treating it as "no truststore" would quietly discard the operator's intent to pin
     * IdP trust to a specific CA — broadening the trust anchors of the discovery/token/JWKS connection.
     */
    private static @Nullable SSLSocketFactory buildSslSocketFactory(final @Nullable OidcTruststoreEntity truststore) {
        if (truststore == null) {
            return null;
        }
        final String configuredPath = truststore.getTruststorePath();
        if (configuredPath == null || configuredPath.isBlank()) {
            // A present <truststore> with a password or type but no path is a misconfiguration, not "no
            // truststore" — fail at startup instead of silently validating against the default CA set.
            final boolean hasPassword = truststore.getTruststorePassword() != null;
            final boolean hasType = truststore.getTruststoreType() != null
                    && !truststore.getTruststoreType().isBlank();
            Preconditions.checkArgument(
                    !hasPassword && !hasType,
                    "OIDC truststore is configured without a truststore-path; add the path or remove the "
                            + "truststore element to use the JVM default CA certificates");
            return null;
        }
        final String path = configuredPath.trim();
        final String type = truststore.getTruststoreType() != null
                        && !truststore.getTruststoreType().isBlank()
                ? truststore.getTruststoreType().trim()
                : KeyStore.getDefaultType();
        final char[] password = truststore.getTruststorePassword() != null
                ? truststore.getTruststorePassword().toCharArray()
                : null;
        try {
            final KeyStore keyStore = KeyStore.getInstance(type);
            try (final InputStream in = Files.newInputStream(Path.of(path))) {
                keyStore.load(in, password);
            }
            final TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(keyStore);
            final SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, tmf.getTrustManagers(), null);
            return sslContext.getSocketFactory();
        } catch (final IOException | GeneralSecurityException e) {
            throw new IllegalArgumentException(
                    String.format("OIDC truststore '%s' could not be loaded: %s", path, e.getMessage()), e);
        }
    }

    /**
     * Parses the {@code issuer-uri}. The issuer is the base URL from which Edge fetches the discovery
     * document, and in turn the token and JWKS endpoints, so its integrity is the root of trust for the
     * whole flow. It is therefore required to be {@code https}: over plain {@code http} a network
     * attacker could rewrite discovery, point the endpoints at their own infrastructure, publish their
     * own signing keys, and mint an ID token Edge would accept.
     * <p>
     * Per the OpenID Connect Discovery specification an issuer must use {@code https} and must not carry
     * a query or fragment component; both are rejected here.
     * <p>
     * The single exception is a loopback {@code http} issuer when {@link #ALLOW_INSECURE_LOCAL_IDP_PROPERTY}
     * is set — a development/testing aid that is off by default and warns on every use.
     */
    private static @NotNull URI parseIssuerUri(final @NotNull String value, final boolean allowInsecureLoopbackIssuer) {
        final URI uri = parseAbsoluteHttpUri(value, "issuer-uri");
        Preconditions.checkArgument(
                uri.getQuery() == null, "OIDC issuer-uri '%s' must not contain a query component", value);
        Preconditions.checkArgument(
                uri.getFragment() == null, "OIDC issuer-uri '%s' must not contain a fragment", value);
        if ("https".equalsIgnoreCase(uri.getScheme())) {
            return uri;
        }
        // Not https: only allowed for a loopback host, and only when the insecure-local flag is set.
        Preconditions.checkArgument(
                allowInsecureLoopbackIssuer && isLiteralLoopbackHost(uri.getHost()),
                "OIDC issuer-uri '%s' must use https; the issuer is the root of trust for discovery and "
                        + "signing keys and must not be fetched over plain http",
                value);
        log.warn(
                "OIDC issuer-uri '{}' is using plain HTTP. This is permitted only because {} is set, and is "
                        + "restricted to a loopback host. It is insecure and must never be used in production.",
                value,
                ALLOW_INSECURE_LOCAL_IDP_PROPERTY);
        return uri;
    }

    /**
     * Reads {@link #ALLOW_INSECURE_LOCAL_IDP_PROPERTY} from the environment, falling back to a system
     * property so it can be set in tests without mutating the process environment. Any value other than
     * {@code "true"} (case-insensitive) leaves the flag off.
     */
    private static boolean isInsecureLocalIdpAllowed() {
        final String env = System.getenv(ALLOW_INSECURE_LOCAL_IDP_PROPERTY);
        final String value = env != null ? env : System.getProperty(ALLOW_INSECURE_LOCAL_IDP_PROPERTY);
        return "true".equalsIgnoreCase(value);
    }

    /**
     * Whether {@code host} is a <em>literal</em> loopback identifier — {@code localhost}, an IPv4 address in
     * {@code 127.0.0.0/8}, or the IPv6 loopback {@code ::1}. The comparison is purely textual: the host is
     * never resolved through DNS.
     * <p>
     * This is deliberate. Resolving the host to decide whether it is loopback trusts a DNS answer, and a DNS
     * answer can be attacker-controlled: a public name such as {@code 127.0.0.1.nip.io} resolves to a
     * loopback address, and a low-TTL name can resolve to loopback for this check and to another address when
     * the endpoint is actually fetched (DNS rebinding). Comparing the literal host removes DNS from the
     * security decision entirely — a literal IP has no lookup to poison, and {@code localhost} is resolved by
     * the host's own configuration, not by an external answer.
     */
    static boolean isLiteralLoopbackHost(final @Nullable String host) {
        if (host == null) {
            return false;
        }
        // URI.getHost() returns an IPv6 literal wrapped in brackets, e.g. "[::1]"; compare the inner address.
        final String bare = host.startsWith("[") && host.endsWith("]") ? host.substring(1, host.length() - 1) : host;
        if ("localhost".equalsIgnoreCase(bare) || "::1".equals(bare)) {
            return true;
        }
        // Any address in 127.0.0.0/8 is loopback. Match the literal dotted-quad form without resolving.
        return bare.matches("127\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}") && isValidIpv4(bare);
    }

    private static boolean isValidIpv4(final @NotNull String address) {
        for (final String octet : address.split("\\.")) {
            if (Integer.parseInt(octet) > 255) {
                return false;
            }
        }
        return true;
    }

    /**
     * Validates the discovery metadata fetched from the issuer before it is used or cached. The discovery
     * document is fetched with a size cap and parsed, but parsing does not enforce the OpenID Connect
     * Discovery requirements that the returned issuer match the configured one and that the endpoints use
     * {@code https}. Without these checks a misconfigured — or, over a compromised path, substituted —
     * discovery document could send the browser or Edge to a plain-HTTP or arbitrary endpoint, exposing the
     * state/nonce/PKCE challenge, the authorization code, or (via an HTTP JWKS URL) letting an attacker
     * supply signing keys and forge an Edge-granting ID token.
     * <p>
     * This performs the checks {@code OIDCProviderMetadata.resolve} would have done, so the caller keeps the
     * size-capped fetch and still gets spec validation:
     * <ul>
     *   <li>the discovery {@code issuer} must equal the configured issuer exactly;</li>
     *   <li>the authorization, token, and JWKS endpoints must each be an absolute {@code https} URI with a
     *       host and no user information.</li>
     * </ul>
     * The single exception mirrors {@link #parseIssuerUri}: a loopback {@code http} endpoint is permitted
     * only when {@link #ALLOW_INSECURE_LOCAL_IDP_PROPERTY} is set — the same development/testing aid, off by
     * default. An HTTPS issuer is never allowed to downgrade an individual endpoint to non-loopback HTTP.
     * <p>
     * On any violation an {@link IllegalArgumentException} is thrown, which the service turns into a normal
     * discovery-failure response rather than a {@code 500}.
     *
     * @param expectedIssuer the configured {@code issuer-uri}
     * @param metadata       the discovery document fetched from that issuer
     */
    public static void validateDiscoveryMetadata(
            final @NotNull URI expectedIssuer, final @NotNull OIDCProviderMetadata metadata) {
        final boolean allowInsecureLoopback = isInsecureLocalIdpAllowed();
        final Issuer discoveredIssuer = metadata.getIssuer();
        Preconditions.checkArgument(
                discoveredIssuer != null && expectedIssuer.toString().equals(discoveredIssuer.getValue()),
                "OIDC discovery issuer '%s' does not match the configured issuer '%s'",
                discoveredIssuer,
                expectedIssuer);
        requireHttpsEndpoint("authorization_endpoint", metadata.getAuthorizationEndpointURI(), allowInsecureLoopback);
        requireHttpsEndpoint("token_endpoint", metadata.getTokenEndpointURI(), allowInsecureLoopback);
        requireHttpsEndpoint("jwks_uri", metadata.getJWKSetURI(), allowInsecureLoopback);
    }

    /**
     * Requires a discovery endpoint to be an absolute {@code https} URI with a host and no user information.
     * A loopback {@code http} endpoint is allowed only when the insecure-local flag is set (see
     * {@link #validateDiscoveryMetadata}). A query component is permitted — the OIDC specification allows
     * the authorization and token endpoints to carry one — but a fragment is not.
     */
    private static void requireHttpsEndpoint(
            final @NotNull String name, final @Nullable URI endpoint, final boolean allowInsecureLoopback) {
        Preconditions.checkArgument(endpoint != null, "OIDC discovery is missing the %s endpoint", name);
        Preconditions.checkArgument(
                endpoint.isAbsolute(), "OIDC discovery %s '%s' must be an absolute URI", name, endpoint);
        Preconditions.checkArgument(
                endpoint.getHost() != null, "OIDC discovery %s '%s' must include a host", name, endpoint);
        Preconditions.checkArgument(
                endpoint.getUserInfo() == null,
                "OIDC discovery %s '%s' must not contain user information",
                name,
                endpoint);
        Preconditions.checkArgument(
                endpoint.getFragment() == null, "OIDC discovery %s '%s' must not contain a fragment", name, endpoint);
        if ("https".equalsIgnoreCase(endpoint.getScheme())) {
            return;
        }
        // Not https: only a loopback http endpoint, and only under the insecure-local development flag.
        Preconditions.checkArgument(
                "http".equalsIgnoreCase(endpoint.getScheme())
                        && allowInsecureLoopback
                        && isLiteralLoopbackHost(endpoint.getHost()),
                "OIDC discovery %s '%s' must use https",
                name,
                endpoint);
        log.warn(
                "OIDC discovery {} '{}' is using plain HTTP. This is permitted only because {} is set, and is "
                        + "restricted to a loopback host. It is insecure and must never be used in production.",
                name,
                endpoint,
                ALLOW_INSECURE_LOCAL_IDP_PROPERTY);
    }

    /**
     * Parses the {@code redirect-uri}, the browser-facing callback the Identity Provider redirects the
     * browser back to. This is the leg that carries the authorization code (in the callback query) and, once
     * Edge has processed it, the minted Edge JWT (in the callback page) back to the browser — so over plain
     * {@code http} a network attacker on the browser↔Edge path can intercept both.
     * <p>
     * It is therefore required to be {@code https}, mirroring the treatment of the {@code issuer-uri}. The
     * single exception is a literal loopback host ({@code localhost}, {@code 127.0.0.0/8}, {@code ::1}),
     * which never leaves the machine and is needed for local development and testing. A deployment that
     * terminates TLS at a reverse proxy must configure the public {@code https} address the browser actually
     * uses — which is also the value the Identity Provider requires to match — not Edge's internal http
     * address. A fragment is rejected — the callback carries its result in the query, not the fragment.
     */
    private static @NotNull URI parseRedirectUri(final @NotNull String value) {
        final URI uri = parseAbsoluteHttpUri(value, "redirect-uri");
        Preconditions.checkArgument(
                uri.getFragment() == null, "OIDC redirect-uri '%s' must not contain a fragment", value);
        Preconditions.checkArgument(
                "https".equalsIgnoreCase(uri.getScheme()) || isLiteralLoopbackHost(uri.getHost()),
                "OIDC redirect-uri '%s' must use https; it carries the authorization code and the Edge token "
                        + "back to the browser and must not travel over plain http. A loopback host is the only "
                        + "http exception; behind a TLS-terminating proxy, configure the public https address.",
                value);
        return uri;
    }

    /**
     * Shared validation: an absolute {@code http}/{@code https} URI with a host and no user information.
     * <p>
     * {@code URI.create} accepts a relative reference such as {@code "callback"}, which later yields a
     * nonsensical {@code null://null} postMessage origin and a login that can never succeed. Rejecting
     * it here turns a silent runtime failure into a precise startup error.
     */
    private static @NotNull URI parseAbsoluteHttpUri(final @NotNull String value, final @NotNull String element) {
        final URI uri;
        try {
            uri = new URI(value.trim());
        } catch (final URISyntaxException e) {
            throw new IllegalArgumentException(
                    String.format("OIDC %s '%s' is not a valid URI: %s", element, value, e.getReason()), e);
        }
        Preconditions.checkArgument(
                uri.isAbsolute(),
                "OIDC %s '%s' must be an absolute URI, for example https://idp.example.com",
                element,
                value);
        final String scheme = uri.getScheme();
        Preconditions.checkArgument(
                "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme),
                "OIDC %s '%s' must use http or https, but uses '%s'",
                element,
                value,
                scheme);
        Preconditions.checkArgument(uri.getHost() != null, "OIDC %s '%s' must include a host", element, value);
        Preconditions.checkArgument(
                uri.getUserInfo() == null, "OIDC %s '%s' must not contain user information", element, value);
        return uri;
    }

    private static boolean isValidEdgeRole(final @NotNull String edgeRole) {
        return edgeRole.equalsIgnoreCase(ApiRoles.ADMIN)
                || edgeRole.equalsIgnoreCase(ApiRoles.SUPER)
                || edgeRole.equalsIgnoreCase(ApiRoles.USER);
    }

    /**
     * Warns when a configured value has leading or trailing whitespace. The value is kept verbatim (role
     * matching is literal), so the whitespace is almost certainly a typo that will cause a silent
     * mismatch at login — surfacing it here makes that diagnosable.
     */
    private static void warnOnSurroundingWhitespace(final @NotNull String element, final @NotNull String value) {
        if (!value.equals(value.strip())) {
            log.warn(
                    "OIDC {} '{}' has leading or trailing whitespace. It is used verbatim, so this may be a typo "
                            + "that prevents role matching.",
                    element,
                    value);
        }
    }

    private static @NotNull List<String> parseScopes(final @Nullable String extraScopes) {
        if (extraScopes == null || extraScopes.isBlank()) {
            return List.of();
        }
        return List.of(extraScopes.trim().split("\\s+"));
    }

    public @NotNull URI getIssuerUri() {
        return issuerUri;
    }

    public @NotNull String getClientId() {
        return clientId;
    }

    public @Nullable String getClientSecret() {
        return clientSecret;
    }

    public @NotNull URI getRedirectUri() {
        return redirectUri;
    }

    public @NotNull String getRoleClaimName() {
        return roleClaimName;
    }

    public @NotNull List<String> getExtraScopes() {
        return extraScopes;
    }

    /**
     * The IdP-role → Edge-role mappings. Required and non-empty; keys are matched literally. Only a mapped
     * IdP role produces an Edge role.
     */
    public @NotNull Map<String, String> getRoleMappings() {
        return roleMappings;
    }

    /**
     * The JWS algorithms accepted when verifying an ID token's signature. A token whose {@code alg} is
     * not in this set is rejected. See {@link OidcSigningAlgorithms}.
     */
    public @NotNull Set<String> getIdTokenSigningAlgorithms() {
        return idTokenSigningAlgorithms;
    }

    /**
     * The {@link SSLSocketFactory} for the IdP TLS connection, or {@code null} to use the JVM default CA
     * certificates. Built once from the configured truststore.
     */
    public @Nullable SSLSocketFactory getIdpSslSocketFactory() {
        return idpSslSocketFactory;
    }

    /**
     * The connect and read timeout, in milliseconds, applied to each Identity Provider call (discovery,
     * token exchange, JWKS). Defaults to {@link #DEFAULT_CONNECTION_TIMEOUT_MILLIS}.
     */
    public int getConnectionTimeoutMillis() {
        return connectionTimeoutMillis;
    }

    // No equals/hashCode: this runtime configuration has no value-equality caller, and two of its
    // runtime-significant fields cannot be compared by value here — the SSLSocketFactory has no meaningful
    // value equality (equivalent truststores yield different instances). Value comparison of an OIDC
    // configuration belongs on OidcAuthenticationEntity, which does implement equals/hashCode (including the
    // truststore and timeout) and is what config-change detection compares.
}
