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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.hivemq.configuration.reader.ConfigFileReaderWriter.PreservedAttributes;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclEntryType;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.UserPrincipal;
import java.util.EnumSet;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * EDG-882 review v04, finding 1 — the same guarantee as {@link ConfigWritePermissionsTest}, on a file
 * store that has no mode at all.
 * <p>
 * Windows and every other ACL-only store answers {@code null} for the POSIX view, and the previous
 * version of the replace-by-move read that as "there is nothing here to protect": the replacement was
 * created with whatever the containing directory's inheritance grants, every credential in the
 * configuration was written into it, the target's owner was never restored because it was only ever read
 * through the POSIX view, an access-control list that grants nobody anything was discarded as absent, and
 * the verification step returned without checking anything. Five ways for the replacement of a protected
 * {@code config.xml} to end up readable by principals the original was not.
 * <p>
 * It cannot be pinned on the build machine's own file system — neither Linux nor macOS exposes an ACL
 * view through the JDK, so the whole path is dead code there and would stay untested until it ran on a
 * customer's Windows node. Jimfs provides the store instead: owner and ACL views, no POSIX view, which is
 * exactly the shape of the store this is about.
 */
public class ConfigWriteAclPermissionsTest {

    private @NotNull FileSystem fileSystem;
    private @NotNull Path directory;
    private @NotNull Path partial;
    private @NotNull UserPrincipal alice;
    private @NotNull UserPrincipal bob;

    @BeforeEach
    public void anAclOnlyStore() throws IOException {
        fileSystem = Jimfs.newFileSystem(Configuration.windows().toBuilder()
                .setAttributeViews("basic", "owner", "acl")
                .build());
        directory = fileSystem.getPath("C:\\hivemq\\conf");
        Files.createDirectories(directory);
        partial = directory.resolve("config.xml.partial");
        alice = fileSystem.getUserPrincipalLookupService().lookupPrincipalByName("alice");
        bob = fileSystem.getUserPrincipalLookupService().lookupPrincipalByName("bob");
    }

    @AfterEach
    public void close() throws IOException {
        fileSystem.close();
    }

    /** The premise of every test here: no mode, so nothing the POSIX half of the writer can act on. */
    @Test
    public void theStoreUnderTestHasAnAclAndNoMode() throws IOException {
        final Path target = targetReadableBy(bob);

        assertNotNull(Files.getFileAttributeView(target, AclFileAttributeView.class));
        assertNull(
                Files.getFileAttributeView(target, PosixFileAttributeView.class),
                "this store must have no POSIX view, or it does not exercise the path under test");
    }

    // ------------------------------------------------- the replacement is narrowed before it is written

    /**
     * The defect. A replacement created with the store's default access control and then filled with the
     * configuration is readable, for the duration of the write, by whoever the containing directory
     * grants — on every REST write to any subsystem. It is restricted to its own owner instead, while it
     * is still empty.
     */
    @Test
    public void createPartialFile_restrictsTheReplacementToItsOwnerBeforeItIsWritten() throws IOException {
        final PreservedAttributes preserved = ConfigFileReaderWriter.preservedAttributesOf(targetReadableBy(bob));

        ConfigFileReaderWriter.createPartialFile(partial, preserved);

        final List<AclEntry> actual = aclOf(partial);
        assertEquals(1, actual.size(), "the replacement is open to someone other than its owner");
        final AclEntry only = actual.get(0);
        assertEquals(Files.getOwner(partial), only.principal(), "the one entry must name the file's own owner");
        assertEquals(AclEntryType.ALLOW, only.type());
        assertEquals(EnumSet.allOf(AclEntryPermission.class), only.permissions());
    }

    /**
     * And restricted to its owner rather than given the target's list. The creation-time list is not "the
     * target's protection" but "the narrowest protection that can become it": a replacement created
     * readable by bob is disclosed to bob for the whole write, which is the defect, and one created
     * granting only a principal this process is not would leave nobody able to write the configuration
     * into it.
     */
    @Test
    public void createPartialFile_doesNotGiveTheReplacementTheTargetsOwnAcl() throws IOException {
        final Path target = targetReadableBy(bob);
        final PreservedAttributes preserved = ConfigFileReaderWriter.preservedAttributesOf(target);

        ConfigFileReaderWriter.createPartialFile(partial, preserved);

        assertNotEquals(aclOf(target), aclOf(partial), "the replacement was created as open as its target");
        assertFalse(
                namesPrincipal(aclOf(partial), bob),
                "the replacement was created readable by a principal other than its owner");
    }

