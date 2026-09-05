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
import static com.hivemq.configuration.reader.AclComparisonFixture.BITS;
import static com.hivemq.configuration.reader.AclComparisonFixture.EDGE;
import static com.hivemq.configuration.reader.AclComparisonFixture.PRINCIPALS;
import static com.hivemq.configuration.reader.AclComparisonFixture.READ;
import static com.hivemq.configuration.reader.AclComparisonFixture.READ_WRITE;
import static com.hivemq.configuration.reader.AclComparisonFixture.WRITE;
import static com.hivemq.configuration.reader.AclComparisonFixture.allow;
import static com.hivemq.configuration.reader.AclComparisonFixture.alphabet;
import static com.hivemq.configuration.reader.AclComparisonFixture.deny;
import static com.hivemq.configuration.reader.AclComparisonFixture.inheritOnly;
import static com.hivemq.configuration.reader.AclComparisonFixture.listsUpTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryFlag;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclEntryType;
import java.nio.file.attribute.UserPrincipal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

/**
 * EDG-882 — the part that closes the class of defect rather than the three instances of it.
 * <p>
 * Reviews v04, v05 and v06 each supplied one more clause of the access-check specification, and each had to
 * arrive from outside, because nothing here could produce the next one. The code path exists for NTFS and
 * for Solaris ZFS, {@code AclFileAttributeView} is not provided on Linux or macOS, and every workflow in
 * this repository runs on Ubuntu — so no test that goes near a real file can be the thing that catches it.
 * <p>
 * This is the thing that catches it. {@link #grantedBy} is the access check itself, written straight from
 * MS-DTYP §2.5.3.2 and RFC 7530 §6.2.1: walk the list in order, and the first entry that applies to the
 * object, names an identity the requester holds and mentions the permission being asked for settles it.
 * A requester's identities are given to it explicitly as a token, so <em>every</em> arrangement of users and
 * groups is covered by enumerating every token — which is exactly the thing {@code java.nio} refuses to tell
 * us, and the reason the previous implementation had to guess at it.
 * <p>
 * {@link AclComparison#grantsNoMoreThan} is then asserted to agree with that oracle <b>exactly</b>, over an
 * exhaustively generated universe of lists: not merely to be safe where the oracle is safe, but to accept
 * precisely what the oracle accepts. Soundness would leave room for a v07 that reports a widening we let
 * through; exactness leaves none, and the other direction — refusing a write that discloses nothing — is
 * what review v04's finding 2.1 was, so it is worth pinning too.
 * <p>
 * The two algorithms are unrelated: the oracle quantifies over tokens, the implementation over pairs of
 * principals. They agree because a witness never needs more than two identities, which is the observation
 * the implementation is built on and this is the proof of.
 */
public class AclComparisonOracleTest {

    /**
     * The access check, for one requester and one permission bit.
     * <p>
     * The requester is a token: the set of identities they hold, their own and every group they belong to,
     * which is what both specifications match entries against. Entries that do not decide access to this
     * object take no part — audit and alarm entries grant nothing, and {@code INHERIT_ONLY} says the entry
     * does not apply to the object carrying it. A list with no entry that applies grants nothing.
     */
    private static boolean grantedBy(
            final @NotNull List<AclEntry> acl,
            final @NotNull Set<UserPrincipal> token,
            final @NotNull AclEntryPermission bit) {
        for (final AclEntry entry : acl) {
            if (entry.type() != AclEntryType.ALLOW && entry.type() != AclEntryType.DENY) {
                continue;
            }
            if (entry.flags().contains(AclEntryFlag.INHERIT_ONLY)) {
                continue;
            }
            if (!token.contains(entry.principal())) {
                continue;
            }
            if (!entry.permissions().contains(bit)) {
                continue;
            }
            return entry.type() == AclEntryType.ALLOW;
        }
        return false;
    }

    /** Every token that can be formed from the principals in play — every membership arrangement at once. */
    private static @NotNull List<Set<UserPrincipal>> everyToken(final @NotNull List<UserPrincipal> principals) {
        final List<Set<UserPrincipal>> tokens = new ArrayList<>(1 << principals.size());
        for (int bits = 0; bits < (1 << principals.size()); bits++) {
            final Set<UserPrincipal> token = new LinkedHashSet<>();
            for (int index = 0; index < principals.size(); index++) {
                if ((bits & (1 << index)) != 0) {
                    token.add(principals.get(index));
                }
            }
            tokens.add(Set.copyOf(token));
        }
        return tokens;
    }

