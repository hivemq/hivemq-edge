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

import com.hivemq.security.tls.FileSet;
import com.hivemq.security.tls.MaterialRef;
import com.hivemq.security.tls.Secret;
import com.hivemq.security.tls.TLSConfig;
import com.hivemq.security.tls.TLSConstants;
import com.hivemq.security.tls.TLSMaterialRegistry;
import java.lang.reflect.RecordComponent;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// What an OPC UA adapter's TLS configuration amounts to, read from the adapter's own [Tls] record.
///
/// Sits on top of the configuration layer and reads from it. The configuration records below know
/// nothing of how a preset relates to the elementary checks, which check defaults to what, or that
/// the two spellings exclude one another - to them a preset is a value and the elementary checks are
/// a record with six nullable fields. That ignorance is deliberate: it makes a configuration layer
/// that never modifies what it read the natural thing to write rather than a rule to remember, and
/// reading a file and writing it out again therefore produces the same file.
///
/// This class holds no mutable state, offers only getters, and modifies nothing - not its own
/// fields, which are final, and not the configuration it was handed, which it only reads. Two
/// instances built from the same configuration are indistinguishable.
///
/// All of the resolving and all of the refusing happens in the constructor, so an instance that
/// exists is one Edge can carry out. The getters return what the constructor worked out.
public final class TLSConfigOpcUA implements TLSConfig {

    private static final @NotNull Logger log = LoggerFactory.getLogger(TLSConfigOpcUA.class);

    /// The preset an adapter runs under when it names neither a preset nor the elementary checks.
    ///
    /// Not the strictest value, and deliberately not: it is what this surface validated before the
    /// elementary checks existed, so an untouched configuration keeps the behaviour it shipped with.
    public static final @NotNull TlsChecks DEFAULT_PRESET = TlsChecks.STANDARD;

    /// The container format this surface's key store and truststore are read as.
    ///
    /// This surface's configuration has no format element, and the shipped implementation read both
    /// as JKS, so that is what they are. The JDK's JKS reader also accepts a PKCS#12 file, which is
    /// why deployments holding one have always worked.
    private static final @NotNull String CONTAINER_FORMAT = "jks";

    private final boolean enabled;
    private final @NotNull TLSConstants.TrustMode trustMode;
    private final @NotNull TLSConstants.SanUriCheck sanUri;
    private final @NotNull TLSConstants.HostnameCheck hostname;
    private final @NotNull TLSConstants.ValidityCheck validity;
    private final @NotNull TLSConstants.RevocationCheck revocation;
    private final @NotNull TLSConstants.KeyUsageCheck keyUsage;
    private final @NotNull Optional<MaterialRef> identity;
    private final @NotNull Optional<MaterialRef> truststore;
    private final @NotNull Optional<MaterialRef> allowList;
    private final @NotNull Optional<MaterialRef> revocationList;

