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

import org.jetbrains.annotations.NotNull;

/**
 * Stable error codes posted from the OIDC callback page to the SPA.
 * <p>
 * The SPA maps these onto localized messages. Raw Identity Provider error descriptions are logged
 * server-side but never surfaced to the browser, so a provider cannot inject text into the UI.
 */
public enum OidcErrorCode {

    /** The Identity Provider returned an error, or the user cancelled the login. */
    IDP_ERROR("idp-error"),

    /** The callback did not carry an authorization code and state. */
    INVALID_REQUEST("invalid-request"),

    /** OIDC is not configured on this Edge instance. */
    NOT_CONFIGURED("not-configured"),

    /** The login state is unknown or expired — typically a stale or replayed callback. */
    INVALID_STATE("invalid-state"),

    /** The code could not be exchanged, or the ID token failed validation. */
    EXCHANGE_FAILED("exchange-failed"),

    /** The user authenticated, but none of their Identity Provider roles map to an Edge role. */
    NO_ROLES("no-roles");

    private final @NotNull String code;

    OidcErrorCode(final @NotNull String code) {
        this.code = code;
    }

    public @NotNull String getCode() {
        return code;
    }
}
