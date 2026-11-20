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
package com.hivemq.edge.adapters.opcua.security;

import com.hivemq.security.tls.TLSConstants.HostnameCheck;
import com.hivemq.security.tls.TLSConstants.KeyUsageCheck;
import com.hivemq.security.tls.TLSConstants.RevocationCheck;
import com.hivemq.security.tls.TLSConstants.SanUriCheck;
import com.hivemq.security.tls.TLSConstants.ValidityCheck;
import com.hivemq.security.tls.TLSPolicy;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.security.CertificateValidator;
import org.eclipse.milo.opcua.stack.core.security.DefaultClientCertificateValidator;
import org.eclipse.milo.opcua.stack.core.security.MemoryCertificateQuarantine;
import org.eclipse.milo.opcua.stack.core.security.TrustListManager;
import org.eclipse.milo.opcua.stack.core.util.validation.CertificateValidationUtil;
import org.eclipse.milo.opcua.stack.core.util.validation.ValidationCheck;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Drives Eclipse Milo from a [TLSPolicy].
///
/// The surface-specific half of certificate validation: the policy says what to check and holds the
/// material to check against, and this turns that into the one object Milo accepts. Nothing above
/// this class mentions Milo, and nothing in it decides policy.
///
/// Which validator it builds depends on the trust mode, because Milo has a notion of chaining to an
/// anchor and no notion of trusting a fingerprint:
///
///   - chain trust hands the work to Milo's own validator, which builds and validates the
///     certification path and applies the checks named in a check set;
///   - fingerprint trust and no trust are validators of our own, since Milo would have nothing to
///     do, and they still apply every check that needs no path.
public final class TLSValidatorOpcUA {

    private static final @NotNull Logger log = LoggerFactory.getLogger(TLSValidatorOpcUA.class);

    private TLSValidatorOpcUA() {}

    /// The validator for a policy, never null.
    ///
    /// A configuration that verifies nothing still gets a validator that accepts everything: the
    /// stack is handed one unconditionally, so a null would fail every handshake rather than skip
    /// the checks - which is a connection that never works rather than one that checks nothing.
    public static @NotNull CertificateValidator validatorFor(
            final @NotNull TLSPolicy policy, final @NotNull String endpointUri) {
        return switch (policy.config().trustMode()) {
            case CHAIN ->
                new DefaultClientCertificateValidator(
                        trustListManager(policy), validationChecks(policy), new MemoryCertificateQuarantine());
            case ALLOW_LIST -> new FingerprintValidator(policy, endpointUri);
            case ANY_CERT ->
                policy.config().hasCertificateOnlyChecks()
                        ? new CertificateOnlyValidator(policy)
                        : new CertificateValidator.InsecureCertificateValidator();
        };
    }

    /// The policy's checks as the optional-check set Milo takes.
    ///
    /// Only the checks Milo performs while validating a path appear here. The trust mode does not:
    /// it decided which validator was built. Validity is absent too - Milo always enforces it, so
    /// naming it would say nothing, and a policy that switches it off cannot be honoured by Milo's
    /// own validator.
    public static @NotNull Set<ValidationCheck> validationChecks(final @NotNull TLSPolicy policy) {
        final EnumSet<ValidationCheck> checks = EnumSet.noneOf(ValidationCheck.class);
        if (policy.config().sanUri() == SanUriCheck.APPLICATION_URI) {
            checks.add(ValidationCheck.APPLICATION_URI);
        }
        if (policy.config().hostname() == HostnameCheck.HOSTNAME) {
            checks.add(ValidationCheck.HOSTNAME);
        }
        if (policy.config().validity() == ValidityCheck.NOT_BEFORE_OR_AFTER) {
            checks.add(ValidationCheck.VALIDITY);
        }
        if (policy.config().revocation() != RevocationCheck.NONE) {
            checks.add(ValidationCheck.REVOCATION);
        }
        if (policy.config().revocation() == RevocationCheck.REQUIRE_CRLS) {
            checks.add(ValidationCheck.REVOCATION_LISTS);
        }
        if (policy.config().keyUsage() != KeyUsageCheck.NONE) {
            checks.add(ValidationCheck.KEY_USAGE_END_ENTITY);
        }
        if (policy.config().keyUsage() == KeyUsageCheck.SERVER_AUTH) {
            checks.add(ValidationCheck.EXTENDED_KEY_USAGE_END_ENTITY);
        }
        return Set.copyOf(checks);
    }

