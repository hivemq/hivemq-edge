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
package com.hivemq.api.auth.oidc;

import com.hivemq.api.auth.ApiPrincipal;
import com.hivemq.api.auth.AuthenticationException;
import com.hivemq.api.auth.provider.ITokenGenerator;
import com.hivemq.api.config.OidcConfiguration;
import com.hivemq.api.errors.authentication.OidcUnavailableError;
import com.hivemq.configuration.service.ApiConfigurationService;
import com.hivemq.edge.api.model.OidcLoginRedirect;
import com.hivemq.util.ErrorResponseUtil;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jose.util.DefaultResourceRetriever;
import com.nimbusds.jwt.JWT;
import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.oauth2.sdk.AuthorizationCodeGrant;
import com.nimbusds.oauth2.sdk.ResponseType;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.TokenResponse;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.oauth2.sdk.id.State;
import com.nimbusds.oauth2.sdk.pkce.CodeChallengeMethod;
import com.nimbusds.oauth2.sdk.pkce.CodeVerifier;
import com.nimbusds.openid.connect.sdk.AuthenticationRequest;
import com.nimbusds.openid.connect.sdk.Nonce;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponse;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponseParser;
import com.nimbusds.openid.connect.sdk.claims.IDTokenClaimsSet;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;
import com.nimbusds.openid.connect.sdk.validators.IDTokenValidator;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.net.ssl.SSLSocketFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Nimbus-backed implementation of the OIDC authorization-code flow (with PKCE).
 * <p>
 * IdP endpoints are resolved lazily from the issuer's discovery document and cached with a TTL.
 * The callback validates the ID token, maps the IdP role claim onto Edge roles, and reuses the
 * existing {@link ITokenGenerator} to mint the HiveMQ Edge JWT — no new token-issuance code.
 * <p>
 * The configuration is read from {@link ApiConfigurationService} on each use, so this service always
 * reflects the currently applied config.
 */
@Singleton
public class OidcServiceImpl implements OidcService {

    private static final @NotNull Logger log = LoggerFactory.getLogger(OidcServiceImpl.class);

    private static final long DISCOVERY_TTL_MILLIS = 60 * 60 * 1000L; // 1 hour

    // Cap the JWKS response body via the resource retriever, so a broken IdP cannot grow the heap unbounded.
    // The connect/read timeout for every IdP call comes from the configuration (OidcConfiguration): it
    // bounds a down or hung IdP, and is operator-tunable for a slow or distant network path.
    private static final int JWKS_SIZE_LIMIT_BYTES = 512 * 1024;

    private final @NotNull ApiConfigurationService apiConfigurationService;
    private final @NotNull ITokenGenerator tokenGenerator;
    private final @NotNull OidcStateStore stateStore;

    // cached discovery metadata (keyed implicitly by the current issuer)
    private volatile @Nullable OIDCProviderMetadata cachedMetadata;
    private volatile @Nullable String cachedMetadataIssuer;
    private volatile long cachedMetadataExpiry;

    @Inject
    public OidcServiceImpl(
            final @NotNull ApiConfigurationService apiConfigurationService,
            final @NotNull ITokenGenerator tokenGenerator,
            final @NotNull OidcStateStore stateStore) {
        this.apiConfigurationService = apiConfigurationService;
        this.tokenGenerator = tokenGenerator;
        this.stateStore = stateStore;
    }

