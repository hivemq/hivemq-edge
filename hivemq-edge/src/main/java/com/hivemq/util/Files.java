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
import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.io.File;

/**
 * @author Simon L Johnson
 */
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
    public static String getFileName(@NotNull String filePath){
        Preconditions.checkNotNull(filePath);
        int idx = filePath.lastIndexOf(File.separator);
        if(idx > -1){
            filePath = filePath.substring(idx + 1);
        }
        return filePath;
    }

    /**
     * Given a file path, will return the name of the file determined by the last
     * location of the File.separator excluding any file extension (if it exists)
     *
     * @param filePath - the file path e.g. /some/file/location.txt
     * @return fileName - the fileName e.g. location
     */
    public static String getFileNameExcludingExtension(@NotNull final String filePath){
        Preconditions.checkNotNull(filePath);
        String name = getFileName(filePath);
        if(name.contains(PERIOD)){
            name = name.substring(0, name.lastIndexOf(PERIOD) );
        }
        return name;
    }


    /**
     * Given a file path, will return the extension of the file determined by the last
     * location of a period character
     *
     * @param filePath - the file path e.g. /some/file/location.txt
     * @return fileName - the fileName e.g. txt
     */
    public static String getFileExtension(@NotNull final String filePath){
        Preconditions.checkNotNull(filePath);
        String name = getFileName(filePath);
        int idx = name.lastIndexOf(PERIOD);
        String ext = null;
        if(idx > -1){
            ext = name.substring(idx + 1);
        }
        return ext;
    }

    /**
     * Given a file path, will return the directory of the file, determined by
     * the last location of the File.separator
     * @param filePath - the file path e.g. /some/file/location.txt
     * @return the directory of the file e.g. /some/file
     */
    public static String getFilePathExcludingFile(@NotNull String filePath){
        Preconditions.checkNotNull(filePath);
        int idx = filePath.lastIndexOf(File.separator);
        if(idx > -1){
            filePath = filePath.substring(0, idx);
        }
        return filePath;
    }
}
