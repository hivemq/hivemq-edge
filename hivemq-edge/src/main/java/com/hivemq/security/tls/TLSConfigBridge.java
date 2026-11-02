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

import com.hivemq.configuration.entity.bridge.BridgeTlsChecksEntity;
import com.hivemq.configuration.entity.bridge.BridgeTlsEntity;
import com.hivemq.configuration.entity.listener.tls.KeystoreEntity;
import com.hivemq.configuration.entity.listener.tls.TruststoreEntity;
import com.hivemq.configuration.entity.tls.DirectoryEntity;
import com.hivemq.configuration.entity.tls.PatternEntity;
import com.hivemq.configuration.entity.tls.TlsAllowlistEntity;
import com.hivemq.configuration.entity.tls.TlsIdentityEntity;
import com.hivemq.configuration.entity.tls.TlsRevocationlistEntity;
import com.hivemq.configuration.entity.tls.TlsTruststoreEntity;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// What a bridge's TLS configuration amounts to, read from the bridge's own `tls` element.
///
/// Sits on top of the configuration layer and only reads from it. The configuration entities know
/// nothing of how a preset relates to the individual checks, which check defaults to what, or that
/// the two spellings exclude one another; to them a preset is a string and the checks are five
/// nullable fields. That ignorance is what makes a configuration layer which never modifies what it
/// read the natural thing to write, so reading a file and writing it out again produces the same
/// file.
///
/// A bridge has five checks, not six: MQTT sends no application identifier when it connects, so the
/// check that compares one has no input here.
///
/// Holds no mutable state, offers only getters, and modifies nothing. All of the resolving and all
/// of the refusing happens in the constructor, so an instance that exists is one Edge can carry out.
public final class TLSConfigBridge implements TLSConfig {

    private static final @NotNull Logger log = LoggerFactory.getLogger(TLSConfigBridge.class);

    /// What a bridge validated before any of this existed, and what one writing no checks still
    /// validates: a path built to the configured authorities, validity periods enforced, no
    /// revocation, no key usage, and hostname taken from `verify-hostname`.
    ///
    /// Named for why it exists rather than for what it checks, because no name of reasonable length
    /// carries five values. A new bridge should write a preset explicitly instead.
    public static final @NotNull String BRIDGE_DEFAULT_PRESET = "TLS_BRIDGE_DEFAULT";

    private final boolean enabled;
    private final @NotNull TLSConstants.TrustMode trustMode;
    private final @NotNull TLSConstants.HostnameCheck hostname;
    private final @NotNull TLSConstants.ValidityCheck validity;
    private final @NotNull TLSConstants.RevocationCheck revocation;
    private final @NotNull TLSConstants.KeyUsageCheck keyUsage;
    private final @NotNull Optional<MaterialRef> identity;
    private final @NotNull Optional<MaterialRef> truststore;
    private final @NotNull Optional<MaterialRef> allowList;
    private final @NotNull Optional<MaterialRef> revocationList;
    private final @NotNull List<String> protocols;
    private final @NotNull List<String> cipherSuites;
    private final @NotNull Duration handshakeTimeout;

