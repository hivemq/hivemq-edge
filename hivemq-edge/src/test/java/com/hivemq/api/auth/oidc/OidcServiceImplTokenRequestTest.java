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

import com.hivemq.api.config.OidcConfiguration;
import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.oauth2.sdk.AuthorizationCodeGrant;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.pkce.CodeVerifier;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link OidcServiceImpl#buildTokenRequest}: the confidential vs. public client
 * distinction at the token endpoint. PKCE (the code verifier) is present either way; the only
 * difference is whether the request authenticates with a client secret (HTTP Basic) or is a
 * public-client request that carries {@code client_id} in the body and no {@code Authorization}
 * header.
 */
class OidcServiceImplTokenRequestTest {

    private static final @org.jetbrains.annotations.NotNull URI TOKEN_ENDPOINT =
            URI.create("https://idp.example.com/token");

    @Test
    void publicClient_noSecret_sendsClientIdInBodyAndNoAuthorizationHeader() throws Exception {
        final HTTPRequest http = buildFor(null).toHTTPRequest();

        final Map<String, List<String>> body = http.getBodyAsFormParameters();
        // No client authentication: a public client sends client_id in the body, not an Authorization header.
        assertThat(http.getAuthorization())
                .as("no Authorization header for a public client")
                .isNull();
        assertThat(body).containsKey("client_id");
        assertThat(body.get("client_id")).containsExactly("edge-client");
        // PKCE must still be present — it is what proves the redemption when there is no secret.
        assertThat(body).containsKey("code_verifier");
    }

    @Test
    void confidentialClient_withSecret_sendsBasicAuthAndNoClientIdInBody() throws Exception {
        final HTTPRequest http = buildFor("the-secret").toHTTPRequest();

        // HTTP Basic client authentication: base64(client_id:secret) in the Authorization header.
        assertThat(http.getAuthorization())
                .as("Basic auth header for a confidential client")
                .startsWith("Basic ");
        // With Basic auth the client_id lives in the header, not the body.
        assertThat(http.getBodyAsFormParameters()).doesNotContainKey("client_id");
        // PKCE is present for the confidential client too.
        assertThat(http.getBodyAsFormParameters()).containsKey("code_verifier");
    }

    private static @org.jetbrains.annotations.NotNull TokenRequest buildFor(final String clientSecret) {
        final OidcConfiguration config = new OidcConfiguration(
                URI.create("https://idp.example.com"),
                "edge-client",
                clientSecret,
                URI.create("https://edge.example.com/callback"),
                "roles",
                List.of(),
                Map.of());
        final AuthorizationCodeGrant grant = new AuthorizationCodeGrant(
                new AuthorizationCode("the-code"), config.getRedirectUri(), new CodeVerifier());
        return OidcServiceImpl.buildTokenRequest(TOKEN_ENDPOINT, config, grant);
    }
}
