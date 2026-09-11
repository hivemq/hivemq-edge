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

import static com.hivemq.util.Files.getFileExtension;
import static com.hivemq.util.Files.getFileNameExcludingExtension;
import static com.hivemq.util.Files.getFilePathExcludingFile;

import com.hivemq.exceptions.UnrecoverableException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The rolling backup of the configuration file: {@code config_1.xml} to {@code config_5.xml} beside it,
 * the oldest slot recycled once all are taken, and only those slots -- never an operator's own file that
 * happens to share the name (EDG-949). Each backup is a copy carrying the protections of the file it
 * copies, through {@link ProtectedFileReplacer#copyCarryingProtections(Path, Path)}.
 * <p>
 * Called by {@link ConfigFileReaderWriter} once a replacement is complete and proven and just before it
 * lands, so a refused write consumes no slot.
 */
final class ConfigBackupRotation {

    private static final @NotNull Logger log = LoggerFactory.getLogger(ConfigBackupRotation.class);

    static final int MAX_BACK_FILES = 5;

    private ConfigBackupRotation() {}

    static void backup(final @NotNull File configFile, final boolean enabled) throws IOException {
        if (!enabled) {
            return;
        }
        final String fileNameNoExt = getFileNameExcludingExtension(configFile.getName());
        final String fileExt = getFileExtension(configFile.getName());
        final File copyPath = new File(getFilePathExcludingFile(configFile.getAbsolutePath()));
        if (copyPath.exists() && copyPath.isDirectory()) {
            int idx = 1;
            File copyFile;
            do {
                final String copyFilename = fileNameNoExt + '_' + idx++ + (fileExt != null ? '.' + fileExt : "");
                copyFile = new File(copyPath, copyFilename);
            } while (idx <= MAX_BACK_FILES && copyFile.exists());

            if (copyFile.exists()) {
                // -- every slot is taken: overwrite the oldest of the backups this node wrote. Exactly the
                // slot names, nothing that merely resembles them: the previous prefix-and-suffix match took
                // any "config*xml" in the directory, so an operator's own archived copy was the oldest
                // match and was silently replaced by a routine backup, and the live configuration file
                // matched too. A digits-only pattern would still take "config_2024.xml" (EDG-949).
                final File[] backupFiles = getBackupFiles(fileNameNoExt, fileExt, copyPath);
                Arrays.sort(backupFiles, Comparator.comparingLong(File::lastModified));
                copyFile = backupFiles[0];
            }
            if (log.isDebugEnabled()) {
                log.debug("Rolling backup of configuration file to {}", copyFile.getName());
            }
            ProtectedFileReplacer.copyCarryingProtections(configFile.toPath(), copyFile.toPath());
        } else {
            log.error("Configuration folder {} does not exist or is not a directory", copyPath.getAbsolutePath());
            throw new UnrecoverableException(false);
        }
    }

    private static File @NonNull [] getBackupFiles(
            final @NotNull String fileNameNoExt, final @Nullable String fileExt, final @NotNull File copyPath)
            throws IOException {
        final Set<String> slotNames = new HashSet<>();
        for (int slot = 1; slot <= MAX_BACK_FILES; slot++) {
            slotNames.add(fileNameNoExt + '_' + slot + (fileExt != null ? '.' + fileExt : ""));
        }
        final File[] backupFiles = copyPath.listFiles(child -> child.isFile() && slotNames.contains(child.getName()));
        if (backupFiles == null || backupFiles.length == 0) {
            throw new IOException(
                    "The configuration folder " + copyPath + " cannot be listed, so no" + " backup slot can be chosen");
        }
        return backupFiles;
    }
}
