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
package com.hivemq.security.tls;

/// The values each elementary certificate check can take.
///
/// Collected in one holder rather than spread over six files, and for one practical reason beyond
/// tidiness: a surface commonly has its own enums of the same names, bound to its own configuration
/// schema. Nesting these means a reference reads `TLSConstants.TrustMode.CHAIN` and cannot be
/// confused with a surface's own `TrustMode`, in a package where both are in scope.
///
/// Each enum names the value an omitted check takes. Omitting a check must yield more verification
/// and never less, so that value is the strictest one - an operator's forgotten entry fails safe.
/// The one exception is which preset a surface falls back to when an operator names no checks at
/// all, which is a surface's own decision about its released behaviour and lives with that surface.
public final class TLSConstants {

    private TLSConstants() {}

    /// Where trust in a peer certificate comes from.
    ///
    /// The check that decides whether a certification path is built, and therefore which of the
    /// others can be enforced at all: revocation is decidable only while a path is built.
    public enum TrustMode {

        /// The certificate must chain to a trust anchor in the truststore.
        CHAIN,

        /// The certificate's SHA-256 fingerprint must appear in the allow-list.
        ///
        /// Also what a self-signed preset resolves to: a self-signer's certificate is trusted by its
        /// fingerprint, since there is no authority to chain to.
        ALLOW_LIST,

        /// Any certificate is accepted. Establishes no trust and is open to interception.
        ANY_CERT;

        /// The value an omitted trust mode takes.
        public static final TrustMode STRICTEST = CHAIN;
    }

    /// Whether the application identifier a peer announces must match its certificate.
    ///
    /// Only a surface whose protocol carries an application identifier has this check. MQTT does
    /// not, so a bridge drops it; OPC UA announces an ApplicationUri, so the adapter has it.
    public enum SanUriCheck {

        /// The announced identifier is not compared against the certificate.
        NONE,

        /// The announced identifier must equal a SubjectAltName URI entry in the certificate.
        APPLICATION_URI;

        /// The value an omitted check takes.
        public static final SanUriCheck STRICTEST = APPLICATION_URI;
    }

    /// Whether the host Edge connected to must appear in the peer's certificate.
    ///
    /// Without it, a substituted peer whose certificate is trusted by the same route is accepted in
    /// place of the intended one.
    public enum HostnameCheck {

        /// The host Edge connected to is not compared against the certificate.
        NONE,

        /// The host must match a SubjectAltName DNS name or IP address in the certificate.
        HOSTNAME;

        /// The value an omitted check takes.
        public static final HostnameCheck STRICTEST = HOSTNAME;
    }

    /// Whether a certificate's own validity period is enforced.
    ///
    /// Needs no authority and no extra material: every certificate states its own dates.
    public enum ValidityCheck {

        /// The validity period is not enforced.
        NONE,

        /// The current time must lie within the certificate's notBefore and notAfter.
        NOT_BEFORE_OR_AFTER;

        /// The value an omitted check takes.
        public static final ValidityCheck STRICTEST = NOT_BEFORE_OR_AFTER;
    }

    /// How hard it is enforced that an authority has not withdrawn a certificate.
    ///
    /// The one check that depends on another: revocation is decided while a certification path is
    /// built, so any value other than `NONE` requires [TrustMode#CHAIN]. A configuration asking for
    /// revocation under another trust mode is rejected rather than quietly ignored.
    public enum RevocationCheck {

        /// Revocation status is not consulted.
        NONE,

        /// Revocation status is consulted where a list is available.
        CHECK,

        /// A revocation list is required for every issuing authority in the path.
        REQUIRE_CRLS;

        /// The value an omitted check takes.
        public static final RevocationCheck STRICTEST = REQUIRE_CRLS;
    }

    /// How strictly a certificate's declared purpose is enforced.
    ///
    /// A self-signer commonly omits these extensions altogether, which is why a self-signed preset
    /// turns this check off.
    public enum KeyUsageCheck {

        /// The declared purpose is not enforced.
        NONE,

        /// The KeyUsage extension must be present and permit the certificate's role.
        KEY_USAGE,

        /// Additionally, the ExtendedKeyUsage extension must permit server authentication.
        SERVER_AUTH;

        /// The value an omitted check takes.
        public static final KeyUsageCheck STRICTEST = SERVER_AUTH;
    }
}