    /**
     * What a list grants, as one bit per (token, permission) pair — everything about a list that an access
     * check can observe, in a {@code long}.
     * <p>
     * With this, "grants no more than" is one machine instruction: {@code candidate} grants nothing
     * {@code reference} does not exactly when {@code candidate & ~reference} is zero.
     */
    private static long profile(
            final @NotNull List<AclEntry> acl,
            final @NotNull List<Set<UserPrincipal>> tokens,
            final @NotNull List<AclEntryPermission> bits) {
        long profile = 0;
        int position = 0;
        for (final Set<UserPrincipal> token : tokens) {
            for (final AclEntryPermission bit : bits) {
                if (grantedBy(acl, token, bit)) {
                    profile |= 1L << position;
                }
                position++;
            }
        }
        return profile;
    }

    // -------------------------------------------------------------------- the oracle, checked by hand first

    /**
     * The oracle is the yardstick for everything below, so it is worth a handful of cases whose answer can
     * be read off the specification without running anything.
     */
    @Test
    public void theOracleImplementsTheAccessCheck() {
        final List<AclEntry> denyThenAllow = List.of(deny(ALICE, READ), allow(ADMINISTRATORS, READ));
        final List<AclEntry> allowThenDeny = List.of(allow(ADMINISTRATORS, READ), deny(ALICE, READ));
        final Set<UserPrincipal> aliceTheAdministrator = Set.of(ALICE, ADMINISTRATORS);

        assertFalse(
                grantedBy(denyThenAllow, aliceTheAdministrator, AclEntryPermission.READ_DATA),
                "her own denial comes first and settles the read");
        assertTrue(
                grantedBy(allowThenDeny, aliceTheAdministrator, AclEntryPermission.READ_DATA),
                "the group's allow comes first and settles it the other way");
        assertFalse(
                grantedBy(allowThenDeny, Set.of(ALICE), AclEntryPermission.READ_DATA),
                "someone who is not an administrator meets only the denial");
        assertFalse(
                grantedBy(List.of(allow(EDGE, READ_WRITE)), Set.of(ALICE), AclEntryPermission.READ_DATA),
                "a list that names nobody in the token grants nothing");
        assertFalse(
                grantedBy(List.of(inheritOnly(allow(ALICE, READ))), Set.of(ALICE), AclEntryPermission.READ_DATA),
                "an entry that does not apply to the object decides nothing about it");
        assertFalse(
                grantedBy(List.of(allow(ALICE, READ)), Set.of(ALICE), AclEntryPermission.WRITE_DATA),
                "an entry that does not mention the permission decides nothing about it");
    }

    // --------------------------------------------------------------------------------- exhaustive agreement

    /**
     * Every list of up to two entries over the full alphabet — three principals, both types, all three
     * non-empty permission masks, both states of {@code INHERIT_ONLY} — against every other, which is a
     * little under two million pairs.
     */
    @Test
    public void agreesWithTheOracleOnEveryShortListOverTheFullAlphabet() {
        assertAgreementOver(listsUpTo(alphabet(PRINCIPALS, List.of(READ, WRITE, READ_WRITE)), 2));
    }

    /**
     * The same, traded the other way: one permission bit, so that lists can be a third longer without the
     * universe exploding. Order and shadowing need length to go wrong, and this is where three entries deep
     * gets covered.
     */
    @Test
    public void agreesWithTheOracleOnEveryLongerSinglePermissionList() {
        assertAgreementOver(listsUpTo(alphabet(PRINCIPALS, List.of(READ)), 3));
    }

    /**
     * Every list of up to two entries over an alphabet that also contains the two types which grant nothing.
     * <p>
     * They are not decoration. An audit entry mistaken for a decision sits in front of the allow behind it
     * and hides it, so a replacement that grants a principal access looks like one that grants none and the
     * write is let through — a widening accepted, not merely a safe write refused. The permission alphabet
     * is one bit here to pay for the extra types.
     */
    @Test
    public void agreesWithTheOracleOnListsThatGrantNothing() {
        assertAgreementOver(listsUpTo(alphabet(PRINCIPALS, AclComparisonFixture.ALL_TYPES, List.of(READ)), 2));
    }

