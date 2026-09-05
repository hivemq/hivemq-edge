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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.hivemq.configuration.reader.ConfigFileReaderWriter.PreservedAttributes;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.attribute.UserPrincipal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * EDG-882 review v03, R3-07 and R3-09 — the replacement configuration file must never be readable by
 * anyone the file it replaces was not, at any point while it holds the configuration.
 * <p>
 * {@code config.xml} carries bridge passwords, keystore and truststore passwords and adapter
 * credentials. The first version of the replace-by-move wrote the rendered document into a file created
 * by {@code FileWriter}, which takes the process umask — commonly {@code 0644} — and only narrowed it to
 * the target's mode afterwards. Between those two steps a world-readable file held every secret in the
 * configuration, on every REST write to any subsystem (R3-07).
 * <p>
 * The second version fixed the timing but carried only the mode. A mode names a group without naming
 * <em>which</em> group, and a newly created file takes its group from the creating process or from the
 * directory — so a {@code 0640} file could be replaced by a {@code 0640} file whose group-read was
 * granted to an entirely different set of principals, and a failure to read the original's protections
 * was treated as "there are none" and the write proceeded on the umask (R3-09).
 * <p>
 * That window did not exist before the branch that introduced it: writing in place reused the file's own
 * inode and therefore its own mode, group and ACL. It is a regression, which is why the behaviour here
 * fails closed rather than best-effort.
 * <p>
 * Fails closed on <em>access</em>, which is not the same as refusing the write. A protection this node was
 * never going to be able to set — an owner it may not assign, a group it is not a member of — narrows the
 * replacement instead of aborting it, because aborting means the configuration the operator just asked for
 * is not persisted and comes back missing on the next restart. Both halves are pinned below.
 * <p>
 * The creation-time property is the one that matters and it cannot be observed end to end — the write is
 * synchronous and the temporary file is gone by the time any assertion could run — so it is pinned
 * directly on the helpers that establish it. {@code ConfigFileWriterTest} covers the end-to-end mode
 * preservation and the symbolic-link case.
 */
public class ConfigWritePermissionsTest {

    private static final @NotNull Set<PosixFilePermission> OWNER_ONLY = PosixFilePermissions.fromString("rw-------");
    private static final @NotNull Set<PosixFilePermission> WORLD_READABLE =
            PosixFilePermissions.fromString("rw-r--r--");
    private static final @NotNull Set<PosixFilePermission> WORLD_WRITABLE =
            PosixFilePermissions.fromString("rw-rw-rw-");
    private static final @NotNull Set<PosixFilePermission> GROUP_READABLE =
            PosixFilePermissions.fromString("rw-r-----");

    @TempDir
    private @NotNull Path directory;

    private @NotNull Path partial;

    @BeforeEach
    public void requirePosix() {
        assumeTrue(
                directory.getFileSystem().supportedFileAttributeViews().contains("posix"),
                "no POSIX permissions on this file system, so there is nothing to protect");
        partial = directory.resolve("config.xml.partial");
    }

    private @NotNull Path targetWith(final @NotNull Set<PosixFilePermission> mode) throws IOException {
        final Path target = directory.resolve("config.xml");
        Files.createFile(target, PosixFilePermissions.asFileAttribute(mode));
        return target;
    }

    // ---------------------------------------------------------------- R3-07: the disclosure window

    /**
     * The defect, at the only moment it is observable. A target the operator restricted to {@code 0600}
     * must not produce a replacement that is briefly readable by everyone — so the file is created
     * owner-only and widened afterwards, never the other way round.
     */
    @Test
    public void createPartialFile_createsTheReplacementOwnerOnly() throws IOException {
        ConfigFileReaderWriter.createPartialFile(
                partial, ConfigFileReaderWriter.preservedAttributesOf(targetWith(OWNER_ONLY)));

        assertEquals(
                OWNER_ONLY,
                Files.getPosixFilePermissions(partial),
                "the replacement was readable by someone before it had been written");
    }

