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
package com.hivemq.api.auth.provider.impl.ldap;

/**
 * TLS/SSL modes for LDAP connections.
 */
public enum TlsMode {
    /**
     * No encryption - plain LDAP connection.
     * <p>
     * Uses port 389 by default. Not recommended for production use as credentials
     * and data are transmitted in clear text.
     */
    NONE(389),

    /**
     * LDAPS - LDAP over TLS/SSL.
     * <p>
     * Establishes TLS connection from the start. Uses port 636 by default.
     * Most secure option as the entire connection is encrypted.
     */
    LDAPS(636),

    /**
     * StartTLS - Upgrade plain connection to TLS.
     * <p>
     * Starts as plain LDAP on port 389, then upgrades to TLS using the StartTLS
     * extended operation. Common in production environments.
     */
    START_TLS(389);

    public final int defaultPort;

    TlsMode(final int defaultPort) {
        this.defaultPort = defaultPort;
    }
}
