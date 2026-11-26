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

import java.io.IOException;
import java.math.BigInteger;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Principal;
import java.security.Provider;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.CertPathValidator;
import java.security.cert.CertStore;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateParsingException;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.PKIXRevocationChecker;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.net.ssl.CertPathTrustManagerParameters;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.ManagerFactoryParameters;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.TrustManagerFactorySpi;
import javax.net.ssl.X509ExtendedTrustManager;
import javax.security.auth.x500.X500Principal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/// A bridge's policy expressed in the shape the MQTT client library demands.
///
/// The library takes a trust-manager factory, a key-manager factory, a hostname verifier and three
/// tunnel settings, and hands the first three to the TLS implementation beneath it. So everything
/// this class does is reshape what the policy already decided; it adds no decision of its own.
///
/// All five checks live in the trust manager, because that is the one callback the TLS handshake
/// makes with the peer's certificate chain in hand. The hostname check is among them: where it is
/// on, the library is given no verifier, asks the TLS implementation to identify the endpoint, and
/// that request reaches the trust manager together with the connection it concerns.
///
/// Which checks apply is read from the policy, not decided here. A validator that chose would be a
/// second place where the meaning of a check lives, and two such places diverge.
public final class TLSValidatorBridge {

    /// The OID of id-kp-serverAuth.
    private static final @NotNull String SERVER_AUTHENTICATION = "1.3.6.1.5.5.7.3.1";

    /// The OID of anyExtendedKeyUsage.
    private static final @NotNull String ANY_EXTENDED_KEY_USAGE = "2.5.29.37.0";

    /// Bit positions in the KeyUsage extension, RFC 5280 section 4.2.1.3.
    private static final int DIGITAL_SIGNATURE = 0;

    private static final int KEY_ENCIPHERMENT = 2;
    private static final int KEY_AGREEMENT = 4;

    /// How the TLS implementation words an alert the other side sent.
    private static final @NotNull String RECEIVED_ALERT = "Received fatal alert: ";

    /// The alerts a server sends about the certificate a client presented, or failed to present.
    private static final @NotNull Set<String> CERTIFICATE_ALERTS = Set.of(
            "bad_certificate",
            "unsupported_certificate",
            "certificate_revoked",
            "certificate_expired",
            "certificate_unknown",
            "unknown_ca",
            "access_denied",
            "certificate_required");

    /// Bounds the walk down a cause chain, which a misbehaving library could make circular. Not a
    /// measured value: a connection failure arrives wrapped a few levels deep, and sixteen leaves
    /// room for any library between Edge and the TLS implementation.
    private static final int MAX_CAUSE_DEPTH = 16;

    private TLSValidatorBridge() {}

    /// The trust manager the TLS handshake calls with the broker's certificate chain.
    ///
    /// Never null: the handshake is given one unconditionally, so a trust mode that accepts
    /// everything yields a manager that accepts everything rather than no manager at all. A missing
    /// manager would mean the runtime's own authorities, which is a different policy.
    public static @NotNull TrustManagerFactory trustManagerFactoryFor(final @NotNull TLSPolicy policy)
            throws TLSPolicy.TLSMaterialException {
        return new SingleManagerFactory(managerFor(policy, AnchorDates.CHECKED));
    }

    /// The trust manager for a bridge on the compatibility preset, which validates what a bridge
    /// validated before the elementary checks existed.
    ///
    /// The one difference from [#trustManagerFactoryFor(TLSPolicy)]: a broker certificate that is
    /// itself in the truststore is accepted whatever its dates, as the runtime's path validator
    /// accepts a trust anchor. Every other certificate's dates are checked as before.
    public static @NotNull TrustManagerFactory compatibilityTrustManagerFactoryFor(final @NotNull TLSPolicy policy)
            throws TLSPolicy.TLSMaterialException {
        return new SingleManagerFactory(managerFor(policy, AnchorDates.WAIVED));
    }

    /// The hostname check, as the verifier the MQTT client library takes.
    ///
    /// Returns null where the check is on. Given no verifier, the library sets HTTPS endpoint
    /// identification on the TLS engine, and the trust manager this class builds then matches the
    /// broker's certificate against the host the bridge connected to, under every trust mode. A
    /// verifier would instead be called after the handshake had accepted the certificate. Where the
    /// check is off, the caller installs [#acceptAnyHostname()], which also keeps the library from
    /// requesting endpoint identification.
    public static @Nullable HostnameVerifier hostnameVerifierFor(final @NotNull TLSPolicy policy) {
        return policy.config().isHostname() ? null : acceptAnyHostname();
    }

