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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.File;

import org.junit.Test;

public class FilesTest {

    private static final String TEST_DIRECTORY_PATH = File.separator + "home" + File.separator + "pa.th" + File.separator;

    @Test
    public void test_file_name_when_file_path_contains_separator() {

        assertEquals("somefile.txt", Files.getFileName(TEST_DIRECTORY_PATH + "somefile.txt"));
    }

    @Test
    public void test_file_name_when_no_separator_in_file_path() {

        assertEquals("somefile.txt", Files.getFileName("somefile.txt"));
    }

    @Test
    public void test_file_name_excluding_extension_when_extension() {

        assertEquals("somefile", Files.getFileNameExcludingExtension(TEST_DIRECTORY_PATH + "somefile.txt"));
    }

    @Test
    public void test_file_name_excluding_extension_when_no_extension() {

        assertEquals("somefile", Files.getFileNameExcludingExtension("somefile"));
    }

    @Test
    public void test_get_file_extension_when_extension_exists() {

        assertEquals("txt", Files.getFileExtension(TEST_DIRECTORY_PATH + "somefile.txt"));
    }

    @Test
    public void test_get_file_extension_when_no_extension() {

        assertNull(Files.getFileExtension(TEST_DIRECTORY_PATH + "somefile"));
    }

    @Test
    public void get_file_path_excluding_file() {
        final String filePath = TEST_DIRECTORY_PATH + "somefile.txt";
        final String filePathWithoutLastSeparator = TEST_DIRECTORY_PATH.substring(0, TEST_DIRECTORY_PATH.length() - 1);

        assertEquals(filePathWithoutLastSeparator, Files.getFilePathExcludingFile(filePath));
    }

    @Test
    public void get_file_path_excluding_file_when_root_folder() {

        assertEquals("", Files.getFilePathExcludingFile(File.separator));
    }

    @Test
    public void get_file_path_excluding_file_when_no_file_path() {

        assertEquals("somefile.txt", Files.getFilePathExcludingFile("somefile.txt"));
    }
}