    /**
     * A partial file left behind by a killed write must not be reused: it carries whatever access control
     * <em>it</em> was left with, which on this store is whatever the directory granted at the time.
     */
    @Test
    public void createPartialFile_replacesAStalePartialRatherThanReusingIt() throws IOException {
        Files.createFile(partial);
        aclViewOf(partial).setAcl(readWriteFor(bob));
        Files.writeString(partial, "left behind by a killed write");

        ConfigFileReaderWriter.createPartialFile(
                partial, ConfigFileReaderWriter.preservedAttributesOf(targetReadableBy(bob)));

        assertFalse(namesPrincipal(aclOf(partial), bob), "a stale partial was reopened and kept its access control");
        assertEquals("", Files.readString(partial), "the stale content survived into the new write");
    }

    /**
     * With nothing to reproduce — a configuration file being created for the first time — the replacement
     * is created as the store creates any file. Narrowing it here would silently change the protection of
     * every newly created configuration, which is a different decision than this one and not one a defect
     * report asked for.
     */
    @Test
    public void createPartialFile_withNothingToReproduce_createsTheFileNormally() throws IOException {
        ConfigFileReaderWriter.createPartialFile(partial, PreservedAttributes.NONE);

        assertTrue(Files.exists(partial), "the replacement must still be created");
    }

    // ------------------------------------------------------------------ what is carried, and put back

    /**
     * The owner is part of the answer to "who can read this", and on this store it was previously read
     * only through the POSIX view — which does not exist here, so it was neither carried nor restored.
     */
    @Test
    public void preservedAttributesOf_carriesTheOwnerAndTheAclWithNoMode() throws IOException {
        final Path target = targetReadableBy(bob);
        Files.getFileAttributeView(target, AclFileAttributeView.class).setOwner(alice);

        final PreservedAttributes preserved = ConfigFileReaderWriter.preservedAttributesOf(target);

        assertEquals(alice, preserved.owner(), "the owner of an ACL-only file was not carried");
        assertEquals(aclOf(target), preserved.acl(), "the access-control list was not carried");
        assertNull(preserved.permissions(), "there is no mode on this store to carry");
        assertFalse(preserved.nothingToReproduce());
    }

    /**
     * An access-control list that grants nobody anything is a protection, not an absence. The previous
     * version discarded the empty list as "nothing to carry", which left the replacement of the most
     * tightly protected configuration file possible with whatever the directory's inheritance produces.
     */
    @Test
    public void preservedAttributesOf_keepsAnAclThatGrantsNobodyAnything() throws IOException {
        final Path target = directory.resolve("config.xml");
        Files.createFile(target);
        aclViewOf(target).setAcl(List.of());

        final PreservedAttributes preserved = ConfigFileReaderWriter.preservedAttributesOf(target);

        assertEquals(List.of(), preserved.acl(), "an empty access-control list was read as 'there is none'");
        assertFalse(
                preserved.nothingToReproduce(),
                "a file only its owner may read must not be replaced by one with the store's defaults");
    }

    /** And it is put back: the replacement ends up granting nobody anything either. */
    @Test
    public void applyPreservedAttributes_reproducesAnAclThatGrantsNobodyAnything() throws IOException {
        final Path target = directory.resolve("config.xml");
        Files.createFile(target);
        aclViewOf(target).setAcl(List.of());
        final PreservedAttributes preserved = ConfigFileReaderWriter.preservedAttributesOf(target);
        ConfigFileReaderWriter.createPartialFile(partial, preserved);

        ConfigFileReaderWriter.applyPreservedAttributes(partial, preserved);

        assertEquals(List.of(), aclOf(partial), "the replacement is readable by someone the target was not");
    }

    /** The ordinary case: the target's own list and owner, exactly, on the replacement. */
    @Test
    public void applyPreservedAttributes_reproducesTheTargetsAclAndOwner() throws IOException {
        final Path target = targetReadableBy(bob);
        aclViewOf(target).setOwner(alice);
        final PreservedAttributes preserved = ConfigFileReaderWriter.preservedAttributesOf(target);
        ConfigFileReaderWriter.createPartialFile(partial, preserved);
        Files.writeString(partial, "<hivemq><bridge><password>s3cr3t</password></bridge></hivemq>");

        ConfigFileReaderWriter.applyPreservedAttributes(partial, preserved);

        assertEquals(aclOf(target), aclOf(partial), "the target's access-control list was not reproduced");
        assertEquals(alice, Files.getOwner(partial), "the target's owner was not reproduced");
    }

