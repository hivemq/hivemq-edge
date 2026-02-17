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
package com.hivemq.logging;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The SecurityLog class is used to log certain events that could be important for customers to separate files.
 */
@Singleton
public class SecurityLog {

    public static final String SECURITY_AUTHENTICATION_FAILED = "security.authentication-failed";
    public static final String SECURITY_AUTHENTICATION_SUCCEEDED = "security.authentication-succeeded";
    /**
     * Events are logged to DEBUG, in case customers are using a custom logback.xml
     */
    private static final Logger logAuthenticationFailed = LoggerFactory.getLogger(SECURITY_AUTHENTICATION_FAILED);

    private static final Logger logAuthenticationSucceeded = LoggerFactory.getLogger(SECURITY_AUTHENTICATION_SUCCEEDED);

    @Inject
    public SecurityLog() {}

    /**
     * Log that an authentication was successful
     *
     * @param mechanism the authentication mechanism used
     * @param userName the username of the authenticated user
     */
    public void authenticationSucceeded(final @NotNull String mechanism, final @NotNull String userName) {
        logAuthenticationSucceeded.debug("Successfully authenticated user {} using {}", userName, mechanism);
    }

    /**
     * Log that an authentication was successful
     *
     * @param mechanism the authentication mechanism used
     * @param userName the username of the authenticated user
     * @param reason why the authentication failed
     */
    public void authenticationFailed(
            final @NotNull String mechanism, final @NotNull String userName, final @NotNull String reason) {
        logAuthenticationFailed.debug("Failed to authenticate user {} using {}: {}", userName, mechanism, reason);
    }
}