    /// A verifier that accepts every hostname, for the configuration that turns the check off.
    public static @NotNull HostnameVerifier acceptAnyHostname() {
        return (hostname, session) -> true;
    }

    /// The warnings a bridge logs once at start-up about checks its configuration weakens or
    /// material it names and never reads.
    ///
    /// Each is conditioned on the effective configuration and on what the policy loaded, never on
    /// which elements were written, so none fires on a deployment that is configured correctly. Each
    /// names the bridge and the broker, says what follows from the setting, and names the ways out.
    ///
    /// @param policy the bridge's policy, material loaded
    /// @param bridgeId the bridge, as the configuration names it
    /// @param endpoint the remote broker, as `host:port`
    public static @NotNull List<String> startupWarnings(
            final @NotNull TLSPolicy policy, final @NotNull String bridgeId, final @NotNull String endpoint) {
        final TLSConfig config = policy.config();
        final List<String> warnings = new ArrayList<>();
        if (config.trustMode() == TLSConstants.TrustMode.ANY_CERT) {
            warnings.add(("Bridge '%s' to %s: trust mode ANY_CERT accepts every certificate the broker presents and "
                            + "establishes no trust at all, so an interposed server would be accepted in the broker's "
                            + "place. Name the broker's authority in <tls-truststore> and set <tls-checks>STANDARD"
                            + "</tls-checks>, or pin the broker's certificate with <tls-checks>SELF_SIGNED</tls-checks> "
                            + "and <tls-allowlist>.")
                    .formatted(bridgeId, endpoint));
        }
        if (config.isChainBuilt() && !config.isHostname()) {
            warnings.add(("Bridge '%s' to %s: TLS hostname verification is not enabled. The broker's certificate is "
                            + "trusted through its chain but is not checked against the host the bridge connects to, "
                            + "so a substituted broker whose certificate chains to the same anchor would be accepted. "
                            + "Enable it with <tls-checks>ALL</tls-checks> or <hostname>HOSTNAME</hostname> in "
                            + "<tls-checks-full>.")
                    .formatted(bridgeId, endpoint));
        }
        if (config.trustMode() != TLSConstants.TrustMode.ALLOW_LIST
                && config.allowList().isPresent()) {
            warnings.add(("Bridge '%s' to %s: a certificate allow-list is configured but the effective trust mode is "
                            + "%s, which never reads it. No broker certificate is pinned by its fingerprint. Set "
                            + "<tls-checks>SELF_SIGNED</tls-checks> or <trust-mode>ALLOW_LIST</trust-mode> to enforce "
                            + "the allow-list, or remove the <tls-allowlist> element.")
                    .formatted(bridgeId, endpoint, config.trustMode()));
        }
        if (!config.isChainBuilt() && config.revocationList().isPresent()) {
            warnings.add(("Bridge '%s' to %s: certificate revocation lists are configured but the effective trust "
                            + "mode is %s, which builds no certification path and never reads them. No revocation "
                            + "status is checked. Set <trust-mode>CHAIN</trust-mode> to enforce revocation, or remove "
                            + "the <tls-revocationlist> element.")
                    .formatted(bridgeId, endpoint, config.trustMode()));
        }
        // Conditional on the truststore holding a CA: a truststore of end-entity certificates is
        // direct trust, where no issuer appears in the path and revocation is satisfied vacuously.
        // Warning there would fire on working deployments and train operators to ignore it.
        if (config.isChainBuilt()
                && config.isRevocationLists()
                && policy.revocationLists().isEmpty()
                && anchorsIncludeAnAuthority(policy)) {
            warnings.add(("Bridge '%s' to %s: revocation=%s is enforced and the truststore contains a certificate "
                            + "authority, but no certificate revocation list is configured. The issuer's revocation "
                            + "status is therefore unknown, and unknown fails closed: a broker certificate signed by "
                            + "that authority will be refused however correct the rest of the configuration is. Point "
                            + "<tls-revocationlist> at the authority's CRL, or set <revocation>CHECK</revocation> or "
                            + "<revocation>NONE</revocation> in <tls-checks-full> to accept an unknown revocation "
                            + "status.")
                    .formatted(bridgeId, endpoint, config.revocation()));
        }
        return List.copyOf(warnings);
    }