    /**
     * The whole sequence in the order the writer runs it, with the assertion in the middle that the
     * end-to-end test cannot make: the configuration is only ever written into a file nobody but its
     * owner can read, and the target's protection is applied afterwards.
     */
    @Test
    public void theReplacementIsNeverWiderThanItsTargetWhileItHoldsTheConfiguration() throws IOException {
        final Path target = targetReadableBy(bob);
        final PreservedAttributes preserved = ConfigFileReaderWriter.preservedAttributesOf(target);

        ConfigFileReaderWriter.createPartialFile(partial, preserved);
        assertFalse(namesPrincipal(aclOf(partial), bob), "the secrets are written into a file bob can read");
        Files.writeString(partial, "<hivemq><bridge><password>s3cr3t</password></bridge></hivemq>");
        assertFalse(namesPrincipal(aclOf(partial), bob), "the file was widened while it still held the secrets");

        ConfigFileReaderWriter.applyPreservedAttributes(partial, preserved);

        assertEquals(aclOf(target), aclOf(partial), "the target's access-control list was not reproduced");
    }

    // ------------------------------------------------------------------------------- and it is proved

    /**
     * The verification step, which on this store used to return without checking anything. A store can
     * accept {@code setAcl} and hold something else — a share that rewrites entries, a mask applied on the
     * way in, and on a store with both a mode and a list, the {@code chmod} this runs afterwards. The
     * replacement is only moved onto the configuration file if what is actually on it is what was on the
     * file it replaces.
     */
    @Test
    public void verifyPreservedAttributes_whenTheAclOnTheReplacementIsNotTheTargetsThenTheWriteIsAborted()
            throws IOException {
        Files.createFile(partial);
        aclViewOf(partial).setAcl(readWriteFor(alice));

        final IOException refused = assertThrows(
                IOException.class,
                () -> ConfigFileReaderWriter.verifyPreservedAttributes(
                        partial, new PreservedAttributes(null, null, null, readWriteFor(bob))),
                "an access-control list that did not take must abort the replacement, not be assumed");
        assertTrue(refused.getMessage().contains("access-control list"), refused.getMessage());
    }

    /**
     * EDG-882 review v04, finding 2.1. A replacement that grants <em>less</em> than the file it replaces is
     * not the disclosure this check exists for, so it is reported rather than refused — demanding an
     * identical list is what would have refused every write on a store that hands back an equivalent one.
     */
    @Test
    public void verifyPreservedAttributes_whenTheReplacementIsNarrowerThanTheTarget_thenTheWriteGoesOn()
            throws IOException {
        Files.createFile(partial);
        aclViewOf(partial).setAcl(List.of());

        ConfigFileReaderWriter.verifyPreservedAttributes(
                partial, new PreservedAttributes(null, null, null, readWriteFor(bob)));
    }

    /** And a list that merely says the same thing differently is not a failure either. */
    @Test
    public void verifyPreservedAttributes_whenTheStoreSplitTheEntries_thenTheWriteGoesOn() throws IOException {
        Files.createFile(partial);
        aclViewOf(partial)
                .setAcl(List.of(
                        AclEntry.newBuilder()
                                .setType(AclEntryType.ALLOW)
                                .setPrincipal(bob)
                                .setPermissions(EnumSet.of(AclEntryPermission.READ_DATA))
                                .build(),
                        AclEntry.newBuilder()
                                .setType(AclEntryType.ALLOW)
                                .setPrincipal(bob)
                                .setPermissions(EnumSet.of(AclEntryPermission.WRITE_DATA))
                                .build()));

        ConfigFileReaderWriter.verifyPreservedAttributes(
                partial, new PreservedAttributes(null, null, null, readWriteFor(bob)));
    }

    /** And it passes what it is meant to pass: the list the replacement actually carries. */
    @Test
    public void verifyPreservedAttributes_whenTheAclOnTheReplacementIsTheTargetsThenTheWriteGoesOn()
            throws IOException {
        Files.createFile(partial);
        aclViewOf(partial).setAcl(readWriteFor(bob));

        ConfigFileReaderWriter.verifyPreservedAttributes(
                partial, new PreservedAttributes(null, null, null, readWriteFor(bob)));
    }

