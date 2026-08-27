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

import static com.hivemq.configuration.reader.AclComparisonFixture.ADMINISTRATORS;
import static com.hivemq.configuration.reader.AclComparisonFixture.ALICE;
import static com.hivemq.configuration.reader.AclComparisonFixture.EDGE;
import static com.hivemq.configuration.reader.AclComparisonFixture.READ;
import static com.hivemq.configuration.reader.AclComparisonFixture.READ_WRITE;
import static com.hivemq.configuration.reader.AclComparisonFixture.WRITE;
import static com.hivemq.configuration.reader.AclComparisonFixture.allow;
import static com.hivemq.configuration.reader.AclComparisonFixture.deny;
import static com.hivemq.configuration.reader.AclComparisonFixture.inheritOnly;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryFlag;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclEntryType;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

/**
 * EDG-882 reviews v04, v05 and v06 — the decision the ACL-only configuration write path makes, taken apart
 * from the file store that provokes it.
 * <p>
 * Both ACL checks originally demanded that the list read back be <em>equal</em> to the list set. That is a
 * claim about how a store represents an access-control list, not about who can read the file, and it is the
 * claim nothing outside Windows can test: jimfs hands back exactly what it was given, so those tests passed
 * by construction while a real NTFS volume that reordered entries, merged a permission mask or returned an
 * inherited entry alongside the one just written would have refused every configuration write — on the only
 * platform the code path exists for.
 * <p>
 * Three rounds then found three ways the replacement was wrong about what a list means: it ignored the order
 * entries are evaluated in (v05), it read {@code INHERIT_ONLY} as a propagation flag (v05), and it settled
 * permissions per principal, so an entry naming a group could not settle a permission for a user who belongs
 * to it (v06). Each round arrived from outside, because nothing here was capable of producing the next one.
 * <p>
 * {@link AclComparison} answers the question over every token that could be presented to the two lists
 * rather than modelling any particular directory's groups, which makes it exact. This class pins the rules
 * one shape at a time and every case the three rounds raised; {@link AclComparisonOracleTest} is the part
 * that closes the class rather than the instance, by agreeing with a direct implementation of the
 * access-check algorithm over an exhaustively generated universe of lists.
 */
public class AclComparisonTest {

    private static boolean grantsNoMoreThan(
            final @NotNull List<AclEntry> candidate, final @NotNull List<AclEntry> reference) {
        return AclComparison.grantsNoMoreThan(candidate, reference);
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
        assertTrue(grantsNoMoreThan(List.of(allow(EDGE, READ), allow(EDGE, WRITE)), List.of(allow(EDGE, READ_WRITE))));
    }

    /**
     * Nor are the propagation flags: they decide what a <em>directory</em> propagates to what is created
     * inside it, and the file being written propagates to nothing. {@code INHERIT_ONLY} is not one of them
     * — see below.
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

    /**
     * A denial the list can never reach, because an earlier entry has already allowed the permission, is
     * worth nothing — so a list that puts the same denial where it does decide something is narrower, not
     * wider.
     */
    @Test
    public void movingADenialAheadOfItsAllowGrantsNoMore() {
        assertTrue(grantsNoMoreThan(
                List.of(deny(ALICE, READ), allow(ALICE, READ)), List.of(allow(ALICE, READ), deny(ALICE, READ))));
    }

    /** Which entry decides is settled one permission at a time, not one principal at a time. */
    @Test
    public void aDenialOfOnePermissionLeavesTheOthersToTheNextEntry() {
        assertTrue(grantsNoMoreThan(
                List.of(deny(ALICE, READ), allow(ALICE, READ_WRITE)),
                List.of(deny(ALICE, READ), allow(ALICE, READ_WRITE))));
        assertFalse(
                grantsNoMoreThan(
                        List.of(allow(ALICE, READ_WRITE)), List.of(deny(ALICE, READ), allow(ALICE, READ_WRITE))),
                "the write survives the denial, the read does not");
    }

    /**
     * An entry that does not apply to the file grants nothing on the file, so a list carrying one is no
     * wider than the same list without it.
     */
    @Test
    public void anInheritOnlyEntryGrantsNoMore() {
        assertTrue(grantsNoMoreThan(
                List.of(allow(EDGE, READ_WRITE), inheritOnly(allow(ALICE, READ))), List.of(allow(EDGE, READ_WRITE))));
    }

