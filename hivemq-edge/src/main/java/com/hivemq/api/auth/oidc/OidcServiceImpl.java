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
import com.hivemq.api.auth.ApiRoles;
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
        return Response.ok(
                        new OidcLoginRedirect().authorizeUrl(authRequest.toURI().toString()))
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
            final String detail = errorDescription != null ? errorDescription : error;
            log.info("OIDC login returned an error from the Identity Provider: {}", detail);
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
            final Set<String> edgeRoles = mapRoles(config, claims);
            if (edgeRoles.isEmpty()) {
                log.warn("OIDC login for subject '{}' produced no Edge roles after mapping; denying.", subject);
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
     * timeouts and a response-body size cap, all from the Nimbus library. Redirects are disabled — an
     * IdP endpoint answers directly, and following a redirect would reopen an unbounded fetch. When a
     * truststore is configured its {@link SSLSocketFactory} is used; otherwise the JVM default CAs apply.
     */
    private static @NotNull DefaultResourceRetriever resourceRetriever(final @NotNull OidcConfiguration config) {
        final int timeout = config.getConnectionTimeoutMillis();
        return new DefaultResourceRetriever(
                timeout, timeout, JWKS_SIZE_LIMIT_BYTES, false, config.getIdpSslSocketFactory());
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
     * Two modes, selected by whether {@code <role-mappings>} is configured:
     * <ul>
     *   <li><b>Verbatim</b> (no mappings): each claim value that is an Edge role ({@code admin} /
     *       {@code super} / {@code user}) is used directly; others are dropped. The operator's omission of
     *       a mapping is an explicit statement that the IdP already uses Edge role names.</li>
     *   <li><b>Strict</b> (mappings present): a claim value must be a mapping key to produce its mapped
     *       Edge role; an unmapped value is dropped, so no unrelated IdP role ever grants access.</li>
     * </ul>
     * Matching is literal — no trimming or case-folding — so a mismatch is honest and visible. When a
     * literal miss would have matched leniently (ignoring case/whitespace), a warning is logged to point
     * at the likely typo, but the role is still dropped.
     */
    private static @NotNull Set<String> mapRoles(
            final @NotNull OidcConfiguration config, final @NotNull IDTokenClaimsSet claims) {
        return resolveEdgeRoles(extractRoleClaim(claims, config.getRoleClaimName()), config.getRoleMappings());
    }

    /**
     * Resolves the Edge roles for the given IdP role claim values under the two mapping modes. Package
     * private for direct testing.
     */
    static @NotNull Set<String> resolveEdgeRoles(
            final @NotNull List<String> idpRoles, final @Nullable Map<String, String> mappings) {
        final Set<String> edgeRoles = new HashSet<>();
        for (final String idpRole : idpRoles) {
            if (mappings == null) {
                mapVerbatim(idpRole, edgeRoles);
            } else {
                mapStrict(idpRole, mappings, edgeRoles);
            }
        }
        return edgeRoles;
    }

    private static void mapVerbatim(final @NotNull String idpRole, final @NotNull Set<String> edgeRoles) {
        if (isEdgeRole(idpRole)) {
            edgeRoles.add(idpRole);
        } else if (isEdgeRole(idpRole.strip())) {
            log.warn("OIDC role '{}' looks like an Edge role but has surrounding whitespace; it was ignored.", idpRole);
        }
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

    private static boolean isEdgeRole(final @NotNull String role) {
        return role.equalsIgnoreCase(ApiRoles.ADMIN)
                || role.equalsIgnoreCase(ApiRoles.SUPER)
                || role.equalsIgnoreCase(ApiRoles.USER);
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

        cachedMetadata = metadata;
        cachedMetadataIssuer = issuer;
        cachedMetadataExpiry = System.currentTimeMillis() + DISCOVERY_TTL_MILLIS;
        return metadata;
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
