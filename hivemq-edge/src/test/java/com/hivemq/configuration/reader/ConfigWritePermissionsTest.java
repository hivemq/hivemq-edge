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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * EDG-882 review v03, R3-07 — the replacement configuration file must never be readable by anyone the
 * file it replaces was not, at any point while it holds the configuration.
 * <p>
 * {@code config.xml} carries bridge passwords, keystore and truststore passwords and adapter
 * credentials. The first version of the replace-by-move wrote the rendered document into a file created
 * by {@code FileWriter}, which takes the process umask — commonly {@code 0644} — and only narrowed it to
 * the target's mode afterwards. Between those two steps a world-readable file held every secret in the
 * configuration, on every REST write to any subsystem.
 * <p>
 * That window did not exist before the branch that introduced it: writing in place reused the file's own
 * inode and therefore its own mode. This is a regression, which is why the behaviour here is fail-closed
 * rather than best-effort.
 * <p>
 * The creation-time property is the one that matters and it cannot be observed end to end — the write is
 * synchronous and the temporary file is gone by the time any assertion could run — so it is pinned
 * directly on the helper that establishes it. {@code ConfigFileWriterTest} covers the end-to-end mode
 * preservation and the symbolic-link case.
 */
public class ConfigWritePermissionsTest {

    private static final @NotNull Set<PosixFilePermission> OWNER_ONLY = PosixFilePermissions.fromString("rw-------");
    private static final @NotNull Set<PosixFilePermission> WORLD_READABLE =
            PosixFilePermissions.fromString("rw-r--r--");
    private static final @NotNull Set<PosixFilePermission> WORLD_WRITABLE =
            PosixFilePermissions.fromString("rw-rw-rw-");

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

    /**
     * The defect, at the only moment it is observable. A target the operator restricted to {@code 0600}
     * must not produce a replacement that is briefly readable by everyone — so the file is created
     * owner-only and widened afterwards, never the other way round.
     */
    @Test
    public void createPartialFile_createsTheReplacementOwnerOnly() throws IOException {
        ConfigFileReaderWriter.createPartialFile(partial, OWNER_ONLY);

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
        ConfigFileReaderWriter.createPartialFile(partial, WORLD_READABLE);

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

        ConfigFileReaderWriter.createPartialFile(partial, OWNER_ONLY);

        assertEquals(
                OWNER_ONLY,
                Files.getPosixFilePermissions(partial),
                "a stale partial file was reopened and kept its permissive mode");
        assertEquals("", Files.readString(partial), "the stale content survived into the new write");
    }

    /**
     * With no mode to reproduce — a configuration file being created for the first time — the
     * replacement is created normally and takes the umask, which is what that file would have had
     * anyway. Narrowing it here would silently change the mode of every newly created configuration.
     */
    @Test
    public void createPartialFile_withNothingToReproduce_createsTheFileNormally() throws IOException {
        ConfigFileReaderWriter.createPartialFile(partial, null);

        assertTrue(Files.exists(partial), "the replacement must still be created");
    }

    /** The widening step: the written replacement ends up with exactly the target's mode. */
    @Test
    public void applyPermissions_givesTheReplacementTheTargetsExactMode() throws IOException {
        ConfigFileReaderWriter.createPartialFile(partial, WORLD_READABLE);

        ConfigFileReaderWriter.applyPermissions(partial, WORLD_READABLE);

        assertEquals(WORLD_READABLE, Files.getPosixFilePermissions(partial), "the target's mode was not reproduced");
    }

    /**
     * Fail closed. R3-07's second half: the original carried the mode over best-effort and moved the
     * file either way, so a failure left the umask's mode on a file full of credentials, permanently,
     * announced at debug level. Refusing the replacement keeps the previous configuration file, which is
     * intact and correctly permissioned.
     */
    @Test
    public void applyPermissions_whenTheModeCannotBeReproduced_thenTheWriteIsAborted() throws IOException {
        ConfigFileReaderWriter.createPartialFile(partial, OWNER_ONLY);
        Files.delete(partial); // the replacement is gone, so its mode cannot be set

        assertThrows(
                IOException.class,
                () -> ConfigFileReaderWriter.applyPermissions(partial, OWNER_ONLY),
                "a mode that cannot be reproduced must abort the replacement, not be logged and ignored");
    }

    /** Nothing to reproduce is not a failure — it is the answer for a first-time configuration file. */
    @Test
    public void applyPermissions_withNothingToReproduce_isANoOp() throws IOException {
        ConfigFileReaderWriter.createPartialFile(partial, null);

        ConfigFileReaderWriter.applyPermissions(partial, null);

        assertTrue(Files.exists(partial));
    }

    @Test
    public void posixPermissionsOf_readsTheModeOfAnExistingFile() throws IOException {
        final Path target = directory.resolve("config.xml");
        Files.createFile(target, PosixFilePermissions.asFileAttribute(OWNER_ONLY));

        assertEquals(OWNER_ONLY, ConfigFileReaderWriter.posixPermissionsOf(target));
    }

    /** A file that does not exist yet has no mode to carry, which is a null answer rather than a throw. */
    @Test
    public void posixPermissionsOf_aMissingFile_hasNothingToReproduce() {
        assertNull(ConfigFileReaderWriter.posixPermissionsOf(directory.resolve("not-written-yet.xml")));
    }

    /**
     * The two halves together, in the order the writer runs them: created narrow, written, widened to
     * the target's mode. This is the sequence R3-07 asks for, and the assertion in the middle is the one
     * the end-to-end test cannot make.
     */
    @Test
    public void theReplacementIsNeverWiderThanItsTargetWhileItHoldsTheConfiguration() throws IOException {
        final Path target = directory.resolve("config.xml");
        Files.createFile(target, PosixFilePermissions.asFileAttribute(WORLD_READABLE));
        final Set<PosixFilePermission> targetMode = ConfigFileReaderWriter.posixPermissionsOf(target);

        ConfigFileReaderWriter.createPartialFile(partial, targetMode);
        assertEquals(
                OWNER_ONLY,
                Files.getPosixFilePermissions(partial),
                "the secrets are written into a file wider than owner-only");
        Files.writeString(partial, "<hivemq><bridge><password>s3cr3t</password></bridge></hivemq>");
        assertEquals(
                OWNER_ONLY,
                Files.getPosixFilePermissions(partial),
                "the file was widened while it still held the configuration");

        ConfigFileReaderWriter.applyPermissions(partial, targetMode);

        assertEquals(WORLD_READABLE, Files.getPosixFilePermissions(partial), "the target's mode must be reproduced");
        assertFalse(
                Files.getPosixFilePermissions(partial).isEmpty(),
                "the replacement must carry a mode, not be left with none");
    }
}
