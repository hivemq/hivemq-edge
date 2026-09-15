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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.hivemq.exceptions.UnrecoverableException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFilePermissions;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** {@link ConfigBackupRotation} on its own: which slot a backup lands in, and what it carries. */
public class ConfigBackupRotationTest {

    @TempDir
    public File temporaryFolder;

    private @NotNull Path config;

    @BeforeEach
    public void setUp() throws IOException {
        config = temporaryFolder.toPath().resolve("config.xml");
        Files.writeString(config, "v0");
    }

    @AfterEach
    public void restoreTheFolder() throws IOException {
        if (config.getFileSystem().supportedFileAttributeViews().contains("posix")) {
            Files.setPosixFilePermissions(temporaryFolder.toPath(), PosixFilePermissions.fromString("rwx------"));
        }
    }

    private @NotNull Path slot(final int n) {
        return config.resolveSibling("config_" + n + ".xml");
    }

    /** Writes version {@code n} to the configuration with a distinct, increasing time, then takes a backup. */
    private void writeAndBackUp(final int n) throws IOException {
        Files.writeString(config, "v" + n);
        Files.setLastModifiedTime(config, FileTime.fromMillis(1_700_000_000_000L + n * 60_000L));
        ConfigBackupRotation.backup(config.toFile(), true);
    }

    @Test
    public void fillsTheFiveSlotsInOrder() throws IOException {
        for (int n = 1; n <= 5; n++) {
            writeAndBackUp(n);
            assertThat(slot(n)).hasContent("v" + n);
        }
        assertThat(slot(6)).doesNotExist();
    }

    @Test
    public void recyclesTheOldestSlotOnceAllAreTaken() throws IOException {
        for (int n = 1; n <= 5; n++) {
            writeAndBackUp(n);
        }
        writeAndBackUp(6);
        assertThat(slot(1)).as("the oldest slot is recycled").hasContent("v6");
        assertThat(slot(2)).hasContent("v2");
        writeAndBackUp(7);
        assertThat(slot(2)).as("then the next oldest").hasContent("v7");
        assertThat(slot(1)).hasContent("v6");
    }

    @Test
    public void filesThatOnlyResembleASlotAreNeverCandidates() throws IOException {
        final Path[] foreign = {
            config.resolveSibling("config-archive-2024.xml"),
            config.resolveSibling("config_2024.xml"),
            config.resolveSibling("config.xml.bak"),
            config.resolveSibling("config_1.xml.old")
        };
        for (final Path file : foreign) {
            Files.writeString(file, "operator's");
            Files.setLastModifiedTime(file, FileTime.fromMillis(1_600_000_000_000L)); // older than everything
        }

        for (int n = 1; n <= 8; n++) {
            writeAndBackUp(n);
        }

        for (final Path file : foreign) {
            assertThat(file)
                    .as("%s must not be treated as a backup slot", file.getFileName())
                    .hasContent("operator's");
        }
        assertThat(config).hasContent("v8");
    }

    @Test
    public void disabledTakesNoBackup() throws IOException {
        ConfigBackupRotation.backup(config.toFile(), false);
        assertThat(slot(1)).doesNotExist();
    }

    @Test
    public void aMissingFolderIsRefused() {
        final File orphan =
                temporaryFolder.toPath().resolve("gone").resolve("config.xml").toFile();
        assertThatThrownBy(() -> ConfigBackupRotation.backup(orphan, true)).isInstanceOf(UnrecoverableException.class);
    }

    @Test
    public void theBackupCarriesTheModeAndTimeOfTheFileItCopies() throws IOException {
        assumeTrue(config.getFileSystem().supportedFileAttributeViews().contains("posix"));
        Files.setPosixFilePermissions(config, PosixFilePermissions.fromString("rw-------"));
        Files.setLastModifiedTime(config, FileTime.fromMillis(1_700_000_000_000L));

        ConfigBackupRotation.backup(config.toFile(), true);

        assertThat(Files.getPosixFilePermissions(slot(1))).isEqualTo(PosixFilePermissions.fromString("rw-------"));
        assertThat(Files.getLastModifiedTime(slot(1)).toMillis()).isEqualTo(1_700_000_000_000L);
    }

    /** A folder that exists but cannot be listed must fail the rotation, not pick a slot at random. */
    @Test
    public void aFolderThatCannotBeListedIsAnError() throws IOException {
        assumeTrue(config.getFileSystem().supportedFileAttributeViews().contains("posix"));
        assumeTrue(!"root".equals(System.getProperty("user.name")), "a superuser lists anything");
        for (int n = 1; n <= 5; n++) {
            writeAndBackUp(n);
        }
        Files.setPosixFilePermissions(temporaryFolder.toPath(), PosixFilePermissions.fromString("-wx------"));
        try {
            assertThatThrownBy(() -> ConfigBackupRotation.backup(config.toFile(), true))
                    .isInstanceOf(IOException.class)
                    .hasMessageContaining("cannot be listed");
        } finally {
            Files.setPosixFilePermissions(temporaryFolder.toPath(), PosixFilePermissions.fromString("rwx------"));
        }
    }
}
