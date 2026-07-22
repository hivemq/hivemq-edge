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

import static org.assertj.core.api.Assertions.assertThat;

import com.hivemq.api.auth.ApiPrincipal;
import com.hivemq.api.auth.ApiRoles;
import com.hivemq.api.auth.jwt.JwtAuthenticationProvider;
import com.hivemq.api.auth.oidc.testcontainer.KeycloakContainer;
import com.hivemq.api.config.ApiJwtConfiguration;
import com.hivemq.api.config.OidcConfiguration;
import com.hivemq.configuration.service.ApiConfigurationService;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integration test that drives {@link OidcServiceImpl} against a real Keycloak IdP (via Testcontainers):
 * real discovery, real code exchange, real JWKS-verified ID token, real role-claim → Edge-role mapping.
 * <p>
 * The test plays the role the SPA would: it follows the login redirect, authenticates against Keycloak's
 * form (headless), captures the authorization {@code code}, and feeds it back to
 * {@link OidcServiceImpl#completeLogin}. The Edge JWT returned in the callback's postMessage HTML is then
 * verified with the same {@link JwtAuthenticationProvider} the running gateway uses — no frontend involved.
 */
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OidcServiceKeycloakIntegrationTest {

    private static final @NotNull String REDIRECT_URI = "http://localhost:28080/api/v1/auth/oidc/callback";

    // The Edge JWT is embedded in the callback HTML as: var token = "<jwt>";
    private static final @NotNull Pattern TOKEN_IN_HTML = Pattern.compile("var token = \"([^\"]+)\"");
    // Keycloak login form: <form ... action="<url>" ...>
    private static final @NotNull Pattern FORM_ACTION = Pattern.compile("action=\"([^\"]+)\"");
    // The code arrives on the redirect Location as ...?code=...&state=...
    private static final @NotNull Pattern CODE_PARAM = Pattern.compile("[?&]code=([^&]+)");

    private final @NotNull KeycloakContainer keycloak = new KeycloakContainer();

    private JwtAuthenticationProvider jwtProvider;
    private ApiConfigurationService apiConfigurationService;
    private OidcServiceImpl oidcService;

    @BeforeAll
    void startAll() throws Exception {
        keycloak.start();

        // The real Edge token machinery, so the issued JWT is verified exactly as the gateway would.
        jwtProvider = new JwtAuthenticationProvider(new ApiJwtConfiguration.Builder().build());

        final OidcConfiguration config = new OidcConfiguration(
                URI.create(keycloak.getIssuerUri()),
                KeycloakContainer.CLIENT_ID,
                KeycloakContainer.CLIENT_SECRET,
                URI.create(REDIRECT_URI),
                "roles",
                List.of(),
                Map.of("acme-admin", ApiRoles.ADMIN, "acme-user", ApiRoles.USER));

        apiConfigurationService = Mockito.mock(ApiConfigurationService.class);
        Mockito.when(apiConfigurationService.getOidcConfiguration()).thenReturn(config);

        oidcService = new OidcServiceImpl(apiConfigurationService, jwtProvider, new OidcStateStore());
    }

    @AfterAll
    void stopAll() {
        keycloak.stop();
    }

    @Test
    void adminUser_getsEdgeAdminRole() throws Exception {
        final ApiPrincipal principal = loginAndVerify("alice", "alice-password");
        // The subject is the OIDC `sub` claim — Keycloak's opaque user UUID, not the username.
        assertThat(principal.getName()).isNotBlank();
        assertThat(principal.getRoles()).containsExactly(ApiRoles.ADMIN);
    }

    @Test
    void regularUser_getsEdgeUserRole() throws Exception {
        final ApiPrincipal principal = loginAndVerify("bob", "bob-password");
        assertThat(principal.getName()).isNotBlank();
        assertThat(principal.getRoles()).containsExactly(ApiRoles.USER);
    }

    @Test
    void unmappedUser_isDenied() throws Exception {
        // carol has no realm roles → no Edge roles after mapping → 401 (no token issued).
        final String html = drive("carol", "carol-password");
        final Matcher tokenMatcher = TOKEN_IN_HTML.matcher(html);
        assertThat(tokenMatcher.find())
                .as("no Edge JWT should be issued for a user with no mapped roles")
                .isFalse();
    }

    /**
     * Runs the whole login for the given user and returns the Edge principal decoded from the issued JWT.
     */
    private @NotNull ApiPrincipal loginAndVerify(final @NotNull String user, final @NotNull String password)
            throws Exception {
        final String html = drive(user, password);
        final Matcher tokenMatcher = TOKEN_IN_HTML.matcher(html);
        assertThat(tokenMatcher.find())
                .as("callback HTML should carry the Edge JWT")
                .isTrue();
        final String edgeJwt = tokenMatcher.group(1);

        final Optional<ApiPrincipal> principal = jwtProvider.verify(edgeJwt);
        assertThat(principal).as("the issued Edge JWT should verify").isPresent();
        return principal.get();
    }

    /**
     * Plays the browser: begins login, authenticates at Keycloak headlessly, and drives the captured
     * authorization code back through {@link OidcServiceImpl#completeLogin}. Returns the callback HTML.
     */
    private @NotNull String drive(final @NotNull String user, final @NotNull String password) throws Exception {
        // Manage cookies by hand (name -> value) and set the Cookie header explicitly. This sidesteps the
        // JDK CookieManager, which mangles Keycloak's Domain=localhost cookies into domain "localhost.local"
        // and then fails to attach them — Keycloak answers the login POST with "Cookie not found".
        final HttpClient http = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        final Map<String, String> cookies = new java.util.HashMap<>();

        // 1. beginLogin() → 302 to Keycloak's authorization endpoint.
        final Response beginResponse = oidcService.beginLogin();
        assertThat(beginResponse.getStatus()).isEqualTo(302);
        final URI authUri = beginResponse.getLocation();

        // 2. GET the Keycloak login page, following redirects by hand and collecting cookies.
        final HttpResponse<String> loginPage = getFollowingRedirects(http, authUri, cookies);
        final Matcher actionMatcher = FORM_ACTION.matcher(loginPage.body());
        assertThat(actionMatcher.find()).as("Keycloak login form action").isTrue();
        final String formAction = actionMatcher.group(1).replace("&amp;", "&");

        // 3. POST credentials to the login-actions endpoint. On success Keycloak 302s to the redirect
        //    URI carrying the code; that target isn't listening, so we do not follow it.
        final String form = "username=" + enc(user) + "&password=" + enc(password) + "&credentialId=";
        final HttpResponse<String> loginResult = http.send(
                HttpRequest.newBuilder(URI.create(formAction))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .header("Cookie", cookieHeader(cookies))
                        .POST(HttpRequest.BodyPublishers.ofString(form))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        final String location = loginResult.headers().firstValue("Location").orElse("");
        final Matcher codeMatcher = CODE_PARAM.matcher(location);
        if (!codeMatcher.find()) {
            throw new AssertionError(
                    "No authorization code. POST status=" + loginResult.statusCode() + " location='" + location + "'");
        }
        final String code = codeMatcher.group(1);
        final String state = stateFrom(location);

        // 4. completeLogin() → the callback response (postMessage HTML on success, or a 401 with no token).
        final Response callback = oidcService.completeLogin(code, state, null, null);
        final Object entity = callback.getEntity();
        return entity != null ? entity.toString() : "";
    }

    /**
     * GETs {@code uri}, following 3xx redirects manually (max 10), collecting {@code Set-Cookie} values into
     * {@code cookies} and re-sending them on each hop — sidestepping the JDK CookieManager entirely.
     */
    private static @NotNull HttpResponse<String> getFollowingRedirects(
            final @NotNull HttpClient client, final @NotNull URI uri, final @NotNull Map<String, String> cookies)
            throws Exception {
        URI current = uri;
        for (int hop = 0; hop < 10; hop++) {
            final HttpResponse<String> response = client.send(
                    HttpRequest.newBuilder(current)
                            .header("Cookie", cookieHeader(cookies))
                            .GET()
                            .build(),
                    HttpResponse.BodyHandlers.ofString());
            collectCookies(response, cookies);
            final int status = response.statusCode();
            if (status >= 300 && status < 400) {
                current = current.resolve(
                        response.headers().firstValue("Location").orElseThrow());
                continue;
            }
            return response;
        }
        throw new AssertionError("too many redirects fetching " + uri);
    }

    private static void collectCookies(
            final @NotNull HttpResponse<?> response, final @NotNull Map<String, String> cookies) {
        for (final String setCookie : response.headers().allValues("Set-Cookie")) {
            final String pair = setCookie.split(";", 2)[0];
            final int eq = pair.indexOf('=');
            if (eq > 0) {
                cookies.put(pair.substring(0, eq).trim(), pair.substring(eq + 1).trim());
            }
        }
    }

    private static @NotNull String cookieHeader(final @NotNull Map<String, String> cookies) {
        final StringBuilder sb = new StringBuilder();
        for (final Map.Entry<String, String> e : cookies.entrySet()) {
            if (sb.length() > 0) {
                sb.append("; ");
            }
            sb.append(e.getKey()).append('=').append(e.getValue());
        }
        return sb.toString();
    }

    private static @NotNull String stateFrom(final @NotNull String location) {
        final Matcher m = Pattern.compile("[?&]state=([^&]+)").matcher(location);
        return m.find() ? m.group(1) : "";
    }

    private static @NotNull String enc(final @NotNull String value) {
        return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