    /// The warning a bridge logs on every successful connection, where there is one.
    ///
    /// Only accepting any certificate warrants it: a single line at start-up scrolls out of a
    /// production log within hours, and this is the setting an operator must see while triaging.
    ///
    /// @param config the bridge's effective configuration
    /// @param bridgeId the bridge, as the configuration names it
    /// @param endpoint the remote broker, as `host:port`
    public static @NotNull Optional<String> connectionWarning(
            final @NotNull TLSConfig config, final @NotNull String bridgeId, final @NotNull String endpoint) {
        if (config.trustMode() != TLSConstants.TrustMode.ANY_CERT) {
            return Optional.empty();
        }
        return Optional.of(("Bridge '%s' connected to %s with trust mode ANY_CERT: the broker's certificate was "
                        + "accepted without establishing any trust, so this connection may be to an interposed "
                        + "server. Consider <tls-checks>SELF_SIGNED</tls-checks>, which trusts specific certificates "
                        + "by SHA-256 fingerprint.")
                .formatted(bridgeId, endpoint));
    }

    /// Which side of a TLS connection refused a certificate, where the failure was a refusal.
    ///
    /// Both directions fail the same way from outside, with the connection down, and the TLS
    /// implementation's own wording does not say whose decision it was. Edge's refusal of the
    /// broker's certificate surfaces with the certificate exception its trust manager threw. The
    /// broker's refusal of Edge's certificate surfaces only as an alert the broker sent, which the
    /// TLS implementation exposes as message text and nothing else; reading the text is therefore the
    /// only way to tell, and an unrecognised wording yields no description rather than a wrong one.
    ///
    /// @param failure the connection failure, with its causes
    /// @return a sentence naming the refusing side and where to look, or empty where the failure
    ///     was not a certificate refusal
    public static @NotNull Optional<String> describeRefusal(final @NotNull Throwable failure) {
        Throwable cause = failure;
        for (int depth = 0; cause != null && depth < MAX_CAUSE_DEPTH; depth++, cause = cause.getCause()) {
            if (cause instanceof SSLException && cause.getMessage() != null) {
                final String message = cause.getMessage();
                final int at = message.indexOf(RECEIVED_ALERT);
                if (at >= 0) {
                    final String alert =
                            message.substring(at + RECEIVED_ALERT.length()).trim();
                    if (CERTIFICATE_ALERTS.contains(alert)) {
                        return Optional.of(("The remote broker refused the TLS handshake with the alert '%s': it did "
                                        + "not accept the client certificate Edge presented, or required one and Edge "
                                        + "presented none. Edge's own checks of the broker's certificate had passed. "
                                        + "Check that <tls-identity> names a certificate the broker trusts, and the "
                                        + "broker's own trust configuration for client certificates.")
                                .formatted(alert));
                    }
                }
            }
            if (cause instanceof CertificateException) {
                return Optional.of("Edge refused the remote broker's certificate; the reason is above. Check "
                        + "<tls-truststore>, <tls-allowlist> and <tls-checks> for this bridge.");
            }
        }
        return Optional.empty();
    }

    /// Whether the dates of a broker certificate that is itself a trust anchor are checked.
    private enum AnchorDates {
        CHECKED,
        WAIVED
    }

    private static @NotNull X509ExtendedTrustManager managerFor(
            final @NotNull TLSPolicy policy, final @NotNull AnchorDates anchorDates)
            throws TLSPolicy.TLSMaterialException {
        return switch (policy.config().trustMode()) {
            case CHAIN -> new ChainTrustManager(policy, anchorDates);
            case ALLOW_LIST -> new FingerprintTrustManager(policy);
            // Establishes no trust: any certificate passes as far as who issued it is concerned.
            // The checks on the certificate itself still apply where the policy asks for them, and
            // the bridge logs the trust mode where it starts and on every connection.
            case ANY_CERT -> new CertificateOnlyTrustManager(policy);
        };
    }

    // --- what every trust mode shares ----------------------------------------------------------

    /// The three server-side entry points routed to one decision, and the client side refused.
    ///
    /// The handshake calls the socket or engine form; the form without either is what a caller
    /// outside a handshake uses, and it cannot check the hostname because it has no connection to
    /// check against.
    private abstract static class BridgeTrustManager extends X509ExtendedTrustManager {

        final @NotNull TLSPolicy policy;

        BridgeTrustManager(final @NotNull TLSPolicy policy) {
            this.policy = policy;
        }

