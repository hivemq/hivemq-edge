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

import java.util.LinkedHashMap;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/// The TLS material declared for sharing, indexed by what a reference names it by.
///
/// Built once from the declared material and then only read, so resolving a reference is a lookup
/// rather than a search. Constructing one refuses a declaration that could not be resolved
/// unambiguously later - two entries of one type under one name, two materials under one name, or
/// one uuid on two entries - because an ambiguity discovered at resolution time would already have
/// a surface attached to whichever entry won.
///
/// Holds no files. Every entry is material as the operator wrote it: a reference resolves to that,
/// and reading it happens later.
public final class TLSMaterialRegistry {

    /// What separates a material's name from an entry's name in a reference, matching the
    /// `adapter::tag-name` convention Edge already uses. A name may not contain it.
    public static final @NotNull String SEPARATOR = "::";

    private final @NotNull Map<Key, MaterialRef.Inline> byName;
    private final @NotNull Map<String, Entry> byUuid;

    /// What kind of material an entry is, so a reference by uuid can be checked against the type
    /// the surface asked for. A uuid carries no type of its own.
    public enum Kind {
        IDENTITY,
        TRUSTSTORE,
        ALLOW_LIST,
        REVOCATION_LIST
    }

    /// One declared entry: the material itself and what kind it is.
    public record Entry(@NotNull Kind kind, @NotNull MaterialRef.Inline material) {}

    private record Key(
            @NotNull Kind kind,
            @NotNull String material,
            @NotNull String entry) {}

    private TLSMaterialRegistry(
            final @NotNull Map<Key, MaterialRef.Inline> byName, final @NotNull Map<String, Entry> byUuid) {
        this.byName = Map.copyOf(byName);
        this.byUuid = Map.copyOf(byUuid);
    }

    /// A registry holding nothing, for a deployment that declares no shared material.
    public static @NotNull TLSMaterialRegistry empty() {
        return new TLSMaterialRegistry(Map.of(), Map.of());
    }

    /// Collects declared material into a registry, refusing anything a later lookup could not
    /// resolve unambiguously.
    public static final class Builder {

        private final @NotNull Map<Key, MaterialRef.Inline> byName = new LinkedHashMap<>();
        private final @NotNull Map<String, Entry> byUuid = new LinkedHashMap<>();
        private final @NotNull Map<String, String> materialNames = new LinkedHashMap<>();

        /// Begins a declared material, whose name every entry below it is reached through.
        ///
        /// Returns that name, which [#entry] then takes, so a caller reaching an entry has a name
        /// this method has already accepted rather than the one it read.
        ///
        /// @throws InvalidReferenceException if the name is absent, contains the separator, or was
        ///     already used by another declared material
        public @NotNull String material(final @Nullable String name, final @Nullable String uuid)
                throws InvalidReferenceException {
            if (name == null || name.isBlank()) {
                throw new InvalidReferenceException("A tls-material has no name, so nothing could refer to it. "
                        + "Give it a name, which every reference to its entries begins with.");
            }
            if (name.contains(SEPARATOR)) {
                throw new InvalidReferenceException(("The tls-material name '%s' contains '%s', which separates a "
                                + "material's name from an entry's name in a reference, so Edge could not tell where "
                                + "the first half ends. Choose a name without it.")
                        .formatted(name, SEPARATOR));
            }
            final String existing = materialNames.put(name, name);
            if (existing != null) {
                throw new InvalidReferenceException(("Two tls-material elements are named '%s', so Edge could not "
                                + "tell which one a reference beginning '%s%s' starts from. Name them differently.")
                        .formatted(name, name, SEPARATOR));
            }
            if (uuid != null && !uuid.isBlank()) {
                requireUnusedUuid(uuid, "the tls-material '" + name + "'");
                // A material's own uuid identifies the material rather than any entry, so it is
                // reserved against collisions but resolves to no material. Reserved under a key no
                // name can take, since the separator is forbidden in a name.
                materialNames.put(SEPARATOR + uuid, uuid);
            }
            return name;
        }

        /// Declares one entry under the material most recently begun.
        ///
        /// @throws InvalidReferenceException if the entry has no name, if another entry of the same
        ///     kind under the same material already has that name, or if its uuid is already used
        public @NotNull Builder entry(
                final @NotNull String materialName,
                final @NotNull Kind kind,
                final @Nullable String entryName,
                final @Nullable String uuid,
                final @NotNull MaterialRef.Inline material)
                throws InvalidReferenceException {
            if (entryName == null || entryName.isBlank()) {
                throw new InvalidReferenceException(("A %s in the tls-material '%s' has no name, so nothing could "
                                + "refer to it. Give it a name, or write the material where it is used instead.")
                        .formatted(describe(kind), materialName));
            }
            if (entryName.contains(SEPARATOR)) {
                throw new InvalidReferenceException(("The %s name '%s' contains '%s', which separates a material's "
                                + "name from an entry's name in a reference. Choose a name without it.")
                        .formatted(describe(kind), entryName, SEPARATOR));
            }
            final MaterialRef.Inline clash = byName.put(new Key(kind, materialName, entryName), material);
            if (clash != null) {
                throw new InvalidReferenceException(("Two %s entries in the tls-material '%s' are named '%s', so "
                                + "Edge could not tell which one '%s%s%s' means. Name them differently.")
                        .formatted(describe(kind), materialName, entryName, materialName, SEPARATOR, entryName));
            }
            if (uuid != null && !uuid.isBlank()) {
                requireUnusedUuid(uuid, "the %s '%s'".formatted(describe(kind), entryName));
                byUuid.put(uuid, new Entry(kind, material));
            }
            return this;
        }

