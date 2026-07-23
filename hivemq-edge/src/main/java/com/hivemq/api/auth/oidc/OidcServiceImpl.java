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
import com.hivemq.configuration.service.ApiConfigurationService;
import com.nimbusds.jose.JWSAlgorithm;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
    // Cap the JWKS response size to bound memory from a hostile IdP.
    private static final int JWKS_SIZE_LIMIT_BYTES = 512 * 1024;

    // Discriminator for the message the callback page posts to the SPA, so the opener can tell our
    // result apart from any other same-origin message.
    private static final @NotNull String OIDC_RESULT_MESSAGE_TYPE = "oidc-result";

    // ID token signing algorithms we accept, in preference order. Asymmetric only: the provider signs
    // with its private key and we verify with the public key from its JWKS. Symmetric (HS*) and 'none'
    // are deliberately absent — see selectSigningAlgorithm.
    private static final @NotNull List<JWSAlgorithm> PREFERRED_JWS_ALGORITHMS = List.of(
            JWSAlgorithm.RS256,
            JWSAlgorithm.RS384,
            JWSAlgorithm.RS512,
            JWSAlgorithm.PS256,
            JWSAlgorithm.PS384,
            JWSAlgorithm.PS512,
            JWSAlgorithm.ES256,
            JWSAlgorithm.ES384,
            JWSAlgorithm.ES512);

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
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity("Identity Provider is unreachable")
                    .build();
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

        return Response.status(Response.Status.FOUND)
                .location(authRequest.toURI())
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

            // 2. Validate the ID token (signature via JWKS, iss, aud, exp, nonce).
            final IDTokenValidator validator = new IDTokenValidator(
                    new Issuer(config.getIssuerUri()),
                    new ClientID(config.getClientId()),
                    selectSigningAlgorithm(metadata),
                    metadata.getJWKSetURI().toURL(),
                    new DefaultResourceRetriever(
                            HTTP_CONNECT_TIMEOUT_MILLIS, HTTP_READ_TIMEOUT_MILLIS, JWKS_SIZE_LIMIT_BYTES));
            final IDTokenClaimsSet claims = validator.validate(idToken, new Nonce(entry.nonce()));

            // 3. Map roles and mint the Edge JWT.
            final String subject = claims.getSubject().getValue();
            final Set<String> edgeRoles = mapRoles(config, claims);
            if (edgeRoles.isEmpty()) {
                log.warn("OIDC login for subject '{}' produced no Edge roles after mapping; denying.", subject);
                return callbackError(OidcErrorCode.NO_ROLES, config);
            }
            final String edgeJwt = tokenGenerator.generateToken(new ApiPrincipal(subject, edgeRoles));

            return noStore(Response.ok(tokenDeliveryHtml(edgeJwt, config.getRedirectUri()), MediaType.TEXT_HTML))
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
     * Chooses the JWS algorithm to verify the ID token with, from the algorithms the provider advertises
     * in its discovery document.
     * <p>
     * The algorithm is never taken from the token's own header: that value is attacker-controlled, and
     * trusting it is the classic algorithm-confusion attack. Only asymmetric algorithms are accepted —
     * {@code none} would skip verification entirely, and an HMAC algorithm would let anyone holding the
     * client secret forge a token. {@link #PREFERRED_JWS_ALGORITHMS} is consulted in order, so a provider
     * advertising several algorithms yields a deterministic choice. RS256 is the default when the
     * provider advertises nothing, since it is required of every OpenID Provider.
     *
     * @throws IllegalStateException if the provider advertises only algorithms we do not accept
     */
    static @NotNull JWSAlgorithm selectSigningAlgorithm(final @NotNull OIDCProviderMetadata metadata) {
        final List<JWSAlgorithm> advertised = metadata.getIDTokenJWSAlgs();
        if (advertised == null || advertised.isEmpty()) {
            return JWSAlgorithm.RS256;
        }
        for (final JWSAlgorithm candidate : PREFERRED_JWS_ALGORITHMS) {
            if (advertised.contains(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("The Identity Provider advertises no supported ID token signing algorithm. "
                + "Advertised: " + advertised + "; supported: " + PREFERRED_JWS_ALGORITHMS);
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
        final OIDCProviderMetadata metadata =
                OIDCProviderMetadata.resolve(new Issuer(issuer), HTTP_CONNECT_TIMEOUT_MILLIS, HTTP_READ_TIMEOUT_MILLIS);
        cachedMetadata = metadata;
        cachedMetadataIssuer = issuer;
        cachedMetadataExpiry = System.currentTimeMillis() + DISCOVERY_TTL_MILLIS;
        return metadata;
    }

    private static @NotNull Response oidcNotConfigured() {
        return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                .entity("OIDC authentication is not configured")
                .build();
    }

    /**
     * Callback failure: a 401 whose body is the result page, so the popup posts a stable error code to
     * the opener and closes. The status still marks the failure for any non-browser caller.
     */
    private static @NotNull Response callbackError(
            final @NotNull OidcErrorCode errorCode, final @NotNull OidcConfiguration config) {
        return noStore(Response.status(Response.Status.UNAUTHORIZED)
                        .entity(errorDeliveryHtml(errorCode, config.getRedirectUri()))
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

    /**
     * Minimal HTML page delivered on success: posts the token to the opener window (the popup pattern)
     * and closes the popup, keeping the JWT out of URLs. The origin is derived from the redirect URI.
     */
    private static @NotNull String tokenDeliveryHtml(final @NotNull String jwt, final @NotNull URI redirectUri) {
        // jwt is a compact JWS (base64url segments + dots) — safe to embed in a JSON string literal.
        return resultDeliveryHtml("token: \"" + jwt + "\"", "Signed in. You may close this window.", redirectUri);
    }

    /**
     * Minimal HTML page delivered on failure: posts a stable error code to the opener window and closes
     * the popup, so the opener always settles instead of waiting for a message that never arrives. Only
     * the code is posted — raw Identity Provider error descriptions are logged, not surfaced to the user.
     */
    private static @NotNull String errorDeliveryHtml(
            final @NotNull OidcErrorCode errorCode, final @NotNull URI redirectUri) {
        return resultDeliveryHtml(
                "errorCode: \"" + errorCode.getCode() + "\"", "Login failed. You may close this window.", redirectUri);
    }

    /**
     * Builds the callback result page. Both outcomes post a discriminated {@code oidc-result} message to
     * the opener and close the popup, so the opener can settle on either path.
     */
    private static @NotNull String resultDeliveryHtml(
            final @NotNull String payloadField, final @NotNull String fallbackText, final @NotNull URI redirectUri) {
        final String origin = originOf(redirectUri);
        return "<!DOCTYPE html><html><head><title>HiveMQ Edge</title></head><body><script>\n"
                + "(function () {\n"
                + "  var result = { type: \"" + OIDC_RESULT_MESSAGE_TYPE + "\", " + payloadField + " };\n"
                + "  if (window.opener) {\n"
                + "    window.opener.postMessage(result, \"" + origin + "\");\n"
                + "    window.close();\n"
                + "  } else {\n"
                + "    document.body.textContent = \"" + fallbackText + "\";\n"
                + "  }\n"
                + "})();\n"
                + "</script></body></html>";
    }

    private static @NotNull String originOf(final @NotNull URI uri) {
        final String scheme = uri.getScheme();
        final String host = uri.getHost();
        final int port = uri.getPort();
        final StringBuilder sb = new StringBuilder();
        sb.append(scheme).append("://").append(host);
        if (port != -1 && !(("https".equals(scheme) && port == 443) || ("http".equals(scheme) && port == 80))) {
            sb.append(':').append(port);
        }
        return sb.toString();
    }
}