    /**
     * And owner-only even when the file it will replace is wider. The creation mode is not "the target's
     * mode" but "the narrowest mode that can become the target's mode": a file created as {@code 0644}
     * and written is disclosed for the duration of the write regardless of what it ends up as.
     */
    @Test
    public void createPartialFile_isOwnerOnlyEvenWhenTheTargetIsWorldReadable() throws IOException {
        ConfigFileReaderWriter.createPartialFile(
                partial, ConfigFileReaderWriter.preservedAttributesOf(targetWith(WORLD_READABLE)));

        assertEquals(
                OWNER_ONLY,
                Files.getPosixFilePermissions(partial),
                "the replacement of a world-readable file is still written under owner-only protection");
    }

    /**
     * A partial file left behind by a killed process must not be reopened: {@code FileWriter} on an
     * existing file keeps that file's mode, so reusing a leftover {@code 0666} partial would reinstate
     * exactly the disclosure this closes.
     */
    @Test
    public void createPartialFile_replacesAStalePartialRatherThanReusingIt() throws IOException {
        Files.createFile(partial, PosixFilePermissions.asFileAttribute(WORLD_WRITABLE));
        Files.writeString(partial, "left behind by a killed write");

        ConfigFileReaderWriter.createPartialFile(
                partial, ConfigFileReaderWriter.preservedAttributesOf(targetWith(OWNER_ONLY)));

        assertEquals(
                OWNER_ONLY,
                Files.getPosixFilePermissions(partial),
                "a stale partial file was reopened and kept its permissive mode");
        assertEquals("", Files.readString(partial), "the stale content survived into the new write");
    }

    /**
     * With nothing to reproduce — a configuration file being created for the first time — the replacement
     * is created normally and takes the umask, which is what that file would have had anyway. Narrowing
     * it here would silently change the mode of every newly created configuration.
     */
    @Test
    public void createPartialFile_withNothingToReproduce_createsTheFileNormally() throws IOException {
        ConfigFileReaderWriter.createPartialFile(partial, PreservedAttributes.NONE);

        assertTrue(Files.exists(partial), "the replacement must still be created");
    }

    /** The widening step: the written replacement ends up with exactly the target's mode. */
    @Test
    public void applyPreservedAttributes_givesTheReplacementTheTargetsExactMode() throws IOException {
        final PreservedAttributes preserved = ConfigFileReaderWriter.preservedAttributesOf(targetWith(WORLD_READABLE));
        ConfigFileReaderWriter.createPartialFile(partial, preserved);

        ConfigFileReaderWriter.applyPreservedAttributes(partial, preserved);

        assertEquals(WORLD_READABLE, Files.getPosixFilePermissions(partial), "the target's mode was not reproduced");
    }

    /**
     * Fail closed. R3-07's second half: the original carried the mode over best-effort and moved the
     * file either way, so a failure left the umask's mode on a file full of credentials, permanently,
     * announced at debug level. Refusing the replacement keeps the previous configuration file, which is
     * intact and correctly permissioned.
     */
    @Test
    public void applyPreservedAttributes_whenTheModeCannotBeReproduced_thenTheWriteIsAborted() throws IOException {
        final PreservedAttributes preserved = ConfigFileReaderWriter.preservedAttributesOf(targetWith(OWNER_ONLY));
        ConfigFileReaderWriter.createPartialFile(partial, preserved);
        Files.delete(partial); // the replacement is gone, so its protections cannot be set

        assertThrows(
                IOException.class,
                () -> ConfigFileReaderWriter.applyPreservedAttributes(partial, preserved),
                "protections that cannot be reproduced must abort the replacement, not be logged and ignored");
    }

    /** Nothing to reproduce is not a failure — it is the answer for a first-time configuration file. */
    @Test
    public void applyPreservedAttributes_withNothingToReproduce_isANoOp() throws IOException {
        ConfigFileReaderWriter.createPartialFile(partial, PreservedAttributes.NONE);

        ConfigFileReaderWriter.applyPreservedAttributes(partial, PreservedAttributes.NONE);

        assertTrue(Files.exists(partial));
    }

    // ---------------------------------------------------------- R3-09: who the mode actually refers to

