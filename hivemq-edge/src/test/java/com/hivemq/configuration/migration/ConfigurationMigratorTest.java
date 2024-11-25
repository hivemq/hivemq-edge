package com.hivemq.configuration.migration;

import com.hivemq.configuration.reader.ConfigurationFile;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

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
