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

import ch.qos.logback.classic.Logger;
import com.hivemq.combining.model.DataIdentifierReference;
import com.hivemq.configuration.entity.HiveMQConfigEntity;
import com.hivemq.configuration.entity.adapter.fieldmapping.InstructionEntity;
import com.hivemq.configuration.entity.combining.DataCombinerEntity;
import com.hivemq.configuration.entity.combining.DataCombiningEntity;
import com.hivemq.configuration.entity.combining.DataCombiningSourcesEntity;
import com.hivemq.configuration.info.SystemInformation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import util.LogbackCapturingAppender;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class DataCombiningExtractorTest {

    private static final @NotNull String MINIMAL_COMBINER_PREFIX = """
            <hivemq>
                <data-combiners>
                    <data-combiner>
                        <id>067ce8b4-8bd7-47ef-b300-1b58177437cc</id>
                        <name>test-combiner</name>
                        <description>desc</description>
                        <entity-references>
                            <entity-reference>
                                <type>EDGE_BROKER</type>
                                <id>broker-1</id>
                            </entity-reference>
                        </entity-references>
                        <data-combinings>
                            <data-combining>
                                <id>c374fdbf-31e1-4b0b-9518-45f3efd2016d</id>
            """;
    private static final @NotNull String MINIMAL_COMBINER_SUFFIX = """
                                <destination>
                                    <topic>out/topic</topic>
                                    <schema>{}</schema>
                                </destination>
                                <instructions/>
                            </data-combining>
                        </data-combinings>
                    </data-combiner>
                </data-combiners>
            </hivemq>
            """;
    @Mock
    protected @NotNull SystemInformation systemInformation;
    protected @NotNull LogbackCapturingAppender logCapture;
    @TempDir
    protected @NotNull File tempDir;

    @BeforeEach
    public void setup() {
        final Logger logger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        logCapture = LogbackCapturingAppender.Factory.weaveInto(logger);
    }

    // --- Helper: minimal valid data-combiner XML skeleton ---

    @AfterEach
    public void tearDown() throws Exception {
        LogbackCapturingAppender.Factory.cleanUp();
    }

    protected @NotNull ConfigFileReaderWriter getConfigFileReaderWriter(@Nullable String xmlString) throws IOException {
        if (xmlString == null) {
            xmlString = "<hivemq></hivemq>";
        }
        final File tempFile = new File(tempDir, "conf.xml");
        Files.writeString(tempFile.toPath(), xmlString);
        return new ConfigFileReaderWriter(systemInformation, new ConfigurationFile(tempFile), List.of());
    }

    // --- No data-combiners ---

    @Test
    public void whenDataCombinersAbsent_thenApplyConfigPasses() throws IOException {
        final ConfigFileReaderWriter reader = getConfigFileReaderWriter(null);
        final HiveMQConfigEntity config = reader.applyConfig();
        assertThat(config).isNotNull();
        assertThat(config.getDataCombinerEntities()).isEmpty();
    }

    // --- Primary reference with TOPIC_FILTER (no scope) ---

    @Test
    public void whenPrimaryIsTopicFilter_thenScopeIsNull() throws IOException {
        final String xml = MINIMAL_COMBINER_PREFIX + """
                                <sources>
                                    <primary-reference>
                                        <id>sensor/#</id>
                                        <type>TOPIC_FILTER</type>
                                    </primary-reference>
                                    <topic-filters>
                                        <topic-filter>sensor/#</topic-filter>
                                    </topic-filters>
                                </sources>
                """ + MINIMAL_COMBINER_SUFFIX;

        final HiveMQConfigEntity config = getConfigFileReaderWriter(xml).applyConfig();
        final DataCombiningSourcesEntity sources = extractFirstSources(config);

        assertThat(sources.getPrimaryIdentifier().getId()).isEqualTo("sensor/#");
        assertThat(sources.getPrimaryIdentifier().getType()).isEqualTo(DataIdentifierReference.Type.TOPIC_FILTER);
        assertThat(sources.getPrimaryIdentifier().getScope()).isNull();
        assertThat(sources.getTopicFilters()).containsExactly("sensor/#");
    }

    // --- Primary reference with TAG and scope ---

    @Test
    public void whenPrimaryIsTagWithScope_thenScopeIsDeserialized() throws IOException {
        final String xml = MINIMAL_COMBINER_PREFIX + """
                                <sources>
                                    <primary-reference>
                                        <id>temperature</id>
                                        <type>TAG</type>
                                        <scope>my-adapter-1</scope>
                                    </primary-reference>
                                    <tags>
                                        <tag>temperature</tag>
                                    </tags>
                                </sources>
                """ + MINIMAL_COMBINER_SUFFIX;

        final HiveMQConfigEntity config = getConfigFileReaderWriter(xml).applyConfig();
        final DataCombiningSourcesEntity sources = extractFirstSources(config);

        assertThat(sources.getPrimaryIdentifier().getId()).isEqualTo("temperature");
        assertThat(sources.getPrimaryIdentifier().getType()).isEqualTo(DataIdentifierReference.Type.TAG);
        assertThat(sources.getPrimaryIdentifier().getScope()).isEqualTo("my-adapter-1");
        assertThat(sources.getTags()).containsExactly("temperature");
    }

    // --- TAG primary without scope (legacy config) ---

    @Test
    public void whenPrimaryIsTagWithoutScope_thenScopeIsNull() throws IOException {
        final String xml = MINIMAL_COMBINER_PREFIX + """
                                <sources>
                                    <primary-reference>
                                        <id>temperature</id>
                                        <type>TAG</type>
                                    </primary-reference>
                                    <tags>
                                        <tag>temperature</tag>
                                    </tags>
                                </sources>
                """ + MINIMAL_COMBINER_SUFFIX;

        final HiveMQConfigEntity config = getConfigFileReaderWriter(xml).applyConfig();
        final DataCombiningSourcesEntity sources = extractFirstSources(config);

        assertThat(sources.getPrimaryIdentifier().getId()).isEqualTo("temperature");
        assertThat(sources.getPrimaryIdentifier().getType()).isEqualTo(DataIdentifierReference.Type.TAG);
        assertThat(sources.getPrimaryIdentifier().getScope()).isNull();
    }

    // --- PULSE_ASSET primary without scope ---

    @Test
    public void whenPrimaryIsPulseAssetWithoutScope_thenScopeIsNull() throws IOException {
        final String xml = MINIMAL_COMBINER_PREFIX + """
                                <sources>
                                    <primary-reference>
                                        <id>asset-1</id>
                                        <type>PULSE_ASSET</type>
                                    </primary-reference>
                                </sources>
                """ + MINIMAL_COMBINER_SUFFIX;

        final HiveMQConfigEntity config = getConfigFileReaderWriter(xml).applyConfig();
        final DataCombiningSourcesEntity sources = extractFirstSources(config);

        assertThat(sources.getPrimaryIdentifier().getId()).isEqualTo("asset-1");
        assertThat(sources.getPrimaryIdentifier().getType()).isEqualTo(DataIdentifierReference.Type.PULSE_ASSET);
        assertThat(sources.getPrimaryIdentifier().getScope()).isNull();
    }

    // --- Scope on TOPIC_FILTER (unusual but should deserialize) ---

    @Test
    public void whenTopicFilterHasScope_thenScopeIsDeserialized() throws IOException {
        final String xml = MINIMAL_COMBINER_PREFIX + """
                                <sources>
                                    <primary-reference>
                                        <id>sensor/#</id>
                                        <type>TOPIC_FILTER</type>
                                        <scope>unexpected-scope</scope>
                                    </primary-reference>
                                    <topic-filters>
                                        <topic-filter>sensor/#</topic-filter>
                                    </topic-filters>
                                </sources>
                """ + MINIMAL_COMBINER_SUFFIX;

        final HiveMQConfigEntity config = getConfigFileReaderWriter(xml).applyConfig();
        final DataCombiningSourcesEntity sources = extractFirstSources(config);

        // Deserialization should not reject it — validation is a separate concern
        assertThat(sources.getPrimaryIdentifier().getScope()).isEqualTo("unexpected-scope");
    }

    // --- Empty scope element ---

    @Test
    public void whenScopeElementIsEmpty_thenScopeIsEmptyString() throws IOException {
        final String xml = MINIMAL_COMBINER_PREFIX + """
                                <sources>
                                    <primary-reference>
                                        <id>temperature</id>
                                        <type>TAG</type>
                                        <scope></scope>
                                    </primary-reference>
                                    <tags>
                                        <tag>temperature</tag>
                                    </tags>
                                </sources>
                """ + MINIMAL_COMBINER_SUFFIX;

        final HiveMQConfigEntity config = getConfigFileReaderWriter(xml).applyConfig();
        final DataCombiningSourcesEntity sources = extractFirstSources(config);

        assertThat(sources.getPrimaryIdentifier().getScope()).isEmpty();
    }

    // --- Instructions with scoped origin ---

    @Test
    public void whenInstructionOriginHasScope_thenScopeIsDeserialized() throws IOException {
        final String xml = """
                <hivemq>
                    <data-combiners>
                        <data-combiner>
                            <id>067ce8b4-8bd7-47ef-b300-1b58177437cc</id>
                            <name>test-combiner</name>
                            <description>desc</description>
                            <entity-references>
                                <entity-reference>
                                    <type>EDGE_BROKER</type>
                                    <id>broker-1</id>
                                </entity-reference>
                            </entity-references>
                            <data-combinings>
                                <data-combining>
                                    <id>c374fdbf-31e1-4b0b-9518-45f3efd2016d</id>
                                    <sources>
                                        <primary-reference>
                                            <id>temperature</id>
                                            <type>TAG</type>
                                            <scope>adapter-1</scope>
                                        </primary-reference>
                                        <tags>
                                            <tag>temperature</tag>
                                            <tag>humidity</tag>
                                        </tags>
                                    </sources>
                                    <destination>
                                        <topic>out/topic</topic>
                                        <schema>{}</schema>
                                    </destination>
                                    <instructions>
                                        <instruction>
                                            <source>value</source>
                                            <destination>temp_value</destination>
                                            <origin>
                                                <id>temperature</id>
                                                <type>TAG</type>
                                                <scope>adapter-1</scope>
                                            </origin>
                                        </instruction>
                                        <instruction>
                                            <source>value</source>
                                            <destination>hum_value</destination>
                                            <origin>
                                                <id>humidity</id>
                                                <type>TAG</type>
                                                <scope>adapter-2</scope>
                                            </origin>
                                        </instruction>
                                    </instructions>
                                </data-combining>
                            </data-combinings>
                        </data-combiner>
                    </data-combiners>
                </hivemq>
                """;

        final HiveMQConfigEntity config = getConfigFileReaderWriter(xml).applyConfig();
        final DataCombiningEntity combining = extractFirstCombining(config);

        // Verify sources
        final DataCombiningSourcesEntity sources = combining.getSources();
        assertThat(sources.getPrimaryIdentifier().getId()).isEqualTo("temperature");
        assertThat(sources.getPrimaryIdentifier().getScope()).isEqualTo("adapter-1");
        assertThat(sources.getTags()).containsExactly("temperature", "humidity");

        // Verify instructions
        final List<InstructionEntity> instructions = combining.getInstructions();
        assertThat(instructions).hasSize(2);

        final InstructionEntity first = instructions.get(0);
        assertThat(first.getSourceFieldName()).isEqualTo("value");
        assertThat(first.getDestinationFieldName()).isEqualTo("temp_value");
        assertThat(first.getOrigin()).isNotNull();
        assertThat(first.getOrigin().getId()).isEqualTo("temperature");
        assertThat(first.getOrigin().getType()).isEqualTo(DataIdentifierReference.Type.TAG);
        assertThat(first.getOrigin().getScope()).isEqualTo("adapter-1");

        final InstructionEntity second = instructions.get(1);
        assertThat(second.getOrigin()).isNotNull();
        assertThat(second.getOrigin().getId()).isEqualTo("humidity");
        assertThat(second.getOrigin().getType()).isEqualTo(DataIdentifierReference.Type.TAG);
        assertThat(second.getOrigin().getScope()).isEqualTo("adapter-2");
    }

    // --- Instruction origin without scope (legacy) ---

    @Test
    public void whenInstructionOriginHasNoScope_thenScopeIsNull() throws IOException {
        final String xml = """
                <hivemq>
                    <data-combiners>
                        <data-combiner>
                            <id>067ce8b4-8bd7-47ef-b300-1b58177437cc</id>
                            <name>test-combiner</name>
                            <description>desc</description>
                            <entity-references>
                                <entity-reference>
                                    <type>EDGE_BROKER</type>
                                    <id>broker-1</id>
                                </entity-reference>
                            </entity-references>
                            <data-combinings>
                                <data-combining>
                                    <id>c374fdbf-31e1-4b0b-9518-45f3efd2016d</id>
                                    <sources>
                                        <primary-reference>
                                            <id>temperature</id>
                                            <type>TAG</type>
                                        </primary-reference>
                                        <tags>
                                            <tag>temperature</tag>
                                        </tags>
                                    </sources>
                                    <destination>
                                        <topic>out/topic</topic>
                                        <schema>{}</schema>
                                    </destination>
                                    <instructions>
                                        <instruction>
                                            <source>value</source>
                                            <destination>temp_value</destination>
                                            <origin>
                                                <id>temperature</id>
                                                <type>TAG</type>
                                            </origin>
                                        </instruction>
                                    </instructions>
                                </data-combining>
                            </data-combinings>
                        </data-combiner>
                    </data-combiners>
                </hivemq>
                """;

        final HiveMQConfigEntity config = getConfigFileReaderWriter(xml).applyConfig();
        final DataCombiningEntity combining = extractFirstCombining(config);

        final InstructionEntity instruction = combining.getInstructions().get(0);
        assertThat(instruction.getOrigin()).isNotNull();
        assertThat(instruction.getOrigin().getId()).isEqualTo("temperature");
        assertThat(instruction.getOrigin().getType()).isEqualTo(DataIdentifierReference.Type.TAG);
        assertThat(instruction.getOrigin().getScope()).isNull();
    }

    // --- Instruction without origin at all ---

    @Test
    public void whenInstructionHasNoOrigin_thenOriginIsNull() throws IOException {
        final String xml = """
                <hivemq>
                    <data-combiners>
                        <data-combiner>
                            <id>067ce8b4-8bd7-47ef-b300-1b58177437cc</id>
                            <name>test-combiner</name>
                            <description>desc</description>
                            <entity-references>
                                <entity-reference>
                                    <type>EDGE_BROKER</type>
                                    <id>broker-1</id>
                                </entity-reference>
                            </entity-references>
                            <data-combinings>
                                <data-combining>
                                    <id>c374fdbf-31e1-4b0b-9518-45f3efd2016d</id>
                                    <sources>
                                        <primary-reference>
                                            <id>sensor/#</id>
                                            <type>TOPIC_FILTER</type>
                                        </primary-reference>
                                        <topic-filters>
                                            <topic-filter>sensor/#</topic-filter>
                                        </topic-filters>
                                    </sources>
                                    <destination>
                                        <topic>out/topic</topic>
                                        <schema>{}</schema>
                                    </destination>
                                    <instructions>
                                        <instruction>
                                            <source>value</source>
                                            <destination>temp_value</destination>
                                        </instruction>
                                    </instructions>
                                </data-combining>
                            </data-combinings>
                        </data-combiner>
                    </data-combiners>
                </hivemq>
                """;

        final HiveMQConfigEntity config = getConfigFileReaderWriter(xml).applyConfig();
        final DataCombiningEntity combining = extractFirstCombining(config);

        final InstructionEntity instruction = combining.getInstructions().get(0);
        assertThat(instruction.getOrigin()).isNull();
    }

    // --- Tags and topicFilters both present ---

    @Test
    public void whenTagsAndTopicFiltersBothPresent_thenBothDeserialized() throws IOException {
        final String xml = MINIMAL_COMBINER_PREFIX + """
                                <sources>
                                    <primary-reference>
                                        <id>sensor/#</id>
                                        <type>TOPIC_FILTER</type>
                                    </primary-reference>
                                    <tags>
                                        <tag>temperature</tag>
                                        <tag>humidity</tag>
                                    </tags>
                                    <topic-filters>
                                        <topic-filter>sensor/#</topic-filter>
                                        <topic-filter>device/+/status</topic-filter>
                                    </topic-filters>
                                </sources>
                """ + MINIMAL_COMBINER_SUFFIX;

        final HiveMQConfigEntity config = getConfigFileReaderWriter(xml).applyConfig();
        final DataCombiningSourcesEntity sources = extractFirstSources(config);

        assertThat(sources.getTags()).containsExactly("temperature", "humidity");
        assertThat(sources.getTopicFilters()).containsExactly("sensor/#", "device/+/status");
    }

    // --- Empty tags and topicFilters ---

    @Test
    public void whenTagsAndTopicFiltersAreEmpty_thenEmptyLists() throws IOException {
        final String xml = MINIMAL_COMBINER_PREFIX + """
                                <sources>
                                    <primary-reference>
                                        <id>sensor/#</id>
                                        <type>TOPIC_FILTER</type>
                                    </primary-reference>
                                    <tags/>
                                    <topic-filters/>
                                </sources>
                """ + MINIMAL_COMBINER_SUFFIX;

        final HiveMQConfigEntity config = getConfigFileReaderWriter(xml).applyConfig();
        final DataCombiningSourcesEntity sources = extractFirstSources(config);

        assertThat(sources.getTags()).isEmpty();
        assertThat(sources.getTopicFilters()).isEmpty();
    }

    // --- Tags and topicFilters absent ---

    @Test
    public void whenTagsAndTopicFiltersAbsent_thenEmptyLists() throws IOException {
        final String xml = MINIMAL_COMBINER_PREFIX + """
                                <sources>
                                    <primary-reference>
                                        <id>sensor/#</id>
                                        <type>TOPIC_FILTER</type>
                                    </primary-reference>
                                </sources>
                """ + MINIMAL_COMBINER_SUFFIX;

        final HiveMQConfigEntity config = getConfigFileReaderWriter(xml).applyConfig();
        final DataCombiningSourcesEntity sources = extractFirstSources(config);

        assertThat(sources.getTags()).isEmpty();
        assertThat(sources.getTopicFilters()).isEmpty();
    }

    // --- Multiple data-combinings with different scope values ---

    @Test
    public void whenMultipleCombiningsDifferentScopes_thenEachScopePreserved() throws IOException {
        final String xml = """
                <hivemq>
                    <data-combiners>
                        <data-combiner>
                            <id>067ce8b4-8bd7-47ef-b300-1b58177437cc</id>
                            <name>test-combiner</name>
                            <description>desc</description>
                            <entity-references>
                                <entity-reference>
                                    <type>ADAPTER</type>
                                    <id>adapter-1</id>
                                </entity-reference>
                            </entity-references>
                            <data-combinings>
                                <data-combining>
                                    <id>c374fdbf-31e1-4b0b-9518-45f3efd2016d</id>
                                    <sources>
                                        <primary-reference>
                                            <id>temp</id>
                                            <type>TAG</type>
                                            <scope>adapter-1</scope>
                                        </primary-reference>
                                    </sources>
                                    <destination>
                                        <topic>out/1</topic>
                                        <schema>{}</schema>
                                    </destination>
                                    <instructions/>
                                </data-combining>
                                <data-combining>
                                    <id>d485feae-42f2-5c1c-a629-56a4fae3127e</id>
                                    <sources>
                                        <primary-reference>
                                            <id>temp</id>
                                            <type>TAG</type>
                                            <scope>adapter-2</scope>
                                        </primary-reference>
                                    </sources>
                                    <destination>
                                        <topic>out/2</topic>
                                        <schema>{}</schema>
                                    </destination>
                                    <instructions/>
                                </data-combining>
                            </data-combinings>
                        </data-combiner>
                    </data-combiners>
                </hivemq>
                """;

        final HiveMQConfigEntity config = getConfigFileReaderWriter(xml).applyConfig();
        final List<DataCombiningEntity> combinings = extractFirstCombiner(config).getDataCombiningEntities();
        assertThat(combinings).hasSize(2);

        assertThat(combinings.get(0).getSources().getPrimaryIdentifier().getId()).isEqualTo("temp");
        assertThat(combinings.get(0).getSources().getPrimaryIdentifier().getScope()).isEqualTo("adapter-1");
        assertThat(combinings.get(1).getSources().getPrimaryIdentifier().getId()).isEqualTo("temp");
        assertThat(combinings.get(1).getSources().getPrimaryIdentifier().getScope()).isEqualTo("adapter-2");
    }

    // --- Write-back round-trip: scope survives serialization and re-read ---

    @Test
    public void whenConfigWithScopeIsWrittenAndReRead_thenScopeIsPreserved() throws IOException {
        final String xml = MINIMAL_COMBINER_PREFIX + """
                                <sources>
                                    <primary-reference>
                                        <id>temperature</id>
                                        <type>TAG</type>
                                        <scope>my-adapter</scope>
                                    </primary-reference>
                                    <tags>
                                        <tag>temperature</tag>
                                    </tags>
                                </sources>
                """ + MINIMAL_COMBINER_SUFFIX;

        final ConfigFileReaderWriter reader = getConfigFileReaderWriter(xml);
        final HiveMQConfigEntity config = reader.applyConfig();

        // Write back via internalApplyConfig
        assertThat(reader.internalApplyConfig(config)).isTrue();

        // Re-read from the file that was just written
        final HiveMQConfigEntity reRead = reader.applyConfig();
        final DataCombiningSourcesEntity sources = extractFirstSources(reRead);

        assertThat(sources.getPrimaryIdentifier().getId()).isEqualTo("temperature");
        assertThat(sources.getPrimaryIdentifier().getType()).isEqualTo(DataIdentifierReference.Type.TAG);
        assertThat(sources.getPrimaryIdentifier().getScope()).isEqualTo("my-adapter");
        assertThat(sources.getTags()).containsExactly("temperature");
    }

    // --- Write-back round-trip: null scope stays null ---

    @Test
    public void whenConfigWithNullScopeIsWrittenAndReRead_thenScopeRemainsNull() throws IOException {
        final String xml = MINIMAL_COMBINER_PREFIX + """
                                <sources>
                                    <primary-reference>
                                        <id>sensor/#</id>
                                        <type>TOPIC_FILTER</type>
                                    </primary-reference>
                                    <topic-filters>
                                        <topic-filter>sensor/#</topic-filter>
                                    </topic-filters>
                                </sources>
                """ + MINIMAL_COMBINER_SUFFIX;

        final ConfigFileReaderWriter reader = getConfigFileReaderWriter(xml);
        final HiveMQConfigEntity config = reader.applyConfig();

        assertThat(reader.internalApplyConfig(config)).isTrue();

        final HiveMQConfigEntity reRead = reader.applyConfig();
        final DataCombiningSourcesEntity sources = extractFirstSources(reRead);

        assertThat(sources.getPrimaryIdentifier().getScope()).isNull();
    }

    // --- Write-back round-trip: instruction origin scope preserved ---

    @Test
    public void whenInstructionOriginScopeIsWrittenAndReRead_thenScopeIsPreserved() throws IOException {
        final String xml = """
                <hivemq>
                    <data-combiners>
                        <data-combiner>
                            <id>067ce8b4-8bd7-47ef-b300-1b58177437cc</id>
                            <name>test-combiner</name>
                            <description>desc</description>
                            <entity-references>
                                <entity-reference>
                                    <type>EDGE_BROKER</type>
                                    <id>broker-1</id>
                                </entity-reference>
                            </entity-references>
                            <data-combinings>
                                <data-combining>
                                    <id>c374fdbf-31e1-4b0b-9518-45f3efd2016d</id>
                                    <sources>
                                        <primary-reference>
                                            <id>temperature</id>
                                            <type>TAG</type>
                                            <scope>adapter-1</scope>
                                        </primary-reference>
                                    </sources>
                                    <destination>
                                        <topic>out/topic</topic>
                                        <schema>{}</schema>
                                    </destination>
                                    <instructions>
                                        <instruction>
                                            <source>value</source>
                                            <destination>temp_value</destination>
                                            <origin>
                                                <id>temperature</id>
                                                <type>TAG</type>
                                                <scope>adapter-1</scope>
                                            </origin>
                                        </instruction>
                                    </instructions>
                                </data-combining>
                            </data-combinings>
                        </data-combiner>
                    </data-combiners>
                </hivemq>
                """;

        final ConfigFileReaderWriter reader = getConfigFileReaderWriter(xml);
        final HiveMQConfigEntity config = reader.applyConfig();

        assertThat(reader.internalApplyConfig(config)).isTrue();

        final HiveMQConfigEntity reRead = reader.applyConfig();
        final DataCombiningEntity combining = extractFirstCombining(reRead);

        final InstructionEntity instruction = combining.getInstructions().get(0);
        assertThat(instruction.getOrigin()).isNotNull();
        assertThat(instruction.getOrigin().getScope()).isEqualTo("adapter-1");
    }

    // --- Helpers ---

    private @NotNull DataCombinerEntity extractFirstCombiner(final @NotNull HiveMQConfigEntity config) {
        assertThat(config).isNotNull();
        assertThat(config.getDataCombinerEntities()).isNotEmpty();
        return config.getDataCombinerEntities().get(0);
    }

    private @NotNull DataCombiningEntity extractFirstCombining(final @NotNull HiveMQConfigEntity config) {
        final DataCombinerEntity combiner = extractFirstCombiner(config);
        assertThat(combiner.getDataCombiningEntities()).isNotEmpty();
        return combiner.getDataCombiningEntities().get(0);
    }

    private @NotNull DataCombiningSourcesEntity extractFirstSources(final @NotNull HiveMQConfigEntity config) {
        final DataCombiningEntity combining = extractFirstCombining(config);
        assertThat(combining.getSources()).isNotNull();
        return combining.getSources();
    }
}
