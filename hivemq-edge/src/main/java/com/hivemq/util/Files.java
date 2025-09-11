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
package com.hivemq.util;

import com.google.common.base.Preconditions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

public class Files {

    static final String PERIOD = ".";

    private Files() {
        //This is a utility class, don't instantiate it!
    }

    /**
     * Given a file path, will return the name of the file determined by the last
     * location of the File.separator
     *
     * @param filePath - the file path e.g. /some/file/location.txt
     * @return fileName - the fileName e.g. location.txt
     */
    public static String getFileName(final @NotNull String filePath) {
        Preconditions.checkNotNull(filePath);
        final int idx = filePath.lastIndexOf(File.separator);
        return idx > -1 ? filePath.substring(idx + 1) : filePath;
    }

    /**
     * Given a file path, will return the name of the file determined by the last
     * location of the File.separator excluding any file extension (if it exists)
     *
     * @param filePath - the file path e.g. /some/file/location.txt
     * @return fileName - the fileName e.g. location
     */
    public static String getFileNameExcludingExtension(final @NotNull String filePath) {
        Preconditions.checkNotNull(filePath);
        final String name = getFileName(filePath);
        final int idx = name.lastIndexOf(PERIOD);
        return idx > -1 ? name.substring(0, idx) : name;
    }

    /**
     * Given a file path, will return the extension of the file determined by the last
     * location of a period character
     *
     * @param filePath - the file path e.g. /some/file/location.txt
     * @return fileName - the fileName e.g. txt
     */
    public static @Nullable String getFileExtension(final @NotNull String filePath) {
        Preconditions.checkNotNull(filePath);
        final String name = getFileName(filePath);
        final int idx = name.lastIndexOf(PERIOD);
        return idx > -1 ? name.substring(idx + 1) : null;
    }

    /**
     * Given a file path, will return the directory of the file, determined by
     * the last location of the File.separator
     *
     * @param filePath - the file path e.g. /some/file/location.txt
     * @return the directory of the file e.g. /some/file
     */
    public static @NotNull String getFilePathExcludingFile(final @NotNull String filePath) {
        Preconditions.checkNotNull(filePath);
        final int idx = filePath.lastIndexOf(File.separator);
        return idx > -1 ? filePath.substring(0, idx) : filePath;
    }
}
