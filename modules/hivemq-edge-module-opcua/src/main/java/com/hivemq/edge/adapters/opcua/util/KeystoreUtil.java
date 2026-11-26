/*
 * Copyright 2023-present HiveMQ GmbH
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
package com.hivemq.edge.adapters.opcua.util;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/// The shape Eclipse Milo's client builder takes Edge's own key and certificate chain in.
///
/// Nothing here reads a file any more. Every piece of TLS material - this key, the trust anchors,
/// the fingerprint allow-list and the revocation lists - is read once by `TLSPolicy`, which handles
/// containers and PEM alike; this record only carries the result across to Milo in the shape its API
/// expects.
public class KeystoreUtil {

    private KeystoreUtil() {}

    /// @param privateKey the key Edge signs the OPC UA handshake with
    /// @param publicKey the certificate belonging to that key
    /// @param certificateChain that certificate and the intermediates vouching for it, leaf first
    /// @param applicationUri the URI in the certificate's subject alternative name, which OPC UA
    ///     compares against the application identifier, or `null` where it carries none
    @SuppressWarnings("ArrayRecordComponent")
    public record KeyPairWithChain(
            @NotNull PrivateKey privateKey,
            @NotNull X509Certificate publicKey,
            @NotNull X509Certificate[] certificateChain,
            @Nullable String applicationUri) {}
}
