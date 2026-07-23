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
        if (error != null) {
            final String detail = errorDescription != null ? errorDescription : error;
            log.info("OIDC login returned an error from the Identity Provider: {}", detail);
            return unauthorized("Login failed: " + detail);
        }
        if (code == null || state == null) {
            return unauthorized("Missing authorization code or state.");
        }

        final OidcConfiguration config = apiConfigurationService.getOidcConfiguration();
        if (config == null) {
            return oidcNotConfigured();
        }

        final Optional<OidcStateStore.StateEntry> entryOpt = stateStore.consume(state);
        if (entryOpt.isEmpty()) {
            return unauthorized("Unknown or expired login state.");
        }
        final OidcStateStore.StateEntry entry = entryOpt.get();

        try {
            final OIDCProviderMetadata metadata = resolveMetadata(config);

            // 1. Exchange the code for tokens (PKCE verifier + client-secret basic auth).
            final AuthorizationCodeGrant grant = new AuthorizationCodeGrant(
                    new AuthorizationCode(code), config.getRedirectUri(), new CodeVerifier(entry.codeVerifier()));
            final TokenRequest tokenRequest = new TokenRequest(
                    metadata.getTokenEndpointURI(),
                    new ClientSecretBasic(
                            new ClientID(config.getClientId()),
                            new Secret(config.getClientSecret() != null ? config.getClientSecret() : "")),
                    grant);
            final HTTPRequest tokenHttpRequest = tokenRequest.toHTTPRequest();
            tokenHttpRequest.setConnectTimeout(HTTP_CONNECT_TIMEOUT_MILLIS);
            tokenHttpRequest.setReadTimeout(HTTP_READ_TIMEOUT_MILLIS);
            final TokenResponse tokenResponse = OIDCTokenResponseParser.parse(tokenHttpRequest.send());
            if (!tokenResponse.indicatesSuccess()) {
                log.info(
                        "OIDC token exchange failed: {}",
                        tokenResponse.toErrorResponse().getErrorObject());
                return unauthorized("Token exchange failed.");
            }
            final JWT idToken = ((OIDCTokenResponse) tokenResponse.toSuccessResponse())
                    .getOIDCTokens()
                    .getIDToken();

            // 2. Validate the ID token (signature via JWKS, iss, aud, exp, nonce).
            final IDTokenValidator validator = new IDTokenValidator(
                    new Issuer(config.getIssuerUri()),
                    new ClientID(config.getClientId()),
                    JWSAlgorithm.RS256,
                    metadata.getJWKSetURI().toURL(),
                    new DefaultResourceRetriever(
                            HTTP_CONNECT_TIMEOUT_MILLIS, HTTP_READ_TIMEOUT_MILLIS, JWKS_SIZE_LIMIT_BYTES));
            final IDTokenClaimsSet claims = validator.validate(idToken, new Nonce(entry.nonce()));

            // 3. Map roles and mint the Edge JWT.
            final String subject = claims.getSubject().getValue();
            final Set<String> edgeRoles = mapRoles(config, claims);
            if (edgeRoles.isEmpty()) {
                log.warn("OIDC login for subject '{}' produced no Edge roles after mapping; denying.", subject);
                return unauthorized("No authorized roles for this user.");
            }
            final String edgeJwt = tokenGenerator.generateToken(new ApiPrincipal(subject, edgeRoles));

            return Response.ok(tokenDeliveryHtml(edgeJwt, config.getRedirectUri()), MediaType.TEXT_HTML)
                    .build();
        } catch (final AuthenticationException e) {
            log.warn("OIDC login failed while issuing the Edge token", e);
            return unauthorized("Could not issue a session token.");
        } catch (final Exception e) {
            log.warn("OIDC login failed during code exchange / token validation: {}", e.getMessage());
            return unauthorized("Login could not be completed.");
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

    private static @NotNull Response unauthorized(final @NotNull String message) {
        return Response.status(Response.Status.UNAUTHORIZED)
                .type(MediaType.TEXT_PLAIN)
                .entity(message)
                .build();
    }

    /**
     * Minimal HTML page delivered on success: posts the token to the opener window (the popup pattern)
     * and closes the popup, keeping the JWT out of URLs. The origin is derived from the redirect URI.
     */
    private static @NotNull String tokenDeliveryHtml(final @NotNull String jwt, final @NotNull URI redirectUri) {
        final String origin = originOf(redirectUri);
        // jwt is a compact JWS (base64url segments + dots) — safe to embed in a JSON string literal.
        return "<!DOCTYPE html><html><head><title>Signing in…</title></head><body><script>\n"
                + "(function () {\n"
                + "  var token = \"" + jwt + "\";\n"
                + "  if (window.opener) {\n"
                + "    window.opener.postMessage({ token: token }, \"" + origin + "\");\n"
                + "    window.close();\n"
                + "  } else {\n"
                + "    document.body.textContent = \"Signed in. You may close this window.\";\n"
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