    @Override
    public @NotNull Response beginLogin() {
        final OidcConfiguration config = apiConfigurationService.getOidcConfiguration();
        if (config == null) {
            return oidcNotConfigured();
        }

        final OIDCProviderMetadata metadata;
        try {
            metadata = resolveMetadata(config);
        } catch (final Exception e) {
            log.warn("OIDC discovery failed for issuer {}: {}", config.getIssuerUri(), e.getMessage());
            return ErrorResponseUtil.errorResponse(
                    new OidcUnavailableError("The Identity Provider could not be reached."));
        }

        final State state = new State();
        final Nonce nonce = new Nonce();
        final CodeVerifier codeVerifier = new CodeVerifier();

        stateStore.put(state.getValue(), nonce.getValue(), codeVerifier.getValue());

        final AuthenticationRequest authRequest = new AuthenticationRequest.Builder(
                        new ResponseType(ResponseType.Value.CODE),
                        scopeFor(config),
                        new ClientID(config.getClientId()),
                        config.getRedirectUri())
                .endpointURI(metadata.getAuthorizationEndpointURI())
                .state(state)
                .nonce(nonce)
                .codeChallenge(codeVerifier, CodeChallengeMethod.S256)
                .build();

        // Return the authorization URL for the SPA to open, rather than redirecting. A start-time failure
        // (config missing, IdP unreachable) is then a normal error response the SPA presents, instead of a
        // popup stranded on this endpoint's response.
        //
        // no-store: the authorization URL embeds the one-time state, nonce, and PKCE challenge just
        // allocated in the state store. A cache or proxy that retained and replayed this response would
        // hand the same one-time values to a later login, whose callback would then fail as invalid-state.
        return noStore(Response.ok(
                        new OidcLoginRedirect().authorizeUrl(authRequest.toURI().toString())))
                .build();
    }

    @Override
    public @NotNull Response completeLogin(
            final @Nullable String code,
            final @Nullable String state,
            final @Nullable String error,
            final @Nullable String errorDescription) {
        final OidcConfiguration config = apiConfigurationService.getOidcConfiguration();
        if (config == null) {
            return oidcNotConfigured();
        }

        // Validate the state before classifying the callback, so an unsolicited or forged callback — with
        // no state or an attacker-chosen one — cannot be accepted as a genuine IdP error. State is
        // consumed on every valid callback (success or error), releasing its slot rather than holding it
        // until the TTL.
        if (state == null) {
            return callbackError(OidcErrorCode.INVALID_REQUEST, config);
        }
        final Optional<OidcStateStore.StateEntry> entryOpt = stateStore.consume(state);
        if (entryOpt.isEmpty()) {
            return callbackError(OidcErrorCode.INVALID_STATE, config);
        }
        final OidcStateStore.StateEntry entry = entryOpt.get();

        if (error != null) {
            // Log the standardized error code, not the free-text error_description: both are callback query
            // parameters an unauthenticated caller controls, and the description can carry CR/LF or control
            // characters that would forge extra log lines. Strip control characters from whatever we log.
            log.info("OIDC login returned an error from the Identity Provider: {}", sanitizeForLog(error));
            return callbackError(OidcErrorCode.IDP_ERROR, config);
        }
        if (code == null) {
            return callbackError(OidcErrorCode.INVALID_REQUEST, config);
        }

        try {
            final OIDCProviderMetadata metadata = resolveMetadata(config);

            // 1. Exchange the code for tokens.
            final AuthorizationCodeGrant grant = new AuthorizationCodeGrant(
                    new AuthorizationCode(code), config.getRedirectUri(), new CodeVerifier(entry.codeVerifier()));
            final TokenRequest tokenRequest = buildTokenRequest(metadata.getTokenEndpointURI(), config, grant);
            final HTTPRequest tokenHttpRequest = tokenRequest.toHTTPRequest();
            tokenHttpRequest.setConnectTimeout(config.getConnectionTimeoutMillis());
            tokenHttpRequest.setReadTimeout(config.getConnectionTimeoutMillis());
            // Do not follow redirects: the token endpoint is the validated one from discovery and answers
            // directly, so a redirect would only send the code and PKCE verifier to another host.
            tokenHttpRequest.setFollowRedirects(false);
            // Use the configured truststore for the token TLS connection, if any; otherwise the JVM default.
            final SSLSocketFactory idpSslSocketFactory = config.getIdpSslSocketFactory();
            if (idpSslSocketFactory != null) {
                tokenHttpRequest.setSSLSocketFactory(idpSslSocketFactory);
            }
            final TokenResponse tokenResponse = OIDCTokenResponseParser.parse(tokenHttpRequest.send());
            if (!tokenResponse.indicatesSuccess()) {
                log.info(
                        "OIDC token exchange failed: {}",
                        tokenResponse.toErrorResponse().getErrorObject());
                return callbackError(OidcErrorCode.EXCHANGE_FAILED, config);
            }
            final JWT idToken = ((OIDCTokenResponse) tokenResponse.toSuccessResponse())
                    .getOIDCTokens()
                    .getIDToken();

            // 2. Validate the ID token (signature via JWKS, iss, aud, exp, nonce). The token is accepted
            //    only if its algorithm is in the configured set; the token header never selects the
            //    algorithm, which is the defence against downgrade.
            final IDTokenValidator validator = new IDTokenValidator(
                    new Issuer(config.getIssuerUri()),
                    new ClientID(config.getClientId()),
                    new JWSVerificationKeySelector<>(acceptedAlgorithms(config), jwkSource(config, metadata)),
                    null);
            final IDTokenClaimsSet claims = validator.validate(idToken, new Nonce(entry.nonce()));

            // 3. Map roles and mint the Edge JWT.
            final String subject = claims.getSubject().getValue();
            // mapRoles logs the specific reason (claim absent / empty / no mapping matched) for the operator.
            final Set<String> edgeRoles = mapRoles(config, claims);
            if (edgeRoles.isEmpty()) {
                log.warn(
                        "OIDC login for subject '{}' produced no Edge roles; denying (see the reason above).", subject);
                return callbackError(OidcErrorCode.NO_ROLES, config);
            }
            final String edgeJwt = tokenGenerator.generateToken(new ApiPrincipal(subject, edgeRoles));

            return noStore(Response.ok(OidcCallbackPage.success(edgeJwt, config.getRedirectUri()), MediaType.TEXT_HTML))
                    .build();
        } catch (final AuthenticationException e) {
            log.warn("OIDC login failed while issuing the Edge token", e);
            return callbackError(OidcErrorCode.EXCHANGE_FAILED, config);
        } catch (final Exception e) {
            log.warn("OIDC login failed during code exchange / token validation: {}", e.getMessage());
            return callbackError(OidcErrorCode.EXCHANGE_FAILED, config);
        }
    }