        abstract void validate(
                X509Certificate @NotNull [] chain,
                @NotNull String authType,
                @Nullable Socket socket,
                @Nullable SSLEngine engine)
                throws CertificateException;

        @Override
        public final void checkServerTrusted(final X509Certificate @NotNull [] chain, final @NotNull String authType)
                throws CertificateException {
            validate(requirePresented(chain), authType, null, null);
        }

        @Override
        public final void checkServerTrusted(
                final X509Certificate @NotNull [] chain, final @NotNull String authType, final @Nullable Socket socket)
                throws CertificateException {
            validate(requirePresented(chain), authType, socket, null);
        }

        @Override
        public final void checkServerTrusted(
                final X509Certificate @NotNull [] chain,
                final @NotNull String authType,
                final @Nullable SSLEngine engine)
                throws CertificateException {
            validate(requirePresented(chain), authType, null, engine);
        }

        /// Edge is the client here and presents its own certificate; it never judges one.
        @Override
        public final void checkClientTrusted(final X509Certificate @NotNull [] chain, final @NotNull String authType)
                throws CertificateException {
            throw new CertificateException("A bridge is a client and does not validate client certificates.");
        }

        @Override
        public final void checkClientTrusted(
                final X509Certificate @NotNull [] chain, final @NotNull String authType, final @Nullable Socket socket)
                throws CertificateException {
            throw new CertificateException("A bridge is a client and does not validate client certificates.");
        }

        @Override
        public final void checkClientTrusted(
                final X509Certificate @NotNull [] chain,
                final @NotNull String authType,
                final @Nullable SSLEngine engine)
                throws CertificateException {
            throw new CertificateException("A bridge is a client and does not validate client certificates.");
        }

        private static X509Certificate @NotNull [] requirePresented(final X509Certificate @Nullable [] chain)
                throws CertificateException {
            if (chain == null || chain.length == 0) {
                throw new CertificateException("The broker presented no certificate.");
            }
            return chain;
        }
    }

    /// The checks on the broker's own certificate that need no path: its validity period and its
    /// declared purpose.
    ///
    /// Validity is checked on the broker's certificate here, with a message naming it and its
    /// dates; under chain trust the path validator checks the issuers above it.
    private static void requireCertificateChecks(
            final @NotNull TLSPolicy policy, final @NotNull X509Certificate certificate) throws CertificateException {
        if (policy.config().isValidity()) {
            try {
                policy.requireValidity(certificate);
            } catch (final TLSPolicy.TLSCheckFailedException e) {
                throw new CertificateException(e.getMessage(), e);
            }
        }
        requireDeclaredPurpose(policy.config().keyUsage(), certificate);
    }

    /// Requires the certificate's declared purpose to permit authenticating a TLS server.
    ///
    /// `KEY_USAGE` requires the KeyUsage extension, asserting at least one of the uses a TLS server
    /// key is put to. `SERVER_AUTH` requires that and an ExtendedKeyUsage extension permitting
    /// server authentication. The extensions are required, not merely consulted where present:
    /// RFC 5280 reads an absent extension as "any purpose", and that reading is what `NONE` is for.
    private static void requireDeclaredPurpose(
            final @NotNull TLSConstants.KeyUsageCheck check, final @NotNull X509Certificate certificate)
            throws CertificateException {
        if (check == TLSConstants.KeyUsageCheck.NONE) {
            return;
        }
        final String subject = certificate.getSubjectX500Principal().getName();
        final boolean[] usage = certificate.getKeyUsage();
        if (usage == null) {
            throw new CertificateException(("The broker's certificate '%s' carries no KeyUsage extension, which "
                            + "key-usage %s requires. Either the broker presents the wrong certificate, or set "
                            + "<key-usage>NONE</key-usage> for a broker whose certificate omits it.")
                    .formatted(subject, check));
        }
        if (!asserts(usage, DIGITAL_SIGNATURE) && !asserts(usage, KEY_ENCIPHERMENT) && !asserts(usage, KEY_AGREEMENT)) {
            throw new CertificateException(("The broker's certificate '%s' is not valid for a TLS server: its "
                            + "KeyUsage permits none of digitalSignature, keyEncipherment and keyAgreement. Either "
                            + "the broker presents the wrong certificate, or the key-usage check does not apply to "
                            + "this deployment.")
                    .formatted(subject));
        }
        if (check != TLSConstants.KeyUsageCheck.SERVER_AUTH) {
            return;
        }
        final List<String> purposes = certificate.getExtendedKeyUsage();
        if (purposes == null) {
            throw new CertificateException(("The broker's certificate '%s' carries no ExtendedKeyUsage extension, "
                            + "which key-usage SERVER_AUTH requires. Either the broker presents the wrong certificate, "
                            + "or set <key-usage>KEY_USAGE</key-usage> for a broker whose certificate omits it.")
                    .formatted(subject));
        }
        if (!purposes.contains(SERVER_AUTHENTICATION) && !purposes.contains(ANY_EXTENDED_KEY_USAGE)) {
            throw new CertificateException(("The broker's certificate '%s' is not valid for server "
                            + "authentication: it declares the purposes %s. Either the broker presents the wrong "
                            + "certificate, or the key-usage check does not apply to this deployment.")
                    .formatted(subject, purposes));
        }
    }