    /// A bridge's TLS configuration built from the superseded elements alone, for a caller that has
    /// those values rather than a configuration element.
    ///
    /// The REST API is such a caller: its bridge model carries exactly the older fields - two store
    /// paths, their passwords, the protocols, the cipher suites, the handshake timeout and the
    /// hostname switch - and no new element. Deriving here rather than in the API keeps every
    /// compatibility rule in one place: the format guessed from the file name, the runtime's own
    /// authorities where no truststore is named, and the checks a bridge has always performed.
    ///
    /// The container format is not a parameter: this spelling has never had one, so it is guessed
    /// from the file name exactly as it is for a bridge read from a configuration file.
    @SuppressWarnings("checkstyle:ParameterNumber")
    public static @NotNull TLSConfigBridge fromSupersededSettings(
            final @NotNull String bridgeId,
            final @Nullable String keystorePath,
            final @Nullable String keystorePassword,
            final @Nullable String privateKeyPassword,
            final @Nullable String truststorePath,
            final @Nullable String truststorePassword,
            final @NotNull List<String> protocols,
            final @NotNull List<String> cipherSuites,
            final boolean verifyHostname,
            final int handshakeTimeoutSeconds)
            throws InvalidTlsConfigException {
        final BridgeTlsEntity entity = new BridgeTlsEntity();
        entity.setEnabled(true);
        entity.setProtocols(new ArrayList<>(protocols));
        entity.setCipherSuites(new ArrayList<>(cipherSuites));
        entity.setHandshakeTimeout(handshakeTimeoutSeconds);
        entity.setVerifyHostname(verifyHostname);
        if (present(keystorePath)) {
            final KeystoreEntity keystore = new KeystoreEntity();
            keystore.setPath(Objects.requireNonNull(keystorePath));
            keystore.setPassword(keystorePassword == null ? "" : keystorePassword);
            keystore.setPrivateKeyPassword(privateKeyPassword == null ? "" : privateKeyPassword);
            entity.setKeyStore(keystore);
        }
        if (present(truststorePath)) {
            final TruststoreEntity truststore = new TruststoreEntity();
            truststore.setPath(Objects.requireNonNull(truststorePath));
            truststore.setPassword(truststorePassword == null ? "" : truststorePassword);
            entity.setTrustStore(truststore);
        }
        return new TLSConfigBridge(bridgeId, entity);
    }

    /// Resolves a bridge's TLS configuration.
    ///
    /// @param bridgeId names the bridge in every refusal, since one unreadable bridge must not leave
    ///     an operator searching the file for which
    /// @throws InvalidTlsConfigException if the configuration cannot be carried out as written
    public TLSConfigBridge(final @NotNull String bridgeId, final @NotNull BridgeTlsEntity tls)
            throws InvalidTlsConfigException {

        refuseMixedSpellings(bridgeId, tls);

        final Checks checks = resolveChecks(bridgeId, tls);
        this.enabled = tls.isEnabled();
        this.trustMode = checks.trustMode();
        this.hostname = checks.hostname();
        this.validity = checks.validity();
        this.revocation = checks.revocation();
        this.keyUsage = checks.keyUsage();

        // Revocation is decided while a certification path is built, and no trust mode other than
        // CHAIN builds one. A check that quietly does nothing is worse than a refusal.
        if (this.revocation != TLSConstants.RevocationCheck.NONE && this.trustMode != TLSConstants.TrustMode.CHAIN) {
            throw new InvalidTlsConfigException(("Bridge '%s': revocation checking (revocation=%s) requires a "
                            + "certification path and cannot be performed with trust-mode=%s. Set revocation=NONE "
                            + "explicitly, or use trust-mode=CHAIN.")
                    .formatted(bridgeId, this.revocation, this.trustMode));
        }

        // Under ALLOW_LIST the fingerprints are what establishes trust, so their absence is not a
        // weaker configuration but one that cannot be carried out.
        if (this.trustMode == TLSConstants.TrustMode.ALLOW_LIST && tls.getTlsAllowlist() == null) {
            throw new InvalidTlsConfigException(("Bridge '%s': trust-mode=ALLOW_LIST requires an allow-list of "
                            + "permitted server-certificate fingerprints, but no 'tls-allowlist' is configured.")
                    .formatted(bridgeId));
        }

        if (namesSupersedingMaterial(tls)) {
            this.identity = inlineIdentity(bridgeId, tls.getTlsIdentity());
            this.truststore = inlineTruststore(bridgeId, tls.getTlsTruststore(), this.trustMode, tls.isEnabled());
            this.allowList = inlineAllowList(bridgeId, tls.getTlsAllowlist());
            this.revocationList = inlineRevocationList(bridgeId, tls.getTlsRevocationlist());
        } else {
            this.identity = supersededIdentity(tls.getKeyStore());
            this.truststore = supersededTruststore(tls.getTrustStore(), this.trustMode, tls.isEnabled());
            // A bridge has never had either of these in the superseded spelling.
            this.allowList = Optional.empty();
            this.revocationList = Optional.empty();
        }

        this.protocols = List.copyOf(tls.getProtocols());
        this.cipherSuites = List.copyOf(tls.getCipherSuites());
        this.handshakeTimeout = Duration.ofSeconds(tls.getHandshakeTimeout());
    }

