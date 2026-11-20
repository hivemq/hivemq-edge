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
package com.hivemq.configuration.reader;

import com.hivemq.configuration.entity.tls.DirectoryEntity;
import com.hivemq.configuration.entity.tls.PatternEntity;
import com.hivemq.configuration.entity.tls.TlsAllowlistEntity;
import com.hivemq.configuration.entity.tls.TlsIdentityEntity;
import com.hivemq.configuration.entity.tls.TlsMaterialEntity;
import com.hivemq.configuration.entity.tls.TlsRevocationlistEntity;
import com.hivemq.configuration.entity.tls.TlsTruststoreEntity;
import com.hivemq.security.tls.FileSet;
import com.hivemq.security.tls.MaterialRef;
import com.hivemq.security.tls.Secret;
import com.hivemq.security.tls.TLSConfigBridge;
import com.hivemq.security.tls.TLSMaterialRegistry;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/// Turns the declared `tls-material` elements into the registry a reference resolves against.
///
/// A pure function on the configuration: it reads the entities and returns a registry, touching no
/// files and changing nothing it read. Whether the material a surface names actually exists is
/// settled here, before any surface starts, so an operator learns about a misspelled reference from
/// one message at start-up rather than from whichever surface connected first.
///
/// An entry inside a `tls-material` may not itself be a reference. A reference exists so a surface
/// can point at declared material; a declaration pointing at another declaration would be a chain
/// to follow, with a cycle to detect, for no case anybody has.
public final class TlsMaterialRegistryBuilder {

    private TlsMaterialRegistryBuilder() {}

    /// The registry the declared material forms.
    ///
    /// @throws TLSMaterialRegistry.InvalidReferenceException if a declaration is unnamed, is itself
    ///     a reference, or collides with another in a way a later lookup could not resolve
    public static @NotNull TLSMaterialRegistry registryOf(final @NotNull List<TlsMaterialEntity> materials)
            throws TLSMaterialRegistry.InvalidReferenceException {
        final TLSMaterialRegistry.Builder builder = new TLSMaterialRegistry.Builder();
        for (final TlsMaterialEntity material : materials) {
            final String name = builder.material(material.getName(), material.getUuid());
            for (final TlsIdentityEntity identity : material.getIdentities()) {
                refuseReference(
                        name,
                        TLSMaterialRegistry.Kind.IDENTITY,
                        identity.getName(),
                        identity.getReferenceName(),
                        identity.getReferenceUuid());
                builder.entry(
                        name,
                        TLSMaterialRegistry.Kind.IDENTITY,
                        identity.getName(),
                        identity.getUuid(),
                        inlineIdentity(name, identity));
            }
            for (final TlsTruststoreEntity truststore : material.getTruststores()) {
                refuseReference(
                        name,
                        TLSMaterialRegistry.Kind.TRUSTSTORE,
                        truststore.getName(),
                        truststore.getReferenceName(),
                        truststore.getReferenceUuid());
                builder.entry(
                        name,
                        TLSMaterialRegistry.Kind.TRUSTSTORE,
                        truststore.getName(),
                        truststore.getUuid(),
                        inlineTruststore(name, truststore));
            }
            for (final TlsAllowlistEntity allowlist : material.getAllowlists()) {
                refuseReference(
                        name,
                        TLSMaterialRegistry.Kind.ALLOW_LIST,
                        allowlist.getName(),
                        allowlist.getReferenceName(),
                        allowlist.getReferenceUuid());
                builder.entry(
                        name,
                        TLSMaterialRegistry.Kind.ALLOW_LIST,
                        allowlist.getName(),
                        allowlist.getUuid(),
                        new MaterialRef.Inline(
                                null,
                                namedSources(
                                        name,
                                        TLSMaterialRegistry.Kind.ALLOW_LIST,
                                        allowlist.getName(),
                                        fileSet(
                                                name,
                                                TLSMaterialRegistry.Kind.ALLOW_LIST,
                                                allowlist.getName(),
                                                allowlist.getFiles(),
                                                allowlist.getDirectories(),
                                                allowlist.getPatterns(),
                                                List.of())),
                                null,
                                null,
                                null));
            }
            for (final TlsRevocationlistEntity revocationlist : material.getRevocationlists()) {
                refuseReference(
                        name,
                        TLSMaterialRegistry.Kind.REVOCATION_LIST,
                        revocationlist.getName(),
                        revocationlist.getReferenceName(),
                        revocationlist.getReferenceUuid());
                builder.entry(
                        name,
                        TLSMaterialRegistry.Kind.REVOCATION_LIST,
                        revocationlist.getName(),
                        revocationlist.getUuid(),
                        new MaterialRef.Inline(
                                null,
                                namedSources(
                                        name,
                                        TLSMaterialRegistry.Kind.REVOCATION_LIST,
                                        revocationlist.getName(),
                                        fileSet(
                                                name,
                                                TLSMaterialRegistry.Kind.REVOCATION_LIST,
                                                revocationlist.getName(),
                                                revocationlist.getFiles(),
                                                revocationlist.getDirectories(),
                                                revocationlist.getPatterns(),
                                                List.of())),
                                null,
                                null,
                                null));
            }
        }
        return builder.build();
    }