    /**
     * The mode is not the access control. R3-09: the reader captured only {@code Set<PosixFilePermission>},
     * so owner and group were whatever the newly created replacement happened to get — and {@code 0640}
     * then granted group-read to a different set of principals than the file it replaced did.
     */
    @Test
    public void preservedAttributesOf_capturesOwnerAndGroupNotJustTheMode() throws IOException {
        final Path target = targetWith(GROUP_READABLE);
        final PosixFileAttributes expected = Files.readAttributes(target, PosixFileAttributes.class);

        final PreservedAttributes preserved = ConfigFileReaderWriter.preservedAttributesOf(target);

        assertEquals(GROUP_READABLE, preserved.permissions(), "the mode must still be carried");
        assertNotNull(preserved.owner(), "the owner must be carried, or 0640 names a group it cannot name");
        assertNotNull(preserved.group(), "the group must be carried, or 0640 names a group it cannot name");
        assertEquals(expected.owner(), preserved.owner());
        assertEquals(expected.group(), preserved.group());
    }

    /**
     * Sam's reproduction, as a test. A {@code 0640} file owned by a group the replacement would not
     * naturally get must come back out of the replacement owned by that same group — otherwise the mode
     * survives and the access control does not.
     * <p>
     * Skipped where the account running the build belongs to only one group, or where it may not set the
     * group of a file it owns; there is then no second principal for the assertion to distinguish.
     */
    @Test
    public void applyPreservedAttributes_reproducesTheTargetsGroupNotTheProcessDefault() throws IOException {
        final Path target = targetWith(GROUP_READABLE);
        final GroupPrincipal defaultGroup =
                Files.readAttributes(target, PosixFileAttributes.class).group();
        final GroupPrincipal other = aDifferentGroupOfThisUser(target, defaultGroup);
        assumeTrue(other != null, "this account has no second group, so there is nothing to tell apart");

        Files.getFileAttributeView(target, PosixFileAttributeView.class).setGroup(other);
        final PreservedAttributes preserved = ConfigFileReaderWriter.preservedAttributesOf(target);
        ConfigFileReaderWriter.createPartialFile(partial, preserved);
        Files.writeString(partial, "<hivemq><bridge><password>s3cr3t</password></bridge></hivemq>");

        ConfigFileReaderWriter.applyPreservedAttributes(partial, preserved);

        final PosixFileAttributes actual = Files.readAttributes(partial, PosixFileAttributes.class);
        assertEquals(
                other,
                actual.group(),
                "the replacement kept the mode but granted group-read to a different group than the file it replaces");
        assertEquals(GROUP_READABLE, actual.permissions(), "the mode must be reproduced as well as the group");
    }

    /**
     * R3-09's second half. An {@code IOException} or a {@code SecurityException} reading an existing
     * file's protections is not the same answer as "this file has no protections", and treating them the
     * same let the write proceed on the umask. Only a genuinely absent file is "nothing to preserve".
     * <p>
     * Provoked by making the containing directory untraversable, which is skipped when the build runs as
     * a superuser because the permission check does not apply to one.
     */
    @Test
    public void preservedAttributesOf_whenTheProtectionsCannotBeRead_thenTheWriteIsAborted() throws IOException {
        assumeTrue(!"root".equals(System.getProperty("user.name")), "a superuser is not refused by mode bits");
        final Path enclosing = Files.createDirectory(directory.resolve("locked"));
        final Path target = enclosing.resolve("config.xml");
        Files.createFile(target, PosixFilePermissions.asFileAttribute(GROUP_READABLE));
        Files.setPosixFilePermissions(enclosing, PosixFilePermissions.fromString("---------"));
        try {
            assertThrows(
                    IOException.class,
                    () -> ConfigFileReaderWriter.preservedAttributesOf(target),
                    "a failure to read the target's protections must abort the write, not fall back to the umask");
        } finally {
            Files.setPosixFilePermissions(enclosing, PosixFilePermissions.fromString("rwx------"));
        }
    }

    /** A file that does not exist yet has no protections to carry, which is an answer rather than a throw. */
    @Test
    public void preservedAttributesOf_aMissingFile_hasNothingToReproduce() throws IOException {
        final PreservedAttributes preserved =
                ConfigFileReaderWriter.preservedAttributesOf(directory.resolve("not-written-yet.xml"));

        assertTrue(preserved.nothingToReproduce(), "a first-time configuration file has nothing to preserve");
    }

