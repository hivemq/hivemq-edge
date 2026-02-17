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
package com.hivemq.extensions;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import util.TestExtensionUtil;

/**
 * @author Georg Held
 */
public class ExtensionUtilExtensionTest extends AbstractExtensionTest {

    @TempDir
    public File folder;

    @Test
    @Timeout(5)
    public void test_empty_extension_folder() {
        final File emptyFolder = new File(folder, "emptyFolder");
        emptyFolder.mkdir();
        assertFalse(ExtensionUtil.isValidExtensionFolder(emptyFolder.toPath(), true));
    }

    @Test
    @Timeout(5)
    public void test_null_folder() {
        assertFalse(ExtensionUtil.isValidExtensionFolder(new File("some-path").toPath(), true));
    }

    @Test
    @Timeout(5)
    public void test_valid_extension_folder() throws Exception {
        final File validExtensionFolder =
                TestExtensionUtil.createValidExtension(newFolder("extensions"), "validExtension");

        assertTrue(ExtensionUtil.isValidExtensionFolder(validExtensionFolder.toPath(), true));
    }

    @Test
    @Timeout(5)
    public void test_folder_jar_only() throws Exception {
        final File validExtensionFolder =
                TestExtensionUtil.createValidExtension(newFolder("extensions"), "validExtension");
        assertTrue(new File(validExtensionFolder, "hivemq-extension.xml").delete());

        assertFalse(ExtensionUtil.isValidExtensionFolder(validExtensionFolder.toPath(), true));
    }

    @Test
    @Timeout(5)
    public void test_folder_xml_only() throws Exception {
        final File validExtensionFolder =
                TestExtensionUtil.createValidExtension(newFolder("extension"), "validExtension");
        assertTrue(new File(validExtensionFolder, "extension.jar").delete());

        assertFalse(ExtensionUtil.isValidExtensionFolder(validExtensionFolder.toPath(), true));
    }

    @Test
    public void test_extension_disable() throws Exception {
        final File validExtensionFolder =
                TestExtensionUtil.createValidExtension(newFolder("extensions"), "validExtension");

        final boolean result = ExtensionUtil.disableExtensionFolder(validExtensionFolder.toPath());

        assertTrue(result);

        assertTrue(new File(validExtensionFolder, "DISABLED").exists());
    }

    File newFolder(String name) {
        File folder = new File(this.folder, name);
        folder.mkdir();
        return folder;
    }
}