    private static @NotNull MaterialRef.Inline inlineIdentity(
            final @NotNull String materialName, final @NotNull TlsIdentityEntity identity)
            throws TLSMaterialRegistry.InvalidReferenceException {
        // A container names one file; PEM names up to three, which merge into the same set because
        // reading them is one operation either way.
        final List<String> files = new ArrayList<>();
        addIfPresent(files, identity.getFile());
        addIfPresent(files, identity.getKeyFile());
        addIfPresent(files, identity.getCertificateFile());
        addIfPresent(files, identity.getChainFile());
        return new MaterialRef.Inline(
                identity.getFormat(),
                namedSources(
                        materialName,
                        TLSMaterialRegistry.Kind.IDENTITY,
                        identity.getName(),
                        new FileSet(files, List.of(), List.of(), List.of())),
                secret(identity.getPassword(), identity.getPasswordFile()),
                identity.getKeyEntry(),
                // A PEM key's password and a container key entry's password never both apply, so
                // whichever the operator wrote is the one that unlocks the key.
                secret(
                        firstPresent(identity.getKeyEntryPassword(), identity.getKeyPassword()),
                        firstPresent(identity.getKeyEntryPasswordFile(), identity.getKeyPasswordFile())));
    }

    private static @NotNull MaterialRef.Inline inlineTruststore(
            final @NotNull String materialName, final @NotNull TlsTruststoreEntity truststore)
            throws TLSMaterialRegistry.InvalidReferenceException {
        final FileSet files = namedSources(
                materialName,
                TLSMaterialRegistry.Kind.TRUSTSTORE,
                truststore.getName(),
                fileSet(
                        materialName,
                        TLSMaterialRegistry.Kind.TRUSTSTORE,
                        truststore.getName(),
                        truststore.getFiles(),
                        truststore.getDirectories(),
                        truststore.getPatterns(),
                        truststore.getBuiltins()));
        return new MaterialRef.Inline(
                truststore.getFormat(), files, integrityOf(materialName, truststore, files), null, null);
    }

    /// What opens a declared truststore: its password, or the operator's explicit waiver of the
    /// integrity check, kept distinct from an absent password because only the waiver opens a
    /// container unchecked. A container naming a path with neither, or a truststore naming both, is
    /// refused: the first leaves the decision to skip the check unwritten, the second leaves it
    /// unclear.
    private static @Nullable Secret integrityOf(
            final @NotNull String materialName,
            final @NotNull TlsTruststoreEntity truststore,
            final @NotNull FileSet files)
            throws TLSMaterialRegistry.InvalidReferenceException {
        final Secret password = secret(truststore.getPassword(), truststore.getPasswordFile());
        if (truststore.isSkipIntegrityCheck()) {
            if (password != null) {
                throw new TLSMaterialRegistry.InvalidReferenceException(("The tls-truststore '%s' in the tls-material "
                                + "'%s' names both a password and skip-integrity-check, so Edge cannot tell whether "
                                + "the file is to be checked. Keep the password to check it, or skip-integrity-check "
                                + "for a store that has none.")
                        .formatted(truststore.getName(), materialName));
            }
            return new Secret.Waived();
        }
        if (password == null && files.namesAPath() && TLSConfigBridge.isContainerFormat(truststore.getFormat())) {
            throw new TLSMaterialRegistry.InvalidReferenceException(("The tls-truststore '%s' in the tls-material "
                            + "'%s' names a %s container but neither a password nor skip-integrity-check. Write the "
                            + "password with 'password' or 'password-file', or write "
                            + "<skip-integrity-check>true</skip-integrity-check> to read the store unchecked.")
                    .formatted(truststore.getName(), materialName, truststore.getFormat()));
        }
        return password;
    }

