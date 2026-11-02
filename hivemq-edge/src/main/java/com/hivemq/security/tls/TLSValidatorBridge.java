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

import java.net.Socket;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.cert.CertPathValidator;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.PKIXRevocationChecker;
import java.security.cert.X509CertSelector;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509ExtendedTrustManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/// A bridge's policy expressed in the shape the MQTT client library demands.
///
/// The library takes a trust-manager factory, a key-manager factory, a hostname verifier and three
/// tunnel settings, and hands the first three to the TLS implementation beneath it. So everything
/// this class does is reshape what the policy already decided; it adds no decision of its own.
///
/// Four of the five checks live in the trust manager, because that is the one callback the TLS
/// handshake makes with the peer's certificate chain in hand. The fifth - hostname - is a separate
/// hook the library calls after the chain is accepted, so it is produced separately.
///
/// Which checks apply is read from the policy, not decided here. A validator that chose would be a
/// second place where the meaning of a check lives, and two such places diverge.
public final class TLSValidatorBridge {

    private TLSValidatorBridge() {}

    /// The trust manager the TLS handshake calls with the broker's certificate chain.
    ///
    /// Never null: the handshake is given one unconditionally, so a trust mode that accepts
    /// everything yields a manager that accepts everything rather than no manager at all. A missing
    /// manager would mean the runtime's own authorities, which is a different policy.
    public static @NotNull TrustManagerFactory trustManagerFactoryFor(final @NotNull TLSPolicy policy)
            throws TLSPolicy.TLSMaterialException {
        return new SingleManagerFactory(managerFor(policy));
    }

    /// The hostname check, as the verifier the MQTT client library takes.
    ///
    /// Returns null where the check is off, which the library reads as "use the default verifier" -
    /// so the caller must substitute one that accepts everything. Expressed this way because the
    /// library has no "no verifier" state, and saying so here keeps the decision visible.
    public static @Nullable HostnameVerifier hostnameVerifierFor(final @NotNull TLSPolicy policy) {
        return policy.config().hostname() == TLSConstants.HostnameCheck.HOSTNAME ? null : acceptAnyHostname();
    }

    /// A verifier that accepts every hostname, for the configuration that turns the check off.
    public static @NotNull HostnameVerifier acceptAnyHostname() {
        return (hostname, session) -> true;
    }

    private static @NotNull X509ExtendedTrustManager managerFor(final @NotNull TLSPolicy policy)
            throws TLSPolicy.TLSMaterialException {
        return switch (policy.config().trustMode()) {
            case CHAIN -> new ChainTrustManager(policy);
            case ALLOW_LIST -> new FingerprintTrustManager(policy);
            // Accepts any certificate, including one presented by anybody at all. Configured
            // explicitly, logged as such where the bridge starts, and the reason this returns a
            // manager rather than none: no manager means the runtime's own authorities.
            case ANY_CERT -> new AcceptAnyTrustManager();
        };
    }

    // --- chain trust -------------------------------------------------------------------------------

    /// Builds a certification path from the broker's chain to one of the configured anchors, and
    /// enforces the hygiene checks the policy asks for.
    ///
    /// The JDK's own path validator does the building, because the rules it implements - name
    /// constraints, basic constraints, path length, signature verification at every step - are
    /// exactly where a hand-written implementation goes wrong. What this class supplies is the
    /// parameters: the anchors, whether validity is checked, whether revocation is demanded, and
    /// what the certificate's declared purpose must permit.
    private static final class ChainTrustManager extends X509ExtendedTrustManager {

        private final @NotNull TLSPolicy policy;
        private final @NotNull CertificateFactory certificates;

        ChainTrustManager(final @NotNull TLSPolicy policy) throws TLSPolicy.TLSMaterialException {
            this.policy = policy;
            try {
                this.certificates = CertificateFactory.getInstance("X.509");
            } catch (final CertificateException e) {
                throw new TLSPolicy.TLSMaterialException("The X.509 certificate factory is unavailable.", e);
            }
            if (policy.trustAnchors().isEmpty()) {
                // A path with nothing to build to refuses every connection. Caught here rather than
                // at connect time, where it arrives as a certificate error naming no cause.
                throw new TLSPolicy.TLSMaterialException("The trust mode builds a certification path but no trust "
                        + "anchors were loaded, so no certificate could be trusted.");
            }
        }

        @Override
        public void checkServerTrusted(final X509Certificate @NotNull [] chain, final @NotNull String authType)
                throws CertificateException {
            validate(chain);
        }