    // --- what the constructor worked out ----------------------------------------------------------

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public @NotNull TLSConstants.TrustMode trustMode() {
        return trustMode;
    }

    /// Always off. MQTT sends no application identifier when it connects, so there is nothing for
    /// this check to compare; a bridge cannot configure it and never performs it.
    @Override
    public @NotNull TLSConstants.SanUriCheck sanUri() {
        return TLSConstants.SanUriCheck.NONE;
    }

    @Override
    public @NotNull TLSConstants.HostnameCheck hostname() {
        return hostname;
    }

    @Override
    public @NotNull TLSConstants.ValidityCheck validity() {
        return validity;
    }

    @Override
    public @NotNull TLSConstants.RevocationCheck revocation() {
        return revocation;
    }

    @Override
    public @NotNull TLSConstants.KeyUsageCheck keyUsage() {
        return keyUsage;
    }

    @Override
    public @NotNull Optional<MaterialRef> identity() {
        return identity;
    }

    @Override
    public @NotNull Optional<MaterialRef> truststore() {
        return truststore;
    }

    @Override
    public @NotNull Optional<MaterialRef> allowList() {
        return allowList;
    }

    @Override
    public @NotNull Optional<MaterialRef> revocationList() {
        return revocationList;
    }

    @Override
    public @NotNull List<String> protocols() {
        return protocols;
    }

    @Override
    public @NotNull List<String> cipherSuites() {
        return cipherSuites;
    }

    @Override
    public @NotNull Duration handshakeTimeout() {
        return handshakeTimeout;
    }

    @Override
    public @NotNull String toString() {
        return "TLSConfigBridge[enabled=" + enabled + ", " + describeChecks() + "]";
    }

    // --- resolving the checks ---------------------------------------------------------------------

    /// The five resolved checks, before the constructor stores them.
    private record Checks(
            @NotNull TLSConstants.TrustMode trustMode,
            @NotNull TLSConstants.HostnameCheck hostname,
            @NotNull TLSConstants.ValidityCheck validity,
            @NotNull TLSConstants.RevocationCheck revocation,
            @NotNull TLSConstants.KeyUsageCheck keyUsage) {}

    private static @NotNull Checks resolveChecks(final @NotNull String bridgeId, final @NotNull BridgeTlsEntity tls)
            throws InvalidTlsConfigException {
        final String preset = tls.getTlsChecks();
        final BridgeTlsChecksEntity individual = tls.getTlsChecksFull();

        if (preset != null && individual != null) {
            throw new InvalidTlsConfigException(("Bridge '%s': both 'tls-checks' (preset %s) and 'tls-checks-full' "
                            + "are configured; they are mutually exclusive. Keep the preset for a named profile, or "
                            + "keep tls-checks-full for full control, but not both.")
                    .formatted(bridgeId, preset));
        }
        // verify-hostname is the superseded spelling of one check. Honouring it beside the elements
        // that set all five would leave an operator reading a line that may contradict them.
        if (tls.isVerifyHostnameWritten() && (preset != null || individual != null)) {
            throw new InvalidTlsConfigException(("Bridge '%s': 'verify-hostname' is configured together with %s. "
                            + "They both decide the hostname check, so Edge cannot tell which was meant. Remove "
                            + "'verify-hostname' and set the hostname check there instead.")
                    .formatted(bridgeId, preset != null ? "'tls-checks'" : "'tls-checks-full'"));
        }

        if (individual != null) {
            return fromIndividualChecks(bridgeId, individual);
        }
        if (preset != null) {
            return fromPreset(bridgeId, preset, tls.isVerifyHostname());
        }
        return fromPreset(bridgeId, BRIDGE_DEFAULT_PRESET, tls.isVerifyHostname());
    }