    private static @NotNull Scope scopeFor(final @NotNull OidcConfiguration config) {
        final Scope scope = new Scope();
        scope.add("openid");
        for (final String extra : config.getExtraScopes()) {
            scope.add(extra);
        }
        return scope;
    }

    /**
     * The set of JWS algorithms the ID-token validator will accept, taken from the configuration. A
     * token whose {@code alg} is outside this set is rejected. The algorithm is never read from the
     * token header (attacker-controlled) — this set is the sole authority, which is the defence against
     * algorithm downgrade.
     */
    private static @NotNull Set<JWSAlgorithm> acceptedAlgorithms(final @NotNull OidcConfiguration config) {
        final Set<JWSAlgorithm> algorithms = new HashSet<>();
        for (final String name : config.getIdTokenSigningAlgorithms()) {
            algorithms.add(JWSAlgorithm.parse(name));
        }
        return algorithms;
    }

    /**
     * A remote JWK source for the provider's JWKS, fetched through the shared resource retriever
     * (connect/read timeouts and a size cap), so key retrieval is bounded consistently.
     */
    private static @NotNull JWKSource<SecurityContext> jwkSource(
            final @NotNull OidcConfiguration config, final @NotNull OIDCProviderMetadata metadata) throws Exception {
        return JWKSourceBuilder.<SecurityContext>create(metadata.getJWKSetURI().toURL(), resourceRetriever(config))
                .build();
    }

    /**
     * The resource retriever used for the GET-style IdP fetches (discovery and JWKS): connect/read
     * timeouts and a response-body size cap, all from the Nimbus library. When a truststore is configured
     * its {@link SSLSocketFactory} is used; otherwise the JVM default CAs apply.
     * <p>
     * Redirects are disabled: the discovery and JWKS URLs are derived from the validated issuer and answer
     * directly, so following a redirect would only widen the set of hosts Edge contacts (an SSRF surface)
     * and let a chain multiply the per-call timeout. {@link DefaultResourceRetriever} has no redirect
     * setting, so {@link #openConnection} is overridden to turn instance-following off on the connection.
     */
    static @NotNull DefaultResourceRetriever resourceRetriever(final @NotNull OidcConfiguration config) {
        final int timeout = config.getConnectionTimeoutMillis();
        return new DefaultResourceRetriever(
                timeout, timeout, JWKS_SIZE_LIMIT_BYTES, false, config.getIdpSslSocketFactory()) {
            @Override
            protected @NotNull HttpURLConnection openConnection(final @NotNull URL url) throws IOException {
                final HttpURLConnection connection = super.openConnection(url);
                connection.setInstanceFollowRedirects(false);
                return connection;
            }
        };
    }

