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

import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryFlag;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclEntryType;
import java.nio.file.attribute.UserPrincipal;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

/**
 * The principals, permissions and entry shapes the ACL comparison tests are written in terms of, and the
 * generator the exhaustive pass draws its universe from.
 * <p>
 * Three principals is not an arbitrary size. A witness that one list grants more than another needs at most
 * two — the principal whose entry decided it one way and the principal whose entry decided it the other —
 * so a universe of three carries every witness shape plus one principal that takes no part, which is what
 * catches a rule that accidentally depends on a principal being named at all.
 */
final class AclComparisonFixture {

    private AclComparisonFixture() {}

    /** The account the node runs as: the owner of the file being written, in every test here. */
    static final @NotNull UserPrincipal EDGE = new Principal("edge");

    /** A user who is a member of {@link #ADMINISTRATORS} — asserted nowhere, because it cannot be. */
    static final @NotNull UserPrincipal ALICE = new Principal("alice");

    /** A group. Indistinguishable from a user through this API, which is the point of review v06. */
    static final @NotNull UserPrincipal ADMINISTRATORS = new Principal("Administrators");

    /** A fourth identity, named by nothing in most tests: the one that must change no answer. */
    static final @NotNull UserPrincipal SYSTEM = new Principal("SYSTEM");

    static final @NotNull List<UserPrincipal> PRINCIPALS = List.of(EDGE, ALICE, ADMINISTRATORS);

    /**
     * Four identities, for the pass that tests the claim the whole implementation rests on. A witness needs
     * at most two principals; with four in play and every subset of them presented as a token, an
     * implementation that needed three would be caught here and nowhere else.
     */
    static final @NotNull List<UserPrincipal> FOUR_PRINCIPALS = List.of(EDGE, ALICE, ADMINISTRATORS, SYSTEM);

    static final @NotNull Set<AclEntryPermission> READ = Set.of(AclEntryPermission.READ_DATA);
    static final @NotNull Set<AclEntryPermission> WRITE = Set.of(AclEntryPermission.WRITE_DATA);
    static final @NotNull Set<AclEntryPermission> READ_WRITE =
            Set.of(AclEntryPermission.READ_DATA, AclEntryPermission.WRITE_DATA);

    /** The permission bits an access check is run for. Every entry below is built out of these two. */
    static final @NotNull List<AclEntryPermission> BITS =
            List.of(AclEntryPermission.READ_DATA, AclEntryPermission.WRITE_DATA);

    static @NotNull AclEntry allow(
            final @NotNull UserPrincipal principal, final @NotNull Set<AclEntryPermission> permissions) {
        return entry(AclEntryType.ALLOW, principal, permissions);
    }

    static @NotNull AclEntry deny(
            final @NotNull UserPrincipal principal, final @NotNull Set<AclEntryPermission> permissions) {
        return entry(AclEntryType.DENY, principal, permissions);
    }

    static @NotNull AclEntry entry(
            final @NotNull AclEntryType type,
            final @NotNull UserPrincipal principal,
            final @NotNull Set<AclEntryPermission> permissions) {
        return AclEntry.newBuilder()
                .setType(type)
                .setPrincipal(principal)
                .setPermissions(permissions)
                .build();
    }

    /** The same entry, marked as not applying to the object that carries it. */
    static @NotNull AclEntry inheritOnly(final @NotNull AclEntry entry) {
        return AclEntry.newBuilder(entry)
                .setFlags(AclEntryFlag.INHERIT_ONLY, AclEntryFlag.FILE_INHERIT)
                .build();
    }

    /** The entry types that decide access. */
    static final @NotNull List<AclEntryType> DECIDING_TYPES = List.of(AclEntryType.ALLOW, AclEntryType.DENY);

    /**
     * Every entry type there is. The two that grant nothing are not decoration in a generated universe:
     * an audit entry read as a decision would sit in front of the allow behind it and hide it, so a list
     * that grants access would look like one that grants none.
     */
    static final @NotNull List<AclEntryType> ALL_TYPES =
            List.of(AclEntryType.ALLOW, AclEntryType.DENY, AclEntryType.AUDIT, AclEntryType.ALARM);

    /**
     * Every entry that can be built from the given principals, types, permission sets and the two states of
     * {@code INHERIT_ONLY}: the alphabet the exhaustive pass forms its lists over.
     */
    static @NotNull List<AclEntry> alphabet(
            final @NotNull List<UserPrincipal> principals,
            final @NotNull List<Set<AclEntryPermission>> permissionSets) {
        return alphabet(principals, DECIDING_TYPES, permissionSets);
    }