    /// Fills every omitted check with its strictest value, so that a forgotten entry yields more
    /// verification and never less.
    private static @NotNull Checks fromIndividualChecks(
            final @NotNull String bridgeId, final @NotNull BridgeTlsChecksEntity checks)
            throws InvalidTlsConfigException {
        return new Checks(
                parse(
                        bridgeId,
                        "trust-mode",
                        checks.getTrustMode(),
                        TLSConstants.TrustMode.class,
                        TLSConstants.TrustMode.values(),
                        TLSConstants.TrustMode.STRICTEST),
                parse(
                        bridgeId,
                        "hostname",
                        checks.getHostname(),
                        TLSConstants.HostnameCheck.class,
                        TLSConstants.HostnameCheck.values(),
                        TLSConstants.HostnameCheck.STRICTEST),
                parse(
                        bridgeId,
                        "validity",
                        checks.getValidity(),
                        TLSConstants.ValidityCheck.class,
                        TLSConstants.ValidityCheck.values(),
                        TLSConstants.ValidityCheck.STRICTEST),
                parse(
                        bridgeId,
                        "revocation",
                        checks.getRevocation(),
                        TLSConstants.RevocationCheck.class,
                        TLSConstants.RevocationCheck.values(),
                        TLSConstants.RevocationCheck.STRICTEST),
                parse(
                        bridgeId,
                        "key-usage",
                        checks.getKeyUsage(),
                        TLSConstants.KeyUsageCheck.class,
                        TLSConstants.KeyUsageCheck.values(),
                        TLSConstants.KeyUsageCheck.STRICTEST));
    }

    /// The preset table. A bridge has four presets plus the compatibility one; it omits
    /// `APPLICATION_URI`, which without the application-identifier check would say nothing that
    /// chain trust alone does not.
    private static @NotNull Checks fromPreset(
            final @NotNull String bridgeId, final @NotNull String preset, final boolean verifyHostname)
            throws InvalidTlsConfigException {
        return switch (preset.trim().toUpperCase(Locale.ROOT).replace("-", "_")) {
            case "TLS_BRIDGE_DEFAULT" ->
                new Checks(
                        TLSConstants.TrustMode.CHAIN,
                        verifyHostname ? TLSConstants.HostnameCheck.HOSTNAME : TLSConstants.HostnameCheck.NONE,
                        TLSConstants.ValidityCheck.NOT_BEFORE_OR_AFTER,
                        TLSConstants.RevocationCheck.NONE,
                        TLSConstants.KeyUsageCheck.NONE);
            // `hostname=NONE` is what the model's preset table gives STANDARD, and an adapter's
            // STANDARD sets the same. A preset name means one thing across Edge, so a bridge that
            // wants the hostname checked writes ALL or the checks individually.
            case "STANDARD" ->
                new Checks(
                        TLSConstants.TrustMode.CHAIN,
                        TLSConstants.HostnameCheck.NONE,
                        TLSConstants.ValidityCheck.NOT_BEFORE_OR_AFTER,
                        TLSConstants.RevocationCheck.REQUIRE_CRLS,
                        TLSConstants.KeyUsageCheck.NONE);
            case "ALL" ->
                new Checks(
                        TLSConstants.TrustMode.CHAIN,
                        TLSConstants.HostnameCheck.HOSTNAME,
                        TLSConstants.ValidityCheck.NOT_BEFORE_OR_AFTER,
                        TLSConstants.RevocationCheck.REQUIRE_CRLS,
                        TLSConstants.KeyUsageCheck.SERVER_AUTH);
            // No authority to chain to: trust comes from the fingerprint, and of the hygiene checks
            // only validity survives. A self-signer publishes no revocation lists and commonly omits
            // the key-usage extension, so those two would reject exactly what this preset is for.
            case "SELF_SIGNED" ->
                new Checks(
                        TLSConstants.TrustMode.ALLOW_LIST,
                        TLSConstants.HostnameCheck.HOSTNAME,
                        TLSConstants.ValidityCheck.NOT_BEFORE_OR_AFTER,
                        TLSConstants.RevocationCheck.NONE,
                        TLSConstants.KeyUsageCheck.NONE);
            case "NO_VERIFICATION" ->
                new Checks(
                        TLSConstants.TrustMode.ANY_CERT,
                        TLSConstants.HostnameCheck.NONE,
                        TLSConstants.ValidityCheck.NONE,
                        TLSConstants.RevocationCheck.NONE,
                        TLSConstants.KeyUsageCheck.NONE);
            default ->
                throw new InvalidTlsConfigException(("Bridge '%s': '%s' is not a certificate-validation preset a "
                                + "bridge has. Write STANDARD, ALL, SELF_SIGNED, NO_VERIFICATION or "
                                + "TLS_BRIDGE_DEFAULT, or use tls-checks-full to set the checks individually.")
                        .formatted(bridgeId, preset));
        };
    }