    /// The policy's anchors and revocation lists, as the trust list Milo reads.
    ///
    /// Every anchor is offered as both trusted and issuer: Milo's own validator distinguishes the
    /// two, but the distinction only matters for its trust-list management API, which Edge does not
    /// expose - an operator names a set of certificates to trust and Milo may use any of them at
    /// either position in a path.
    public static @NotNull TrustListManager trustListManager(final @NotNull TLSPolicy policy) {
        final List<X509Certificate> certificates = new ArrayList<>();
        policy.trustAnchors().forEach(anchor -> certificates.add(anchor.getTrustedCert()));
        return new PolicyTrustListManager(certificates, policy.revocationLists());
    }

    /// Trusts a server certificate by its SHA-256 fingerprint.
    ///
    /// For a deployment with no certificate authority. An operator writes the fingerprints down, so
    /// a server present at first connect cannot enrol itself, and the list can be deployed read-only.
    private static final class FingerprintValidator implements CertificateValidator {

        private final @NotNull TLSPolicy policy;
        private final @NotNull String endpointUri;

        private FingerprintValidator(final @NotNull TLSPolicy policy, final @NotNull String endpointUri) {
            this.policy = policy;
            this.endpointUri = endpointUri;
        }

        @Override
        public void validateCertificateChain(
                final @NotNull List<X509Certificate> chain,
                final @Nullable String applicationUri,
                final String @Nullable [] validHostnames)
                throws UaException {
            final X509Certificate endEntity = endEntityOf(chain);
            try {
                policy.requireAllowedFingerprint(endEntity);
            } catch (final TLSPolicy.TLSCheckFailedException e) {
                // Logged as well as thrown. The message names the fingerprint the server presented,
                // in the form the allow-list file accepts, so an operator who recognises the server
                // can add it out of band without extracting the certificate first. A rejected
                // connection is where an operator looks, and whatever catches the exception is not
                // guaranteed to put its message there. Logged on rejection only, and never added.
                log.warn("OPC UA adapter endpoint '{}': {}", endpointUri, e.getMessage());
                throw new UaException(StatusCodes.Bad_SecurityChecksFailed, endpointUri + ": " + e.getMessage());
            }
            applyCertificateOnlyChecks(policy, endEntity, applicationUri, validHostnames);
        }
    }

    /// Establishes no trust, and still applies every check that needs no certification path.
    ///
    /// What keeps the checks independent: an operator who turns trust off has not thereby turned off
    /// the hostname or validity check, and this is where that holds.
    private static final class CertificateOnlyValidator implements CertificateValidator {

        private final @NotNull TLSPolicy policy;

        private CertificateOnlyValidator(final @NotNull TLSPolicy policy) {
            this.policy = policy;
        }

        @Override
        public void validateCertificateChain(
                final @NotNull List<X509Certificate> chain,
                final @Nullable String applicationUri,
                final String @Nullable [] validHostnames)
                throws UaException {
            applyCertificateOnlyChecks(policy, endEntityOf(chain), applicationUri, validHostnames);
        }
    }

