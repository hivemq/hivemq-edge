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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryFlag;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclEntryType;
import java.nio.file.attribute.UserPrincipal;
import java.util.List;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

/**
 * EDG-882 review v04, finding 2.1 — the decision the ACL-only write path makes, taken apart from the file
 * store that provokes it.
 * <p>
 * Both ACL checks used to demand that the list read back be <em>equal</em> to the list set. That is a claim
 * about how a store represents an access-control list, not about who can read the file, and it is the
 * claim nothing outside Windows can test: jimfs hands back exactly what it was given, so those tests passed
 * by construction while a real NTFS volume that reordered entries, merged a permission mask or returned an
 * inherited entry alongside the one just written would have refused every configuration write — on the only
 * platform the code path exists for.
 * <p>
 * The question is now "does this grant anyone more than that does", which is answerable without a store at
 * all. This class is that answer: every shape a store might plausibly hand back, judged directly. What
 * remains untested off Windows is only what NTFS actually returns — and under this rule, any answer that
 * does not widen access is accepted.
 */
public class AclComparisonTest {

    private static final @NotNull UserPrincipal EDGE = new Principal("edge");
    private static final @NotNull UserPrincipal ALICE = new Principal("alice");
    private static final @NotNull UserPrincipal ADMINISTRATORS = new Principal("Administrators");

    private static final @NotNull Set<AclEntryPermission> READ = Set.of(AclEntryPermission.READ_DATA);
    private static final @NotNull Set<AclEntryPermission> READ_WRITE =
            Set.of(AclEntryPermission.READ_DATA, AclEntryPermission.WRITE_DATA);

    private static @NotNull AclEntry allow(
            final @NotNull UserPrincipal principal, final @NotNull Set<AclEntryPermission> permissions) {
        return AclEntry.newBuilder()
                .setType(AclEntryType.ALLOW)
                .setPrincipal(principal)
                .setPermissions(permissions)
                .build();
    }

    private static @NotNull AclEntry deny(
            final @NotNull UserPrincipal principal, final @NotNull Set<AclEntryPermission> permissions) {
        return AclEntry.newBuilder()
                .setType(AclEntryType.DENY)
                .setPrincipal(principal)
                .setPermissions(permissions)
                .build();
    }

    private static boolean grantsNoMoreThan(
            final @NotNull List<AclEntry> candidate, final @NotNull List<AclEntry> reference) {
        return ConfigFileReaderWriter.grantsNoMoreThan(candidate, reference);
    }

    // ------------------------------------------------------------- what a store may change and still pass

    /** The same list, which is what a store that changes nothing hands back. */
    @Test
    public void anIdenticalListGrantsNoMore() {
        final List<AclEntry> acl = List.of(allow(EDGE, READ_WRITE), allow(ALICE, READ));

        assertTrue(grantsNoMoreThan(acl, acl));
    }

    /** Order is not access. A store that canonicalises the list still grants exactly what it was given. */
    @Test
    public void aReorderedListGrantsNoMore() {
        assertTrue(grantsNoMoreThan(
                List.of(allow(ALICE, READ), allow(EDGE, READ_WRITE)),
                List.of(allow(EDGE, READ_WRITE), allow(ALICE, READ))));
    }

    /** Nor is how the permissions are split across entries for the same principal. */
    @Test
    public void permissionsSplitAcrossEntriesGrantNoMore() {
        assertTrue(grantsNoMoreThan(
                List.of(allow(EDGE, READ), allow(EDGE, Set.of(AclEntryPermission.WRITE_DATA))),
                List.of(allow(EDGE, READ_WRITE))));
    }

    /**
     * Nor are inheritance flags: they decide what a <em>directory</em> propagates to what is created inside
     * it, and the file being written propagates to nothing.
     */
    @Test
    public void inheritanceFlagsGrantNoMore() {
        final AclEntry flagged = AclEntry.newBuilder()
                .setType(AclEntryType.ALLOW)
                .setPrincipal(EDGE)
                .setPermissions(READ_WRITE)
                .setFlags(AclEntryFlag.FILE_INHERIT, AclEntryFlag.DIRECTORY_INHERIT)
                .build();

        assertTrue(grantsNoMoreThan(List.of(flagged), List.of(allow(EDGE, READ_WRITE))));
    }