    /// One check value, matched case-insensitively and tolerating missing separators, with an
    /// unrecognised value refusing the bridge rather than resolving to a default that might check
    /// less than the operator asked for.
    private static <E extends Enum<E>> @NotNull E parse(
            final @NotNull String bridgeId,
            final @NotNull String element,
            final @Nullable String value,
            final @NotNull Class<E> type,
            final E @NotNull [] values,
            final @NotNull E whenOmitted)
            throws InvalidTlsConfigException {
        if (value == null || value.isBlank()) {
            return whenOmitted;
        }
        final String normalised = value.trim().replace("-", "").replace("_", "");
        for (final E candidate : values) {
            if (candidate.name().replace("_", "").equalsIgnoreCase(normalised)) {
                return candidate;
            }
        }
        final List<String> accepted = new ArrayList<>();
        for (final E candidate : values) {
            accepted.add(candidate.name());
        }
        throw new InvalidTlsConfigException(
                ("Bridge '%s': '%s' is not a value the '%s' check takes. Write one of " + "%s.")
                        .formatted(bridgeId, value, element, String.join(", ", accepted)));
    }

    // --- the two spellings ------------------------------------------------------------------------

    private static boolean namesSupersedingMaterial(final @NotNull BridgeTlsEntity tls) {
        return tls.getTlsIdentity() != null
                || tls.getTlsTruststore() != null
                || tls.getTlsAllowlist() != null
                || tls.getTlsRevocationlist() != null;
    }

    private static boolean namesSupersededMaterial(final @NotNull BridgeTlsEntity tls) {
        return tls.getKeyStore() != null || tls.getTrustStore() != null;
    }

    /// Refuses a configuration mixing the two spellings of the material elements.
    ///
    /// Not translatable rather than merely untidy: the superseding elements reach material the
    /// superseded ones cannot express, so there is no rule that could combine a `keystore` with a
    /// `tls-truststore` and still mean one thing.
    private static void refuseMixedSpellings(final @NotNull String bridgeId, final @NotNull BridgeTlsEntity tls)
            throws InvalidTlsConfigException {
        if (!namesSupersedingMaterial(tls) || !namesSupersededMaterial(tls)) {
            return;
        }
        throw new InvalidTlsConfigException(("Bridge '%s': the TLS material is configured in both spellings, %s and "
                        + "%s. They are mutually exclusive, because the tls-* elements reach material the older "
                        + "ones cannot express. Move the remaining older elements to their tls-* equivalents, or "
                        + "keep the older spelling throughout.")
                .formatted(bridgeId, namedSuperseded(tls), namedSuperseding(tls)));
    }

    private static @NotNull String namedSuperseded(final @NotNull BridgeTlsEntity tls) {
        final List<String> elements = new ArrayList<>();
        if (tls.getKeyStore() != null) {
            elements.add("keystore");
        }
        if (tls.getTrustStore() != null) {
            elements.add("truststore");
        }
        return String.join(", ", elements);
    }

    private static @NotNull String namedSuperseding(final @NotNull BridgeTlsEntity tls) {
        final List<String> elements = new ArrayList<>();
        if (tls.getTlsIdentity() != null) {
            elements.add("tls-identity");
        }
        if (tls.getTlsTruststore() != null) {
            elements.add("tls-truststore");
        }
        if (tls.getTlsAllowlist() != null) {
            elements.add("tls-allowlist");
        }
        if (tls.getTlsRevocationlist() != null) {
            elements.add("tls-revocationlist");
        }
        return String.join(", ", elements);
    }

    // --- naming the material, superseding spelling ------------------------------------------------