    /// Every check decidable from the server's own certificate.
    ///
    /// Validity comes from the policy, which needs no library. The other three go to Milo's own
    /// helpers, which the OPC UA conformance suite exercises: the application-identifier comparison
    /// and the key-usage rules are specified by OPC UA rather than by TLS, and hostname matching has
    /// wildcard rules where a subtle mistake is a security hole rather than a bug.
    ///
    /// A check whose input the stack did not supply is skipped rather than failed, which is what
    /// Milo's own validator does in the same situation: failing would reject every server rather
    /// than the wrong one.
    private static void applyCertificateOnlyChecks(
            final @NotNull TLSPolicy policy,
            final @NotNull X509Certificate endEntity,
            final @Nullable String applicationUri,
            final String @Nullable [] validHostnames)
            throws UaException {

        if (policy.config().sanUri() == SanUriCheck.APPLICATION_URI && applicationUri != null) {
            CertificateValidationUtil.checkApplicationUri(endEntity, applicationUri);
        }
        if (policy.config().hostname() == HostnameCheck.HOSTNAME && validHostnames != null) {
            CertificateValidationUtil.checkHostnameOrIpAddress(endEntity, validHostnames);
        }
        try {
            policy.requireValidity(endEntity);
        } catch (final TLSPolicy.TLSCheckFailedException e) {
            throw new UaException(StatusCodes.Bad_CertificateTimeInvalid, e.getMessage());
        }
        if (policy.config().keyUsage() != KeyUsageCheck.NONE) {
            CertificateValidationUtil.checkEndEntityKeyUsage(endEntity);
        }
        if (policy.config().keyUsage() == KeyUsageCheck.SERVER_AUTH) {
            // The remote end entity is a server, so its extended key usage must permit server
            // authentication rather than client authentication.
            CertificateValidationUtil.checkEndEntityExtendedKeyUsage(endEntity, false);
        }
    }

    private static @NotNull X509Certificate endEntityOf(final @NotNull List<X509Certificate> chain) throws UaException {
        if (chain.isEmpty()) {
            throw new UaException(StatusCodes.Bad_SecurityChecksFailed, "the server presented no certificate");
        }
        return chain.get(0);
    }

    /// The policy's material, offered through the interface Milo reads a trust list through.
    ///
    /// Read-only: every method that would add or remove material throws, because the material came
    /// from files an operator wrote and Edge never writes back to them.
    private static final class PolicyTrustListManager implements TrustListManager {

        private final @NotNull List<X509Certificate> certificates;
        private final @NotNull List<X509CRL> revocationLists;

        private PolicyTrustListManager(
                final @NotNull List<X509Certificate> certificates, final @NotNull List<X509CRL> revocationLists) {
            this.certificates = List.copyOf(certificates);
            this.revocationLists = List.copyOf(revocationLists);
        }

        @Override
        public @NotNull List<X509CRL> getIssuerCrls() {
            return revocationLists;
        }

        @Override
        public @NotNull List<X509CRL> getTrustedCrls() {
            return revocationLists;
        }

        @Override
        public @NotNull List<X509Certificate> getIssuerCertificates() {
            return certificates;
        }

        @Override
        public @NotNull List<X509Certificate> getTrustedCertificates() {
            return certificates;
        }

        @Override
        public @NotNull org.eclipse.milo.opcua.stack.core.types.builtin.DateTime getLastUpdateTime() {
            // The material is read once, before any connection, and never afterwards. Milo offers
            // this so a validator can notice a trust list that changed under it; nothing here
            // changes, so the answer is fixed.
            return org.eclipse.milo.opcua.stack.core.types.builtin.DateTime.MIN_VALUE;
        }

        @Override
        public void setIssuerCrls(final List<X509CRL> crls) {
            throw new UnsupportedOperationException("trust material is read from the configuration only");
        }

        @Override
        public void setTrustedCrls(final List<X509CRL> crls) {
            throw new UnsupportedOperationException("trust material is read from the configuration only");
        }

        @Override
        public void setIssuerCertificates(final List<X509Certificate> certificates) {
            throw new UnsupportedOperationException("trust material is read from the configuration only");
        }

        @Override
        public void setTrustedCertificates(final List<X509Certificate> certificates) {
            throw new UnsupportedOperationException("trust material is read from the configuration only");
        }

        @Override
        public void addIssuerCertificate(final X509Certificate certificate) {
            throw new UnsupportedOperationException("trust material is read from the configuration only");
        }

        @Override
        public void addTrustedCertificate(final X509Certificate certificate) {
            throw new UnsupportedOperationException("trust material is read from the configuration only");
        }

        @Override
        public boolean removeIssuerCertificate(
                final org.eclipse.milo.opcua.stack.core.types.builtin.ByteString thumbprint) {
            throw new UnsupportedOperationException("trust material is read from the configuration only");
        }

        @Override
        public boolean removeTrustedCertificate(
                final org.eclipse.milo.opcua.stack.core.types.builtin.ByteString thumbprint) {
            throw new UnsupportedOperationException("trust material is read from the configuration only");
        }
    }
}