    /**
     * The whole sequence, in the order the writer runs them: created narrow, written, widened to the
     * target's protections. This is what R3-07 and R3-09 ask for together, and the assertion in the
     * middle is the one the end-to-end test cannot make.
     */
    @Test
    public void theReplacementIsNeverWiderThanItsTargetWhileItHoldsTheConfiguration() throws IOException {
        final Path target = targetWith(WORLD_READABLE);
        final PreservedAttributes preserved = ConfigFileReaderWriter.preservedAttributesOf(target);

        ConfigFileReaderWriter.createPartialFile(partial, preserved);
        assertEquals(
                OWNER_ONLY,
                Files.getPosixFilePermissions(partial),
                "the secrets are written into a file wider than owner-only");
        Files.writeString(partial, "<hivemq><bridge><password>s3cr3t</password></bridge></hivemq>");
        assertEquals(
                OWNER_ONLY,
                Files.getPosixFilePermissions(partial),
                "the file was widened while it still held the configuration");

        ConfigFileReaderWriter.applyPreservedAttributes(partial, preserved);

        final PosixFileAttributes actual = Files.readAttributes(partial, PosixFileAttributes.class);
        assertEquals(WORLD_READABLE, actual.permissions(), "the target's mode must be reproduced");
        assertEquals(
                Files.readAttributes(target, PosixFileAttributes.class).group(),
                actual.group(),
                "the target's group must be reproduced along with its mode");
    }

    // ------------------------------------------------- EDG-882 review v04: an owner this node cannot set

    /**
     * The regression this closes. Changing a file's owner is privileged on every platform this runs on, so
     * a configuration installed by one account and served by another — root-owned {@code config.xml}, Edge
     * running as a service user — made {@code setOwner} fail, and failing there refused the whole write.
     * The node then silently stopped persisting anything at all.
     * <p>
     * Provoked by asking for {@code root}, which this account may not assign and which every POSIX system
     * has. Skipped when the build itself runs as a superuser, for whom the call succeeds.
     */
    @Test
    public void applyPreservedAttributes_whenTheOwnerCannotBeSet_thenTheWriteGoesOnWithoutIt() throws IOException {
        assumeTrue(!"root".equals(System.getProperty("user.name")), "a superuser may set any owner");
        final Path target = targetWith(GROUP_READABLE);
        final PosixFileAttributes attributes = Files.readAttributes(target, PosixFileAttributes.class);
        final UserPrincipal root =
                directory.getFileSystem().getUserPrincipalLookupService().lookupPrincipalByName("root");
        final PreservedAttributes rootOwned =
                new PreservedAttributes(attributes.permissions(), root, attributes.group(), null);
        ConfigFileReaderWriter.createPartialFile(partial, rootOwned);
        Files.writeString(partial, "<hivemq><bridge><password>s3cr3t</password></bridge></hivemq>");

        ConfigFileReaderWriter.applyPreservedAttributes(partial, rootOwned);

        final PosixFileAttributes actual = Files.readAttributes(partial, PosixFileAttributes.class);
        assertEquals(attributes.owner(), actual.owner(), "the replacement is owned by the account that wrote it");
        assertEquals(GROUP_READABLE, actual.permissions(), "the mode is still reproduced exactly");
        assertEquals(attributes.group(), actual.group(), "and so is the group, which is who else can read it");
    }

    // ------------------------------------------------- EDG-882 QA: a group this node is not a member of