    private static boolean asserts(final boolean @NotNull [] usage, final int bit) {
        return bit < usage.length && usage[bit];
    }

    /// Requires the broker's certificate to name the host the bridge connected to, where the policy
    /// asks for it and a connection is in hand.
    ///
    /// The JDK's own trust manager makes the comparison, because the matching rules (wildcards, the
    /// preference of subject alternative names over the common name, IP addresses) are where a
    /// hand-written implementation goes wrong. Given a truststore holding exactly the broker's
    /// certificate, it trusts that certificate without building a path and then identifies the
    /// endpoint the connection reports; trust itself was decided before this is called.
    private static void requireIdentity(
            final @NotNull TLSPolicy policy,
            final @NotNull X509Certificate certificate,
            final @NotNull String authType,
            final @Nullable Socket socket,
            final @Nullable SSLEngine engine)
            throws CertificateException {
        if (!policy.config().isHostname() || (socket == null && engine == null)) {
            return;
        }
        final X509ExtendedTrustManager identityCheck;
        try {
            final KeyStore alone = KeyStore.getInstance("PKCS12");
            alone.load(null, null);
            alone.setCertificateEntry("broker", certificate);
            final TrustManagerFactory factory = TrustManagerFactory.getInstance("PKIX");
            factory.init(alone);
            identityCheck = extendedManagerOf(factory);
        } catch (final GeneralSecurityException | IOException e) {
            throw new CertificateException("The hostname check could not be prepared: %s".formatted(e.getMessage()), e);
        }
        final X509Certificate[] presented = {certificate};
        try {
            if (socket != null) {
                identityCheck.checkServerTrusted(presented, authType, socket);
            } else {
                identityCheck.checkServerTrusted(presented, authType, engine);
            }
        } catch (final CertificateException e) {
            throw new CertificateException(
                    "The broker's certificate failed the hostname check: %s".formatted(e.getMessage()), e);
        }
    }

    private static @NotNull X509ExtendedTrustManager extendedManagerOf(final @NotNull TrustManagerFactory factory)
            throws NoSuchAlgorithmException {
        for (final TrustManager manager : factory.getTrustManagers()) {
            if (manager instanceof final X509ExtendedTrustManager extended) {
                return extended;
            }
        }
        throw new NoSuchAlgorithmException("The PKIX trust-manager factory produced no X.509 trust manager.");
    }

    private static boolean anchorsIncludeAnAuthority(final @NotNull TLSPolicy policy) {
        return policy.trustAnchors().stream()
                .anyMatch(anchor -> anchor.getTrustedCert() != null
                        && anchor.getTrustedCert().getBasicConstraints() != -1);
    }

    // --- chain trust -------------------------------------------------------------------------------

    /// Builds a certification path from the broker's chain to one of the configured anchors, and
    /// enforces the hygiene checks the policy asks for.
    ///
    /// The JDK's own trust manager does the path work, because the rules it implements - a path that
    /// ends at whichever configured certificate it reaches first, a broker certificate trusted
    /// directly, name and basic constraints, signatures at every step, the TLS end-entity rules,
    /// the hostname - are exactly where a hand-written implementation goes wrong. What this class
    /// supplies is the parameters: the anchors, the revocation demand and its lists. The declared
    /// purpose and the broker certificate's validity are checked here first, because the policy
    /// asks for more than the JDK checks and for messages that name the certificate.
    private static final class ChainTrustManager extends BridgeTrustManager {

        private final @NotNull X509ExtendedTrustManager pathValidator;
        private final @NotNull AnchorDates anchorDates;