    /** And adding the flag to an entry that did apply narrows it to nothing. */
    @Test
    public void addingInheritOnlyGrantsNoMore() {
        assertTrue(grantsNoMoreThan(List.of(inheritOnly(allow(ALICE, READ))), List.of(allow(ALICE, READ))));
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

    /**
     * An audit entry grants nothing, so it also takes nothing away: it does not stand in front of the entry
     * behind it and hide what that one grants.
     * <p>
     * Both directions matter and they fail differently. Read as a decision, an audit entry ahead of an allow
     * makes a replacement that grants access look like one that grants none — a widening waved through,
     * which is the whole disclosure this path exists to prevent. Read as a decision in the file being
     * replaced, it makes that file look narrower than it is and refuses a write that discloses nothing.
     */
    @Test
    public void anAuditEntryDoesNotHideTheEntryBehindIt() {
        final AclEntry audit = AclEntry.newBuilder()
                .setType(AclEntryType.AUDIT)
                .setPrincipal(ALICE)
                .setPermissions(READ)
                .build();

        assertFalse(
                grantsNoMoreThan(List.of(audit, allow(ALICE, READ)), List.of()),
                "the allow behind the audit entry grants Alice a read the file being replaced does not");
        assertTrue(
                grantsNoMoreThan(List.of(allow(ALICE, READ)), List.of(audit, allow(ALICE, READ))),
                "the file being replaced grants that read too, whatever is recorded in front of it");
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

    /** Adding a denial is a narrowing whatever else the list says. */
    @Test
    public void addingADenialGrantsNoMore() {
        assertTrue(grantsNoMoreThan(
                List.of(allow(EDGE, READ_WRITE), deny(ALICE, READ)), List.of(allow(EDGE, READ_WRITE))));
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

    // ------------------------------------------------------- review v05: order is part of what a list says

    /**
     * The first of the two shapes review v05 named. A list is evaluated in order, so the entry that comes
     * first settles the permission: {@code DENY} then {@code ALLOW} keeps Alice out, and the same two
     * entries the other way round let her in. Nothing about the entries themselves changed.
     */
    @Test
    public void movingAnAllowAheadOfItsDenialGrantsMore() {
        assertFalse(grantsNoMoreThan(
                List.of(allow(ALICE, READ), deny(ALICE, READ)), List.of(deny(ALICE, READ), allow(ALICE, READ))));
    }

    /**
     * The second. {@code INHERIT_ONLY} says the entry does not apply to the object carrying it, so taking
     * the flag off an entry hands its principal access to the file that the reference never gave.
     */
    @Test
    public void removingInheritOnlyGrantsMore() {
        assertFalse(grantsNoMoreThan(List.of(allow(ALICE, READ)), List.of(inheritOnly(allow(ALICE, READ)))));
    }

    /** The same, where the flag is the only thing keeping the principal off an otherwise owner-only file. */
    @Test
    public void removingInheritOnlyFromAnOwnerOnlyListGrantsMore() {
        assertFalse(grantsNoMoreThan(
                List.of(allow(EDGE, READ_WRITE), allow(ADMINISTRATORS, READ)),
                List.of(allow(EDGE, READ_WRITE), inheritOnly(allow(ADMINISTRATORS, READ)))));
    }

    // ------------------------------- review v06: an entry naming a group settles a permission for its members

    /**
     * Review v06, exactly as reported. A token carries the user's own identity <em>and</em> every group they
     * belong to, so entries naming different principals settle the same person's access and the order
     * between them decides who wins.
     * <p>
     * The reference denies Alice and then allows the administrators; a request from Alice, who is one, meets
     * her denial first and is refused. The candidate has the same two entries the other way round, so the
     * group's allow is reached first and she is let in. Every per-principal summary of these two lists is
     * identical, which is why the previous implementation called them equivalent.
     */
    @Test
    public void reorderingAGroupAllowAheadOfAUserDenialGrantsMore() {
        assertFalse(
                grantsNoMoreThan(
                        List.of(allow(ADMINISTRATORS, READ), deny(ALICE, READ)),
                        List.of(deny(ALICE, READ), allow(ADMINISTRATORS, READ))),
                "Alice is an administrator: the group's allow now settles the read her own denial used to");
    }

    /** The same pair the other way round, which is a narrowing and has to stay accepted. */
    @Test
    public void reorderingAUserDenialAheadOfAGroupAllowGrantsNoMore() {
        assertTrue(grantsNoMoreThan(
                List.of(deny(ALICE, READ), allow(ADMINISTRATORS, READ)),
                List.of(allow(ADMINISTRATORS, READ), deny(ALICE, READ))));
    }

    /**
     * The membership is never asserted anywhere, because it cannot be: {@code java.nio} will not say who
     * belongs to what. The comparison assumes any principals may appear in one token, so a group allow that
     * the reference does not have is refused whether or not anybody is actually in the group.
     */
    @Test
    public void aGroupAllowIsJudgedWithoutKnowingWhoIsInTheGroup() {
        assertFalse(
                grantsNoMoreThan(List.of(deny(ALICE, READ), allow(ADMINISTRATORS, READ)), List.of(deny(ALICE, READ))));
    }

    // ------------------------------------------------- a denial is worth what its position makes it worth

    /**
     * A denial sitting behind an allow that already settles the permission for anyone who can reach it
     * decides nothing, so dropping it takes nothing away.
     * <p>
     * This is the one case whose answer changed with review v06's fix, from refuse to accept, and it is
     * the same reasoning in the opposite direction: whoever holds Alice's identity alone is denied by both
     * lists, and whoever also holds Edge's is allowed by both, at the entry that comes first. There is no
     * token that can tell them apart. The previous implementation refused it on the blanket rule that
     * losing any {@code DENY} widens access, which is over-conservative rather than unsafe — it refused
     * configuration writes that disclose nothing. {@link AclComparisonOracleTest} is what makes changing it
     * a measurement instead of an opinion.
     */
    @Test
    public void losingADenialNothingCanReachGrantsNoMore() {
        assertTrue(grantsNoMoreThan(
                List.of(allow(EDGE, READ_WRITE)), List.of(allow(EDGE, READ_WRITE), deny(ALICE, READ))));
    }

    /** And losing one that does decide something is the widening the rule is there to catch. */
    @Test
    public void losingADenialThatDecidesGrantsMore() {
        assertFalse(
                grantsNoMoreThan(List.of(allow(EDGE, READ_WRITE)), List.of(deny(ALICE, READ), allow(EDGE, READ_WRITE))),
                "a token holding both identities meets the denial first in the reference and nothing in the"
                        + " candidate");
    }

    // ----------------------------------------------------------------------------- structural properties

    /** Whatever a store returns, it is never wider than itself. */
    @Test
    public void everyListGrantsNoMoreThanItself() {
        for (final List<AclEntry> acl : AclComparisonFixture.assortedLists()) {
            assertTrue(grantsNoMoreThan(acl, acl), () -> "not reflexive for " + acl);
        }
    }

    /**
     * A list of entries that mention no permission at all decides nothing. {@code AclEntry} allows an empty
     * mask and a store is free to hand one back; it grants nothing and shadows nothing.
     */
    @Test
    public void anEntryMentioningNoPermissionDecidesNothing() {
        final AclEntry nothing = AclComparisonFixture.entry(AclEntryType.ALLOW, ALICE, Set.of());
        final AclEntry denyNothing = AclComparisonFixture.entry(AclEntryType.DENY, ALICE, Set.of());

        assertTrue(grantsNoMoreThan(List.of(nothing), List.of()), "an empty mask grants nothing");
        assertFalse(
                grantsNoMoreThan(List.of(denyNothing, allow(ALICE, READ)), List.of()),
                "and takes nothing away from the entry behind it");
        assertTrue(grantsNoMoreThan(List.of(nothing, allow(ALICE, READ)), List.of(allow(ALICE, READ))));
    }

    /** The same entry twice is the same list: the second one was settled before it was reached. */
    @Test
    public void aRepeatedEntryChangesNothing() {
        final List<AclEntry> once = List.of(allow(EDGE, READ_WRITE), deny(ALICE, READ));
        final List<AclEntry> twice =
                List.of(allow(EDGE, READ_WRITE), deny(ALICE, READ), allow(EDGE, READ_WRITE), deny(ALICE, READ));

        assertTrue(grantsNoMoreThan(twice, once));
        assertTrue(grantsNoMoreThan(once, twice));
    }

    /** A list that contradicts itself is not undefined: the entry that comes first is the one that counts. */
    @Test
    public void aListThatContradictsItselfIsSettledByItsFirstEntry() {
        assertTrue(
                grantsNoMoreThan(List.of(deny(ALICE, READ), allow(ALICE, READ)), List.of()),
                "the denial comes first, so the list grants nothing and is no wider than an empty one");
        assertFalse(
                grantsNoMoreThan(List.of(allow(ALICE, READ), deny(ALICE, READ)), List.of()),
                "the allow comes first, so it grants a read an empty list does not");
    }

    /** A list made only of entries that do not apply to the file grants nothing, whatever they say. */
    @Test
    public void aListOfEntriesThatDoNotApplyGrantsNothing() {
        assertTrue(grantsNoMoreThan(
                List.of(inheritOnly(allow(ALICE, READ_WRITE)), inheritOnly(allow(ADMINISTRATORS, READ_WRITE))),
                List.of()));
    }

    /**
     * No permission bit is special. The comparison is written in terms of {@code AclEntryPermission} and
     * never in terms of a particular one, and the cases that matter give the same answers when the read and
     * the write are swapped for the two bits that decide who may take the file over.
     */
    @Test
    public void noPermissionBitIsTreatedDifferentlyFromAnother() {
        final Set<AclEntryPermission> takeOwnership = Set.of(AclEntryPermission.WRITE_OWNER);
        final Set<AclEntryPermission> changeTheList = Set.of(AclEntryPermission.WRITE_ACL);

        assertFalse(
                grantsNoMoreThan(
                        List.of(allow(ADMINISTRATORS, takeOwnership), deny(ALICE, takeOwnership)),
                        List.of(deny(ALICE, takeOwnership), allow(ADMINISTRATORS, takeOwnership))),
                "review v06's shape, in a bit nothing else in these tests uses");
        assertTrue(grantsNoMoreThan(
                List.of(allow(EDGE, changeTheList)), List.of(allow(EDGE, changeTheList), allow(ALICE, READ))));
        assertFalse(grantsNoMoreThan(List.of(allow(EDGE, changeTheList)), List.of(allow(EDGE, takeOwnership))));
    }

    /** The list the write path actually sets on a replacement: its owner, everything, and nobody else. */
    @Test
    public void theOwnerOnlyListTheWritePathSetsIsAcceptedAgainstItself() {
        final List<AclEntry> ownerOnly = List.of(allow(EDGE, EnumSet.allOf(AclEntryPermission.class)));

        assertTrue(grantsNoMoreThan(ownerOnly, ownerOnly));
        assertFalse(
                grantsNoMoreThan(
                        List.of(allow(EDGE, EnumSet.allOf(AclEntryPermission.class)), allow(ALICE, READ)), ownerOnly),
                "every bit granted to the owner still grants none of them to anybody else");
    }

    /**
     * Length is not a special case. An inherited list on a long-lived installation tree can carry dozens of
     * entries, and the one that decides is still the first one that applies.
     */
    @Test
    public void aLongListIsStillDecidedByTheFirstEntryThatApplies() {
        final List<AclEntry> padded = new ArrayList<>();
        padded.add(deny(ALICE, READ));
        for (int index = 0; index < 64; index++) {
            padded.add(inheritOnly(allow(ALICE, READ_WRITE)));
            padded.add(allow(ADMINISTRATORS, READ));
        }
        padded.add(allow(ALICE, READ_WRITE));

        assertTrue(
                grantsNoMoreThan(padded, List.of(deny(ALICE, READ), allow(ADMINISTRATORS, READ), allow(ALICE, WRITE))),
                "Alice's read was settled by the denial at the front, sixty-four entries ago");
        assertFalse(
                grantsNoMoreThan(padded, List.of(deny(ALICE, READ), allow(ALICE, WRITE))),
                "the administrators' read is granted all the same");
    }

    /**
     * A list that is not there is not a list that grants nothing. The caller has failed to read one, and the
     * write it was about to authorise must not proceed on a cheerful answer.
     */
    @Test
    public void aMissingListIsNotQuietlyAccepted() {
        assertThrows(
                NullPointerException.class, () -> AclComparison.grantsNoMoreThan(null, List.of(allow(EDGE, READ))));
        assertThrows(
                NullPointerException.class, () -> AclComparison.grantsNoMoreThan(List.of(allow(EDGE, READ)), null));
    }

    /**
     * The property the write path relies on when it accepts the strong outcome: a list every one of whose
     * deciding allows names the owner cannot let anyone else in, in any order, under any token. It is not a
     * branch in the implementation — the general rule already decides it — but it is the invariant
     * {@code narrowToItsOwner} is asserting when it asks whether the store took the owner-only list.
     */
    @Test
    public void aListWhoseAllowsAllNameTheOwnerIsOwnerOnly() {
        final List<AclEntry> ownerOnly = List.of(allow(EDGE, READ_WRITE));

        assertTrue(grantsNoMoreThan(
                List.of(deny(ALICE, READ_WRITE), allow(EDGE, READ), allow(EDGE, WRITE), deny(ADMINISTRATORS, READ)),
                ownerOnly));
        assertFalse(
                grantsNoMoreThan(List.of(allow(EDGE, READ_WRITE), allow(ADMINISTRATORS, READ)), ownerOnly),
                "one allow naming anyone else is the whole difference");
    }
}