    /**
     * The regression this closes, and it is the same shape as the owner one above. Setting a file's group
     * requires membership of it, so a {@code config.xml} installed under one group and served by a node
     * running under another — the ordinary container case — failed here, and failing here refused the
     * whole <em>write</em>. Edge went on running correctly on the configuration it already had, so nothing
     * looked broken until a restart came up on the older file with every REST change since missing.
     * <p>
     * The first fix for it aborted rather than granted, because a mode names a group without naming which
     * one, and reproducing {@code 0640} while the group falls back to this node's own hands group-read of
     * every credential in the configuration to the wrong principals (R3-09). Narrowing does neither: the
     * group's permissions come off the mode with the group, so the replacement grants the group nothing.
     * <p>
     * Provoked with a group this account is not in, which every POSIX system has and which no ordinary
     * account may assign. Skipped when the build runs as a superuser, for whom the call succeeds.
     */
    @Test
    public void applyPreservedAttributes_whenTheGroupCannotBeSet_thenTheGroupsAccessComesOffTheMode()
            throws IOException {
        assumeTrue(!"root".equals(System.getProperty("user.name")), "a superuser may set any group");
        final Path target = targetWith(GROUP_READABLE);
        final PosixFileAttributes attributes = Files.readAttributes(target, PosixFileAttributes.class);
        final GroupPrincipal foreign = aGroupThisUserIsNotIn(target);
        assumeTrue(foreign != null, "this account can assign every group it can name, so there is nothing to refuse");
        final PreservedAttributes elsewhere =
                new PreservedAttributes(attributes.permissions(), attributes.owner(), foreign, null);
        ConfigFileReaderWriter.createPartialFile(partial, elsewhere);
        Files.writeString(partial, "<hivemq><bridge><password>s3cr3t</password></bridge></hivemq>");

        ConfigFileReaderWriter.applyPreservedAttributes(partial, elsewhere);

        final PosixFileAttributes actual = Files.readAttributes(partial, PosixFileAttributes.class);
        assertEquals(
                OWNER_ONLY,
                actual.permissions(),
                "the group's permissions stayed on a mode whose group is now this node's own, which is R3-09");
        assertNotEquals(foreign, actual.group(), "the group cannot have been set, or this test proves nothing");
        assertEquals(attributes.owner(), actual.owner(), "the owner is this account either way and must be unchanged");
    }

    /**
     * And only the group's own bits come off. What the mode grants to others is granted to the same others
     * on the file being replaced, so removing it would take away access the operator deliberately gave —
     * a narrowing nobody asked for is still a change nobody asked for.
     */
    @Test
    public void applyPreservedAttributes_whenTheGroupCannotBeSet_thenOthersKeepWhatTheModeGaveThem()
            throws IOException {
        assumeTrue(!"root".equals(System.getProperty("user.name")), "a superuser may set any group");
        final Path target = targetWith(WORLD_READABLE);
        final PosixFileAttributes attributes = Files.readAttributes(target, PosixFileAttributes.class);
        final GroupPrincipal foreign = aGroupThisUserIsNotIn(target);
        assumeTrue(foreign != null, "this account can assign every group it can name, so there is nothing to refuse");
        final PreservedAttributes elsewhere =
                new PreservedAttributes(attributes.permissions(), attributes.owner(), foreign, null);
        ConfigFileReaderWriter.createPartialFile(partial, elsewhere);

        ConfigFileReaderWriter.applyPreservedAttributes(partial, elsewhere);

        assertEquals(
                PosixFilePermissions.fromString("rw----r--"),
                Files.getPosixFilePermissions(partial),
                "only the group's permissions come off; the world's are the same world the target grants");
    }

    /**
     * A group that decides nobody's access. When the mode grants the group nothing, which group the file
     * names is decoration, and dropping it must change the mode not at all — the narrowing is of access,
     * not of bits.
     */
    @Test
    public void applyPreservedAttributes_whenTheGroupCannotBeSetAndGrantsNothing_thenTheModeIsUntouched()
            throws IOException {
        assumeTrue(!"root".equals(System.getProperty("user.name")), "a superuser may set any group");
        final Path target = targetWith(OWNER_ONLY);
        final PosixFileAttributes attributes = Files.readAttributes(target, PosixFileAttributes.class);
        final GroupPrincipal foreign = aGroupThisUserIsNotIn(target);
        assumeTrue(foreign != null, "this account can assign every group it can name, so there is nothing to refuse");
        final PreservedAttributes elsewhere =
                new PreservedAttributes(attributes.permissions(), attributes.owner(), foreign, null);
        ConfigFileReaderWriter.createPartialFile(partial, elsewhere);

        ConfigFileReaderWriter.applyPreservedAttributes(partial, elsewhere);

        assertEquals(
                OWNER_ONLY,
                Files.getPosixFilePermissions(partial),
                "a group that grants nothing must not change the mode when it is dropped");
    }

