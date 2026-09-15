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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Whether one access-control list grants anybody anything another one does not.
 * <p>
 * {@code config.xml} holds bridge passwords, keystore and truststore passwords and adapter credentials, and
 * it is replaced by writing a new file beside it and moving that onto it. On a store that expresses
 * protection as an access-control list rather than as a mode, something has to decide whether the
 * replacement is safe to write those credentials into. This is that decision and nothing else.
 *
 * <h2>Why the obvious implementations are wrong</h2>
 *
 * Two specifications govern the lists this code sees, and neither evaluates a list against <em>a</em>
 * principal:
 * <ul>
 *   <li><b>RFC 7530 §6.2.1</b> — the NFSv4 model that
 *       {@link java.nio.file.attribute.AclFileAttributeView} declares itself to be based on. The list is
 *       ordered; the first entry whose <em>who</em> matches the requester and whose mask covers a requested
 *       permission settles it. The <em>who</em> may be a group or a special identifier such as
 *       {@code EVERYONE@}.</li>
 *   <li><b>MS-DTYP §2.5.3.2</b> — the Windows access check, which is what actually runs once the JDK has
 *       written one of these lists to NTFS. The DACL is walked in order against the requester's <em>access
 *       token</em>: their own SID, every group SID they hold, and well-known SIDs. Any entry naming any of
 *       them matches.</li>
 * </ul>
 * So a check is made against a <em>set</em> of identities, and an entry naming a group can settle a
 * permission for a user before a later entry naming that user is reached. Comparing per-principal
 * summaries misses it, comparing entry lists for equality refuses every store that normalises anything, and
 * evaluating faithfully looks like it needs group membership — which {@code java.nio} does not expose:
 * {@link java.nio.file.attribute.UserPrincipalLookupService} maps a name to a principal and nothing in the
 * other direction. {@link java.nio.file.attribute.GroupPrincipal} is a <em>subtype</em> of
 * {@link UserPrincipal}, so a structure keyed by {@link AclEntry#principal()} treats a group as a peer of a
 * user rather than a superset of them, and no compiler will say so (EDG-882 reviews v04, v05, v06).
 *
 * <h2>The observation this is built on</h2>
 *
 * The unknown is which principals share a token, so no assumption is made about it: the comparison
 * quantifies over <em>every</em> token, which is the conservative closure over what we are not told. That
 * is finite because of one fact.
 * <p>
 * <b>A witness never needs more than two principals.</b> Suppose some token is granted a permission by the
 * candidate and not by the reference. Let {@code x} be the principal of the candidate entry that settled it
 * and {@code y} the principal of the reference entry that settled it the other way — or {@code y = x} when
 * the reference has no entry that matches at all. Then {@code {x, y}} is a witness too: every entry ahead
 * of the deciding one failed to match the larger token, so it fails to match the smaller one, and both
 * deciding entries still match.
 * <p>
 * Trying every pair is therefore not an approximation of quantifying over tokens, it <em>is</em> quantifying
 * over tokens — so this accepts exactly what a real access check accepts, and there is no arrangement of
 * groups, however contrived, that it gets wrong. {@code AclComparisonOracleTest} asserts that agreement
 * against a direct implementation of the access check rather than leaving it as an argument.
 * <p>
 * The rest is bookkeeping. Both specifications settle one permission bit at a time — Windows refuses a
 * request when any requested bit meets a {@code DENY} and grants it when every bit meets an {@code ALLOW} —
 * so each bit is decided on its own, and only the bits the candidate can actually grant are worth trying.
 * For one bit, all that matters about a principal is where the list first mentions them and which way that
 * entry went; {@link #firstDecisions} reduces a list to exactly that, once per bit, and a pair is then
 * decided by whichever of its two principals the list reaches first.
 *
 * <h2>What takes part</h2>
 *
 * <ul>
 *   <li>{@code ALLOW} and {@code DENY} decide. {@code AUDIT} and {@code ALARM} grant nothing and take no
 *       part — read as decisions they would stand in front of the entry behind them and hide it, so a
 *       replacement that grants access would look like one that grants none.</li>
 *   <li>{@code INHERIT_ONLY} says the entry does not apply to the object carrying it, so it decides nothing
 *       about this file. It is not a propagation flag, and reading it as one was an error of ours rather
 *       than a gap (review v05).</li>
 *   <li>{@code FILE_INHERIT}, {@code DIRECTORY_INHERIT} and {@code NO_PROPAGATE_INHERIT} govern what a
 *       <em>directory</em> gives to what is created inside it. A file gives nothing to anything, so they
 *       change no answer here and are never interpreted.</li>
 *   <li>Two principals are the same when they are {@link Object#equals equal} — SID or UID equality on the
 *       stores this runs on, and what the platform itself compares.</li>
 *   <li>An empty list grants nobody anything. On Windows a NULL DACL (everyone, full access) and an empty
 *       DACL (nobody, anything) both arrive here as an empty list and mean opposite things; reading it as
 *       the narrow one can cost a refused write and cannot cost a disclosure.</li>
 * </ul>
 *
 * <p>
 * Stateless, and safe to call from any number of threads at once.
 */
final class AclComparison {

    private AclComparison() {}

    /**
     * Whether {@code candidate} grants no principal, under any token, anything {@code reference} does not.
     * <p>
     * Precise in both directions rather than merely safe in one: a list granting strictly less than the
     * reference is accepted, because nobody gains anything by it, and a caller that cares about a narrowing
     * asks the question the other way round.
     */
    static boolean grantsNoMoreThan(final @NotNull List<AclEntry> candidate, final @NotNull List<AclEntry> reference) {
        for (final AclEntryPermission bit : bitsTheCandidateCanGrant(candidate)) {
            final Map<UserPrincipal, Decision> byCandidate = firstDecisions(candidate, bit);
            final List<UserPrincipal> allowed = principalsAllowedBy(byCandidate);
            if (allowed.isEmpty()) {
                continue;
            }
            final Map<UserPrincipal, Decision> byReference = firstDecisions(reference, bit);
            final Set<UserPrincipal> named = principalsNamedBy(byCandidate, byReference);
            // One of a witness pair is always a principal the candidate allows -- it is the entry that
            // settled the bit for that token, and it settled it by allowing. The other can be anyone
            // either list names, itself included: that is the token of one identity.
            for (final UserPrincipal one : allowed) {
                for (final UserPrincipal other : named) {
                    if (grants(byCandidate, one, other) && !grants(byReference, one, other)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * Where a list first mentions a principal for one permission, and which way that entry went.
     * <p>
     * Everything an access check for that permission can observe about that principal. The position is an
     * index into the list the decision came from, and only ever compared with another index from the same
     * list.
     */
    private record Decision(int position, boolean allows) {}

    /**
     * Whether a list grants a permission to a requester holding exactly these two identities.
     * <p>
     * The list is walked in order, so of the two entries that could settle it the earlier one does, and a
     * list that mentions neither identity grants nothing — which is the default in both specifications.
     */
    private static boolean grants(
            final @NotNull Map<UserPrincipal, Decision> decisions,
            final @NotNull UserPrincipal one,
            final @NotNull UserPrincipal other) {
        final Decision settled = earlier(decisions.get(one), decisions.get(other));
        return settled != null && settled.allows();
    }

    private static @Nullable Decision earlier(final @Nullable Decision one, final @Nullable Decision other) {
        if (one == null) {
            return other;
        }
        if (other == null) {
            return one;
        }
        return one.position() <= other.position() ? one : other;
    }

    /**
     * The list reduced to what an access check for one permission can see: for each principal it names, the
     * entry that settles that permission for them.
     * <p>
     * {@code putIfAbsent} is the ordering rule itself — the first entry that applies to the file, names the
     * principal and mentions the permission settles it, and no later entry can reopen it.
     */
    private static @NotNull Map<UserPrincipal, Decision> firstDecisions(
            final @NotNull List<AclEntry> acl, final @NotNull AclEntryPermission bit) {
        final Map<UserPrincipal, Decision> decisions = new HashMap<>();
        int position = 0;
        for (final AclEntry entry : acl) {
            position++;
            if (decides(entry) && entry.permissions().contains(bit)) {
                decisions.putIfAbsent(entry.principal(), new Decision(position, entry.type() == AclEntryType.ALLOW));
            }
        }
        return decisions;
    }

    /** Whether an entry decides access to the object carrying the list, rather than to something else. */
    private static boolean decides(final @NotNull AclEntry entry) {
        return (entry.type() == AclEntryType.ALLOW || entry.type() == AclEntryType.DENY)
                && !entry.flags().contains(AclEntryFlag.INHERIT_ONLY);
    }

    /**
     * The permissions the candidate could grant anybody at all. One it never allows is one it cannot grant
     * where the reference does not, whatever the reference says about it.
     */
    private static @NotNull Set<AclEntryPermission> bitsTheCandidateCanGrant(final @NotNull List<AclEntry> candidate) {
        final Set<AclEntryPermission> bits = EnumSet.noneOf(AclEntryPermission.class);
        for (final AclEntry entry : candidate) {
            if (entry.type() == AclEntryType.ALLOW && decides(entry)) {
                bits.addAll(entry.permissions());
            }
        }
        return bits;
    }

    private static @NotNull List<UserPrincipal> principalsAllowedBy(
            final @NotNull Map<UserPrincipal, Decision> decisions) {
        final List<UserPrincipal> allowed = new ArrayList<>(decisions.size());
        for (final Map.Entry<UserPrincipal, Decision> decision : decisions.entrySet()) {
            if (decision.getValue().allows()) {
                allowed.add(decision.getKey());
            }
        }
        return allowed;
    }

    /**
     * Every principal either list settles this permission for. A principal neither of them names is matched
     * by neither, so it cannot tell them apart and adding it to a token changes no answer.
     */
    private static @NotNull Set<UserPrincipal> principalsNamedBy(
            final @NotNull Map<UserPrincipal, Decision> byCandidate,
            final @NotNull Map<UserPrincipal, Decision> byReference) {
        final Set<UserPrincipal> named = new HashSet<>(byCandidate.keySet());
        named.addAll(byReference.keySet());
        return named;
    }
}
