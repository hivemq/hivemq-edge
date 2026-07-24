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
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
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
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
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
 * The configuration is read from {@link ApiConfigurationService} on each use, so this service
 * reflects the currently applied config and correctly reports {@link #isEnabled()} as {@code false}
 * when OIDC is not configured.
 */
@Singleton
public class OidcServiceImpl implements OidcService {

    private static final @NotNull Logger log = LoggerFactory.getLogger(OidcServiceImpl.class);

    private static final long DISCOVERY_TTL_MILLIS = 60 * 60 * 1000L; // 1 hour

    // Bounded timeouts for all outbound IdP calls (discovery, token exchange, JWKS). Nimbus defaults these
    // to 0 (no timeout); without bounds an unavailable or malicious IdP could hold request threads forever.
    private static final int HTTP_CONNECT_TIMEOUT_MILLIS = 5_000;
    private static final int HTTP_READ_TIMEOUT_MILLIS = 5_000;
    // Cap IdP response bodies (discovery, token, JWKS) to bound memory from a hostile or broken IdP.
    private static final int JWKS_SIZE_LIMIT_BYTES = 512 * 1024;
    private static final int IDP_RESPONSE_SIZE_LIMIT_BYTES = 512 * 1024;

    // Total wall-clock deadline for a single IdP call. The connect/read timeouts only bound periods of no
    // progress; a peer that trickles bytes just under the read timeout could otherwise hold a request
    // thread far longer. Set above the connect + read budget with slack.
    private static final long IDP_REQUEST_DEADLINE_MILLIS = 15_000L;

    // Small bounded pool that runs the outbound IdP call off the request thread, so the deadline can
    // interrupt a stuck call. Daemon threads, so it never holds up JVM shutdown.
    private static final @NotNull ExecutorService IDP_CALL_EXECUTOR = Executors.newFixedThreadPool(4, runnable -> {
        final Thread thread = new Thread(runnable, "oidc-idp-call");
        thread.setDaemon(true);
        return thread;
    });

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
    public boolean isEnabled() {
        return apiConfigurationService.getOidcConfiguration() != null;
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

        // Release the login state on every callback, including error and cancellation, so a denied or
        // abandoned flow does not hold its slot until the TTL expires.
        final Optional<OidcStateStore.StateEntry> entryOpt =
                state != null ? stateStore.consume(state) : Optional.empty();

        if (error != null) {
            final String detail = errorDescription != null ? errorDescription : error;
            log.info("OIDC login returned an error from the Identity Provider: {}", detail);
            return callbackError(OidcErrorCode.IDP_ERROR, config);
        }
        if (code == null || state == null) {
            return callbackError(OidcErrorCode.INVALID_REQUEST, config);
        }
        if (entryOpt.isEmpty()) {
            return callbackError(OidcErrorCode.INVALID_STATE, config);
        }
        final OidcStateStore.StateEntry entry = entryOpt.get();

        try {
            final OIDCProviderMetadata metadata = resolveMetadata(config);

            // 1. Exchange the code for tokens.
            final AuthorizationCodeGrant grant = new AuthorizationCodeGrant(
                    new AuthorizationCode(code), config.getRedirectUri(), new CodeVerifier(entry.codeVerifier()));
            final TokenRequest tokenRequest = buildTokenRequest(metadata.getTokenEndpointURI(), config, grant);
            final HTTPRequest tokenHttpRequest = tokenRequest.toHTTPRequest();
            tokenHttpRequest.setConnectTimeout(HTTP_CONNECT_TIMEOUT_MILLIS);
            tokenHttpRequest.setReadTimeout(HTTP_READ_TIMEOUT_MILLIS);
            // Read the token response through a size cap, so a hostile IdP cannot grow the heap unbounded.
            final TokenResponse tokenResponse =
                    OIDCTokenResponseParser.parse(withDeadline(() -> tokenHttpRequest.send(boundedSender())));
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
                    new JWSVerificationKeySelector<>(acceptedAlgorithms(config), jwkSource(metadata)),
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
     * A remote JWK source for the provider's JWKS, fetched through the bounded retriever (connect/read
     * timeouts and a size cap), so key retrieval is bounded like the other IdP calls.
     */
    private static @NotNull JWKSource<SecurityContext> jwkSource(final @NotNull OIDCProviderMetadata metadata)
            throws Exception {
        return JWKSourceBuilder.<SecurityContext>create(
                        metadata.getJWKSetURI().toURL(),
                        new DefaultResourceRetriever(
                                HTTP_CONNECT_TIMEOUT_MILLIS, HTTP_READ_TIMEOUT_MILLIS, JWKS_SIZE_LIMIT_BYTES))
                .build();
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
     * Maps the IdP role claim (string or string-array) onto Edge roles via the configured mappings.
     * <p>
     * Fails closed: only IdP roles with an explicit mapping produce an Edge role. An IdP role without a
     * mapping is dropped, so an unrelated or generic IdP role (for example a realm role named {@code admin})
     * never becomes an Edge role without an operator decision. Matching is case-insensitive.
     */
    private static @NotNull Set<String> mapRoles(
            final @NotNull OidcConfiguration config, final @NotNull IDTokenClaimsSet claims) {
        final List<String> idpRoles = extractRoleClaim(claims, config.getRoleClaimName());
        final Map<String, String> mappings = config.getRoleMappings();
        final Set<String> edgeRoles = new HashSet<>();
        for (final String idpRole : idpRoles) {
            final String mapped = mappings.get(idpRole.toLowerCase(Locale.ROOT));
            if (mapped != null) {
                edgeRoles.add(mapped);
            }
        }
        return edgeRoles;
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
        return new ArrayList<>();
    }

    private @NotNull OIDCProviderMetadata resolveMetadata(final @NotNull OidcConfiguration config) throws Exception {
        final String issuer = config.getIssuerUri().toString();
        final OIDCProviderMetadata cached = cachedMetadata;
        if (cached != null
                && issuer.equals(cachedMetadataIssuer)
                && System.currentTimeMillis() < cachedMetadataExpiry) {
            return cached;
        }
        // Fetch discovery through the bounded sender (timeouts + size cap) rather than the default
        // OIDCProviderMetadata.resolve, whose HTTPRequest.send reads the body into an unbounded buffer.
        final HTTPRequest discoveryRequest =
                new HTTPRequest(HTTPRequest.Method.GET, OIDCProviderMetadata.resolveURL(new Issuer(issuer)));
        discoveryRequest.setConnectTimeout(HTTP_CONNECT_TIMEOUT_MILLIS);
        discoveryRequest.setReadTimeout(HTTP_READ_TIMEOUT_MILLIS);
        final HTTPResponse discoveryResponse = withDeadline(() -> discoveryRequest.send(boundedSender()));
        discoveryResponse.ensureStatusCode(HTTPResponse.SC_OK);
        final OIDCProviderMetadata metadata = OIDCProviderMetadata.parse(discoveryResponse.getBodyAsJSONObject());

        cachedMetadata = metadata;
        cachedMetadataIssuer = issuer;
        cachedMetadataExpiry = System.currentTimeMillis() + DISCOVERY_TTL_MILLIS;
        return metadata;
    }

    private static @NotNull BoundedHttpRequestSender boundedSender() {
        return new BoundedHttpRequestSender(IDP_RESPONSE_SIZE_LIMIT_BYTES);
    }

    private static <T> @NotNull T withDeadline(final @NotNull Callable<T> call) throws Exception {
        return withDeadline(call, IDP_REQUEST_DEADLINE_MILLIS);
    }

    /**
     * Runs an outbound IdP call under a total wall-clock deadline. On timeout the call is interrupted
     * and an {@link IOException} is thrown, so a peer that stays under the read timeout by trickling
     * bytes cannot hold the request thread beyond the deadline.
     */
    static <T> @NotNull T withDeadline(final @NotNull Callable<T> call, final long deadlineMillis) throws Exception {
        final Future<T> future = IDP_CALL_EXECUTOR.submit(call);
        try {
            return future.get(deadlineMillis, TimeUnit.MILLISECONDS);
        } catch (final TimeoutException e) {
            future.cancel(true);
            throw new IOException("The Identity Provider did not respond within " + deadlineMillis + " ms; aborting.");
        } catch (final ExecutionException e) {
            final Throwable cause = e.getCause();
            if (cause instanceof Exception exception) {
                throw exception;
            }
            throw e;
        }
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
