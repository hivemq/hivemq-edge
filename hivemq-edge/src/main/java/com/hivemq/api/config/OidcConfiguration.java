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
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
 * {@code roleMappings} maps an IdP role name onto an Edge role string, or is {@code null} in verbatim
 * mode (no mappings configured). Keys are matched literally — see {@code OidcServiceImpl#resolveEdgeRoles}.
 */
public class OidcConfiguration {

    private static final @NotNull Logger log = LoggerFactory.getLogger(OidcConfiguration.class);

    private final @NotNull URI issuerUri;
    private final @NotNull String clientId;
    private final @Nullable String clientSecret;
    private final @NotNull URI redirectUri;
    private final @NotNull String roleClaimName;
    private final @NotNull List<String> extraScopes;
    // null => verbatim mode (no <role-mappings>): claim values are used as Edge roles directly.
    // non-null => strict mode: only mapped IdP roles produce an Edge role. Guaranteed non-empty.
    private final @Nullable Map<String, String> roleMappings;
    private final @NotNull Set<String> idTokenSigningAlgorithms;

    public OidcConfiguration(
            final @NotNull URI issuerUri,
            final @NotNull String clientId,
            final @Nullable String clientSecret,
            final @NotNull URI redirectUri,
            final @NotNull String roleClaimName,
            final @NotNull List<String> extraScopes,
            final @Nullable Map<String, String> roleMappings,
            final @NotNull Set<String> idTokenSigningAlgorithms) {
        this.issuerUri = Preconditions.checkNotNull(issuerUri);
        this.clientId = Preconditions.checkNotNull(clientId);
        this.clientSecret = clientSecret;
        this.redirectUri = Preconditions.checkNotNull(redirectUri);
        this.roleClaimName = Preconditions.checkNotNull(roleClaimName);
        this.idTokenSigningAlgorithms = Set.copyOf(idTokenSigningAlgorithms);
        this.extraScopes = List.copyOf(extraScopes);
        this.roleMappings = roleMappings == null ? null : Map.copyOf(roleMappings);
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

        // Role mappings define two distinct modes. An absent <role-mappings> element is verbatim mode:
        // the claim values are used as Edge roles directly. A present element (the XSD guarantees it is
        // non-empty) is strict mode: only mapped IdP roles produce an Edge role. Keys are stored and
        // matched literally — no trimming or case-folding — so a stray space or wrong case is an honest
        // mismatch the operator can see in the logs, not a silent, surprising match.
        final List<OidcRoleMappingEntity> mappingEntities = entity.getRoleMappings();
        final Map<String, String> mappings;
        if (mappingEntities == null || mappingEntities.isEmpty()) {
            mappings = null;
        } else {
            mappings = new LinkedHashMap<>();
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
        }

        // Accepted ID-token signing algorithms. An absent/empty list defaults to the full asymmetric set;
        // a configured list is validated against it (the XSD requires at least one entry when present).
        final List<String> configuredAlgorithms = entity.getIdTokenSigningAlgorithms();
        final Set<String> signingAlgorithms = configuredAlgorithms == null || configuredAlgorithms.isEmpty()
                ? OidcSigningAlgorithms.DEFAULT
                : OidcSigningAlgorithms.validate(configuredAlgorithms);

        return new OidcConfiguration(
                issuerUri,
                clientId.trim(),
                entity.getClientSecret(),
                redirectUri,
                roleClaimName,
                scopes,
                mappings,
                signingAlgorithms);
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
                allowInsecureLoopbackIssuer && isLoopbackHost(uri.getHost()),
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

    private static boolean isLoopbackHost(final @Nullable String host) {
        if (host == null) {
            return false;
        }
        if ("localhost".equalsIgnoreCase(host)) {
            return true;
        }
        try {
            return InetAddress.getByName(host).isLoopbackAddress();
        } catch (final UnknownHostException e) {
            return false;
        }
    }

    /**
     * Parses the {@code redirect-uri}, the browser-facing callback the Identity Provider redirects to.
     * <p>
     * Plain {@code http} is permitted, because the redirect is reached by the browser and a deployment
     * may legitimately terminate TLS at a reverse proxy; a non-HTTPS redirect is logged as a warning. A
     * fragment is rejected — the callback carries its result in the query, not the fragment.
     */
    private static @NotNull URI parseRedirectUri(final @NotNull String value) {
        final URI uri = parseAbsoluteHttpUri(value, "redirect-uri");
        Preconditions.checkArgument(
                uri.getFragment() == null, "OIDC redirect-uri '%s' must not contain a fragment", value);
        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            log.warn(
                    "OIDC redirect-uri '{}' does not use HTTPS. This is only appropriate for local testing or "
                            + "when TLS is terminated by a reverse proxy.",
                    value);
        }
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
     * The IdP-role → Edge-role mappings, or {@code null} in verbatim mode (no {@code <role-mappings>}
     * configured). When non-null it is non-empty, and keys are matched literally.
     */
    public @Nullable Map<String, String> getRoleMappings() {
        return roleMappings;
    }

    /**
     * The JWS algorithms accepted when verifying an ID token's signature. A token whose {@code alg} is
     * not in this set is rejected. See {@link OidcSigningAlgorithms}.
     */
    public @NotNull Set<String> getIdTokenSigningAlgorithms() {
        return idTokenSigningAlgorithms;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof OidcConfiguration that)) {
            return false;
        }
        return issuerUri.equals(that.issuerUri)
                && clientId.equals(that.clientId)
                && Objects.equals(clientSecret, that.clientSecret)
                && redirectUri.equals(that.redirectUri)
                && roleClaimName.equals(that.roleClaimName)
                && extraScopes.equals(that.extraScopes)
                && Objects.equals(roleMappings, that.roleMappings)
                && idTokenSigningAlgorithms.equals(that.idTokenSigningAlgorithms);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                issuerUri,
                clientId,
                clientSecret,
                redirectUri,
                roleClaimName,
                extraScopes,
                roleMappings,
                idTokenSigningAlgorithms);
    }
}