    /** The same for the owner, which is half of what an access-control list means. */
    @Test
    public void applyPreservedAttributes_whenTheOwnerDoesNotTake_thenTheWriteIsAborted() throws IOException {
        final PreservedAttributes preserved = new PreservedAttributes(null, new NeverTheSame("alice"), null, null);
        ConfigFileReaderWriter.createPartialFile(partial, preserved);

        final IOException refused = assertThrows(
                IOException.class,
                () -> ConfigFileReaderWriter.applyPreservedAttributes(partial, preserved),
                "an owner that did not take must abort the replacement, not be assumed");
        assertTrue(refused.getMessage().contains("owned by"), refused.getMessage());
    }

    /**
     * Fail closed at the other end too: protections that cannot be applied at all keep the previous
     * configuration file, which is intact and correctly protected.
     */
    @Test
    public void applyPreservedAttributes_whenTheAclCannotBeApplied_thenTheWriteIsAborted() throws IOException {
        final PreservedAttributes preserved = ConfigFileReaderWriter.preservedAttributesOf(targetReadableBy(bob));
        ConfigFileReaderWriter.createPartialFile(partial, preserved);
        Files.delete(partial); // the replacement is gone, so its protections cannot be set

        assertThrows(
                IOException.class,
                () -> ConfigFileReaderWriter.applyPreservedAttributes(partial, preserved),
                "protections that cannot be reproduced must abort the replacement, not be logged and ignored");
    }

    /**
     * The sequence both the configuration file and its rolling backup now go through, end to end on this
     * store: created restricted to its own owner, filled, given the target's protections, moved onto the
     * target, nothing left beside it.
     */
    @Test
    public void replaceCarryingProtections_writesUnderTheTargetsProtectionsAndLeavesNothingBehind() throws IOException {
        final Path target = targetReadableBy(bob);
        final PreservedAttributes preserved = ConfigFileReaderWriter.preservedAttributesOf(target);

        ConfigFileReaderWriter.replaceCarryingProtections(target, preserved, written -> {
            assertFalse(namesPrincipal(aclOf(written), bob), "the configuration is written into a file bob can read");
            Files.writeString(written, "<hivemq><bridge><password>s3cr3t</password></bridge></hivemq>");
        });

        assertEquals(
                "<hivemq><bridge><password>s3cr3t</password></bridge></hivemq>",
                Files.readString(target),
                "the content did not reach the target");
        assertEquals(aclOf(target), preserved.acl(), "the target's access-control list was not reproduced");
        assertFalse(
                Files.exists(target.resolveSibling(target.getFileName() + ".partial")),
                "a partial file was left beside the target");
    }

    /** A file that does not exist yet has no protections to carry, which is an answer rather than a throw. */
    @Test
    public void preservedAttributesOf_aMissingFile_hasNothingToReproduce() throws IOException {
        final PreservedAttributes preserved =
                ConfigFileReaderWriter.preservedAttributesOf(directory.resolve("not-written-yet.xml"));

        assertTrue(preserved.nothingToReproduce(), "a first-time configuration file has nothing to preserve");
    }

    // ------------------------------------------------------------------------------------------ helpers

    private @NotNull Path targetReadableBy(final @NotNull UserPrincipal principal) throws IOException {
        final Path target = directory.resolve("config.xml");
        Files.createFile(target);
        aclViewOf(target).setAcl(readWriteFor(principal));
        return target;
    }

    private @NotNull AclFileAttributeView aclViewOf(final @NotNull Path path) {
        return Files.getFileAttributeView(path, AclFileAttributeView.class);
    }

    private @NotNull List<AclEntry> aclOf(final @NotNull Path path) throws IOException {
        return aclViewOf(path).getAcl();
    }

    private static @NotNull List<AclEntry> readWriteFor(final @NotNull UserPrincipal principal) {
        return List.of(AclEntry.newBuilder()
                .setType(AclEntryType.ALLOW)
                .setPrincipal(principal)
                .setPermissions(EnumSet.of(AclEntryPermission.READ_DATA, AclEntryPermission.WRITE_DATA))
                .build());
    }

    private static boolean namesPrincipal(final @NotNull List<AclEntry> acl, final @NotNull UserPrincipal principal) {
        return acl.stream().anyMatch(entry -> entry.principal().equals(principal));
    }

    /**
     * A principal that is never equal to itself: a file store that stores something other than what it
     * was handed, seen from the only side this code has.
     */
    private record NeverTheSame(@NotNull String name) implements UserPrincipal {

        @Override
        public @NotNull String getName() {
            return name;
        }

        @Override
        public boolean equals(final Object other) {
            return false;
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }
    }
}