    /**
     * And beyond what can be enumerated: lists of up to eight entries over every type and every mask, drawn
     * deterministically so a failure is reproducible from the seed alone. Nothing here is expected to fail
     * that the exhaustive passes would not have caught, which is the claim being tested — that the small
     * universes above are not small enough to be hiding anything.
     */
    @Test
    public void agreesWithTheOracleOnDeeperRandomLists() {
        final List<AclEntry> alphabet =
                alphabet(PRINCIPALS, AclComparisonFixture.ALL_TYPES, List.of(READ, WRITE, READ_WRITE));
        final List<Set<UserPrincipal>> tokens = everyToken(PRINCIPALS);
        final Random random = new Random(882L);

        for (int iteration = 0; iteration < 200_000; iteration++) {
            final List<AclEntry> candidate = randomList(alphabet, random);
            final List<AclEntry> reference = randomList(alphabet, random);
            assertAgreement(candidate, reference, profile(candidate, tokens, BITS), profile(reference, tokens, BITS));
        }
    }

    /**
     * The claim the whole implementation rests on, tested where it could actually fail.
     * <p>
     * A witness needs at most two principals, so the implementation tries pairs and never triples. With
     * three identities in play that is nearly vacuous — a pair is most of a triple. This pass puts four in
     * play and presents all sixteen subsets of them as tokens, so an implementation that in fact needed
     * three identities to see a widening would be caught here, and here only.
     */
    @Test
    public void agreesWithTheOracleWithAFourthIdentityInPlay() {
        assertAgreementOver(
                listsUpTo(alphabet(AclComparisonFixture.FOUR_PRINCIPALS, List.of(READ, WRITE, READ_WRITE)), 2),
                AclComparisonFixture.FOUR_PRINCIPALS);
    }

    private static @NotNull List<AclEntry> randomList(
            final @NotNull List<AclEntry> alphabet, final @NotNull Random random) {
        final int length = random.nextInt(9);
        final List<AclEntry> acl = new ArrayList<>(length);
        for (int index = 0; index < length; index++) {
            acl.add(alphabet.get(random.nextInt(alphabet.size())));
        }
        return List.copyOf(acl);
    }

    // ------------------------------------------------------------------------------ algebraic properties

    /**
     * Who the principals are carries no meaning to a comparison that cannot look any of them up, so renaming
     * every one of them through a bijection must change no answer.
     * <p>
     * What this catches is an accidental dependence on identity rather than on structure — a set iterated in
     * hash order deciding which pair is tried first, a principal treated specially because it happens to be
     * the one the tests call the owner.
     */
    @Test
    public void theAnswerDoesNotDependOnWhoThePrincipalsAre() {
        final Map<UserPrincipal, UserPrincipal> rotated =
                Map.of(EDGE, ALICE, ALICE, ADMINISTRATORS, ADMINISTRATORS, AclComparisonFixture.SYSTEM);

        forRandomPairs((candidate, reference) -> assertEquals(
                AclComparison.grantsNoMoreThan(candidate, reference),
                AclComparison.grantsNoMoreThan(
                        AclComparisonFixture.renamePrincipals(candidate, rotated),
                        AclComparisonFixture.renamePrincipals(reference, rotated)),
                () -> "renaming the principals changed the answer for " + render(candidate) + " against "
                        + render(reference)));
    }

    /**
     * And no permission bit is special either. The two the tests are written in terms of are a read and a
     * write because those are what a configuration file discloses, but the comparison never mentions them:
     * swapping them for the two bits that decide who may take the file over must change no answer.
     */
    @Test
    public void theAnswerDoesNotDependOnWhichPermissionBitsAreUsed() {
        final Map<AclEntryPermission, AclEntryPermission> swapped = Map.of(
                AclEntryPermission.READ_DATA, AclEntryPermission.WRITE_OWNER,
                AclEntryPermission.WRITE_DATA, AclEntryPermission.WRITE_ACL);

        forRandomPairs((candidate, reference) -> assertEquals(
                AclComparison.grantsNoMoreThan(candidate, reference),
                AclComparison.grantsNoMoreThan(
                        AclComparisonFixture.renamePermissions(candidate, swapped),
                        AclComparisonFixture.renamePermissions(reference, swapped)),
                () -> "renaming the permissions changed the answer for " + render(candidate) + " against "
                        + render(reference)));
    }

    /**
     * Both specifications settle one permission bit at a time, and the implementation says so in its shape.
     * This is that claim on its own: judging two lists whole gives the same answer as judging each bit's
     * slice of them separately and taking every answer together.
     */
    @Test
    public void oneBitAtATimeIsTheSameAsAllBitsAtOnce() {
        forRandomPairs((candidate, reference) -> {
            boolean bitByBit = true;
            for (final AclEntryPermission bit : BITS) {
                bitByBit &= AclComparison.grantsNoMoreThan(
                        AclComparisonFixture.project(candidate, bit), AclComparisonFixture.project(reference, bit));
            }
            assertEquals(
                    AclComparison.grantsNoMoreThan(candidate, reference),
                    bitByBit,
                    () -> "the bits do not decide independently for " + render(candidate) + " against "
                            + render(reference));
        });
    }