    static @NotNull List<AclEntry> alphabet(
            final @NotNull List<UserPrincipal> principals,
            final @NotNull List<AclEntryType> types,
            final @NotNull List<Set<AclEntryPermission>> permissionSets) {
        final List<AclEntry> alphabet = new ArrayList<>();
        for (final UserPrincipal principal : principals) {
            for (final AclEntryType type : types) {
                for (final Set<AclEntryPermission> permissions : permissionSets) {
                    final AclEntry plain = entry(type, principal, permissions);
                    alphabet.add(plain);
                    alphabet.add(inheritOnly(plain));
                }
            }
        }
        return List.copyOf(alphabet);
    }

    /** Every list of up to {@code maxLength} entries drawn from an alphabet, the empty list included. */
    static @NotNull List<List<AclEntry>> listsUpTo(final @NotNull List<AclEntry> alphabet, final int maxLength) {
        final List<List<AclEntry>> lists = new ArrayList<>();
        lists.add(List.of());
        List<List<AclEntry>> previous = List.of(List.of());
        for (int length = 1; length <= maxLength; length++) {
            final List<List<AclEntry>> current = new ArrayList<>(previous.size() * alphabet.size());
            for (final List<AclEntry> prefix : previous) {
                for (final AclEntry entry : alphabet) {
                    final List<AclEntry> extended = new ArrayList<>(prefix.size() + 1);
                    extended.addAll(prefix);
                    extended.add(entry);
                    current.add(List.copyOf(extended));
                }
            }
            lists.addAll(current);
            previous = current;
        }
        return lists;
    }

    /**
     * The same list with every principal renamed through a bijection. Who the principals are carries no
     * meaning to a comparison that cannot look any of them up, so the answer has to survive this.
     */
    static @NotNull List<AclEntry> renamePrincipals(
            final @NotNull List<AclEntry> acl, final @NotNull Map<UserPrincipal, UserPrincipal> renaming) {
        return acl.stream()
                .map(entry -> AclEntry.newBuilder(entry)
                        .setPrincipal(renaming.getOrDefault(entry.principal(), entry.principal()))
                        .build())
                .toList();
    }

    /**
     * The same list with every permission bit swapped for another through a bijection. No bit is special —
     * a read is compared the same way as a change of owner — so the answer has to survive this too.
     */
    static @NotNull List<AclEntry> renamePermissions(
            final @NotNull List<AclEntry> acl, final @NotNull Map<AclEntryPermission, AclEntryPermission> renaming) {
        return acl.stream()
                .map(entry -> {
                    final Set<AclEntryPermission> renamed = entry.permissions().stream()
                            .map(permission -> renaming.getOrDefault(permission, permission))
                            .collect(Collectors.toCollection(() -> EnumSet.noneOf(AclEntryPermission.class)));
                    return AclEntry.newBuilder(entry).setPermissions(renamed).build();
                })
                .toList();
    }

    /**
     * The list as it looks to an access check for one permission: the entries that mention that bit, in
     * order, with everything else about them left alone.
     */
    static @NotNull List<AclEntry> project(final @NotNull List<AclEntry> acl, final @NotNull AclEntryPermission bit) {
        return acl.stream()
                .filter(entry -> entry.permissions().contains(bit))
                .map(entry ->
                        AclEntry.newBuilder(entry).setPermissions(Set.of(bit)).build())
                .toList();
    }

    /** A handful of lists of every shape the tests talk about, for properties that hold of all of them. */
    static @NotNull List<List<AclEntry>> assortedLists() {
        return List.of(
                List.of(),
                List.of(allow(EDGE, READ_WRITE)),
                List.of(allow(EDGE, READ), allow(EDGE, WRITE)),
                List.of(deny(ALICE, READ), allow(ADMINISTRATORS, READ)),
                List.of(allow(ADMINISTRATORS, READ), deny(ALICE, READ)),
                List.of(inheritOnly(allow(ALICE, READ_WRITE))),
                List.of(allow(EDGE, READ_WRITE), inheritOnly(allow(ADMINISTRATORS, READ)), deny(ALICE, WRITE)),
                List.of(deny(EDGE, READ), allow(EDGE, READ_WRITE), allow(ALICE, READ)));
    }

    /** A principal whose identity is its name, which is all the comparison treats it as. */
    private record Principal(@NotNull String name) implements UserPrincipal {

        @Override
        public @NotNull String getName() {
            return name;
        }

        @Override
        public @NotNull String toString() {
            return name;
        }
    }
}