    /**
     * A mode that grants nobody anything is a legal mode, and the set expressing it is empty. Building the
     * narrowed mode with {@code EnumSet.copyOf} would throw {@code IllegalArgumentException} on exactly
     * that set — an empty collection that is not itself an {@code EnumSet} — turning a protection this
     * code is meant to keep into the one input that breaks it.
     */
    @Test
    public void applyPreservedAttributes_whenTheGroupCannotBeSetOnAFileNobodyMayRead_thenItStillGoesOn()
            throws IOException {
        assumeTrue(!"root".equals(System.getProperty("user.name")), "a superuser may set any group");
        final Path target = targetWith(OWNER_ONLY);
        final GroupPrincipal foreign = aGroupThisUserIsNotIn(target);
        assumeTrue(foreign != null, "this account can assign every group it can name, so there is nothing to refuse");
        final PreservedAttributes unreadable = new PreservedAttributes(Set.of(), Files.getOwner(target), foreign, null);
        ConfigFileReaderWriter.createPartialFile(partial, unreadable);

        ConfigFileReaderWriter.applyPreservedAttributes(partial, unreadable);

        assertEquals(
                Set.<PosixFilePermission>of(),
                Files.getPosixFilePermissions(partial),
                "a file nobody may read must be replaced by one nobody may read");
    }

    /**
     * Neither of the two fallbacks may cancel the other. An owner this account cannot set <em>and</em> a
     * group it is not in is the root-installed configuration served by a service user, which is the
     * deployment both findings came from; the replacement must end up owned by this account, granting its
     * group nothing, and written.
     */
    @Test
    public void applyPreservedAttributes_whenNeitherOwnerNorGroupCanBeSet_thenBothNarrowAndTheWriteGoesOn()
            throws IOException {
        assumeTrue(!"root".equals(System.getProperty("user.name")), "a superuser may set any owner or group");
        final Path target = targetWith(GROUP_READABLE);
        final UserPrincipal root =
                directory.getFileSystem().getUserPrincipalLookupService().lookupPrincipalByName("root");
        final GroupPrincipal foreign = aGroupThisUserIsNotIn(target);
        assumeTrue(foreign != null, "this account can assign every group it can name, so there is nothing to refuse");
        final PreservedAttributes elsewhere = new PreservedAttributes(GROUP_READABLE, root, foreign, null);
        ConfigFileReaderWriter.createPartialFile(partial, elsewhere);
        Files.writeString(partial, "<hivemq><bridge><password>s3cr3t</password></bridge></hivemq>");

        ConfigFileReaderWriter.applyPreservedAttributes(partial, elsewhere);

        final PosixFileAttributes actual = Files.readAttributes(partial, PosixFileAttributes.class);
        assertEquals(Files.getOwner(target), actual.owner(), "the replacement is owned by the account that wrote it");
        assertEquals(OWNER_ONLY, actual.permissions(), "the unreproducible group must not keep its permissions");
    }

    /**
     * End to end, which is the failure QA actually saw: an adapter created through REST, the node
     * restarted, the adapter gone. The write must complete and the content must reach the target.
     */
    @Test
    public void replaceCarryingProtections_whenTheGroupCannotBeSet_thenTheConfigurationIsStillWritten()
            throws IOException {
        assumeTrue(!"root".equals(System.getProperty("user.name")), "a superuser may set any group");
        final Path target = targetWith(GROUP_READABLE);
        final GroupPrincipal foreign = aGroupThisUserIsNotIn(target);
        assumeTrue(foreign != null, "this account can assign every group it can name, so there is nothing to refuse");
        final PreservedAttributes elsewhere =
                new PreservedAttributes(GROUP_READABLE, Files.getOwner(target), foreign, null);

        ConfigFileReaderWriter.replaceCarryingProtections(
                target,
                elsewhere,
                written -> Files.writeString(written, "<hivemq><bridge><password>s3cr3t</password></bridge></hivemq>"));

        assertEquals(
                "<hivemq><bridge><password>s3cr3t</password></bridge></hivemq>",
                Files.readString(target),
                "the configuration was not persisted, which is the defect: a restart would lose the change");
        assertEquals(
                OWNER_ONLY,
                Files.getPosixFilePermissions(target),
                "the mode kept permissions for a group the replacement could not be given");
        assertFalse(
                Files.exists(target.resolveSibling(target.getFileName() + ".partial")),
                "a partial file was left beside the target");
    }