    /**
     * "Grants no more than" orders lists, so it has to be transitive: a replacement no wider than the file it
     * replaces, which is itself no wider than what an operator set, is no wider than what the operator set.
     * <p>
     * The write path leans on this without saying so — the partial file is judged against the target, then
     * the written file is judged again — and a comparison that was merely nearly right would be free to
     * break it.
     */
    @Test
    public void theRelationIsTransitive() {
        final List<AclEntry> alphabet =
                alphabet(PRINCIPALS, AclComparisonFixture.ALL_TYPES, List.of(READ, WRITE, READ_WRITE));
        final Random random = new Random(884L);
        final List<List<AclEntry>> sample = new ArrayList<>(150);
        for (int index = 0; index < 150; index++) {
            sample.add(randomList(alphabet, random));
        }

        final boolean[][] noWiderThan = new boolean[sample.size()][sample.size()];
        for (int one = 0; one < sample.size(); one++) {
            for (int other = 0; other < sample.size(); other++) {
                noWiderThan[one][other] = AclComparison.grantsNoMoreThan(sample.get(one), sample.get(other));
            }
        }
        for (int a = 0; a < sample.size(); a++) {
            assertTrue(noWiderThan[a][a], () -> "not reflexive");
            for (int b = 0; b < sample.size(); b++) {
                if (!noWiderThan[a][b]) {
                    continue;
                }
                for (int c = 0; c < sample.size(); c++) {
                    if (noWiderThan[b][c] && !noWiderThan[a][c]) {
                        final int first = a;
                        final int second = b;
                        final int third = c;
                        fail(() -> "not transitive: " + render(sample.get(first)) + " is no wider than "
                                + render(sample.get(second)) + ", which is no wider than "
                                + render(sample.get(third)) + ", but the first is judged wider than the third");
                    }
                }
            }
        }
    }

    /** A deterministic sweep of list pairs, for the properties that hold whatever two lists are given. */
    private static void forRandomPairs(final @NotNull PairAssertion assertion) {
        final List<AclEntry> alphabet =
                alphabet(PRINCIPALS, AclComparisonFixture.ALL_TYPES, List.of(READ, WRITE, READ_WRITE));
        final Random random = new Random(883L);
        for (int iteration = 0; iteration < 100_000; iteration++) {
            assertion.check(randomList(alphabet, random), randomList(alphabet, random));
        }
    }

    @FunctionalInterface
    private interface PairAssertion {

        void check(@NotNull List<AclEntry> candidate, @NotNull List<AclEntry> reference);
    }

    private static void assertAgreementOver(final @NotNull List<List<AclEntry>> lists) {
        assertAgreementOver(lists, PRINCIPALS);
    }

    private static void assertAgreementOver(
            final @NotNull List<List<AclEntry>> lists, final @NotNull List<UserPrincipal> principals) {
        final List<Set<UserPrincipal>> tokens = everyToken(principals);
        final long[] profiles = new long[lists.size()];
        for (int index = 0; index < lists.size(); index++) {
            profiles[index] = profile(lists.get(index), tokens, BITS);
        }
        for (int candidate = 0; candidate < lists.size(); candidate++) {
            for (int reference = 0; reference < lists.size(); reference++) {
                assertAgreement(lists.get(candidate), lists.get(reference), profiles[candidate], profiles[reference]);
            }
        }
    }

    private static void assertAgreement(
            final @NotNull List<AclEntry> candidate,
            final @NotNull List<AclEntry> reference,
            final long candidateProfile,
            final long referenceProfile) {
        final boolean expected = (candidateProfile & ~referenceProfile) == 0;
        if (AclComparison.grantsNoMoreThan(candidate, reference) != expected) {
            fail((expected
                            ? "refused a replacement that grants nobody anything the file it replaces does not"
                            : "accepted a replacement that grants access the file it replaces does not")
                    + "\n  candidate: " + render(candidate) + "\n  reference: " + render(reference));
        }
    }

