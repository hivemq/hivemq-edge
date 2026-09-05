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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * EDG-882 review v04 — the rolling backup is a second copy of every credential in {@code config.xml}, so
 * it is protected like the file it copies.
 * <p>
 * {@link ConfigWritePermissionsTest} pins that property for the replacement written over {@code
 * config.xml}. The same write produces {@code config_1.xml} beside it, holding the same passwords, and
 * that one was produced by a plain file copy: it carried the mode and left the group to whatever the
 * process happened to have. A {@code 0640} configuration owned by group {@code operators} was therefore
 * backed up into a {@code 0640} file readable by group {@code staff} — R3-09 exactly, in the sibling file
 * of the path R3-09 was raised against.
 * <p>
 * The backup now goes through the same replace-by-move as the configuration file itself, so the two
 * cannot drift apart again.
 */
public class ConfigBackupPermissionsTest extends AbstractConfigurationTest {

    private static final @NotNull Set<PosixFilePermission> OWNER_ONLY = PosixFilePermissions.fromString("rw-------");
    private static final @NotNull Set<PosixFilePermission> GROUP_READABLE =
            PosixFilePermissions.fromString("rw-r-----");
    private static final @NotNull Set<PosixFilePermission> WORLD_WRITABLE =
            PosixFilePermissions.fromString("rw-rw-rw-");

    private @NotNull Path config;
    private @NotNull Path backup;

    @BeforeEach
    public void requirePosix() {
        config = Path.of(xmlFile.getPath());
        backup = config.resolveSibling("config_1.xml");
        assumeTrue(
                config.getFileSystem().supportedFileAttributeViews().contains("posix"),
                "no POSIX permissions on this file system, so there is nothing to protect");
    }

    /** A configuration with a credential in it, so that what the backup holds is what the finding is about. */
    private static @NotNull String config(final @NotNull String password) {
        return "<hivemq>\n<mqtt-bridges>\n    <mqtt-bridge>\n        <id>edg-882-backup-bridge</id>\n"
                + "        <remote-broker>\n            <host>testhost</host>\n            <authentication>\n"
                + "                <mqtt-simple-authentication>\n"
                + "                    <username>hivemq-edge</username>\n"
                + "                    <password>" + password + "</password>\n"
                + "                </mqtt-simple-authentication>\n            </authentication>\n"
                + "        </remote-broker>\n        <forwarded-topics>\n            <forwarded-topic>\n"
                + "                <filters>\n                    <mqtt-topic-filter>plant/#</mqtt-topic-filter>\n"
                + "                </filters>\n                <destination>{#}</destination>\n"
                + "                <max-qos>1</max-qos>\n            </forwarded-topic>\n"
                + "        </forwarded-topics>\n    </mqtt-bridge>\n</mqtt-bridges></hivemq>";
    }

    private void writeConfigurationWithBackup(final @NotNull Set<PosixFilePermission> mode) throws IOException {
        Files.writeString(config, config("s3cr3t-do-not-write-me"));
        Files.setPosixFilePermissions(config, mode);
        reader.applyConfig();
        reader.writeConfigToXML(xmlFile, true, true);
    }

    private static @NotNull PosixFileAttributes attributesOf(final @NotNull Path path) throws IOException {
        return Files.readAttributes(path, PosixFileAttributes.class);
    }

    /**
     * The finding. The backup of a {@code 0640} configuration must grant group-read to the same group the
     * configuration does, not to whichever group the writing process belongs to — a mode names a group
     * without naming which one.
     * <p>
     * Skipped where the account running the build belongs to only one group, or may not set the group of a
     * file it owns: there is then no second principal for the assertion to tell apart.
     */
    @Test
    public void theBackupCarriesTheConfigurationsGroupNotTheProcessDefault() throws IOException {
        Files.writeString(config, config("s3cr3t-do-not-write-me"));
        Files.setPosixFilePermissions(config, GROUP_READABLE);
        final GroupPrincipal defaultGroup = attributesOf(config).group();
        final GroupPrincipal other = ConfigWritePermissionsTest.aDifferentGroupOfThisUser(config, defaultGroup);
        assumeTrue(other != null, "this account has no second group, so there is nothing to tell apart");
        Files.getFileAttributeView(config, PosixFileAttributeView.class).setGroup(other);

        reader.applyConfig();
        reader.writeConfigToXML(xmlFile, true, true);

        assertTrue(Files.exists(backup), "the rolling backup must still be taken");
        final PosixFileAttributes actual = attributesOf(backup);
        assertEquals(
                other,
                actual.group(),
                "the backup kept the mode but granted group-read to a different group than the configuration it"
                        + " copies");
        assertEquals(GROUP_READABLE, actual.permissions(), "the mode must be reproduced as well as the group");
    }

