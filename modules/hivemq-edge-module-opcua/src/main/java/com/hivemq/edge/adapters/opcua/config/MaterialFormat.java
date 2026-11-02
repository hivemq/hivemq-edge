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
package com.hivemq.edge.adapters.opcua.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/// The container or encoding a piece of TLS material is written in.
///
/// Declared rather than guessed from a file extension: a `.crt` file holds PEM or DER, a `.pem` file
/// holds a key or a certificate or both, and a file Kubernetes mounts is named after the key in the
/// secret. Guessing produces a parse failure naming the wrong format, which sends an operator
/// hunting for a corrupt file.
public enum MaterialFormat {
    /// PKCS#12, the format `openssl pkcs12` writes and the one most tools default to.
    P12,
    /// The JVM's own key-store format, which `keytool` writes.
    JKS,
    /// Base64 text between `BEGIN` and `END` lines, which names its parts in separate elements
    /// because PEM has no container to hold them together.
    PEM;

    /// Accepts the spelling an operator writes, in any case, through the same parser every other
    /// security enum on this adapter uses: blank or absent means unset, and an unrecognised value
    /// stops the adapter rather than resolving to a default that might check less.
    @JsonCreator
    public static @Nullable MaterialFormat fromString(final @Nullable String value) {
        return EnumParsing.parse(MaterialFormat.class, values(), value);
    }

    /// The spelling the material-reading stage takes.
    public @NotNull String asDeclared() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}