        @Override
        public void checkServerTrusted(
                final X509Certificate @NotNull [] chain, final @NotNull String authType, final Socket socket)
                throws CertificateException {
            validate(chain);
        }

        @Override
        public void checkServerTrusted(
                final X509Certificate @NotNull [] chain, final @NotNull String authType, final SSLEngine engine)
                throws CertificateException {
            validate(chain);
        }

        /// Edge is the client here and presents its own certificate; it never judges one.
        @Override
        public void checkClientTrusted(final X509Certificate @NotNull [] chain, final @NotNull String authType)
                throws CertificateException {
            throw new CertificateException("A bridge is a client and does not validate client certificates.");
        }

        @Override
        public void checkClientTrusted(
                final X509Certificate @NotNull [] chain, final @NotNull String authType, final Socket socket)
                throws CertificateException {
            throw new CertificateException("A bridge is a client and does not validate client certificates.");
        }

        @Override
        public void checkClientTrusted(
                final X509Certificate @NotNull [] chain, final @NotNull String authType, final SSLEngine engine)
                throws CertificateException {
            throw new CertificateException("A bridge is a client and does not validate client certificates.");
        }

        @Override
        public X509Certificate @NotNull [] getAcceptedIssuers() {
            return policy.trustAnchors().stream()
                    .map(anchor -> anchor.getTrustedCert())
                    .toArray(X509Certificate[]::new);
        }

        private void validate(final X509Certificate @NotNull [] chain) throws CertificateException {
            if (chain.length == 0) {
                throw new CertificateException("The broker presented no certificate.");
            }
            // Validity first, and separately: the path validator reports an expired certificate as a
            // generic path failure, while this names the certificate and its dates. Skipped entirely
            // where the policy does not ask for it, which is also why it cannot be left to the
            // validator - there is no way to ask the validator for a path without date checking
            // other than pinning its clock, which would hide a genuinely expired anchor.
            if (policy.config().validity() == TLSConstants.ValidityCheck.NOT_BEFORE_OR_AFTER) {
                for (final X509Certificate certificate : chain) {
                    try {
                        policy.requireValidity(certificate);
                    } catch (final TLSPolicy.TLSCheckFailedException e) {
                        throw new CertificateException(e.getMessage(), e);
                    }
                }
            }
            // The declared purpose, checked here rather than through the path validator's target
            // selector: that selector filters candidates while a path is *built*, and validating a
            // path the handshake already supplied never consults it. Asserting it there would have
            // looked right and checked nothing.
            if (policy.config().keyUsage() == TLSConstants.KeyUsageCheck.SERVER_AUTH) {
                requireServerAuthentication(chain[0]);
            }
            try {
                final PKIXBuilderParameters parameters = parameters(chain);
                CertPathValidator.getInstance("PKIX")
                        .validate(certificates.generateCertPath(Arrays.asList(chain)), parameters);
            } catch (final GeneralSecurityException | java.io.IOException e) {
                throw new CertificateException(
                        ("The broker's certificate could not be verified against the " + "configured trust anchors: %s")
                                .formatted(e.getMessage()),
                        e);
            }
        }

        private @NotNull PKIXBuilderParameters parameters(final X509Certificate @NotNull [] chain)
                throws GeneralSecurityException, java.io.IOException {
            final X509CertSelector target = new X509CertSelector();
            target.setCertificate(chain[0]);
            final PKIXBuilderParameters parameters;
            try {
                parameters = new PKIXBuilderParameters(policy.trustAnchors(), target);
            } catch (final InvalidAlgorithmParameterException e) {
                throw new GeneralSecurityException("The configured trust anchors cannot build a path.", e);
            }

            // The JDK's path validation always checks dates, and there is no way to ask it not to:
            // setDate(null) means "use the current time", not "skip". So where the policy turns
            // validity off, the path is validated at a moment the chain is valid at - the latest
            // notBefore across the chain - which is the only way to let the rest of the path
            // verification run without the dates deciding the outcome.
            //
            // Where validity is on, the dates were already checked above with a message naming the
            // certificate, so the validator's own check can only agree.
            if (policy.config().validity() != TLSConstants.ValidityCheck.NOT_BEFORE_OR_AFTER) {
                parameters.setDate(latestNotBefore(chain));
            }
            parameters.setRevocationEnabled(false);

            switch (policy.config().revocation()) {
                case NONE -> {
                    // Nothing to add: revocation stays off.
                }
                case CHECK -> parameters.addCertPathChecker(revocationChecker(true));
                case REQUIRE_CRLS -> parameters.addCertPathChecker(revocationChecker(false));
            }

            if (!policy.revocationLists().isEmpty()) {
                parameters.addCertStore(java.security.cert.CertStore.getInstance(
                        "Collection", new java.security.cert.CollectionCertStoreParameters(policy.revocationLists())));
            }

            return parameters;
        }