    /**
     * Builds the token-endpoint request. PKCE (carried in the {@code grant}) always proves the code
     * redemption. When a client secret is configured, the request additionally authenticates as a
     * confidential client via HTTP Basic; without one it is a public-client request that carries the
     * {@code client_id} in the body and no {@code Authorization} header. The secret is optional in the
     * configuration and guaranteed non-empty when present (enforced by the config schema).
     */
    static @NotNull TokenRequest buildTokenRequest(
            final @NotNull URI tokenEndpoint,
            final @NotNull OidcConfiguration config,
            final @NotNull AuthorizationCodeGrant grant) {
        final ClientID clientId = new ClientID(config.getClientId());
        final String clientSecret = config.getClientSecret();
        if (clientSecret != null) {
            return new TokenRequest(tokenEndpoint, new ClientSecretBasic(clientId, new Secret(clientSecret)), grant);
        }
        return new TokenRequest(tokenEndpoint, clientId, grant);
    }

    /**
     * Maps the IdP role claim (string or string-array) onto Edge roles.
     * <p>
     * Mapping is always strict: {@code <role-mappings>} is required, and a claim value produces an Edge role
     * only when it is an explicitly configured mapping key. An unmapped value is dropped, so no unrelated IdP
     * role ever grants access (there is no verbatim mode in which IdP role names are used directly).
     * <p>
     * Matching is literal — no trimming or case-folding — so a mismatch is honest and visible. When a
     * literal miss would have matched leniently (ignoring case/whitespace), a warning is logged to point
     * at the likely typo, but the role is still dropped.
     */
    private static @NotNull Set<String> mapRoles(
            final @NotNull OidcConfiguration config, final @NotNull IDTokenClaimsSet claims) {
        final String claimName = config.getRoleClaimName();
        final Map<String, String> mappings = config.getRoleMappings();
        final List<String> idpRoles = extractRoleClaim(claims, claimName);
        final Set<String> edgeRoles = resolveEdgeRoles(idpRoles, mappings);
        logRoleMappingOutcome(claims, claimName, idpRoles, mappings, edgeRoles);
        return edgeRoles;
    }