        ChainTrustManager(final @NotNull TLSPolicy policy, final @NotNull AnchorDates anchorDates)
                throws TLSPolicy.TLSMaterialException {
            super(policy);
            this.anchorDates = anchorDates;
            if (policy.trustAnchors().isEmpty()) {
                // A path with nothing to build to refuses every connection. Caught here rather than
                // at connect time, where it arrives as a certificate error naming no cause.
                throw new TLSPolicy.TLSMaterialException("The trust mode builds a certification path but no trust "
                        + "anchors were loaded, so no certificate could be trusted.");
            }
            try {
                this.pathValidator = pathValidatorFor(policy);
            } catch (final GeneralSecurityException e) {
                throw new TLSPolicy.TLSMaterialException(
                        "The certificate path validator could not be built: %s".formatted(e.getMessage()), e);
            }
        }

        @Override
        void validate(
                final X509Certificate @NotNull [] chain,
                final @NotNull String authType,
                final @Nullable Socket socket,
                final @Nullable SSLEngine engine)
                throws CertificateException {
            if (anchorDates == AnchorDates.WAIVED && isTrustAnchor(chain[0])) {
                // The path validator ends the path at a trusted broker certificate without looking
                // at its dates, and that is what a bridge on the compatibility preset always got.
                requireDeclaredPurpose(policy.config().keyUsage(), chain[0]);
            } else {
                requireCertificateChecks(policy, chain[0]);
            }
            // Where validity is off, the chain goes to the validator with the date checks waived on
            // each certificate rather than with the validation date moved. A moved date would also
            // move the moment revocation is judged at, accepting a certificate revoked since, and no
            // single date fits a chain whose certificates' validity periods do not overlap.
            final X509Certificate[] presented = policy.config().isValidity() ? chain : waiveValidity(chain);
            try {
                if (socket != null) {
                    pathValidator.checkServerTrusted(presented, authType, socket);
                } else if (engine != null) {
                    pathValidator.checkServerTrusted(presented, authType, engine);
                } else {
                    pathValidator.checkServerTrusted(presented, authType);
                }
            } catch (final CertificateException e) {
                throw new CertificateException(
                        "The broker's certificate could not be verified: %s".formatted(e.getMessage()), e);
            }
        }

        @Override
        public X509Certificate @NotNull [] getAcceptedIssuers() {
            return pathValidator.getAcceptedIssuers();
        }

        private boolean isTrustAnchor(final @NotNull X509Certificate certificate) {
            for (final TrustAnchor anchor : policy.trustAnchors()) {
                if (certificate.equals(anchor.getTrustedCert())) {
                    return true;
                }
            }
            return false;
        }

        /// The JDK's PKIX trust manager over the policy's anchors, revocation demand and lists.
        ///
        /// Built once: the JDK copies the parameters, checkers included, for every validation, so
        /// one manager serves concurrent handshakes.
        private static @NotNull X509ExtendedTrustManager pathValidatorFor(final @NotNull TLSPolicy policy)
                throws GeneralSecurityException {
            final PKIXBuilderParameters parameters = new PKIXBuilderParameters(policy.trustAnchors(), null);
            parameters.setRevocationEnabled(false);
            switch (policy.config().revocation()) {
                case NONE -> {
                    // Nothing to add: revocation stays off.
                }
                case CHECK -> parameters.addCertPathChecker(revocationChecker(true));
                case REQUIRE_CRLS -> parameters.addCertPathChecker(revocationChecker(false));
            }
            if (!policy.revocationLists().isEmpty()) {
                parameters.addCertStore(CertStore.getInstance(
                        "Collection", new CollectionCertStoreParameters(policy.revocationLists())));
            }
            final TrustManagerFactory factory = TrustManagerFactory.getInstance("PKIX");
            factory.init(new CertPathTrustManagerParameters(parameters));
            return extendedManagerOf(factory);
        }

        /// The revocation checker, set to fail closed or to tolerate an unknown status.
        ///
        /// `REQUIRE_CRLS` means an issuer whose status cannot be determined is refused, which is what
        /// "unknown fails closed" amounts to. `CHECK` means a known revocation refuses the
        /// certificate but an unobtainable list does not, which is the weaker reading an operator
        /// chooses when their authority publishes lists unreliably.
        private static @NotNull PKIXRevocationChecker revocationChecker(final boolean tolerateUnknown)
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