    /// Resolves an adapter's TLS configuration.
    ///
    /// Refuses rather than guesses, in this order: settings that could not be read at all, an
    /// element carrying an entry that is not one of its settings, both spellings of the checks at
    /// once, a revocation check that no certification path could satisfy, and fingerprint trust with
    /// no fingerprints to trust.
    ///
    /// @throws InvalidTlsConfigException if the configuration cannot be carried out as written
    public TLSConfigOpcUA(final @NotNull Tls tls) throws InvalidTlsConfigException {

        final TlsChecks preset = tls.tlsChecks();
        final TlsChecksFull elementary = tls.tlsChecksFull();

        // Checked before anything else: nothing useful can be said about settings that could not be
        // read, and this message names the actual mistake. Edge's XML-to-map conversion collapses a
        // nested element to its text whenever the first child element is empty, so
        // <trustMode></trustMode> turns the whole element into a bare token. Which check each value
        // belonged to is then unrecoverable, and resolving silently to maximum validation would
        // discard a setting the operator wrote with nothing but a log line to say so.
        if (elementary != null && elementary.collapsedText() != null) {
            throw new InvalidTlsConfigException("The 'tlsChecksFull' certificate-validation settings could not be "
                    .concat("read: they arrived as the text '%s' rather than as a set of checks, which happens when "
                            .formatted(elementary.collapsedText()))
                    .concat("the first element is left empty (for example <trustMode></trustMode>). Which check each ")
                    .concat("value belonged to cannot be recovered, so no validation setting has been applied. Give ")
                    .concat("every element a value or remove it entirely. An empty <tlsChecksFull/> is valid and ")
                    .concat("means maximum validation."));
        }

        // A setting the model does not know cannot be applied, and here that is never harmless: the
        // application-wide handling for unknown adapter settings warns and drops, which for a
        // misspelled 'tlsChecks' would silently run the adapter under the default preset instead of
        // whatever the entry was meant to select. Checked before the both-spellings error so that an
        // operator is sent to repair the misspelled entry rather than to delete a valid one.
        refuseUnknownSettings("tls", tls.unknownSettings(), Tls.class);
        if (elementary != null) {
            refuseUnknownSettings("tlsChecksFull", elementary.unknownSettings(), TlsChecksFull.class);
        }
        if (tls.allowList() != null) {
            refuseUnknownSettings("allowList", tls.allowList().unknownSettings(), AllowList.class);
        }
        if (tls.revocationList() != null) {
            refuseUnknownSettings("revocationList", tls.revocationList().unknownSettings(), RevocationList.class);
        }
        if (tls.tlsIdentity() != null) {
            refuseUnknownSettings("tls-identity", tls.tlsIdentity().unknownSettings(), TlsIdentity.class);
        }
        if (tls.tlsTruststore() != null) {
            refuseUnknownSettings("tls-truststore", tls.tlsTruststore().unknownSettings(), TlsTruststore.class);
        }
        if (tls.tlsAllowlist() != null) {
            refuseUnknownSettings("tls-allowlist", tls.tlsAllowlist().unknownSettings(), TlsFileList.class);
        }
        if (tls.tlsRevocationlist() != null) {
            refuseUnknownSettings("tls-revocationlist", tls.tlsRevocationlist().unknownSettings(), TlsFileList.class);
        }

        // Two spellings of the same six checks. Honouring one and ignoring the other would leave an
        // operator reading a setting that does nothing.
        if (preset != null && elementary != null) {
            throw new InvalidTlsConfigException("Both 'tlsChecks' (preset "
                    + preset
                    + ") and 'tlsChecksFull' are configured; they are mutually exclusive. Keep the preset for a "
                    + "named profile, or keep tlsChecksFull for full control, but not both.");
        }

        final Checks checks = elementary != null
                ? fromElementaryChecks(elementary)
                : fromPreset(preset == null ? DEFAULT_PRESET : preset);

        this.enabled = tls.enabled();
        this.trustMode = checks.trustMode();
        this.sanUri = checks.sanUri();
        this.hostname = checks.hostname();
        this.validity = checks.validity();
        this.revocation = checks.revocation();
        this.keyUsage = checks.keyUsage();

        // Revocation is decided while a certification path is built, and no trust mode other than
        // CHAIN builds one. A check that quietly does nothing is worse than a refusal.
        if (this.revocation != TLSConstants.RevocationCheck.NONE && this.trustMode != TLSConstants.TrustMode.CHAIN) {
            throw new InvalidTlsConfigException("Revocation checking (revocation=%s) requires a certification path "
                    .formatted(this.revocation)
                    .concat("and cannot be performed with trustMode=%s. Set revocation=NONE explicitly, or use "
                            .formatted(this.trustMode))
                    .concat("trustMode=CHAIN."));
        }

        // Under ALLOW_LIST the fingerprints are what establishes trust, so their absence is not a
        // weaker configuration but one that cannot be carried out. Enforced through the same method
        // callers use to retrieve the path, so the requirement and the accessor cannot drift apart.
        if (this.trustMode == TLSConstants.TrustMode.ALLOW_LIST) {
            requireAllowList(tls);
        }

        // Two spellings of the same four pieces of material. Honouring one and ignoring the other
        // would leave an operator reading elements that do nothing, and the superseding spelling
        // reaches material the superseded one cannot express, so a mixture is not translatable
        // either. Refused for the same reason the two spellings of the checks refuse each other.
        refuseMixedSpellings(tls);

        if (namesSupersedingMaterial(tls)) {
            this.identity = identityOf(tls.tlsIdentity());
            this.truststore = truststoreOf(tls.tlsTruststore(), this.trustMode, tls.enabled());
            this.allowList = fileListOf("tls-allowlist", tls.tlsAllowlist());
            this.revocationList = fileListOf("tls-revocationlist", tls.tlsRevocationlist());
        } else {
            this.identity = identityOf(tls.keystore());
            this.truststore = truststoreNeedingAFallback(tls.truststore(), this.trustMode, tls.enabled());
            // An allow-list holds one hash per line and a revocation list is read as either encoding
            // by the same call, so neither declares a format.
            this.allowList =
                    fileOf(tls.allowList() == null ? null : tls.allowList().path());
            this.revocationList = fileOrDirectoryOf(
                    tls.revocationList() == null ? null : tls.revocationList().path());
        }
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

    @Override
    public @NotNull TLSConstants.SanUriCheck sanUri() {
        return sanUri;
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

    /// An OPC UA connection negotiates its tunnel through the OPC UA stack, which Edge does not
    /// configure per endpoint, so these three carry nothing on this surface. A bridge fills them.
    @Override
    public @NotNull List<String> protocols() {
        return List.of();
    }

    @Override
    public @NotNull List<String> cipherSuites() {
        return List.of();
    }

    @Override
    public @NotNull Duration handshakeTimeout() {
        return Duration.ZERO;
    }

    @Override
    public @NotNull String toString() {
        return "TLSConfigOpcUA[enabled=" + enabled + ", " + describeChecks() + "]";
    }

    // --- resolving the checks ---------------------------------------------------------------------

    /// The six resolved checks, before the constructor stores them in its fields.
    private record Checks(
            @NotNull TLSConstants.TrustMode trustMode,
            @NotNull TLSConstants.SanUriCheck sanUri,
            @NotNull TLSConstants.HostnameCheck hostname,
            @NotNull TLSConstants.ValidityCheck validity,
            @NotNull TLSConstants.RevocationCheck revocation,
            @NotNull TLSConstants.KeyUsageCheck keyUsage) {}

    /// Fills every omitted elementary check with its strictest value, so that a forgotten entry
    /// yields more verification and never less.
    private static @NotNull Checks fromElementaryChecks(final @NotNull TlsChecksFull elementary) {
        return new Checks(
                elementary.trustMode() == null ? TLSConstants.TrustMode.STRICTEST : shared(elementary.trustMode()),
                elementary.sanUri() == null ? TLSConstants.SanUriCheck.STRICTEST : shared(elementary.sanUri()),
                elementary.hostname() == null ? TLSConstants.HostnameCheck.STRICTEST : shared(elementary.hostname()),
                elementary.validity() == null ? TLSConstants.ValidityCheck.STRICTEST : shared(elementary.validity()),
                elementary.revocation() == null
                        ? TLSConstants.RevocationCheck.STRICTEST
                        : shared(elementary.revocation()),
                elementary.keyUsage() == null ? TLSConstants.KeyUsageCheck.STRICTEST : shared(elementary.keyUsage()));
    }

    /// The preset table. Each row is an exact combination of the six checks, taken from the shipped
    /// implementation: changing a row changes what existing deployments validate.
    private static @NotNull Checks fromPreset(final @NotNull TlsChecks preset) {
        return switch (preset) {
            case NONE ->
                new Checks(
                        TLSConstants.TrustMode.CHAIN,
                        TLSConstants.SanUriCheck.NONE,
                        TLSConstants.HostnameCheck.NONE,
                        TLSConstants.ValidityCheck.NONE,
                        TLSConstants.RevocationCheck.NONE,
                        TLSConstants.KeyUsageCheck.NONE);
            case APPLICATION_URI ->
                new Checks(
                        TLSConstants.TrustMode.CHAIN,
                        TLSConstants.SanUriCheck.APPLICATION_URI,
                        TLSConstants.HostnameCheck.NONE,
                        TLSConstants.ValidityCheck.NONE,
                        TLSConstants.RevocationCheck.NONE,
                        TLSConstants.KeyUsageCheck.NONE);
            case STANDARD ->
                new Checks(
                        TLSConstants.TrustMode.CHAIN,
                        TLSConstants.SanUriCheck.APPLICATION_URI,
                        TLSConstants.HostnameCheck.NONE,
                        TLSConstants.ValidityCheck.NOT_BEFORE_OR_AFTER,
                        TLSConstants.RevocationCheck.REQUIRE_CRLS,
                        TLSConstants.KeyUsageCheck.NONE);
            case ALL ->
                new Checks(
                        TLSConstants.TrustMode.CHAIN,
                        TLSConstants.SanUriCheck.APPLICATION_URI,
                        TLSConstants.HostnameCheck.HOSTNAME,
                        TLSConstants.ValidityCheck.NOT_BEFORE_OR_AFTER,
                        TLSConstants.RevocationCheck.REQUIRE_CRLS,
                        TLSConstants.KeyUsageCheck.SERVER_AUTH);
            // No authority to chain to: trust comes from the fingerprint, both identity checks stay
            // on, and of the three hygiene checks only validity survives. A self-signer publishes no
            // revocation lists and commonly omits the KeyUsage extension, so those two would reject
            // exactly the certificates this preset exists for.
            case SELF_SIGNED ->
                new Checks(
                        TLSConstants.TrustMode.ALLOW_LIST,
                        TLSConstants.SanUriCheck.APPLICATION_URI,
                        TLSConstants.HostnameCheck.HOSTNAME,
                        TLSConstants.ValidityCheck.NOT_BEFORE_OR_AFTER,
                        TLSConstants.RevocationCheck.NONE,
                        TLSConstants.KeyUsageCheck.NONE);
            case NO_VERIFICATION ->
                new Checks(
                        TLSConstants.TrustMode.ANY_CERT,
                        TLSConstants.SanUriCheck.NONE,
                        TLSConstants.HostnameCheck.NONE,
                        TLSConstants.ValidityCheck.NONE,
                        TLSConstants.RevocationCheck.NONE,
                        TLSConstants.KeyUsageCheck.NONE);
        };
    }

    // --- this module's enums onto the shared ones -------------------------------------------------
    // The configuration layer has its own enums, bound to this adapter's JSON schema; the shared
    // package has the same constants for every surface. Each mapping is exhaustive by construction,
    // so a constant added to one side and not the other breaks these switches rather than silently
    // picking a value.

    private static @NotNull TLSConstants.TrustMode shared(final @NotNull TrustMode mode) {
        return switch (mode) {
            case CHAIN -> TLSConstants.TrustMode.CHAIN;
            case ALLOW_LIST -> TLSConstants.TrustMode.ALLOW_LIST;
            case ANY_CERT -> TLSConstants.TrustMode.ANY_CERT;
        };
    }

    private static @NotNull TLSConstants.SanUriCheck shared(final @NotNull SanUriCheck check) {
        return switch (check) {
            case NONE -> TLSConstants.SanUriCheck.NONE;
            case APPLICATION_URI -> TLSConstants.SanUriCheck.APPLICATION_URI;
        };
    }

    private static @NotNull TLSConstants.HostnameCheck shared(final @NotNull HostnameCheck check) {
        return switch (check) {
            case NONE -> TLSConstants.HostnameCheck.NONE;
            case HOSTNAME -> TLSConstants.HostnameCheck.HOSTNAME;
        };
    }

    private static @NotNull TLSConstants.ValidityCheck shared(final @NotNull ValidityCheck check) {
        return switch (check) {
            case NONE -> TLSConstants.ValidityCheck.NONE;
            case NOT_BEFORE_OR_AFTER -> TLSConstants.ValidityCheck.NOT_BEFORE_OR_AFTER;
        };
    }

    private static @NotNull TLSConstants.RevocationCheck shared(final @NotNull RevocationCheck check) {
        return switch (check) {
            case NONE -> TLSConstants.RevocationCheck.NONE;
            case CHECK -> TLSConstants.RevocationCheck.CHECK;
            case REQUIRE_CRLS -> TLSConstants.RevocationCheck.REQUIRE_CRLS;
        };
    }

    private static @NotNull TLSConstants.KeyUsageCheck shared(final @NotNull KeyUsageCheck check) {
        return switch (check) {
            case NONE -> TLSConstants.KeyUsageCheck.NONE;
            case KEY_USAGE -> TLSConstants.KeyUsageCheck.KEY_USAGE;
            case SERVER_AUTH -> TLSConstants.KeyUsageCheck.SERVER_AUTH;
        };
    }

    // --- naming the material ----------------------------------------------------------------------

    // --- the two spellings ------------------------------------------------------------------------

    /// Whether the configuration names material in the superseding spelling.
    private static boolean namesSupersedingMaterial(final @NotNull Tls tls) {
        return tls.tlsIdentity() != null
                || tls.tlsTruststore() != null
                || tls.tlsAllowlist() != null
                || tls.tlsRevocationlist() != null;
    }

    /// Whether the configuration names material in the superseded spelling.
    private static boolean namesSupersededMaterial(final @NotNull Tls tls) {
        return tls.keystore() != null
                || tls.truststore() != null
                || tls.allowList() != null
                || tls.revocationList() != null;
    }

    /// Refuses a configuration mixing the two spellings of the material elements.
    ///
    /// Not translatable rather than merely untidy: the superseding elements reach material the
    /// superseded ones cannot express - PEM, several files, a directory, a reference to shared
    /// material - so there is no rule that could combine a `keystore` with a `tls-truststore` and
    /// still mean one thing. Deciding a precedence would leave an operator reading elements that do
    /// nothing, on the settings that decide which certificates Edge trusts.
    private static void refuseMixedSpellings(final @NotNull Tls tls) throws InvalidTlsConfigException {
        if (!namesSupersedingMaterial(tls) || !namesSupersededMaterial(tls)) {
            return;
        }
        throw new InvalidTlsConfigException(("The TLS material is configured in both spellings: %s and %s. They are "
                        + "mutually exclusive, because the tls-* elements reach material the older ones cannot "
                        + "express. Move the remaining older elements to their tls-* equivalents, or keep the "
                        + "older spelling throughout.")
                .formatted(named(tls, false), named(tls, true)));
    }

    /// The material elements of one spelling that the configuration actually names, for a refusal
    /// that quotes what an operator wrote rather than what they might have written.
    private static @NotNull String named(final @NotNull Tls tls, final boolean superseding) {
        final List<String> elements = new java.util.ArrayList<>();
        if (superseding) {
            if (tls.tlsIdentity() != null) {
                elements.add("tls-identity");
            }
            if (tls.tlsTruststore() != null) {
                elements.add("tls-truststore");
            }
            if (tls.tlsAllowlist() != null) {
                elements.add("tls-allowlist");
            }
            if (tls.tlsRevocationlist() != null) {
                elements.add("tls-revocationlist");
            }
        } else {
            if (tls.keystore() != null) {
                elements.add("keystore");
            }
            if (tls.truststore() != null) {
                elements.add("truststore");
            }
            if (tls.allowList() != null) {
                elements.add("allowList");
            }
            if (tls.revocationList() != null) {
                elements.add("revocationList");
            }
        }
        return String.join(", ", elements);
    }

    // --- naming the material, superseding spelling ------------------------------------------------

    /// This surface's client identity, from the superseding spelling.
    private static @NotNull Optional<MaterialRef> identityOf(final @Nullable TlsIdentity identity)
            throws InvalidTlsConfigException {
        if (identity == null) {
            return Optional.empty();
        }
        final List<String> files = new java.util.ArrayList<>();
        addIfPresent(files, identity.file());
        addIfPresent(files, identity.keyFile());
        addIfPresent(files, identity.certificateFile());
        addIfPresent(files, identity.chainFile());

        final Optional<MaterialRef> reference =
                referenceOf("tls-identity", identity.referenceName(), identity.referenceUuid(), !files.isEmpty());
        if (reference.isPresent()) {
            return reference;
        }
        if (files.isEmpty()) {
            return Optional.empty();
        }
        // A PEM identity's key password and a container key entry's password never both apply, so
        // whichever the operator wrote is the one that unlocks the key.
        return Optional.of(new MaterialRef.Inline(
                identity.format() == null ? null : identity.format().asDeclared(),
                new FileSet(files, List.of(), List.of(), List.of()),
                secretOrFile("tls-identity", identity.password(), identity.passwordFile()),
                identity.keyEntry(),
                secretOrFile(
                        "tls-identity",
                        firstPresent(identity.keyEntryPassword(), identity.keyPassword()),
                        firstPresent(identity.keyEntryPasswordFile(), identity.keyPasswordFile()))));
    }

    /// This surface's trust anchors, from the superseding spelling, with the same fallback to the
    /// runtime's own authorities that the superseded spelling gets.
    private static @NotNull Optional<MaterialRef> truststoreOf(
            final @Nullable TlsTruststore truststore,
            final @NotNull TLSConstants.TrustMode trustMode,
            final boolean tlsEnabled)
            throws InvalidTlsConfigException {
        if (truststore == null) {
            return fallbackWhereAPathIsBuilt(trustMode, tlsEnabled, "tls-truststore");
        }
        final FileSet files = fileSetOf(
                "tls-truststore",
                truststore.files(),
                truststore.directories(),
                truststore.patterns(),
                truststore.builtins());
        final Optional<MaterialRef> reference =
                referenceOf("tls-truststore", truststore.referenceName(), truststore.referenceUuid(), !files.isEmpty());
        if (reference.isPresent()) {
            return reference;
        }
        if (files.isEmpty()) {
            return fallbackWhereAPathIsBuilt(trustMode, tlsEnabled, "tls-truststore");
        }
        return Optional.of(new MaterialRef.Inline(
                truststore.format() == null ? null : truststore.format().asDeclared(),
                files,
                integrityOf(truststore, files),
                null,
                null));
    }

    /// The password a truststore is opened with, or the waiver an operator with none writes instead.
    ///
    /// The waiver is carried as itself: an absent password opens a container store as an empty one
    /// and so still checks it, and only the waiver opens one unchecked. A container named by path
    /// with neither is refused here, where the element can still be named; a PEM file has no
    /// integrity to check, and a set naming only the runtime's truststore reads nothing the
    /// operator supplied. Writing both a password and the waiver is refused: Edge could not tell
    /// whether the file is to be checked.
    private static @Nullable Secret integrityOf(final @NotNull TlsTruststore truststore, final @NotNull FileSet files)
            throws InvalidTlsConfigException {
        final Secret password = secretOrFile("tls-truststore", truststore.password(), truststore.passwordFile());
        if (truststore.isSkipIntegrityCheck()) {
            if (password != null) {
                throw new InvalidTlsConfigException("The 'tls-truststore' element names both a password and "
                        + "skip-integrity-check, so Edge cannot tell whether the file is to be checked. Keep the "
                        + "password to check it, or skip-integrity-check for a store that has none.");
            }
            return new Secret.Waived();
        }
        final MaterialFormat format = truststore.format();
        if (password == null && files.namesAPath() && (format == MaterialFormat.P12 || format == MaterialFormat.JKS)) {
            throw new InvalidTlsConfigException(("The 'tls-truststore' element names a %s container but neither a "
                            + "password nor skip-integrity-check. The password lets Edge confirm the file is the one "
                            + "somebody built; write it with <password> or <password-file>, or write "
                            + "<skip-integrity-check>true</skip-integrity-check> to read the store unchecked.")
                    .formatted(format.asDeclared()));
        }
        return password;
    }

    /// An allow-list or revocation list from the superseding spelling. One method for both, which the
    /// model gives the same three elements.
    private static @NotNull Optional<MaterialRef> fileListOf(
            final @NotNull String element, final @Nullable TlsFileList list) throws InvalidTlsConfigException {
        if (list == null) {
            return Optional.empty();
        }
        final FileSet files = fileSetOf(element, list.files(), list.directories(), list.patterns(), null);
        final Optional<MaterialRef> reference =
                referenceOf(element, list.referenceName(), list.referenceUuid(), !files.isEmpty());
        if (reference.isPresent()) {
            return reference;
        }
        if (files.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new MaterialRef.Inline(null, files, null, null, null));
    }

    /// The sources an element names.
    ///
    /// A `directory` or `pattern` without a `location`, and an empty `file` or `builtin`, is refused
    /// rather than skipped. Skipped, it would leave the element naming nothing, and a truststore naming
    /// nothing falls back to the runtime's own authorities: the entry lost while templating would
    /// widen what Edge trusts.
    private static @NotNull FileSet fileSetOf(
            final @NotNull String element,
            final @Nullable List<String> files,
            final @Nullable List<FileLocation> directories,
            final @Nullable List<FileLocation> patterns,
            final @Nullable List<String> builtins)
            throws InvalidTlsConfigException {
        final List<FileSet.Directory> asDirectories = new java.util.ArrayList<>();
        if (directories != null) {
            for (final FileLocation directory : directories) {
                asDirectories.add(
                        new FileSet.Directory(locationOf(element, "directory", directory), directory.isMayBeEmpty()));
            }
        }
        final List<FileSet.Pattern> asPatterns = new java.util.ArrayList<>();
        if (patterns != null) {
            for (final FileLocation pattern : patterns) {
                asPatterns.add(new FileSet.Pattern(locationOf(element, "pattern", pattern), pattern.isMayBeEmpty()));
            }
        }
        final List<String> named = new java.util.ArrayList<>();
        if (files != null) {
            for (final String file : files) {
                named.add(valueOf(element, "file", file));
            }
        }
        final List<String> runtimeStores = new java.util.ArrayList<>();
        if (builtins != null) {
            for (final String builtin : builtins) {
                runtimeStores.add(valueOf(element, "builtin", builtin));
            }
        }
        return new FileSet(named, asDirectories, asPatterns, runtimeStores);
    }

    private static @NotNull String locationOf(
            final @NotNull String element, final @NotNull String entry, final @Nullable FileLocation source)
            throws InvalidTlsConfigException {
        final String location = source == null ? null : source.location();
        if (!present(location)) {
            throw new InvalidTlsConfigException(("The '%s' element has a '%s' entry without a 'location'. Name the "
                            + "%s in <%s><%s><location>...</location></%s></%s>, or remove the entry.")
                    .formatted(element, entry, entry, element, entry, entry, element));
        }
        return Objects.requireNonNull(location);
    }

    private static @NotNull String valueOf(
            final @NotNull String element, final @NotNull String entry, final @Nullable String value)
            throws InvalidTlsConfigException {
        if (!present(value)) {
            throw new InvalidTlsConfigException(("The '%s' element has an empty '%s' entry. Name it in "
                            + "<%s><%s>...</%s></%s>, or remove the entry.")
                    .formatted(element, entry, element, entry, entry, element));
        }
        return Objects.requireNonNull(value);
    }

    /// The runtime's own authorities, where a certification path is to be built and nothing was named
    /// to build it to. See [#truststoreNeedingAFallback] for why this exists at all.
    private static @NotNull Optional<MaterialRef> fallbackWhereAPathIsBuilt(
            final @NotNull TLSConstants.TrustMode trustMode, final boolean tlsEnabled, final @NotNull String element) {
        if (trustMode != TLSConstants.TrustMode.CHAIN) {
            return Optional.empty();
        }
        if (tlsEnabled) {
            log.warn(
                    "The certificate validation builds a certification path but no truststore is configured, so "
                            + "the certificate authorities the runtime ships with are trusted. Name them explicitly "
                            + "with <{}><builtin>JVM-cacerts</builtin></{}> to keep this behaviour, point <{}> at "
                            + "the authority that signed the server's certificate, or use tlsChecks=SELF_SIGNED to "
                            + "trust the server by its fingerprint instead.",
                    element,
                    element,
                    element);
        }
        return Optional.of(new MaterialRef.Inline(null, FileSet.ofJvmCacerts(), null, null, null));
    }

    private static void addIfPresent(final @NotNull List<String> into, final @Nullable String value) {
        if (present(value)) {
            into.add(java.util.Objects.requireNonNull(value));
        }
    }

    private static @Nullable String firstPresent(final @Nullable String first, final @Nullable String second) {
        return present(first) ? first : second;
    }

    /// A password as written or as a file to read it from, refusing both at once: they could
    /// disagree, and picking one silently would leave an operator reading the wrong line to learn
    /// why a key did not open.
    private static @Nullable Secret secretOrFile(
            final @NotNull String element, final @Nullable String literal, final @Nullable String file)
            throws InvalidTlsConfigException {
        if (present(literal) && present(file)) {
            throw new InvalidTlsConfigException(("The '%s' element names both a password and a password file. They "
                            + "could disagree, so Edge cannot tell which one was meant. Keep one of them.")
                    .formatted(element));
        }
        if (present(literal)) {
            return new Secret.Literal(java.util.Objects.requireNonNull(literal));
        }
        if (present(file)) {
            return new Secret.FromFile(java.util.Objects.requireNonNull(file));
        }
        return null;
    }

    // --- naming the material, superseded spelling -------------------------------------------------

    /// This surface's client identity from the superseded `keystore`: one container, read as JKS,
    /// with the two passwords that open it. Exactly the reach this element always had.
    ///
    /// The path is checked for null although a store naming none is refused elsewhere: a `<keystore>`
    /// whose `<path>` child is missing binds null regardless.
    private static @NotNull Optional<MaterialRef> identityOf(final @Nullable Keystore keystore) {
        if (keystore == null) {
            return Optional.empty();
        }
        final String path = keystore.path();
        if (path == null || path.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new MaterialRef.Inline(
                CONTAINER_FORMAT,
                FileSet.of(path),
                secretOf(keystore.password()),
                null,
                secretOf(keystore.privateKeyPassword())));
    }

    /// This surface's trust anchors, falling back to the runtime's own authorities where a
    /// certification path is to be built and the configuration names nothing to build it to.
    ///
    /// The shipped implementation read the JVM's `cacerts` whenever no truststore was configured, so
    /// every deployment trusting a server with a certificate from a public authority relies on it.
    /// Absent material trusting nothing is the better rule and is what the model says, but applying
    /// it here would stop those deployments connecting, so the fallback stays and says so.
    ///
    /// Resolved at this stage rather than deeper down, which is what makes it visible: what comes out
    /// names `JVM-cacerts` exactly as an operator would write it, so the stage that reads files sees
    /// no special case and the policy cannot differ depending on how its anchors were chosen.
    ///
    /// Only where the trust mode builds a path. Fingerprint trust and accept-anything read no
    /// truststore, so substituting one there would log a warning about material nothing consults.
    private static @NotNull Optional<MaterialRef> truststoreNeedingAFallback(
            final @Nullable Truststore truststore,
            final @NotNull TLSConstants.TrustMode trustMode,
            final boolean tlsEnabled) {
        if (truststore != null) {
            final String path = truststore.path();
            if (path != null && !path.isBlank()) {
                return Optional.of(new MaterialRef.Inline(
                        CONTAINER_FORMAT, FileSet.of(path), secretOf(truststore.password()), null, null));
            }
        }
        return fallbackWhereAPathIsBuilt(trustMode, tlsEnabled, "truststore");
    }

    /// A file whose contents describe themselves, so no format is declared. The whole reach of the
    /// superseded `allowList`: one file, no directory, no pattern - which is all it ever read.
    private static @NotNull Optional<MaterialRef> fileOf(final @Nullable String path) {
        if (path == null || path.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new MaterialRef.Inline(null, FileSet.of(path), null, null, null));
    }

    /// The whole reach of the superseded `revocationList`: one path naming either a file of
    /// revocation lists or a directory of such files. Which of the two it is, is decided when the
    /// material is read, so this stage stays free of file access.
    private static @NotNull Optional<MaterialRef> fileOrDirectoryOf(final @Nullable String path) {
        if (path == null || path.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new MaterialRef.Inline(null, FileSet.ofLegacyPath(path), null, null, null));
    }

    /// A reference into shared material, by name or by uuid, refusing more than one way of naming it.
    ///
    /// Two elements rather than one told apart by shape. A uuid carries no `::`, so a combined
    /// element would have to guess, and a mistyped name missing its separator would be reported as a
    /// uuid that names nothing - pointing the operator at the wrong mistake.
    ///
    /// Any two at once is refused rather than resolved by precedence: they can name different
    /// material, and honouring one silently would leave an operator reading a line that does nothing
    /// - the same posture this class takes towards the two spellings of the checks.
    ///
    /// @param alreadyNamed whether the element also names the material directly
    private static @NotNull Optional<MaterialRef> referenceOf(
            final @NotNull String element,
            final @Nullable String referenceName,
            final @Nullable String referenceUuid,
            final boolean alreadyNamed)
            throws InvalidTlsConfigException {
        final boolean byName = present(referenceName);
        final boolean byUuid = present(referenceUuid);
        if (byName && byUuid) {
            throw new InvalidTlsConfigException(("The '%s' element references material both by name ('%s') and by "
                            + "uuid ('%s'). They can name different material, so Edge cannot tell which one was "
                            + "meant. Keep one of them.")
                    .formatted(element, referenceName, referenceUuid));
        }
        if (!byName && !byUuid) {
            return Optional.empty();
        }
        if (alreadyNamed) {
            throw new InvalidTlsConfigException(("The '%s' element names material directly and also references "
                            + "'%s' from a shared tls-material. They can name different material, so Edge cannot "
                            + "tell which one was meant. Keep the reference, or keep the material written here, "
                            + "but not both.")
                    .formatted(element, byName ? referenceName : referenceUuid));
        }
        if (byUuid) {
            // A uuid is taken as written. What it names, and whether that is material of the right
            // type, is settled when the reference is resolved against the declared material.
            return Optional.of(new MaterialRef.ByUuid(Objects.requireNonNull(referenceUuid)));
        }
        try {
            return Optional.of(TLSMaterialRegistry.parseReference(Objects.requireNonNull(referenceName)));
        } catch (final TLSMaterialRegistry.InvalidReferenceException e) {
            throw new InvalidTlsConfigException(
                    "The '%s' element's reference-name is not usable. %s".formatted(element, e.reason()));
        }
    }

    /// Whether an element carries a value at all. Blank counts as absent: a present element with an
    /// empty value would otherwise read as material that was named.
    private static boolean present(final @Nullable String value) {
        return value != null && !value.isBlank();
    }

    private static @Nullable Secret secretOf(final @Nullable String password) {
        return password == null ? null : new Secret.Literal(password);
    }

    // --- what trust by fingerprint requires -------------------------------------------------------

    /// The single definition of "an allow-list is configured", in either spelling.
    ///
    /// A blank value counts as absent: a present element with an empty path would otherwise fall
    /// through to trusting everything, while the configuration said an allow-list was in force.
    public static boolean hasAllowList(final @NotNull Tls tls) {
        final AllowList superseded = tls.allowList();
        if (superseded != null && present(superseded.path())) {
            return true;
        }
        final TlsFileList superseding = tls.tlsAllowlist();
        return superseding != null
                && (!superseding.namesNothing()
                        || present(superseding.referenceName())
                        || present(superseding.referenceUuid()));
    }

    /// Refuses fingerprint trust with no fingerprints to trust.
    ///
    /// Shares its presence test with [#hasAllowList], so the requirement and the accessor cannot
    /// drift apart.
    public static void requireAllowList(final @NotNull Tls tls) throws InvalidTlsConfigException {
        if (!hasAllowList(tls)) {
            throw new InvalidTlsConfigException("Trust mode ALLOW_LIST requires an allow-list of permitted "
                    + "server-certificate fingerprints, but none is configured. Add "
                    + "<tls-allowlist><file>...</file></tls-allowlist> to the TLS configuration, or reference "
                    + "one declared in a tls-material with "
                    + "<tls-allowlist><reference-name>material-name::entry-name</reference-name></tls-allowlist>.");
        }
    }

    /// The single definition of "a revocation list is configured", for the same reason as
    /// [#hasAllowList].
    ///
    /// Deliberately not turned into a start-up requirement when revocation is enforced: revocation
    /// is satisfied vacuously when no authority appears in the path, which is every direct-trust and
    /// self-signed deployment, so demanding a revocation list there would refuse configurations that
    /// work today - including the default preset.
    public static boolean hasRevocationList(final @NotNull Tls tls) {
        final RevocationList superseded = tls.revocationList();
        if (superseded != null && present(superseded.path())) {
            return true;
        }
        final TlsFileList superseding = tls.tlsRevocationlist();
        return superseding != null
                && (!superseding.namesNothing()
                        || present(superseding.referenceName())
                        || present(superseding.referenceUuid()));
    }

    /// Refuses an element carrying an entry that is not one of its settings.
    ///
    /// Names the settings the element does have, so that a misspelling is visible by comparison. The
    /// two internal record components that hold the trapped entries are not settings and are left
    /// out of that list.
    private static void refuseUnknownSettings(
            final @NotNull String element,
            final @NotNull Map<String, Object> unknownSettings,
            final @NotNull Class<? extends Record> type)
            throws InvalidTlsConfigException {
        if (unknownSettings.isEmpty()) {
            return;
        }
        final Set<String> internal = Set.of("collapsedText", "unknownSettings");
        final String known = Arrays.stream(type.getRecordComponents())
                .map(RecordComponent::getName)
                .filter(name -> !internal.contains(name))
                .collect(Collectors.joining(", "));
        final String unknown =
                unknownSettings.keySet().stream().map(name -> "'" + name + "'").collect(Collectors.joining(", "));
        throw new InvalidTlsConfigException("The certificate-validation element '%s' contains %s, which is not a "
                .formatted(element, unknown)
                .concat("setting it has. It cannot be applied, and running without it could mean weaker validation ")
                .concat("than was written, so the adapter is refused instead. Correct or remove the entry. Settings ")
                .concat("of '%s': %s.".formatted(element, known)));
    }

    /// A configuration that cannot be carried out as elementary. The message names the setting and the
    /// fix, because it reaches an operator in the adapter's start-up failure.
    public static final class InvalidTlsConfigException extends Exception {

        private static final long serialVersionUID = 1L;

        /// The message, non-null, so a caller can hand it on without a null check. `getMessage` is
        /// declared nullable on `Throwable` and the static analysis rejects passing it where
        /// non-null is required.
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