    /**
     * Emits an operator-facing diagnostic for the role-mapping outcome, so a login that fails (or grants
     * fewer roles than expected) is not opaque. The four cases are distinguished by what the token actually
     * carries — the login decision itself is unchanged, only the logging:
     * <ul>
     *   <li><b>claim present, some roles mapped</b> — the login succeeds. A warning is emitted <em>only when
     *       some IdP roles were dropped</em> (unmapped), which may be a missing {@code <role-mapping>};</li>
     *   <li><b>claim present, non-empty, none mapped</b> — denied; the IdP roles are listed so the operator
     *       can see they are all unmapped;</li>
     *   <li><b>claim present but empty</b> — denied; the user genuinely carries no roles;</li>
     *   <li><b>claim absent</b> — denied; the configured {@code role-claim-name} is not in the token, which is
     *       likely (but not certainly) a misconfiguration, since some IdPs omit an empty claim entirely.</li>
     * </ul>
     * IdP <em>role</em> values are logged (they are the names the operator maps, not personal data); other
     * claim <em>names</em> are logged in the absent case to help spot the right claim, never their values.
     */
    private static void logRoleMappingOutcome(
            final @NotNull IDTokenClaimsSet claims,
            final @NotNull String claimName,
            final @NotNull List<String> idpRoles,
            final @NotNull Map<String, String> mappings,
            final @NotNull Set<String> edgeRoles) {
        if (!idpRoles.isEmpty()) {
            // An IdP role is "unmatched" when it is not a mapping key (so it produced no Edge role).
            final List<String> unmatched = idpRoles.stream()
                    .filter(role -> !mappings.containsKey(role))
                    .toList();
            if (!edgeRoles.isEmpty()) {
                // Login succeeds. Unmatched IdP roles are the norm, not a problem: a real IdP token carries
                // standard roles the operator never maps (Keycloak sends offline_access, uma_authorization and
                // default-roles-<realm> alongside the mapped group), so every successful login has some. Log at
                // DEBUG so it is available when troubleshooting without desensitising operators to the WARN-level
                // denial diagnostics below, which are the ones that matter.
                if (!unmatched.isEmpty() && log.isDebugEnabled()) {
                    log.debug(
                            "OIDC login authenticated with {} Edge role(s); {} IdP role(s) under claim '{}' did "
                                    + "not match any <role-mapping> and were ignored: {}",
                            edgeRoles.size(),
                            unmatched.size(),
                            claimName,
                            unmatched);
                }
            } else {
                log.warn(
                        "OIDC login denied: none of the {} IdP role(s) under claim '{}' matched a "
                                + "<role-mapping>: {}",
                        idpRoles.size(),
                        claimName,
                        idpRoles);
            }
        } else if (claims.toJSONObject().containsKey(claimName)) {
            final Object rawClaim = claims.toJSONObject().get(claimName);
            if (isSupportedRoleClaimShape(rawClaim)) {
                // Present and of a supported shape (string or array) but yielding no role names: genuinely empty.
                log.warn(
                        "OIDC login denied: the role claim '{}' is present in the ID token but empty; the user has "
                                + "no roles.",
                        claimName);
            } else {
                // Present but neither a string nor an array of strings -- e.g. a nested object such as Keycloak's
                // realm_access. Unlike an absent claim, this is unambiguous: a role-less user never produces this
                // shape, so it is a role-claim-name misconfiguration pointing at a container rather than the list
                // of role names inside it. The value's type is named; its contents are not logged.
                log.warn(
                        "OIDC login denied: the role claim '{}' is present in the ID token but is a {}, not a string "
                                + "or an array of role names. Set role-claim-name to the claim that holds the role "
                                + "names directly (for example, a nested object like Keycloak's realm_access is a "
                                + "container -- point role-claim-name at the list inside it).",
                        claimName,
                        rawClaim == null ? "null value" : rawClaim.getClass().getSimpleName());
            }
        } else {
            log.warn(
                    "OIDC login denied: the configured role-claim-name '{}' is not present in the ID token. The "
                            + "token carries these claims: {}. This may be a role-claim-name misconfiguration -- set "
                            + "it to the claim your Identity Provider emits roles under -- or a user with no roles "
                            + "on an Identity Provider that omits an empty claim.",
                    claimName,
                    claims.toJSONObject().keySet());
        }
    }

    /**
     * Whether a raw ID-token claim value is a shape from which role names can be read: a string, or an array
     * whose entries are strings. A nested object (e.g. Keycloak's {@code realm_access}) or any other shape is
     * unsupported — the operator has pointed {@code role-claim-name} at a container rather than the role list.
     */
    static boolean isSupportedRoleClaimShape(final @Nullable Object rawClaim) {
        if (rawClaim instanceof String) {
            return true;
        }
        if (rawClaim instanceof final List<?> list) {
            return list.stream().allMatch(entry -> entry instanceof String);
        }
        return false;
    }

    /**
     * Resolves the Edge roles for the given IdP role claim values. An IdP role produces an Edge role only
     * when the operator has mapped it explicitly; an unmapped value grants nothing. Package private for
     * direct testing.
     */
    static @NotNull Set<String> resolveEdgeRoles(
            final @NotNull List<String> idpRoles, final @NotNull Map<String, String> mappings) {
        final Set<String> edgeRoles = new HashSet<>();
        for (final String idpRole : idpRoles) {
            mapStrict(idpRole, mappings, edgeRoles);
        }
        return edgeRoles;
    }

    private static void mapStrict(
            final @NotNull String idpRole,
            final @NotNull Map<String, String> mappings,
            final @NotNull Set<String> edgeRoles) {
        final String mapped = mappings.get(idpRole);
        if (mapped != null) {
            edgeRoles.add(mapped);
            return;
        }
        // Literal miss: if a mapping key matches leniently, the operator likely has a case/whitespace typo.
        final String lenient = idpRole.strip().toLowerCase(Locale.ROOT);
        for (final String key : mappings.keySet()) {
            if (key.strip().toLowerCase(Locale.ROOT).equals(lenient)) {
                log.warn(
                        "OIDC role '{}' did not match mapping key '{}' exactly (they differ only by case or "
                                + "whitespace); the role was ignored. This may be a configuration typo.",
                        idpRole,
                        key);
                return;
            }
        }
    }