        public @NotNull TLSMaterialRegistry build() {
            return new TLSMaterialRegistry(byName, byUuid);
        }

        private void requireUnusedUuid(final @NotNull String uuid, final @NotNull String what)
                throws InvalidReferenceException {
            if (byUuid.containsKey(uuid) || materialNames.containsKey(SEPARATOR + uuid)) {
                // Edge searches every material and every type to resolve a uuid, so a repeated one
                // would attach a surface to material its operator never chose.
                throw new InvalidReferenceException(("The uuid '%s' is used twice, once by %s. A uuid identifies one "
                                + "piece of material across every tls-material and every type, so Edge could not "
                                + "tell which one a reference to it means.")
                        .formatted(uuid, what));
            }
        }
    }

    /// The material a reference names, as if the operator had written it where it is used.
    ///
    /// @param kind what the surface asked for, so a reference by uuid - which carries no type - can
    ///     be checked against it
    /// @throws InvalidReferenceException if the reference names nothing, or names material of
    ///     another kind
    public @NotNull MaterialRef.Inline resolve(final @NotNull MaterialRef ref, final @NotNull Kind kind)
            throws InvalidReferenceException {
        return switch (ref) {
            case MaterialRef.Inline inline -> inline;
            case MaterialRef.ByName byNameRef -> resolveByName(byNameRef, kind);
            case MaterialRef.ByUuid byUuidRef -> resolveByUuid(byUuidRef, kind);
        };
    }

    private @NotNull MaterialRef.Inline resolveByName(final @NotNull MaterialRef.ByName ref, final @NotNull Kind kind)
            throws InvalidReferenceException {
        final MaterialRef.Inline found = byName.get(new Key(kind, ref.material(), ref.entry()));
        if (found == null) {
            // Never a fallback: a surface would then run on material its operator never named.
            throw new InvalidReferenceException(("No %s named '%s' is declared in the tls-material '%s'. Declare it "
                            + "there, or correct the reference.")
                    .formatted(describe(kind), ref.entry(), ref.material()));
        }
        return found;
    }

    private @NotNull MaterialRef.Inline resolveByUuid(final @NotNull MaterialRef.ByUuid ref, final @NotNull Kind kind)
            throws InvalidReferenceException {
        final Entry found = byUuid.get(ref.uuid());
        if (found == null) {
            throw new InvalidReferenceException(("No material with the uuid '%s' is declared. Declare it in a "
                            + "tls-material, or correct the reference.")
                    .formatted(ref.uuid()));
        }
        if (found.kind() != kind) {
            // A uuid carries no type, so a reference to one of the wrong kind is only detectable
            // here - and silently using it would attach a surface to material of another sort.
            throw new InvalidReferenceException(
                    ("The uuid '%s' names %s, but a %s was expected here. Reference the " + "uuid of a %s instead.")
                            .formatted(ref.uuid(), describe(found.kind()), describe(kind), describe(kind)));
        }
        return found.material();
    }

    /// A reference written as `material-name::entry-name`, or by uuid where it holds no separator.
    ///
    /// @throws InvalidReferenceException if a name carries the separator more than once, or either
    ///     half is empty
    public static @NotNull MaterialRef parseReference(final @NotNull String reference)
            throws InvalidReferenceException {
        final int separator = reference.indexOf(SEPARATOR);
        if (separator < 0) {
            throw new InvalidReferenceException(("The reference '%s' names no tls-material. A reference is written "
                            + "'material-name%sentry-name', so that two parties may each use the same entry name "
                            + "without either reference becoming ambiguous.")
                    .formatted(reference, SEPARATOR));
        }
        final String material = reference.substring(0, separator);
        final String entry = reference.substring(separator + SEPARATOR.length());
        if (material.isBlank() || entry.isBlank()) {
            throw new InvalidReferenceException(
                    ("The reference '%s' has an empty half. Write it as " + "'material-name%sentry-name'.")
                            .formatted(reference, SEPARATOR));
        }
        if (entry.contains(SEPARATOR)) {
            throw new InvalidReferenceException(("The reference '%s' contains '%s' more than once, so Edge could not "
                            + "tell where the material's name ends. A name may not contain it.")
                    .formatted(reference, SEPARATOR));
        }
        return new MaterialRef.ByName(material, entry);
    }

    private static @NotNull String describe(final @NotNull Kind kind) {
        return switch (kind) {
            case IDENTITY -> "tls-identity";
            case TRUSTSTORE -> "tls-truststore";
            case ALLOW_LIST -> "tls-allowlist";
            case REVOCATION_LIST -> "tls-revocationlist";
        };
    }

    /// A reference, or a declaration a reference could not resolve against, that cannot be honoured
    /// as written. The message names what was referenced and what to do, because it reaches an
    /// operator as the reason a surface did not start.
    public static final class InvalidReferenceException extends Exception {

        private static final long serialVersionUID = 1L;

        private final @NotNull String reason;

        public InvalidReferenceException(final @NotNull String reason) {
            super(reason);
            this.reason = reason;
        }

        public @NotNull String reason() {
            return reason;
        }
    }
}
