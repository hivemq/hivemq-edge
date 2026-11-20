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
package com.hivemq.configuration.entity.bridge;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/// The five independent certificate-validation checks a bridge has, for configurations the presets
/// do not cover.
///
/// Five rather than six: a bridge has no `sanUri` check, because MQTT sends no application
/// identifier when it connects, so that check would have no input.
///
/// Every element is optional and defaults to its strictest value when omitted, so an empty
/// `tls-checks-full` means maximum validation. Mutually exclusive with `tls-checks`, and with
/// `verify-hostname`.
///
/// Holds what the operator wrote and nothing else: no preset is expanded here and no omitted check
/// is filled in. That is what makes writing an unchanged configuration back out a no-op; resolution
/// happens in the stage that derives the effective configuration.
@XmlRootElement(name = "tls-checks-full")
@XmlAccessorType(XmlAccessType.NONE)
@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
public class BridgeTlsChecksEntity {

    /// How trust is established: `CHAIN` builds a certification path, `ALLOW_LIST` accepts the
    /// individual certificates an operator wrote down, `ANY_CERT` accepts everything.
    @XmlElement(name = "trust-mode")
    private @Nullable String trustMode = null;

    /// Whether the certificate is checked to belong to the host the bridge connects to.
    @XmlElement(name = "hostname")
    private @Nullable String hostname = null;

    /// Whether the certificate's validity period is enforced.
    @XmlElement(name = "validity")
    private @Nullable String validity = null;

    /// Whether an issuer's revocation status is consulted while a path is built.
    @XmlElement(name = "revocation")
    private @Nullable String revocation = null;

    /// Whether the certificate's declared purpose is enforced.
    @XmlElement(name = "key-usage")
    private @Nullable String keyUsage = null;

    public @Nullable String getTrustMode() {
        return trustMode;
    }

    public @Nullable String getHostname() {
        return hostname;
    }

    public @Nullable String getValidity() {
        return validity;
    }

    public @Nullable String getRevocation() {
        return revocation;
    }

    public @Nullable String getKeyUsage() {
        return keyUsage;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BridgeTlsChecksEntity that)) {
            return false;
        }
        return Objects.equals(trustMode, that.trustMode)
                && Objects.equals(hostname, that.hostname)
                && Objects.equals(validity, that.validity)
                && Objects.equals(revocation, that.revocation)
                && Objects.equals(keyUsage, that.keyUsage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(trustMode, hostname, validity, revocation, keyUsage);
    }

    @Override
    public @NotNull String toString() {
        return "BridgeTlsChecksEntity{trustMode=" + trustMode + ", hostname=" + hostname + ", validity=" + validity
                + ", revocation=" + revocation + ", keyUsage=" + keyUsage + "}";
    }
}