    /** An audit entry grants nothing, so a store that adds one has not widened anything. */
    @Test
    public void anAuditEntryGrantsNoMore() {
        final AclEntry audit = AclEntry.newBuilder()
                .setType(AclEntryType.AUDIT)
                .setPrincipal(ALICE)
                .setPermissions(READ)
                .setFlags(AclEntryFlag.FILE_INHERIT)
                .build();

        assertTrue(grantsNoMoreThan(List.of(allow(EDGE, READ_WRITE), audit), List.of(allow(EDGE, READ_WRITE))));
    }

    /** Granting less is not granting more. */
    @Test
    public void aNarrowerListGrantsNoMore() {
        assertTrue(grantsNoMoreThan(List.of(allow(EDGE, READ)), List.of(allow(EDGE, READ_WRITE), allow(ALICE, READ))));
    }

    /** And granting nothing at all is the narrowest there is. */
    @Test
    public void anEmptyListGrantsNoMore() {
        assertTrue(grantsNoMoreThan(List.of(), List.of(allow(EDGE, READ_WRITE))));
    }

    // ---------------------------------------------------------------------- what must still be refused

    /** A principal the file being replaced does not name at all. */
    @Test
    public void anExtraPrincipalGrantsMore() {
        assertFalse(grantsNoMoreThan(
                List.of(allow(EDGE, READ_WRITE), allow(ADMINISTRATORS, READ)), List.of(allow(EDGE, READ_WRITE))));
    }

    /** A permission the file being replaced does not grant that principal. */
    @Test
    public void anExtraPermissionGrantsMore() {
        assertFalse(grantsNoMoreThan(List.of(allow(EDGE, READ_WRITE)), List.of(allow(EDGE, READ))));
    }

    /** Anything at all, where the file being replaced grants nobody anything. */
    @Test
    public void anythingGrantsMoreThanAnEmptyList() {
        assertFalse(grantsNoMoreThan(List.of(allow(EDGE, READ)), List.of()));
    }

    /**
     * A denial that was dropped. It reads like a narrowing and is the opposite: a DENY can be the only
     * thing keeping a member of an allowed group out of the file, so losing one widens access even though
     * no ALLOW changed.
     */
    @Test
    public void losingADenialGrantsMore() {
        assertFalse(grantsNoMoreThan(
                List.of(allow(EDGE, READ_WRITE)), List.of(allow(EDGE, READ_WRITE), deny(ALICE, READ))));
    }

    /** Adding one is the other direction, and allowed. */
    @Test
    public void addingADenialGrantsNoMore() {
        assertTrue(grantsNoMoreThan(
                List.of(allow(EDGE, READ_WRITE), deny(ALICE, READ)), List.of(allow(EDGE, READ_WRITE))));
    }

    /**
     * The shape the whole change is about, end to end: the list a store might return after being asked for
     * owner-only — reordered, flagged, and with the permissions merged differently — is accepted, while the
     * same list with the directory's inherited entry still in it is not.
     */
    @Test
    public void whatAStoreMayReturnAfterBeingAskedForOwnerOnly() {
        final List<AclEntry> ownerOnly = List.of(allow(EDGE, READ_WRITE));
        final AclEntry rewritten = AclEntry.newBuilder()
                .setType(AclEntryType.ALLOW)
                .setPrincipal(EDGE)
                .setPermissions(READ_WRITE)
                .setFlags(AclEntryFlag.NO_PROPAGATE_INHERIT)
                .build();

        assertTrue(grantsNoMoreThan(List.of(rewritten), ownerOnly), "a store that rewrote the entry did not widen it");
        assertFalse(
                grantsNoMoreThan(List.of(rewritten, allow(ADMINISTRATORS, READ)), ownerOnly),
                "an inherited entry the store kept is exactly what the narrowing is for");
    }

    /** A principal whose identity is its name, which is all the comparison treats it as. */
    private record Principal(@NotNull String name) implements UserPrincipal {

        @Override
        public @NotNull String getName() {
            return name;
        }
    }
}
