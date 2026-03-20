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

import static org.assertj.core.api.Assertions.assertThat;

import com.hivemq.combining.model.DataCombiner;
import com.hivemq.combining.model.DataCombining;
import com.hivemq.combining.model.DataIdentifierReference;
import com.hivemq.configuration.ConfigurationBootstrap;
import com.hivemq.configuration.info.SystemInformationImpl;
import com.hivemq.configuration.service.ConfigurationService;
import com.hivemq.persistence.mappings.fieldmapping.Instruction;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DataCombiningScopeMigratorIT {

    @TempDir
    File tmp;

    private File conf;
    private File configFile;

    @BeforeEach
    void setUp() throws IOException {
        conf = new File(tmp, "conf");
        conf.mkdir();
        new File(tmp, "data").mkdir();
        configFile = new File(conf, "config.xml");
    }

    @Test
    void startup_unscopedPrimaryTag_singleAdapter_migratesAndPersists() throws IOException {
        writeConfig(configWithUnscopedPrimary());

        final ConfigurationService service = bootstrap();

        final List<DataCombiner> combiners = service.dataCombiningExtractor().getAllCombiners();
        assertThat(combiners).hasSize(1);
        final DataCombining dc = combiners.get(0).dataCombinings().get(0);
        assertThat(dc.sources().primaryReference().scope()).isEqualTo("adapter-1");
        assertThat(dc.sources().primaryReference().type()).isEqualTo(DataIdentifierReference.Type.TAG);

        assertPersistedConfigContains("<scope>adapter-1</scope>");
    }

    @Test
    void startup_unscopedInstructionTag_singleAdapter_migratesAndPersists() throws IOException {
        writeConfig(configWithUnscopedInstruction());

        final ConfigurationService service = bootstrap();

        final List<DataCombiner> combiners = service.dataCombiningExtractor().getAllCombiners();
        assertThat(combiners).hasSize(1);
        final DataCombining dc = combiners.get(0).dataCombinings().get(0);
        final Instruction instruction = dc.instructions().get(0);
        assertThat(instruction.dataIdentifierReference()).isNotNull();
        assertThat(instruction.dataIdentifierReference().scope()).isEqualTo("adapter-1");

        assertPersistedConfigContains("<scope>adapter-1</scope>");
    }

    @Test
    void startup_unscopedPrimaryAndInstruction_bothMigrated() throws IOException {
        writeConfig(configWithUnscopedPrimaryAndInstruction());

        final ConfigurationService service = bootstrap();

        final List<DataCombiner> combiners = service.dataCombiningExtractor().getAllCombiners();
        assertThat(combiners).hasSize(1);
        final DataCombining dc = combiners.get(0).dataCombinings().get(0);
        assertThat(dc.sources().primaryReference().scope()).isEqualTo("adapter-1");
        assertThat(dc.instructions().get(0).dataIdentifierReference().scope()).isEqualTo("adapter-1");
    }

    @Test
    void startup_tagInMultipleAdapters_notMigrated() throws IOException {
        writeConfig(configWithTagInMultipleAdapters());

        final ConfigurationService service = bootstrap();

        final List<DataCombiner> combiners = service.dataCombiningExtractor().getAllCombiners();
        assertThat(combiners).hasSize(1);
        final DataCombining dc = combiners.get(0).dataCombinings().get(0);
        assertThat(dc.sources().primaryReference().scope()).isNull();
    }

    @Test
    void startup_alreadyScopedTag_notModified() throws IOException {
        writeConfig(configWithScopedPrimary());

        final ConfigurationService service = bootstrap();

        final List<DataCombiner> combiners = service.dataCombiningExtractor().getAllCombiners();
        assertThat(combiners).hasSize(1);
        final DataCombining dc = combiners.get(0).dataCombinings().get(0);
        assertThat(dc.sources().primaryReference().scope()).isEqualTo("adapter-1");
    }

    @Test
    void startup_unscopedAssetMapper_migratesAndPersists() throws IOException {
        writeConfig(configWithUnscopedAssetMapper());

        final ConfigurationService service = bootstrap();

        final List<DataCombiner> assetCombiners =
                service.assetMappingExtractor().getAllCombiners();
        assertThat(assetCombiners).hasSize(1);
        final DataCombining dc = assetCombiners.get(0).dataCombinings().get(0);
        assertThat(dc.sources().primaryReference().scope()).isEqualTo("adapter-1");

        assertPersistedConfigContains("<scope>adapter-1</scope>");
    }

    @Test
    void startup_topicFilterPrimary_notMigrated() throws IOException {
        writeConfig(configWithTopicFilterPrimary());

        final ConfigurationService service = bootstrap();

        final List<DataCombiner> combiners = service.dataCombiningExtractor().getAllCombiners();
        assertThat(combiners).hasSize(1);
        final DataCombining dc = combiners.get(0).dataCombinings().get(0);
        assertThat(dc.sources().primaryReference().scope()).isNull();
        assertThat(dc.sources().primaryReference().type()).isEqualTo(DataIdentifierReference.Type.TOPIC_FILTER);
    }

    // --- Helpers ---

    private @NotNull ConfigurationService bootstrap() {
        final SystemInformationImpl sysInfo =
                new SystemInformationImpl(true, true, conf, conf, null, null, null, null, null);
        sysInfo.init();
        return ConfigurationBootstrap.bootstrapConfig(sysInfo);
    }

    private void writeConfig(final @NotNull String xml) throws IOException {
        Files.writeString(configFile.toPath(), xml, StandardCharsets.UTF_8);
    }

    private void assertPersistedConfigContains(final @NotNull String expected) throws IOException {
        final String persisted = Files.readString(configFile.toPath(), StandardCharsets.UTF_8);
        assertThat(persisted).contains(expected);
    }

    private static @NotNull String configWithUnscopedPrimary() {
        return baseConfig(
                singleAdapter("adapter-1", "temperature"),
                dataCombinersWith(dataCombining(unscopedTagPrimary("temperature"), "")),
                "");
    }

    private static @NotNull String configWithUnscopedInstruction() {
        return baseConfig(
                singleAdapter("adapter-1", "temperature"),
                dataCombinersWith(dataCombining(
                        scopedTagPrimary("temperature", "adapter-1"),
                        instruction("$.value", "$.output", unscopedTagOrigin("temperature")))),
                "");
    }

    private static @NotNull String configWithUnscopedPrimaryAndInstruction() {
        return baseConfig(
                singleAdapter("adapter-1", "temperature", "humidity"),
                dataCombinersWith(dataCombining(
                        unscopedTagPrimary("temperature"),
                        instruction("$.value", "$.output", unscopedTagOrigin("humidity")))),
                "");
    }

    private static @NotNull String configWithTagInMultipleAdapters() {
        final String adapters = singleAdapter("adapter-1", "temperature") + singleAdapter("adapter-2", "temperature");
        return baseConfig(adapters, dataCombinersWith(dataCombining(unscopedTagPrimary("temperature"), "")), "");
    }

    private static @NotNull String configWithScopedPrimary() {
        return baseConfig(
                singleAdapter("adapter-1", "temperature"),
                dataCombinersWith(dataCombining(scopedTagPrimary("temperature", "adapter-1"), "")),
                "");
    }

    private static @NotNull String configWithUnscopedAssetMapper() {
        return baseConfig(
                singleAdapter("adapter-1", "temperature"),
                "",
                assetMappersWith(dataCombining(unscopedTagPrimary("temperature"), "")));
    }

    private static @NotNull String configWithTopicFilterPrimary() {
        return baseConfig(
                singleAdapter("adapter-1", "temperature"),
                dataCombinersWith(dataCombining(topicFilterPrimary("my/topic"), "")),
                "");
    }

    private static @NotNull String baseConfig(
            final @NotNull String adapters, final @NotNull String dataCombiners, final @NotNull String assetMappers) {
        return """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <hivemq xsi:schemaLocation="config.xsd" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
                    <mqtt-listeners/>
                    <mqtt-sn-listeners/>
                    <mqtt/>
                    <mqtt-sn/>
                    <restrictions/>
                    <security/>
                    <persistence/>
                    <mqtt-bridges/>
                    <admin-api/>
                    <uns/>
                    <dynamic-configuration>
                        <allow-mutable-configuration>true</allow-mutable-configuration>
                    </dynamic-configuration>
                    <usage-tracking/>
                    <protocol-adapters>
                """ + adapters + """
                    </protocol-adapters>
                    <data-combiners>
                """ + dataCombiners + """
                    </data-combiners>
                    <asset-mappers>
                """ + assetMappers + """
                    </asset-mappers>
                    <pulse><managed-assets/></pulse>
                    <modules/>
                    <internal/>
                </hivemq>
                """;
    }

    private static @NotNull String singleAdapter(final @NotNull String adapterId, final @NotNull String... tagNames) {
        final StringBuilder tags = new StringBuilder();
        for (final String tagName : tagNames) {
            tags.append("""
                                <tag>
                                    <name>%s</name>
                                    <description/>
                                    <definition><node>ns=1;i=1</node></definition>
                                </tag>
                    """.formatted(tagName));
        }
        return """
                        <protocol-adapter>
                            <adapterId>%s</adapterId>
                            <protocolId>simulation</protocolId>
                            <configVersion>1</configVersion>
                            <config/>
                            <tags>
                %s
                            </tags>
                            <northboundMappings/>
                            <southboundMappings/>
                        </protocol-adapter>
                """.formatted(adapterId, tags);
    }

    private static @NotNull String dataCombinersWith(final @NotNull String dataCombiningContent) {
        return """
                        <data-combiner>
                            <id>067ce8b4-8bd7-47ef-b300-1b58177437cc</id>
                            <name>test-combiner</name>
                            <description>test description</description>
                            <entity-references/>
                            <data-combinings>
                """ + dataCombiningContent + """
                            </data-combinings>
                        </data-combiner>
                """;
    }

    private static @NotNull String assetMappersWith(final @NotNull String dataCombiningContent) {
        return """
                        <asset-mapper>
                            <id>167ce8b4-8bd7-47ef-b300-1b58177437cc</id>
                            <name>test-asset-mapper</name>
                            <description>test asset description</description>
                            <entity-references/>
                            <data-combinings>
                """ + dataCombiningContent + """
                            </data-combinings>
                        </asset-mapper>
                """;
    }

    private static @NotNull String dataCombining(final @NotNull String primaryRef, final @NotNull String instructions) {
        return """
                                <data-combining>
                                    <id>c374fdbf-31e1-4b0b-9518-45f3efd2016d</id>
                                    <sources>
                                        <primary-reference>
                """ + primaryRef + """
                                        </primary-reference>
                                    </sources>
                                    <destination>
                                        <topic>dest/topic</topic>
                                        <schema>{}</schema>
                                    </destination>
                                    <instructions>
                """ + instructions + """
                                    </instructions>
                                </data-combining>
                """;
    }

    private static @NotNull String unscopedTagPrimary(final @NotNull String tagName) {
        return """
                                            <id>%s</id>
                                            <type>TAG</type>
                """.formatted(tagName);
    }

    private static @NotNull String scopedTagPrimary(final @NotNull String tagName, final @NotNull String scope) {
        return """
                                            <id>%s</id>
                                            <type>TAG</type>
                                            <scope>%s</scope>
                """.formatted(tagName, scope);
    }

    private static @NotNull String topicFilterPrimary(final @NotNull String topic) {
        return """
                                            <id>%s</id>
                                            <type>TOPIC_FILTER</type>
                """.formatted(topic);
    }

    private static @NotNull String instruction(
            final @NotNull String source, final @NotNull String destination, final @NotNull String origin) {
        return """
                                        <instruction>
                                            <source>%s</source>
                                            <destination>%s</destination>
                %s
                                        </instruction>
                """.formatted(source, destination, origin);
    }

    private static @NotNull String unscopedTagOrigin(final @NotNull String tagName) {
        return """
                                            <origin>
                                                <id>%s</id>
                                                <type>TAG</type>
                                            </origin>
                """.formatted(tagName);
    }
}