        /// Requires the certificate's declared purpose to permit server authentication.
        ///
        /// A certificate carrying no extended-key-usage extension permits every purpose, which is
        /// what the extension's absence means in RFC 5280 - so it passes. One that lists purposes
        /// must list this among them, or it was issued for something else and accepting it would let
        /// a certificate meant for signing code stand in for a broker.
        private static void requireServerAuthentication(final @NotNull X509Certificate certificate)
                throws CertificateException {
            final List<String> purposes = certificate.getExtendedKeyUsage();
            if (purposes == null) {
                return;
            }
            // 1.3.6.1.5.5.7.3.1 is id-kp-serverAuth and 2.5.29.37.0 is anyExtendedKeyUsage.
            if (purposes.contains("1.3.6.1.5.5.7.3.1") || purposes.contains("2.5.29.37.0")) {
                return;
            }
            throw new CertificateException(("The broker's certificate '%s' is not valid for server "
                            + "authentication: it declares the purposes %s. Either the broker presents the wrong "
                            + "certificate, or the key-usage check does not apply to this deployment.")
                    .formatted(certificate.getSubjectX500Principal().getName(), purposes));
        }

        /// The latest moment at which every certificate in the chain has already become valid, used
        /// as the validation date where the policy does not check validity.
        private static @NotNull java.util.Date latestNotBefore(final X509Certificate @NotNull [] chain) {
            java.util.Date latest = chain[0].getNotBefore();
            for (final X509Certificate certificate : chain) {
                if (certificate.getNotBefore().after(latest)) {
                    latest = certificate.getNotBefore();
                }
            }
            return latest;
        }

        /// The revocation checker, set to fail closed or to tolerate an unknown status.
        ///
        /// `REQUIRE_CRLS` means an issuer whose status cannot be determined is refused, which is what
        /// "unknown fails closed" amounts to. `CHECK` means a known revocation refuses the
        /// certificate but an unobtainable list does not, which is the weaker reading an operator
        /// chooses when their authority publishes lists unreliably.
        private @NotNull PKIXRevocationChecker revocationChecker(final boolean tolerateUnknown)
                throws GeneralSecurityException {
            final PKIXRevocationChecker checker = (PKIXRevocationChecker)
                    CertPathValidator.getInstance("PKIX").getRevocationChecker();
            final EnumSet<PKIXRevocationChecker.Option> options =
                    EnumSet.of(PKIXRevocationChecker.Option.PREFER_CRLS, PKIXRevocationChecker.Option.NO_FALLBACK);
            if (tolerateUnknown) {
                options.add(PKIXRevocationChecker.Option.SOFT_FAIL);
            }
            checker.setOptions(options);
            return checker;
        }
    }

    // --- fingerprint trust -------------------------------------------------------------------------

    /// Accepts the individual certificates an operator wrote down, by SHA-256 fingerprint.
    ///
    /// No path is built: the fingerprint is the whole of the trust decision, which is what makes this
    /// usable where there is no certificate authority at all. Validity is still enforced where the
    /// policy asks for it, because a pinned certificate that has expired is one the operator
    /// recorded and then let lapse.
    private static final class FingerprintTrustManager extends X509ExtendedTrustManager {

        private final @NotNull TLSPolicy policy;

        FingerprintTrustManager(final @NotNull TLSPolicy policy) throws TLSPolicy.TLSMaterialException {
            this.policy = policy;
            if (policy.allowedFingerprints().isEmpty()) {
                throw new TLSPolicy.TLSMaterialException("Trust by fingerprint is configured but no fingerprints "
                        + "were loaded, so no certificate could be trusted.");
            }
        }

        @Override
        public void checkServerTrusted(final X509Certificate @NotNull [] chain, final @NotNull String authType)
                throws CertificateException {
            validate(chain);
        }

        @Override
        public void checkServerTrusted(
                final X509Certificate @NotNull [] chain, final @NotNull String authType, final Socket socket)
                throws CertificateException {
            validate(chain);
        }

        @Override
        public void checkServerTrusted(
                final X509Certificate @NotNull [] chain, final @NotNull String authType, final SSLEngine engine)
                throws CertificateException {
            validate(chain);
        }

