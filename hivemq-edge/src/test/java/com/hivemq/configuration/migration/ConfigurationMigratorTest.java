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
package com.hivemq.configuration.migration;

import com.hivemq.configuration.reader.ConfigurationFile;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigurationMigratorTest {

    @TempDir
    @NotNull  File tempDir;


    @Test
    void needsMigration_whenLegacyProtocolAdaptersArePresent_thenReturnTrue() throws IOException {
        final String xml = "<hivemq>\n" +
                "    <protocol-adapters>\n" +
                "        <file>\n" +
                "        </file>\n" +
                "    </protocol-adapters>\n" +
                "</hivemq>";
        final File configFile = new File(tempDir, "config.xml");
        Files.writeString(configFile.toPath(), xml, StandardCharsets.UTF_8);
        final ConfigurationFile configurationFile = new ConfigurationFile(configFile);
        assertTrue(ConfigurationMigrator.needsMigration(configurationFile));
    }


    @Test
    void needsMigration_whenNoLegacyProtocolAdaptersArePresent_thenReturnFalse() throws IOException {
        final String xml = "<hivemq>\n" +
                "    <protocol-adapters>\n" +
                "        <protocol-adapter>\n" +
                "        </protocol-adapter>\n" +
                "    </protocol-adapters>\n" +
                "</hivemq>";
        final File configFile = new File(tempDir, "config.xml");
        Files.writeString(configFile.toPath(), xml, StandardCharsets.UTF_8);
        final ConfigurationFile configurationFile = new ConfigurationFile(configFile);
        assertFalse(ConfigurationMigrator.needsMigration(configurationFile));
    }
}
