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

import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The id of this installation in the phone home: a random UUID kept in the data folder, so it stays the same across
 * restarts, upgrades and recreated containers wherever the data folder is kept. It replaces the MAC address the
 * installation token was derived from, which changed with every recreated container and identified the hardware.
 */
public final class InstallationId {

    /** The file in the data folder that holds the id. */
    public static final @NotNull String FILE_NAME = "installation-id";

    private static final @NotNull Logger log = LoggerFactory.getLogger(InstallationId.class);

    /** A UUID with its line break is 37 bytes; a larger file is not ours, so it is not read. */
    private static final int MAX_FILE_BYTES = 64;

    private static final @NotNull Pattern CANONICAL_UUID =
            Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");

    private InstallationId() {}

    /**
     * Reads the id from {@code <dataFolder>/installation-id}, or creates a new one there when the file is missing,
     * unreadable or does not hold a UUID. The file is read once and written only when an id is created. When the
     * data folder cannot be written, the new id is used for this run only.
     *
     * @param dataFolder the Edge data folder
     * @return the installation id, a lower-case UUID
     */
    public static @NotNull String loadOrCreate(final @NotNull Path dataFolder) {
        final Path file = dataFolder.resolve(FILE_NAME);
        final String existing = read(file);
        if (existing != null) {
            return existing;
        }
        return write(dataFolder, file, UUID.randomUUID().toString());
    }

    /** @return the id the file holds, or {@code null} when it is missing, unreadable or not a UUID */
    static @Nullable String read(final @NotNull Path file) {
        if (!Files.isRegularFile(file)) {
            return null;
        }
        try {
            if (Files.size(file) > MAX_FILE_BYTES) {
                log.warn("Ignoring {}: it is not an installation id; a new one replaces it", file);
                return null;
            }
            final String content = Files.readString(file, US_ASCII).strip().toLowerCase(Locale.ROOT);
            if (CANONICAL_UUID.matcher(content).matches()) {
                return content;
            }
            log.warn("Ignoring {}: it does not hold a UUID; a new one replaces it", file);
        } catch (final CharacterCodingException e) {
            log.warn("Ignoring {}: it does not hold a UUID; a new one replaces it", file);
        } catch (final IOException e) {
            log.warn("Could not read {}; a new installation id replaces it: {}", file, e.getMessage());
        }
        return null;
    }

    /**
     * Writes the id through a temporary file, so a crash never leaves a half-written file. The file is created only
     * if it is missing (a hard link, else a move that does not replace); when it exists, a valid id in it wins, so a
     * process on the same data folder that created it first and this one use the same id, and only a file that does
     * not hold one is replaced.
     *
     * @return the id the file holds afterwards, or {@code id} when it could not be written (this run only)
     */
    static @NotNull String write(final @NotNull Path dataFolder, final @NotNull Path file, final @NotNull String id) {
        Path temp = null;
        try {
            Files.createDirectories(dataFolder);
            // createFile, not createTempFile: the file then gets the umask's mode, like the rest of the data folder,
            // instead of 0600
            temp = Files.createFile(dataFolder.resolve(FILE_NAME + '.' + UUID.randomUUID() + ".tmp"));
            Files.writeString(temp, id + '\n', US_ASCII);
            try {
                createIfMissing(temp, file);
            } catch (final FileAlreadyExistsException e) {
                final String winner = read(file);
                if (winner != null) {
                    return winner;
                }
                try {
                    Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } catch (final AtomicMoveNotSupportedException notAtomic) {
                    Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        } catch (final IOException e) {
            log.warn("Could not write {}; the installation id lasts for this run only: {}", file, e.getMessage());
            return id;
        } finally {
            if (temp != null) {
                try {
                    Files.deleteIfExists(temp);
                } catch (final IOException ignored) {
                    // a leftover temporary file is harmless: the next start writes its own
                }
            }
        }
        final String written = read(file);
        return written != null ? written : id;
    }

    /** Creates {@code file} with the content of {@code temp}, failing if it exists; what is left of {@code temp} is the caller's to delete. */
    private static void createIfMissing(final @NotNull Path temp, final @NotNull Path file) throws IOException {
        try {
            Files.createLink(file, temp);
        } catch (final FileAlreadyExistsException e) {
            throw e;
        } catch (final UnsupportedOperationException | FileSystemException e) {
            // no hard links on this file system: a move that does not replace is the next best
            Files.move(temp, file);
        }
    }
}