        @Override
        public void checkClientTrusted(final X509Certificate @NotNull [] chain, final @NotNull String authType)
                throws CertificateException {
            throw new CertificateException("A bridge is a client and does not validate client certificates.");
        }

        @Override
        public void checkClientTrusted(
                final X509Certificate @NotNull [] chain, final @NotNull String authType, final Socket socket)
                throws CertificateException {
            throw new CertificateException("A bridge is a client and does not validate client certificates.");
        }

        @Override
        public void checkClientTrusted(
                final X509Certificate @NotNull [] chain, final @NotNull String authType, final SSLEngine engine)
                throws CertificateException {
            throw new CertificateException("A bridge is a client and does not validate client certificates.");
        }

        /// Empty: nothing is accepted by issuer, so there is no issuer to advertise.
        @Override
        public X509Certificate @NotNull [] getAcceptedIssuers() {
            return new X509Certificate[0];
        }

        private void validate(final X509Certificate @NotNull [] chain) throws CertificateException {
            if (chain.length == 0) {
                throw new CertificateException("The broker presented no certificate.");
            }
            try {
                // The certificate the broker presents, not any issuer above it: what was pinned is
                // the broker's own certificate.
                policy.requireAllowedFingerprint(chain[0]);
                if (policy.config().validity() == TLSConstants.ValidityCheck.NOT_BEFORE_OR_AFTER) {
                    policy.requireValidity(chain[0]);
                }
            } catch (final TLSPolicy.TLSCheckFailedException e) {
                throw new CertificateException(e.getMessage(), e);
            }
        }
    }

    // --- no trust decision at all --------------------------------------------------------------------

    /// Accepts every certificate. Vulnerable to an interposed server by construction, which is why
    /// the configuration naming it says so and the bridge logs it at start.
    private static final class AcceptAnyTrustManager extends X509ExtendedTrustManager {

        @Override
        public void checkServerTrusted(final X509Certificate @NotNull [] chain, final @NotNull String authType) {}

        @Override
        public void checkServerTrusted(
                final X509Certificate @NotNull [] chain, final @NotNull String authType, final Socket socket) {}

        @Override
        public void checkServerTrusted(
                final X509Certificate @NotNull [] chain, final @NotNull String authType, final SSLEngine engine) {}

        @Override
        public void checkClientTrusted(final X509Certificate @NotNull [] chain, final @NotNull String authType) {}

        @Override
        public void checkClientTrusted(
                final X509Certificate @NotNull [] chain, final @NotNull String authType, final Socket socket) {}

        @Override
        public void checkClientTrusted(
                final X509Certificate @NotNull [] chain, final @NotNull String authType, final SSLEngine engine) {}

        @Override
        public X509Certificate @NotNull [] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }

    // --- handing one manager to a library that wants a factory -----------------------------------------

    /// Wraps one trust manager as a factory, which is the only shape the MQTT client library takes.
    ///
    /// A factory normally builds managers from a key store; here the manager already exists and
    /// carries the policy, so the factory is a container. Extending `TrustManagerFactory` requires a
    /// subclass of its SPI, which is what the inner class is for.
    private static final class SingleManagerFactory extends TrustManagerFactory {

        private SingleManagerFactory(final @NotNull X509ExtendedTrustManager manager) {
            super(new Spi(manager), new PolicyProvider(), TrustManagerFactory.getDefaultAlgorithm());
        }

        private static final class Spi extends javax.net.ssl.TrustManagerFactorySpi {

            private final @NotNull TrustManager @NotNull [] managers;

            private Spi(final @NotNull X509ExtendedTrustManager manager) {
                this.managers = new TrustManager[] {manager};
            }

            @Override
            protected void engineInit(final KeyStore keyStore) {
                // The manager was built from the policy, so there is nothing to initialise from.
            }

            @Override
            protected void engineInit(final javax.net.ssl.ManagerFactoryParameters parameters) {
                // As above.
            }

            @Override
            protected @NotNull TrustManager @NotNull [] engineGetTrustManagers() {
                return managers.clone();
            }
        }

        /// A provider exists only because `TrustManagerFactory`'s constructor demands one. It
        /// registers nothing and is never installed.
        private static final class PolicyProvider extends java.security.Provider {

            private static final long serialVersionUID = 1L;

            private PolicyProvider() {
                super("HiveMQEdgeTLSPolicy", "1.0", "Carries a TLS policy to the TLS implementation.");
            }
        }
    }
}