    /// The sources a declaration names, refusing one that names none: a surface referring to it would
    /// get material with nothing in it, and for a truststore that reads as no truststore at all.
    private static @NotNull FileSet namedSources(
            final @NotNull String materialName,
            final @NotNull TLSMaterialRegistry.Kind kind,
            final @Nullable String entryName,
            final @NotNull FileSet files)
            throws TLSMaterialRegistry.InvalidReferenceException {
        if (!files.isEmpty()) {
            return files;
        }
        throw new TLSMaterialRegistry.InvalidReferenceException(("The %s '%s' in the tls-material '%s' names no "
                        + "file, directory, pattern or builtin, so a surface referring to it would get nothing. Name "
                        + "the material it declares, or remove the declaration.")
                .formatted(describe(kind), entryName, materialName));
    }

    private static @NotNull FileSet fileSet(
            final @NotNull String materialName,
            final @NotNull TLSMaterialRegistry.Kind kind,
            final @Nullable String entryName,
            final @NotNull List<String> files,
            final @NotNull List<DirectoryEntity> directories,
            final @NotNull List<PatternEntity> patterns,
            final @NotNull List<String> builtins)
            throws TLSMaterialRegistry.InvalidReferenceException {
        for (final String file : files) {
            requireWritten(materialName, kind, entryName, "file", file);
        }
        for (final String builtin : builtins) {
            requireWritten(materialName, kind, entryName, "builtin", builtin);
        }
        final List<FileSet.Directory> asDirectories = new ArrayList<>();
        for (final DirectoryEntity directory : directories) {
            asDirectories.add(new FileSet.Directory(
                    requireWritten(materialName, kind, entryName, "directory", directory.getLocation()),
                    directory.isMayBeEmpty()));
        }
        final List<FileSet.Pattern> asPatterns = new ArrayList<>();
        for (final PatternEntity pattern : patterns) {
            asPatterns.add(new FileSet.Pattern(
                    requireWritten(materialName, kind, entryName, "pattern", pattern.getLocation()),
                    pattern.isMayBeEmpty()));
        }
        return new FileSet(files, asDirectories, asPatterns, builtins);
    }

    /// An entry written blank is a source the operator meant to fill; dropping or keeping it would
    /// read the declaration as naming less, or something other, than it does.
    private static @NotNull String requireWritten(
            final @NotNull String materialName,
            final @NotNull TLSMaterialRegistry.Kind kind,
            final @Nullable String entryName,
            final @NotNull String entry,
            final @Nullable String value)
            throws TLSMaterialRegistry.InvalidReferenceException {
        if (value == null || value.isBlank()) {
            throw new TLSMaterialRegistry.InvalidReferenceException(("The %s '%s' in the tls-material '%s' has an "
                            + "empty '%s' entry. Write the path it should name, or remove the entry.")
                    .formatted(describe(kind), entryName, materialName, entry));
        }
        return value;
    }

    private static void refuseReference(
            final @NotNull String materialName,
            final @NotNull TLSMaterialRegistry.Kind kind,
            final @Nullable String entryName,
            final @Nullable String referenceName,
            final @Nullable String referenceUuid)
            throws TLSMaterialRegistry.InvalidReferenceException {
        if (referenceName == null && referenceUuid == null) {
            return;
        }
        throw new TLSMaterialRegistry.InvalidReferenceException(("The %s '%s' in the tls-material '%s' is itself a "
                        + "reference. An entry inside a tls-material is a declaration, which a surface then refers "
                        + "to; write the material here, or refer to it from where it is used.")
                .formatted(describe(kind), entryName, materialName));
    }

    private static @NotNull String describe(final @NotNull TLSMaterialRegistry.Kind kind) {
        return switch (kind) {
            case IDENTITY -> "tls-identity";
            case TRUSTSTORE -> "tls-truststore";
            case ALLOW_LIST -> "tls-allowlist";
            case REVOCATION_LIST -> "tls-revocationlist";
        };
    }

    private static void addIfPresent(final @NotNull List<String> files, final @Nullable String file) {
        if (file != null && !file.isBlank()) {
            files.add(file);
        }
    }

    private static @Nullable String firstPresent(final @Nullable String first, final @Nullable String second) {
        return first != null ? first : second;
    }

    /// A secret as written, or nothing where the operator wrote neither form.
    ///
    /// Writing both is refused: they could disagree, and picking one silently would leave an
    /// operator reading the wrong line to learn why a key did not open.
    private static @Nullable Secret secret(final @Nullable String literal, final @Nullable String file) {
        if (literal != null && file != null) {
            throw new IllegalArgumentException("Both a password and a password-file are declared, which could "
                    + "disagree. Declare one of them.");
        }
        if (literal != null) {
            return new Secret.Literal(literal);
        }
        if (file != null) {
            return new Secret.FromFile(file);
        }
        return null;
    }
}