    private static @NotNull Optional<MaterialRef> inlineIdentity(
            final @NotNull String bridgeId, final @Nullable TlsIdentityEntity identity)
            throws InvalidTlsConfigException {
        if (identity == null) {
            return Optional.empty();
        }
        final Optional<MaterialRef> reference =
                referenceOf(bridgeId, "tls-identity", identity.getReferenceName(), identity.getReferenceUuid());
        if (reference.isPresent()) {
            return reference;
        }
        final List<String> files = new ArrayList<>();
        addIfPresent(files, identity.getFile());
        addIfPresent(files, identity.getKeyFile());
        addIfPresent(files, identity.getCertificateFile());
        addIfPresent(files, identity.getChainFile());
        if (files.isEmpty()) {
            return Optional.empty();
        }
        // A PEM key's password and a container key entry's password never both apply, so whichever
        // the operator wrote is the one that unlocks the key.
        return Optional.of(new MaterialRef.Inline(
                identity.getFormat(),
                new FileSet(files, List.of(), List.of(), List.of()),
                secretOrFile(bridgeId, "tls-identity", identity.getPassword(), identity.getPasswordFile()),
                identity.getKeyEntry(),
                secretOrFile(
                        bridgeId,
                        "tls-identity",
                        firstPresent(identity.getKeyEntryPassword(), identity.getKeyPassword()),
                        firstPresent(identity.getKeyEntryPasswordFile(), identity.getKeyPasswordFile()))));
    }

    private static @NotNull Optional<MaterialRef> inlineTruststore(
            final @NotNull String bridgeId,
            final @Nullable TlsTruststoreEntity truststore,
            final @NotNull TLSConstants.TrustMode trustMode,
            final boolean tlsEnabled)
            throws InvalidTlsConfigException {
        if (truststore == null) {
            return runtimeAuthoritiesWhereAPathIsBuilt(bridgeId, trustMode, tlsEnabled, "tls-truststore");
        }
        final Optional<MaterialRef> reference =
                referenceOf(bridgeId, "tls-truststore", truststore.getReferenceName(), truststore.getReferenceUuid());
        if (reference.isPresent()) {
            return reference;
        }
        final FileSet files = fileSetOf(
                truststore.getFiles(), truststore.getDirectories(), truststore.getPatterns(), truststore.getBuiltins());
        if (files.isEmpty()) {
            return runtimeAuthoritiesWhereAPathIsBuilt(bridgeId, trustMode, tlsEnabled, "tls-truststore");
        }
        return Optional.of(new MaterialRef.Inline(
                truststore.getFormat(),
                files,
                // An operator with no password writes skip-integrity-check instead; the absence of
                // both is what the reading stage refuses, not something to decide here.
                truststore.isSkipIntegrityCheck()
                        ? null
                        : secretOrFile(
                                bridgeId, "tls-truststore", truststore.getPassword(), truststore.getPasswordFile()),
                null,
                null));
    }

    private static @NotNull Optional<MaterialRef> inlineAllowList(
            final @NotNull String bridgeId, final @Nullable TlsAllowlistEntity allowList)
            throws InvalidTlsConfigException {
        if (allowList == null) {
            return Optional.empty();
        }
        final Optional<MaterialRef> reference =
                referenceOf(bridgeId, "tls-allowlist", allowList.getReferenceName(), allowList.getReferenceUuid());
        if (reference.isPresent()) {
            return reference;
        }
        final FileSet files =
                fileSetOf(allowList.getFiles(), allowList.getDirectories(), allowList.getPatterns(), null);
        return files.isEmpty() ? Optional.empty() : Optional.of(new MaterialRef.Inline(null, files, null, null, null));
    }

    private static @NotNull Optional<MaterialRef> inlineRevocationList(
            final @NotNull String bridgeId, final @Nullable TlsRevocationlistEntity revocationList)
            throws InvalidTlsConfigException {
        if (revocationList == null) {
            return Optional.empty();
        }
        final Optional<MaterialRef> reference = referenceOf(
                bridgeId, "tls-revocationlist", revocationList.getReferenceName(), revocationList.getReferenceUuid());
        if (reference.isPresent()) {
            return reference;
        }
        final FileSet files = fileSetOf(
                revocationList.getFiles(), revocationList.getDirectories(), revocationList.getPatterns(), null);
        return files.isEmpty() ? Optional.empty() : Optional.of(new MaterialRef.Inline(null, files, null, null, null));
    }

    // --- naming the material, superseded spelling -------------------------------------------------