    /**
     * The other half of the trade is unchanged: a protection that fails for any reason other than a
     * privilege this node was never going to have still aborts. Provoked by removing the replacement, so
     * that nothing about it can be set or read.
     */
    @Test
    public void applyPreservedAttributes_whenTheReplacementIsGone_thenTheWriteIsStillAborted() throws IOException {
        final Path target = targetWith(GROUP_READABLE);
        final PreservedAttributes preserved = ConfigFileReaderWriter.preservedAttributesOf(target);
        ConfigFileReaderWriter.createPartialFile(partial, preserved);
        Files.delete(partial);

        assertThrows(
                IOException.class,
                () -> ConfigFileReaderWriter.applyPreservedAttributes(partial, preserved),
                "a store failure is not a privilege this node lacks, and must not be narrowed past");
    }

    /** A group this account cannot set on a file it owns, or {@code null} when every named group works. */
    private static @Nullable GroupPrincipal aGroupThisUserIsNotIn(final @NotNull Path file) {
        final List<String> mine = groupNamesOfThisUser();
        for (final String name : new String[] {"daemon", "wheel", "operator", "sys", "tty", "bin"}) {
            if (mine.contains(name)) {
                continue;
            }
            try {
                final GroupPrincipal candidate =
                        file.getFileSystem().getUserPrincipalLookupService().lookupPrincipalByGroupName(name);
                final PosixFileAttributeView view = Files.getFileAttributeView(file, PosixFileAttributeView.class);
                final GroupPrincipal current = view.readAttributes().group();
                try {
                    view.setGroup(candidate);
                    view.setGroup(current); // it worked after all; not a group this can refuse on
                } catch (final IOException notPermitted) {
                    return candidate;
                }
            } catch (final IOException | UnsupportedOperationException ignored) {
                // not a group this system knows; try the next one
            }
        }
        return null;
    }

    /**
     * A group this account belongs to that is not {@code current}, and that it may actually set on a file
     * it owns, or {@code null} when there is none. Both conditions matter: membership is what makes the
     * change legal, and some platforms restrict it further.
     * <p>
     * Shared with {@link ConfigBackupPermissionsTest}, which asks the same question of the rolling backup.
     */
    static @Nullable GroupPrincipal aDifferentGroupOfThisUser(
            final @NotNull Path file, final @NotNull GroupPrincipal current) {
        for (final String name : groupNamesOfThisUser()) {
            if (name.equals(current.getName())) {
                continue;
            }
            try {
                final GroupPrincipal candidate =
                        file.getFileSystem().getUserPrincipalLookupService().lookupPrincipalByGroupName(name);
                final PosixFileAttributeView view = Files.getFileAttributeView(file, PosixFileAttributeView.class);
                view.setGroup(candidate);
                view.setGroup(current); // put it back; the test sets it again itself
                return candidate;
            } catch (final IOException | UnsupportedOperationException ignored) {
                // not a group this account can actually assign; try the next one
            }
        }
        return null;
    }

    static @NotNull List<String> groupNamesOfThisUser() {
        final List<String> names = new ArrayList<>();
        try {
            final Process process =
                    new ProcessBuilder("id", "-Gn").redirectErrorStream(true).start();
            try (final BufferedReader reader =
                    new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                final String line = reader.readLine();
                if (line != null) {
                    for (final String name : line.trim().split("\\s+")) {
                        if (!name.isEmpty()) {
                            names.add(name);
                        }
                    }
                }
            }
            process.waitFor();
        } catch (final IOException e) {
            return List.of();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            return List.of();
        }
        return names;
    }
}
