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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.hivemq.api.auth.provider.ITokenGenerator;
import com.hivemq.api.config.OidcConfiguration;
import com.hivemq.configuration.service.ApiConfigurationService;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the callback classification in {@link OidcServiceImpl#completeLogin} that happens
 * before any Identity Provider call: the state must be validated before an error or code is accepted,
 * so an unsolicited or forged callback cannot be classified as a genuine IdP error.
 */
class OidcServiceImplCallbackStateTest {

    private OidcStateStore stateStore;
    private OidcServiceImpl service;

    @BeforeEach
    void setUp() {
        stateStore = new OidcStateStore();
        final OidcConfiguration config = new OidcConfiguration(
                URI.create("https://idp.example.com"),
                "client",
                "secret",
                URI.create("https://edge.example.com/api/v1/auth/oidc/callback"),
                "roles",
                List.of(),
                Map.of("a", "admin"),
                Set.of("RS256"));
        final ApiConfigurationService apiConfig = mock(ApiConfigurationService.class);
        when(apiConfig.getOidcConfiguration()).thenReturn(config);
        service = new OidcServiceImpl(apiConfig, mock(ITokenGenerator.class), stateStore);
    }

    @Test
    void errorCallbackWithNoState_isInvalidRequest_notAcceptedAsIdpError() {
        final Response response = service.completeLogin(null, null, "access_denied", "user said no");

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(bodyOf(response)).contains(OidcErrorCode.INVALID_REQUEST.getCode());
    }

    @Test
    void errorCallbackWithUnknownState_isInvalidState_notAcceptedAsIdpError() {
        final Response response = service.completeLogin(null, "never-issued", "access_denied", null);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(bodyOf(response)).contains(OidcErrorCode.INVALID_STATE.getCode());
    }

    @Test
    void errorCallbackWithValidState_isReportedAsAnIdpError() {
        stateStore.put("s1", "nonce", "verifier");

        final Response response = service.completeLogin(null, "s1", "access_denied", "user cancelled");

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(bodyOf(response)).contains(OidcErrorCode.IDP_ERROR.getCode());
    }

    @Test
    void validStateIsConsumed_soAReplayedErrorCallbackIsRejected() {
        stateStore.put("s1", "nonce", "verifier");

        assertThat(bodyOf(service.completeLogin(null, "s1", "access_denied", null)))
                .contains(OidcErrorCode.IDP_ERROR.getCode());
        // The state was consumed, so replaying the same callback is now an unknown state.
        assertThat(bodyOf(service.completeLogin(null, "s1", "access_denied", null)))
                .contains(OidcErrorCode.INVALID_STATE.getCode());
    }

    @Test
    void successCallbackWithNoCode_afterValidState_isInvalidRequest() {
        stateStore.put("s1", "nonce", "verifier");

        final Response response = service.completeLogin(null, "s1", null, null);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(bodyOf(response)).contains(OidcErrorCode.INVALID_REQUEST.getCode());
    }

    private static @NotNull String bodyOf(final @NotNull Response response) {
        final Object entity = response.getEntity();
        return entity != null ? entity.toString() : "";
    }
}
