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

import jakarta.ws.rs.core.Response;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Encapsulates the OpenID Connect authorization-code flow: IdP discovery, the login start
 * (with state / nonce / PKCE), and the callback (code exchange, ID-token validation, role
 * mapping, and issuance of a HiveMQ Edge JWT).
 * <p>
 * Implementations must tolerate OIDC not being configured — the flow methods return an appropriate
 * error {@link Response} when it is not. All protocol-specific (Nimbus) types stay inside the
 * implementation; the resource layer deals only in JAX-RS {@link Response}s.
 */
public interface OidcService {

    /**
     * Begins the login flow: mints state / nonce / PKCE, stores them, and builds the IdP authorization
     * URL for the SPA to open.
     *
     * @return a 200 carrying the authorization URL, or a 503 error response if OIDC is not configured or
     *         the Identity Provider is unreachable.
     */
    @NotNull
    Response beginLogin();

    /**
     * Completes the login flow from the IdP callback.
     *
     * @param code             the authorization code (success path)
     * @param state            the state token to match against the stored login
     * @param error            an OAuth2 error code (error path), or {@code null}
     * @param errorDescription a human-readable error description, or {@code null}
     * @return a 200 delivering the HiveMQ Edge JWT to the SPA, or a 401 on any failure.
     */
    @NotNull
    Response completeLogin(
            @Nullable String code, @Nullable String state, @Nullable String error, @Nullable String errorDescription);
}