    /// The bridge's own key store: one container, whose format is guessed from the file name, with
    /// the two passwords that open it. Exactly the reach this element always had.
    private static @NotNull Optional<MaterialRef> supersededIdentity(final @Nullable KeystoreEntity keystore) {
        if (keystore == null || keystore.getPath() == null || keystore.getPath().isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new MaterialRef.Inline(
                formatFromFileName(keystore.getPath()),
                FileSet.of(keystore.getPath()),
                secretOf(keystore.getPassword()),
                null,
                secretOf(keystore.getPrivateKeyPassword())));
    }

    private static @NotNull Optional<MaterialRef> supersededTruststore(
            final @Nullable TruststoreEntity truststore,
            final @NotNull TLSConstants.TrustMode trustMode,
            final boolean tlsEnabled) {
        if (truststore != null
                && truststore.getPath() != null
                && !truststore.getPath().isBlank()) {
            return Optional.of(new MaterialRef.Inline(
                    formatFromFileName(truststore.getPath()),
                    FileSet.of(truststore.getPath()),
                    secretOf(truststore.getPassword()),
                    null,
                    null));
        }
        return runtimeAuthoritiesWhereAPathIsBuilt(null, trustMode, tlsEnabled, "truststore");
    }

    /// The container format the superseded spelling guesses from the file name.
    ///
    /// A bridge has never had a format element, and the shipped implementation read `.p12` and
    /// `.pfx` as PKCS#12 and everything else as a Java key store. Carried across so that a
    /// configuration which works today keeps working, and made case-insensitive, which the shipped
    /// version was not: a store named `CERT.P12` was guessed as a Java key store and loaded only
    /// because the JVM's reader accepts PKCS#12 anyway. Preserving that quirk would buy nothing and
    /// would report the wrong format in an error.
    private static @NotNull String formatFromFileName(final @NotNull String path) {
        final String lower = path.toLowerCase(Locale.ROOT);
        return lower.endsWith(".p12") || lower.endsWith(".pfx") ? "p12" : "jks";
    }

    // --- shared ------------------------------------------------------------------------------------

    /// The runtime's own certificate authorities, where a certification path is to be built and the
    /// configuration names nothing to build it to.
    ///
    /// A bridge configuring no truststore got no trust manager at all, which the JDK reads as "use
    /// the runtime's own authorities" - so every bridge to a cloud broker relies on this. Absent
    /// material trusting nothing is the better rule and is what the model says, but applying it here
    /// would stop those bridges connecting.
    ///
    /// Resolved at this stage rather than deeper down, which is what makes it visible: what comes out
    /// names `JVM-cacerts` exactly as an operator would write it, so the stage that reads files sees
    /// no special case.
    private static @NotNull Optional<MaterialRef> runtimeAuthoritiesWhereAPathIsBuilt(
            final @Nullable String bridgeId,
            final @NotNull TLSConstants.TrustMode trustMode,
            final boolean tlsEnabled,
            final @NotNull String element) {
        if (trustMode != TLSConstants.TrustMode.CHAIN) {
            return Optional.empty();
        }
        if (tlsEnabled) {
            log.warn(
                    "Bridge '{}': the certificate validation builds a certification path but no truststore is "
                            + "configured, so the certificate authorities the runtime ships with are trusted. Name "
                            + "them explicitly with <{}><builtin>JVM-cacerts</builtin></{}> to keep this behaviour, "
                            + "or point <{}> at the authority that signed the broker's certificate.",
                    bridgeId == null ? "" : bridgeId,
                    element,
                    element,
                    element);
        }
        return Optional.of(new MaterialRef.Inline(null, FileSet.ofJvmCacerts(), null, null, null));
    }

