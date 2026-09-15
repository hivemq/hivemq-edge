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
package com.hivemq.api.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hivemq.api.auth.jwt.JwtAuthenticationProvider;
import com.hivemq.api.auth.oidc.OidcService;
import com.hivemq.api.auth.provider.IUsernameRolesProvider;
import com.hivemq.api.config.AuthMode;
import com.hivemq.api.resources.impl.AuthenticationResourceImpl;
import com.hivemq.configuration.service.ApiConfigurationService;
import com.hivemq.edge.api.model.UsernamePasswordCredentials;
import jakarta.ws.rs.core.Response;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * When {@code USERNAME_PASSWORD} is not an active authentication mode (an OIDC-only deployment), the
 * local {@code POST /api/v1/auth/authenticate} endpoint must reject the request before consulting any
 * credential provider — so the default {@code admin}/{@code hivemq} account and any other local user is
 * genuinely closed, not merely unmatched.
 */
class OidcOnlyLocalAuthRejectionTest {

    @Test
    void authenticate_whenUsernamePasswordNotActive_rejectsWithoutConsultingTheProvider() {
        final IUsernameRolesProvider usernamePasswordProvider = mock(IUsernameRolesProvider.class);
        final ApiConfigurationService apiConfigurationService = mock(ApiConfigurationService.class);
        // OIDC-only: USERNAME_PASSWORD is absent from the active modes.
        when(apiConfigurationService.getAuthModes()).thenReturn(Set.of(AuthMode.OPEN_ID));

        final AuthenticationResourceImpl resource = new AuthenticationResourceImpl(
                usernamePasswordProvider,
                mock(JwtAuthenticationProvider.class),
                mock(JwtAuthenticationProvider.class),
                mock(OidcService.class),
                apiConfigurationService);

        final Response response = resource.authenticate(
                new UsernamePasswordCredentials().userName("admin").password("hivemq"));

        assertThat(response.getStatus())
                .as("local login is closed in OIDC-only mode")
                .isEqualTo(401);
        // The endpoint must reject before any credential lookup — closed, not merely unmatched.
        verify(usernamePasswordProvider, never()).findByUsernameAndPassword(anyString(), any());
    }

    @Test
    void authenticate_whenUsernamePasswordActive_consultsTheProvider() {
        final IUsernameRolesProvider usernamePasswordProvider = mock(IUsernameRolesProvider.class);
        when(usernamePasswordProvider.findByUsernameAndPassword(anyString(), any()))
                .thenReturn(Optional.empty());
        final ApiConfigurationService apiConfigurationService = mock(ApiConfigurationService.class);
        when(apiConfigurationService.getAuthModes()).thenReturn(Set.of(AuthMode.USERNAME_PASSWORD));

        final AuthenticationResourceImpl resource = new AuthenticationResourceImpl(
                usernamePasswordProvider,
                mock(JwtAuthenticationProvider.class),
                mock(JwtAuthenticationProvider.class),
                mock(OidcService.class),
                apiConfigurationService);

        resource.authenticate(
                new UsernamePasswordCredentials().userName("admin").password("hivemq"));

        // Contrast with the OIDC-only case: here the provider is consulted (and rejects the bad password).
        verify(usernamePasswordProvider).findByUsernameAndPassword(anyString(), any());
    }
}