    private static @NotNull List<String> extractRoleClaim(
            final @NotNull IDTokenClaimsSet claims, final @NotNull String claimName) {
        // Handle both string-array and single-string claim shapes defensively.
        final List<String> asList = claims.getStringListClaim(claimName);
        if (asList != null && !asList.isEmpty()) {
            return asList;
        }
        final String asString = claims.getStringClaim(claimName);
        if (asString != null && !asString.isBlank()) {
            return List.of(asString);
        }
        return List.of();
    }

    private @NotNull OIDCProviderMetadata resolveMetadata(final @NotNull OidcConfiguration config) throws Exception {
        final String issuer = config.getIssuerUri().toString();
        final OIDCProviderMetadata cached = cachedMetadata;
        if (cached != null
                && issuer.equals(cachedMetadataIssuer)
                && System.currentTimeMillis() < cachedMetadataExpiry) {
            return cached;
        }
        // Fetch discovery through the same resource retriever used for JWKS (connect/read timeouts and a
        // size cap), rather than the default OIDCProviderMetadata.resolve whose send reads the body into an
        // unbounded buffer.
        final URL discoveryUrl = OIDCProviderMetadata.resolveURL(new Issuer(issuer));
        final String discoveryJson =
                resourceRetriever(config).retrieveResource(discoveryUrl).getContent();
        final OIDCProviderMetadata metadata = OIDCProviderMetadata.parse(discoveryJson);

        // The size-capped fetch and parse above do not enforce the OpenID Connect Discovery requirements
        // that the returned issuer match the configured one and that the endpoints use https. Validate them
        // before the metadata is cached or used, so a misconfigured or substituted discovery document cannot
        // send the browser or Edge to an insecure or arbitrary endpoint. A violation throws, which the
        // callers turn into a discovery-failure response rather than a 500 with a stale state entry.
        OidcConfiguration.validateDiscoveryMetadata(config.getIssuerUri(), metadata);

        cachedMetadata = metadata;
        cachedMetadataIssuer = issuer;
        cachedMetadataExpiry = System.currentTimeMillis() + DISCOVERY_TTL_MILLIS;
        return metadata;
    }

    /**
     * Strips CR, LF, and other control characters (including terminal escape sequences) from a value before
     * it is written to a log, so an externally supplied string cannot forge extra log lines or inject
     * terminal control sequences. Replaces each control character with {@code '_'}.
     */
    private static @NotNull String sanitizeForLog(final @NotNull String value) {
        final StringBuilder sanitized = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            final char c = value.charAt(i);
            sanitized.append(Character.isISOControl(c) ? '_' : c);
        }
        return sanitized.toString();
    }

    private static @NotNull Response oidcNotConfigured() {
        return ErrorResponseUtil.errorResponse(
                new OidcUnavailableError("OIDC authentication is not configured on this instance."));
    }

    /**
     * Callback failure: a 401 whose body is the result page, so the popup posts a stable error code to
     * the opener and closes. The status still marks the failure for any non-browser caller.
     */
    private static @NotNull Response callbackError(
            final @NotNull OidcErrorCode errorCode, final @NotNull OidcConfiguration config) {
        return noStore(Response.status(Response.Status.UNAUTHORIZED)
                        .entity(OidcCallbackPage.failure(errorCode, config.getRedirectUri()))
                        .type(MediaType.TEXT_HTML))
                .build();
    }

    /**
     * Applies no-store headers. The callback response carries a bearer token (or the outcome of a login)
     * and must not be retained by a browser, proxy, or back/forward cache.
     */
    private static Response.@NotNull ResponseBuilder noStore(final Response.@NotNull ResponseBuilder builder) {
        return builder.header("Cache-Control", "no-store, no-cache, max-age=0")
                .header("Pragma", "no-cache")
                .header("Referrer-Policy", "no-referrer");
    }
}