        private static X509Certificate @NotNull [] waiveValidity(final X509Certificate @NotNull [] chain) {
            final X509Certificate[] waived = new X509Certificate[chain.length];
            for (int i = 0; i < chain.length; i++) {
                waived[i] = new ValidityWaivedCertificate(chain[i]);
            }
            return waived;
        }
    }

    /// A certificate whose validity period is never enforced, and that is otherwise the certificate
    /// it wraps.
    ///
    /// The JDK's path validation asks each certificate whether it is valid at the validation date,
    /// and offers no setting that skips the question. Answering it here waives exactly that check:
    /// the validation date stays the present, so revocation is judged now, and nothing else about
    /// the path changes. Equality and hashing compare encodings, so the wrapper is the certificate it
    /// wraps wherever the validator looks one up.
    @SuppressWarnings("deprecation") // getIssuerDN and getSubjectDN are abstract and must be delegated.
    private static final class ValidityWaivedCertificate extends X509Certificate {

        private static final long serialVersionUID = 1L;

        private final @NotNull X509Certificate certificate;

        private ValidityWaivedCertificate(final @NotNull X509Certificate certificate) {
            this.certificate = certificate;
        }

        @Override
        public void checkValidity() {
            // Waived.
        }

        @Override
        public void checkValidity(final @NotNull Date date) {
            // Waived.
        }

        @Override
        public int getVersion() {
            return certificate.getVersion();
        }

        @Override
        public @NotNull BigInteger getSerialNumber() {
            return certificate.getSerialNumber();
        }

        @Override
        public @NotNull Principal getIssuerDN() {
            return certificate.getIssuerDN();
        }

        @Override
        public @NotNull Principal getSubjectDN() {
            return certificate.getSubjectDN();
        }

        @Override
        public @NotNull X500Principal getIssuerX500Principal() {
            return certificate.getIssuerX500Principal();
        }

        @Override
        public @NotNull X500Principal getSubjectX500Principal() {
            return certificate.getSubjectX500Principal();
        }

        @Override
        public @NotNull Date getNotBefore() {
            return certificate.getNotBefore();
        }

        @Override
        public @NotNull Date getNotAfter() {
            return certificate.getNotAfter();
        }

        @Override
        public byte @NotNull [] getTBSCertificate() throws CertificateEncodingException {
            return certificate.getTBSCertificate();
        }

        @Override
        public byte @NotNull [] getSignature() {
            return certificate.getSignature();
        }

        @Override
        public @NotNull String getSigAlgName() {
            return certificate.getSigAlgName();
        }

        @Override
        public @NotNull String getSigAlgOID() {
            return certificate.getSigAlgOID();
        }

        @Override
        public byte @Nullable [] getSigAlgParams() {
            return certificate.getSigAlgParams();
        }

        @Override
        public boolean @Nullable [] getIssuerUniqueID() {
            return certificate.getIssuerUniqueID();
        }

        @Override
        public boolean @Nullable [] getSubjectUniqueID() {
            return certificate.getSubjectUniqueID();
        }

        @Override
        public boolean @Nullable [] getKeyUsage() {
            return certificate.getKeyUsage();
        }

        @Override
        public @Nullable List<String> getExtendedKeyUsage() throws CertificateParsingException {
            return certificate.getExtendedKeyUsage();
        }

        @Override
        public int getBasicConstraints() {
            return certificate.getBasicConstraints();
        }

        @Override
        public @Nullable Collection<List<?>> getSubjectAlternativeNames() throws CertificateParsingException {
            return certificate.getSubjectAlternativeNames();
        }

        @Override
        public @Nullable Collection<List<?>> getIssuerAlternativeNames() throws CertificateParsingException {
            return certificate.getIssuerAlternativeNames();
        }

        @Override
        public byte @NotNull [] getEncoded() throws CertificateEncodingException {
            return certificate.getEncoded();
        }

        @Override
        public void verify(final @NotNull PublicKey key)
                throws CertificateException, NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException,
                        SignatureException {
            certificate.verify(key);
        }

        @Override
        public void verify(final @NotNull PublicKey key, final @NotNull String sigProvider)
                throws CertificateException, NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException,
                        SignatureException {
            certificate.verify(key, sigProvider);
        }

        @Override
        public void verify(final @NotNull PublicKey key, final @NotNull Provider sigProvider)
                throws CertificateException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {
            certificate.verify(key, sigProvider);
        }