    private static @NotNull String render(final @NotNull List<AclEntry> acl) {
        if (acl.isEmpty()) {
            return "[]";
        }
        return acl.stream()
                .map(entry -> entry.type()
                        + " " + entry.principal().getName()
                        + " "
                        + entry.permissions().stream().map(Enum::name).sorted().collect(Collectors.joining("+"))
                        + (entry.flags().contains(AclEntryFlag.INHERIT_ONLY) ? " (inherit-only)" : ""))
                .collect(Collectors.joining(", ", "[", "]"));
    }

    // ------------------------------------------------- the reported findings, rediscovered rather than recited

    /**
     * A named regression test proves a fix. It does not prove the search that has to find the next one is
     * wide enough to have found this one — so the shapes reviews v05 and v06 reported are asserted to be
     * inside the exhaustively enumerated universe, and therefore to have been judged there against the
     * oracle rather than only here against a hand-written expectation.
     * <p>
     * If a future change narrows the alphabet or the lengths, this is what says so.
     */
    @Test
    public void theReportedFindingsAreInsideTheExhaustedUniverse() {
        final List<List<AclEntry>> universe = listsUpTo(alphabet(PRINCIPALS, List.of(READ, WRITE, READ_WRITE)), 2);

        final List<AclEntry> v06Candidate = List.of(allow(ADMINISTRATORS, READ), deny(ALICE, READ));
        final List<AclEntry> v06Reference = List.of(deny(ALICE, READ), allow(ADMINISTRATORS, READ));
        final List<AclEntry> v05Candidate = List.of(allow(ALICE, READ), deny(ALICE, READ));
        final List<AclEntry> v05Reference = List.of(deny(ALICE, READ), allow(ALICE, READ));
        final List<AclEntry> inheritOnlyCandidate = List.of(allow(ALICE, READ));
        final List<AclEntry> inheritOnlyReference = List.of(inheritOnly(allow(ALICE, READ)));

        assertTrue(universe.contains(v06Candidate), "review v06's replacement is not among the lists enumerated");
        assertTrue(universe.contains(v06Reference), "review v06's target is not among the lists enumerated");
        assertTrue(universe.contains(v05Candidate), "review v05's replacement is not among the lists enumerated");
        assertTrue(universe.contains(v05Reference), "review v05's target is not among the lists enumerated");
        assertTrue(universe.contains(inheritOnlyCandidate), "the INHERIT_ONLY replacement is not enumerated");
        assertTrue(universe.contains(inheritOnlyReference), "the INHERIT_ONLY target is not enumerated");

        // And that the oracle, independently of the implementation, calls them widenings.
        final List<Set<UserPrincipal>> tokens = everyToken(PRINCIPALS);
        assertFalse(
                (profile(v06Candidate, tokens, BITS) & ~profile(v06Reference, tokens, BITS)) == 0,
                "the oracle should see review v06's reorder as a widening");
        assertFalse(
                (profile(v05Candidate, tokens, BITS) & ~profile(v05Reference, tokens, BITS)) == 0,
                "the oracle should see review v05's reorder as a widening");
        assertFalse(
                (profile(inheritOnlyCandidate, tokens, BITS) & ~profile(inheritOnlyReference, tokens, BITS)) == 0,
                "the oracle should see a dropped INHERIT_ONLY as a widening");
    }

    /**
     * The measurement review v04's finding 2.1 asks for: how often the comparison refuses a replacement that
     * would in fact have disclosed nothing. A rule that refuses every write on a store that normalises is
     * not a safeguard, and this is the number that says whether we have one.
     * <p>
     * It is zero, because the implementation is exact rather than conservative — but it is asserted here as
     * a count over the whole universe rather than inferred from the agreement tests, so that a future change
     * trading exactness for simplicity has to state what it cost.
     */
    @Test
    public void nothingSafeIsRefused() {
        final List<List<AclEntry>> lists = listsUpTo(alphabet(PRINCIPALS, List.of(READ, WRITE, READ_WRITE)), 2);
        final List<Set<UserPrincipal>> tokens = everyToken(PRINCIPALS);
        final long[] profiles = new long[lists.size()];
        for (int index = 0; index < lists.size(); index++) {
            profiles[index] = profile(lists.get(index), tokens, BITS);
        }

        long refusedButSafe = 0;
        for (int candidate = 0; candidate < lists.size(); candidate++) {
            for (int reference = 0; reference < lists.size(); reference++) {
                if ((profiles[candidate] & ~profiles[reference]) == 0
                        && !AclComparison.grantsNoMoreThan(lists.get(candidate), lists.get(reference))) {
                    refusedButSafe++;
                }
            }
        }
        assertEquals(0L, refusedButSafe, "configuration writes refused that would have disclosed nothing");
    }
}
