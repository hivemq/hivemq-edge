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

import java.net.URI;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link OidcCallbackPage}: the rendered callback page, its postMessage target origin,
 * and the escaping of values interpolated into the page's script.
 */
class OidcCallbackPageTest {

    @Test
    void success_postsTheTokenWithTheResultType() {
        final String html = OidcCallbackPage.success("the.jwt.value", URI.create("https://edge.example.com/cb"));

        assertThat(html).contains("type: \"" + OidcCallbackPage.MESSAGE_TYPE + "\"");
        assertThat(html).contains("token: \"the.jwt.value\"");
        assertThat(html).doesNotContain("errorCode");
    }

    @Test
    void failure_postsTheErrorCodeAndNoToken() {
        final String html = OidcCallbackPage.failure(OidcErrorCode.NO_ROLES, URI.create("https://edge.example.com/cb"));

        assertThat(html).contains("errorCode: \"" + OidcErrorCode.NO_ROLES.getCode() + "\"");
        assertThat(html).doesNotContain("token:");
    }

    @Test
    void origin_omitsTheDefaultPortForTheScheme() {
        // https://host:443 and https://host are the same origin; postMessage compares canonical strings.
        assertThat(OidcCallbackPage.success("j", URI.create("https://edge.example.com:443/cb")))
                .contains("\"https://edge.example.com\"")
                .doesNotContain(":443");
        assertThat(OidcCallbackPage.success("j", URI.create("http://edge.example.com:80/cb")))
                .contains("\"http://edge.example.com\"");
    }

    @Test
    void origin_keepsACustomPort() {
        // An operator running Edge on a non-default port must still get a matching target origin.
        assertThat(OidcCallbackPage.success("j", URI.create("http://localhost:8080/cb")))
                .contains("\"http://localhost:8080\"");
    }

    @Test
    void origin_dropsThePathAndQuery() {
        assertThat(OidcCallbackPage.success("j", URI.create("https://edge.example.com/api/v1/auth/oidc/callback")))
                .contains("\"https://edge.example.com\"")
                .doesNotContain("/api/v1/auth/oidc/callback\"");
    }

    @Test
    void values_areEscapedForTheScriptContext() {
        // A quote in an interpolated value must not be able to break out of its JavaScript string
        // literal. The template escapes with ?js_string, so the raw sequence never appears.
        final String html = OidcCallbackPage.success("a\"b", URI.create("https://edge.example.com/cb"));

        assertThat(html).doesNotContain("token: \"a\"b\"");
        assertThat(html).contains("a\\\"b");
    }
}