        @Override
        public @NotNull String toString() {
            return certificate.toString();
        }

        @Override
        public @NotNull PublicKey getPublicKey() {
            return certificate.getPublicKey();
        }

        @Override
        public boolean hasUnsupportedCriticalExtension() {
            return certificate.hasUnsupportedCriticalExtension();
        }

        @Override
        public @Nullable Set<String> getCriticalExtensionOIDs() {
            return certificate.getCriticalExtensionOIDs();
        }

        @Override
        public @Nullable Set<String> getNonCriticalExtensionOIDs() {
            return certificate.getNonCriticalExtensionOIDs();
        }

        @Override
        public byte @Nullable [] getExtensionValue(final @NotNull String oid) {
            return certificate.getExtensionValue(oid);
        }
    }

    // --- fingerprint trust -------------------------------------------------------------------------

    /// Accepts the individual certificates an operator wrote down, by SHA-256 fingerprint.
    ///
    /// No path is built: the fingerprint is the whole of the trust decision, which is what makes this
    /// usable where there is no certificate authority at all. The checks on the certificate itself
    /// still apply where the policy asks for them, because a pinned certificate that has expired is
    /// one the operator recorded and then let lapse.
    private static final class FingerprintTrustManager extends BridgeTrustManager {

        FingerprintTrustManager(final @NotNull TLSPolicy policy) throws TLSPolicy.TLSMaterialException {
            super(policy);
            if (policy.allowedFingerprints().isEmpty()) {
                throw new TLSPolicy.TLSMaterialException("Trust by fingerprint is configured but no fingerprints "
                        + "were loaded, so no certificate could be trusted.");
            }
        }

        @Override
        void validate(
                final X509Certificate @NotNull [] chain,
                final @NotNull String authType,
                final @Nullable Socket socket,
                final @Nullable SSLEngine engine)
                throws CertificateException {
            try {
                // The certificate the broker presents, not any issuer above it: what was pinned is
                // the broker's own certificate.
                policy.requireAllowedFingerprint(chain[0]);
            } catch (final TLSPolicy.TLSCheckFailedException e) {
                throw new CertificateException(e.getMessage(), e);
            }
            requireCertificateChecks(policy, chain[0]);
            requireIdentity(policy, chain[0], authType, socket, engine);
        }

        /// Empty: nothing is accepted by issuer, so there is no issuer to advertise.
        @Override
        public X509Certificate @NotNull [] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }

    // --- no trust decision at all --------------------------------------------------------------------

    /// Establishes no trust: whoever issued the certificate, it passes. Vulnerable to an interposed
    /// server by construction, which is why the configuration naming it says so and the bridge logs
    /// it at start and on every connection.
    ///
    /// The checks on the certificate itself are not trust decisions and still apply where the
    /// policy asks for them: an operator who accepts any issuer but asks for validity has asked for
    /// an expired certificate to be refused.
    private static final class CertificateOnlyTrustManager extends BridgeTrustManager {

        CertificateOnlyTrustManager(final @NotNull TLSPolicy policy) {
            super(policy);
        }

        @Override
        void validate(
                final X509Certificate @NotNull [] chain,
                final @NotNull String authType,
                final @Nullable Socket socket,
                final @Nullable SSLEngine engine)
                throws CertificateException {
            requireCertificateChecks(policy, chain[0]);
            requireIdentity(policy, chain[0], authType, socket, engine);
        }

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

        private static final class Spi extends TrustManagerFactorySpi {

            private final @NotNull TrustManager @NotNull [] managers;

            private Spi(final @NotNull X509ExtendedTrustManager manager) {
                this.managers = new TrustManager[] {manager};
            }

            @Override
            protected void engineInit(final KeyStore keyStore) {
                // The manager was built from the policy, so there is nothing to initialise from.
            }

            @Override
            protected void engineInit(final ManagerFactoryParameters parameters) {
                // As above.
            }

            @Override
            protected @NotNull TrustManager @NotNull [] engineGetTrustManagers() {
                return managers.clone();
            }
        }

        /// A provider exists only because `TrustManagerFactory`'s constructor demands one. It
        /// registers nothing and is never installed.
        private static final class PolicyProvider extends Provider {

            private static final long serialVersionUID = 1L;

            private PolicyProvider() {
                super("HiveMQEdgeTLSPolicy", "1.0", "Carries a TLS policy to the TLS implementation.");
            }
        }
    }
}