    /** And the mode itself: a configuration only its owner may read is backed up into a file only its owner may read. */
    @Test
    public void theBackupCarriesTheConfigurationsMode() throws IOException {
        writeConfigurationWithBackup(OWNER_ONLY);

        assertEquals(
                OWNER_ONLY,
                Files.getPosixFilePermissions(backup),
                "the backup of a 0600 configuration is readable by someone the configuration is not");
    }

    /** The owner travels with it too, on the one platform where this can be asserted without a second account. */
    @Test
    public void theBackupCarriesTheConfigurationsOwner() throws IOException {
        writeConfigurationWithBackup(OWNER_ONLY);

        assertEquals(attributesOf(config).owner(), attributesOf(backup).owner());
    }

    /**
     * The protections come from the file being copied, not from the file being overwritten. Once all five
     * slots exist the rotation recycles the oldest, and a slot left wide by an earlier release — or by an
     * operator — must not decide how the next backup of a restricted configuration is protected.
     * <p>
     * The modification times are set rather than assumed, because "oldest" is what selects the slot and
     * the configuration file itself is in the set the rotation looks at.
     */
    @Test
    public void theRecycledBackupSlotIsNotWidenedByWhatItHeld() throws IOException {
        final long now = 1_700_000_000_000L;
        Files.writeString(config, config("s3cr3t-do-not-write-me"));
        Files.setPosixFilePermissions(config, OWNER_ONLY);
        Files.setLastModifiedTime(config, FileTime.fromMillis(now));
        for (int slot = 1; slot <= 4; slot++) {
            final Path used = config.resolveSibling("config_" + slot + ".xml");
            Files.writeString(used, "an earlier backup left readable by everyone");
            Files.setPosixFilePermissions(used, WORLD_WRITABLE);
            Files.setLastModifiedTime(used, FileTime.fromMillis(now - (5L - slot) * 60_000L));
        }

        reader.applyConfig();
        reader.writeConfigToXML(xmlFile, true, true);

        assertTrue(
                Files.readString(backup).contains("edg-882-backup-bridge"),
                "the oldest slot is the one the rotation should have recycled");
        assertEquals(
                OWNER_ONLY,
                Files.getPosixFilePermissions(backup),
                "the backup took the protections of the file it overwrote instead of the one it copies");
    }

    /** What it is for: the backup holds the configuration as it was before the write. */
    @Test
    public void theBackupHoldsTheConfigurationThatWasReplaced() throws IOException {
        final String before = config("s3cr3t-do-not-write-me");
        Files.writeString(config, before);
        Files.setPosixFilePermissions(config, OWNER_ONLY);
        reader.applyConfig();

        reader.writeConfigToXML(xmlFile, true, true);

        assertEquals(before, Files.readString(backup), "the backup is not the configuration it replaced");
        assertFalse(
                Files.readString(config).equals(before),
                "the configuration file should have been rewritten by the marshaller");
    }

    /**
     * The modification time is carried because the rotation picks which backup to overwrite by it. Losing
     * it would not lose a credential, but it would quietly change which of the five slots is recycled.
     */
    @Test
    public void theBackupKeepsTheConfigurationsModificationTime() throws IOException {
        Files.writeString(config, config("s3cr3t-do-not-write-me"));
        Files.setPosixFilePermissions(config, OWNER_ONLY);
        final FileTime stamped = FileTime.fromMillis(1_600_000_000_000L);
        Files.setLastModifiedTime(config, stamped);
        reader.applyConfig();

        reader.writeConfigToXML(xmlFile, true, true);

        assertEquals(stamped, Files.getLastModifiedTime(backup), "the rotation orders the slots by this");
    }

    /** Written beside the backup and moved onto it, so nothing half-written is left behind either. */
    @Test
    public void noPartialFileSurvivesTheWrite() throws IOException {
        writeConfigurationWithBackup(OWNER_ONLY);

        try (var listing = Files.list(config.getParent())) {
            final List<String> left = listing.map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith(".partial"))
                    .toList();
            assertEquals(List.of(), left, "a partial file was left in the configuration directory");
        }
    }

    /** Every backup taken by the same write is protected, not only the first slot. */
    @Test
    public void everyRotatedBackupCarriesTheConfigurationsProtections() throws IOException {
        for (int i = 0; i < 3; i++) {
            Files.writeString(config, config("s3cr3t-do-not-write-me"));
            Files.setPosixFilePermissions(config, OWNER_ONLY);
            reader.applyConfig();
            reader.writeConfigToXML(xmlFile, true, true);
        }

        try (var listing = Files.list(config.getParent())) {
            for (final Path file : listing.toList()) {
                if (file.getFileName().toString().startsWith("config_")) {
                    assertEquals(
                            OWNER_ONLY,
                            Files.getPosixFilePermissions(file),
                            file.getFileName() + " is readable by someone the configuration is not");
                }
            }
        }
    }
}
