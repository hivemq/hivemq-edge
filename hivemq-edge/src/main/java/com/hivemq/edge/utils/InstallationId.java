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
        final String created = UUID.randomUUID().toString();
        write(dataFolder, file, created);
        return created;
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

    /** Writes the id through a temporary file and a rename, so a crash never leaves a half-written file. */
    private static void write(final @NotNull Path dataFolder, final @NotNull Path file, final @NotNull String id) {
        Path temp = null;
        try {
            Files.createDirectories(dataFolder);
            temp = Files.createTempFile(dataFolder, FILE_NAME, ".tmp");
            Files.writeString(temp, id + '\n', US_ASCII);
            try {
                Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (final AtomicMoveNotSupportedException e) {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
            temp = null;
        } catch (final IOException e) {
            log.warn("Could not write {}; the installation id lasts for this run only: {}", file, e.getMessage());
        } finally {
            if (temp != null) {
                try {
                    Files.deleteIfExists(temp);
                } catch (final IOException ignored) {
                    // a leftover temporary file is harmless: the next start writes its own
                }
            }
        }
    }
}
