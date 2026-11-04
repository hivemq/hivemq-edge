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
package com.hivemq.edge.utils;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/// The installation id: created once in the data folder, then read back on every start; a file that does not hold a
/// UUID is replaced.
class InstallationIdTest {

    private static final @NotNull Pattern CANONICAL_UUID =
            Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");

    @TempDir
    private Path dataFolder;

    /// The first start creates the id and writes it to the data folder.
    @Test
    void loadOrCreate_whenNoFile_thenCreatesAndWritesIt() throws IOException {
        final String id = InstallationId.loadOrCreate(dataFolder);

        assertThat(id).matches(CANONICAL_UUID);
        assertThat(Files.readString(file(), US_ASCII)).isEqualTo(id + "\n");
        try (var files = Files.list(dataFolder)) {
            assertThat(files).containsExactly(file());
        }
    }

    /// The file gets the mode any new file in the data folder gets (the umask's), not the 0600 of a temporary file.
    @Test
    void loadOrCreate_whenNoFile_thenFileHasTheFolderDefaultMode() throws IOException {
        assumeTrue(dataFolder.getFileSystem().supportedFileAttributeViews().contains("posix"));
        final Path reference = Files.createFile(dataFolder.resolve("reference"));

        InstallationId.loadOrCreate(dataFolder);

        assertThat(Files.getPosixFilePermissions(file())).isEqualTo(Files.getPosixFilePermissions(reference));
    }

    /// Every later start reads the same id back and leaves the file as it is.
    @Test
    void loadOrCreate_whenFileHoldsAnId_thenSameIdAndFileUntouched() throws IOException {
        final String first = InstallationId.loadOrCreate(dataFolder);
        final var written = Files.getLastModifiedTime(file());

        assertThat(InstallationId.loadOrCreate(dataFolder)).isEqualTo(first);
        assertThat(InstallationId.loadOrCreate(dataFolder)).isEqualTo(first);
        assertThat(Files.getLastModifiedTime(file())).isEqualTo(written);
    }

    /// An id edited by hand, in upper case or without its line break, is still the same id.
    @Test
    void loadOrCreate_whenUpperCaseWithoutLineBreak_thenReadAsLowerCase() throws IOException {
        final String id = UUID.randomUUID().toString();
        Files.writeString(file(), "  " + id.toUpperCase(Locale.ROOT) + "  ", US_ASCII);

        assertThat(InstallationId.loadOrCreate(dataFolder)).isEqualTo(id);
    }

    /// A file that does not hold a UUID gets a new id, which replaces it.
    @Test
    void loadOrCreate_whenFileBroken_thenNewIdReplacesIt() throws IOException {
        for (final String broken : new String[] {"", "not-a-uuid", "1-1-1-1-1", "x".repeat(10_000)}) {
            Files.writeString(file(), broken, US_ASCII);

            final String id = InstallationId.loadOrCreate(dataFolder);

            assertThat(id).matches(CANONICAL_UUID);
            assertThat(Files.readString(file(), US_ASCII)).isEqualTo(id + "\n");
        }
    }

    /// Bytes that are not text get a new id too.
    @Test
    void loadOrCreate_whenFileIsBinary_thenNewIdReplacesIt() throws IOException {
        Files.write(file(), new byte[] {(byte) 0xff, (byte) 0xfe, 0, 1, 2});

        final String id = InstallationId.loadOrCreate(dataFolder);

        assertThat(id).matches(CANONICAL_UUID);
        assertThat(Files.readString(file(), US_ASCII)).isEqualTo(id + "\n");
    }

    /// A data folder that does not exist yet is created.
    @Test
    void loadOrCreate_whenDataFolderMissing_thenCreatesIt() throws IOException {
        final Path missing = dataFolder.resolve("data");

        final String id = InstallationId.loadOrCreate(missing);

        assertThat(Files.readString(missing.resolve(InstallationId.FILE_NAME), US_ASCII))
                .isEqualTo(id + "\n");
    }

    /// A data folder that cannot be written still gives an id, for this run only.
    @Test
    void loadOrCreate_whenDataFolderReadOnly_thenIdForThisRunOnly() throws IOException {
        assumeTrue(dataFolder.getFileSystem().supportedFileAttributeViews().contains("posix"));
        Files.setPosixFilePermissions(dataFolder, PosixFilePermissions.fromString("r-xr-xr-x"));
        try {
            assumeTrue(!Files.isWritable(dataFolder), "running as a user who writes anyway");

            assertThat(InstallationId.loadOrCreate(dataFolder)).matches(CANONICAL_UUID);
            assertThat(Files.exists(file())).isFalse();
        } finally {
            Files.setPosixFilePermissions(dataFolder, PosixFilePermissions.fromString("rwxr-xr-x"));
        }
    }

    /// Another process on the same data folder created the file between our read and our write: its id is the one
    /// used, and the file is left as it wrote it.
    @Test
    void write_whenAnotherProcessCreatedTheFileFirst_thenItsIdWins() throws IOException {
        final String winner = UUID.randomUUID().toString();
        Files.writeString(file(), winner + "\n", US_ASCII);

        assertThat(InstallationId.write(dataFolder, file(), UUID.randomUUID().toString()))
                .isEqualTo(winner);
        assertThat(Files.readString(file(), US_ASCII)).isEqualTo(winner + "\n");
        try (var files = Files.list(dataFolder)) {
            assertThat(files).containsExactly(file());
        }
    }

    /// A broken file found when creating is replaced, and the new id is the one used.
    @Test
    void write_whenFileExistsButBroken_thenReplacedWithTheNewId() throws IOException {
        Files.writeString(file(), "not-a-uuid", US_ASCII);
        final String id = UUID.randomUUID().toString();

        assertThat(InstallationId.write(dataFolder, file(), id)).isEqualTo(id);
        assertThat(Files.readString(file(), US_ASCII)).isEqualTo(id + "\n");
    }

    /// Two starts racing on a data folder without the file end with one id, the one on disk.
    @Test
    void loadOrCreate_whenTwoStartsRace_thenBothUseTheIdOnDisk() throws Exception {
        for (int round = 0; round < 500; round++) {
            Files.deleteIfExists(file());
            final CyclicBarrier start = new CyclicBarrier(2);
            final Callable<String> race = () -> {
                start.await();
                return InstallationId.loadOrCreate(dataFolder);
            };
            try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
                final Future<String> first = pool.submit(race);
                final Future<String> second = pool.submit(race);
                final String firstId = first.get();
                final String secondId = second.get();
                final String onDisk = Files.readString(file(), US_ASCII).strip();
                assertThat(firstId).isEqualTo(onDisk);
                assertThat(secondId).isEqualTo(onDisk);
            }
        }
    }

    private @NotNull Path file() {
        return dataFolder.resolve(InstallationId.FILE_NAME);
    }
}