    /// A reference into shared material, by name or by uuid, refusing both at once.
    private static @NotNull Optional<MaterialRef> referenceOf(
            final @NotNull String bridgeId,
            final @NotNull String element,
            final @Nullable String referenceName,
            final @Nullable String referenceUuid)
            throws InvalidTlsConfigException {
        final boolean byName = present(referenceName);
        final boolean byUuid = present(referenceUuid);
        if (byName && byUuid) {
            throw new InvalidTlsConfigException(("Bridge '%s': the '%s' element references material both by name "
                            + "('%s') and by uuid ('%s'). They can name different material, so Edge cannot tell "
                            + "which was meant. Keep one of them.")
                    .formatted(bridgeId, element, referenceName, referenceUuid));
        }
        if (byUuid) {
            // Taken as written. What it names, and whether that is material of the right type, is
            // settled when the reference is resolved against the declared material.
            return Optional.of(new MaterialRef.ByUuid(Objects.requireNonNull(referenceUuid)));
        }
        if (!byName) {
            return Optional.empty();
        }
        try {
            return Optional.of(TLSMaterialRegistry.parseReference(Objects.requireNonNull(referenceName)));
        } catch (final TLSMaterialRegistry.InvalidReferenceException e) {
            throw new InvalidTlsConfigException(("Bridge '%s': the '%s' element's reference-name is not usable. %s")
                    .formatted(bridgeId, element, e.reason()));
        }
    }

    private static @NotNull FileSet fileSetOf(
            final @Nullable List<String> files,
            final @Nullable List<DirectoryEntity> directories,
            final @Nullable List<PatternEntity> patterns,
            final @Nullable List<String> builtins) {
        final List<String> named = new ArrayList<>();
        if (files != null) {
            for (final String file : files) {
                addIfPresent(named, file);
            }
        }
        final List<FileSet.Directory> asDirectories = new ArrayList<>();
        if (directories != null) {
            for (final DirectoryEntity directory : directories) {
                if (present(directory.getLocation())) {
                    asDirectories.add(new FileSet.Directory(directory.getLocation(), directory.isMayBeEmpty()));
                }
            }
        }
        final List<FileSet.Pattern> asPatterns = new ArrayList<>();
        if (patterns != null) {
            for (final PatternEntity pattern : patterns) {
                if (present(pattern.getLocation())) {
                    asPatterns.add(new FileSet.Pattern(pattern.getLocation(), pattern.isMayBeEmpty()));
                }
            }
        }
        final List<String> runtimeStores = new ArrayList<>();
        if (builtins != null) {
            for (final String builtin : builtins) {
                addIfPresent(runtimeStores, builtin);
            }
        }
        return new FileSet(named, asDirectories, asPatterns, runtimeStores);
    }

    /// A password as written or as a file to read it from, refusing both at once: they could
    /// disagree, and picking one silently would leave an operator reading the wrong line to learn
    /// why a key did not open.
    private static @Nullable Secret secretOrFile(
            final @NotNull String bridgeId,
            final @NotNull String element,
            final @Nullable String literal,
            final @Nullable String file)
            throws InvalidTlsConfigException {
        if (present(literal) && present(file)) {
            throw new InvalidTlsConfigException(("Bridge '%s': the '%s' element names both a password and a "
                            + "password file. They could disagree, so Edge cannot tell which was meant. Keep one.")
                    .formatted(bridgeId, element));
        }
        if (present(literal)) {
            return new Secret.Literal(Objects.requireNonNull(literal));
        }
        if (present(file)) {
            return new Secret.FromFile(Objects.requireNonNull(file));
        }
        return null;
    }

    private static @Nullable Secret secretOf(final @Nullable String password) {
        return present(password) ? new Secret.Literal(Objects.requireNonNull(password)) : null;
    }

    private static void addIfPresent(final @NotNull List<String> into, final @Nullable String value) {
        if (present(value)) {
            into.add(Objects.requireNonNull(value));
        }
    }

    private static @Nullable String firstPresent(final @Nullable String first, final @Nullable String second) {
        return present(first) ? first : second;
    }

    /// Whether an element carries a value at all. Blank counts as absent: a present element with an
    /// empty value would otherwise read as material that was named.
    private static boolean present(final @Nullable String value) {
        return value != null && !value.isBlank();
    }

    /// A configuration that cannot be carried out as written. The message names the bridge, the
    /// element and the fix, because it reaches an operator as the reason a bridge did not start.
    public static final class InvalidTlsConfigException extends Exception {

        private static final long serialVersionUID = 1L;

        private final @NotNull String reason;

        public InvalidTlsConfigException(final @NotNull String reason) {
            super(reason);
            this.reason = reason;
        }

        public @NotNull String reason() {
            return reason;
        }
    }
}
