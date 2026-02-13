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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.hivemq.adapter.sdk.api.config.MessageHandlingOptions;
import com.hivemq.common.i18n.StringTemplate;
import com.hivemq.configuration.entity.HiveMQConfigEntity;
import com.hivemq.configuration.entity.adapter.NorthboundMappingEntity;
import com.hivemq.configuration.entity.adapter.ProtocolAdapterEntity;
import com.hivemq.configuration.entity.adapter.SouthboundMappingEntity;
import com.hivemq.configuration.entity.adapter.TagEntity;
import com.hivemq.configuration.entity.adapter.fieldmapping.FieldMappingEntity;
import com.hivemq.configuration.info.SystemInformation;
import com.hivemq.exceptions.UnrecoverableException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class ProtocolAdapterExtractorTest {
    @TempDir
    protected @NotNull File tempDir;

    protected @NotNull ConfigFileReaderWriter getConfigFileReaderWriter() throws IOException {
        return getConfigFileReaderWriter(null);
    }

    protected @NotNull ConfigFileReaderWriter getConfigFileReaderWriter(@Nullable String xmlString) throws IOException {
        if (xmlString == null) {
            xmlString = "<hivemq></hivemq>";
        }
        final File tempFile = new File(tempDir, "conf.xml");
        Files.writeString(tempFile.toPath(), xmlString);
        return new ConfigFileReaderWriter(mock(SystemInformation.class), new ConfigurationFile(tempFile), List.of());
    }

    @Test
    public void whenAdapterIdIsAbsent_thenApplyConfigShouldFail() throws IOException {
        final ConfigFileReaderWriter configFileReader = getConfigFileReaderWriter("""
                <hivemq>
                  <protocol-adapters>
                    <protocol-adapter>
                    </protocol-adapter>
                  </protocol-adapters>
                </hivemq>
                """);
        assertThatThrownBy(configFileReader::applyConfig).isInstanceOf(UnrecoverableException.class);
    }

    @Test
    public void whenMessageHandlingOptionsInNorthboundMappingIsAbsent_thenApplyConfigShouldPass() throws IOException {
        final ConfigFileReaderWriter configFileReader = getConfigFileReaderWriter("""
                <hivemq>
                  <protocol-adapters>
                    <protocol-adapter>
                      <adapterId>simInvalid</adapterId>
                      <protocolId>simulation</protocolId>
                      <configVersion>1</configVersion>
                      <config>
                        <pollingIntervalMillis>123</pollingIntervalMillis>
                        <timeout>10000</timeout>
                        <minDelay>0</minDelay>
                        <maxDelay>0</maxDelay>
                      </config>
                      <northboundMappings>
                        <northboundMapping>
                          <tagName>tag1</tagName>
                          <topic>MTConnect/my-steams</topic>
                          <maxQos>1</maxQos>
                          <messageHandlingOptions/>
                          <includeTagNames>false</includeTagNames>
                          <includeTimestamp>true</includeTimestamp>
                          <mqttUserProperties/>
                          <messageExpiryInterval>9223372036854775807</messageExpiryInterval>
                        </northboundMapping>
                      </northboundMappings>
                      <tags>
                        <tag>
                          <name>tag1</name>
                          <description>description1</description>
                        </tag>
                      </tags>
                    </protocol-adapter>
                  </protocol-adapters>
                </hivemq>
                """);
        assertThat(configFileReader.applyConfig()).isNotNull();
        final ProtocolAdapterExtractor protocolAdapterExtractor = configFileReader.getProtocolAdapterExtractor();
        assertThat(protocolAdapterExtractor.getAllConfigs()).hasSize(1);
        final ProtocolAdapterEntity protocolAdapterEntity =
                protocolAdapterExtractor.getAllConfigs().getFirst();
        assertThat(protocolAdapterEntity.getAdapterId()).isEqualTo("simInvalid");
        assertThat(protocolAdapterEntity.getProtocolId()).isEqualTo("simulation");
        assertThat(protocolAdapterEntity.getNorthboundMappings()).hasSize(1);
        final NorthboundMappingEntity northboundMappingEntity =
                protocolAdapterEntity.getNorthboundMappings().getFirst();
        assertThat(northboundMappingEntity.getTopic()).isEqualTo("MTConnect/my-steams");
        assertThat(northboundMappingEntity.getTagName()).isEqualTo("tag1");
        assertThat(northboundMappingEntity.getMessageHandlingOptions())
                .isEqualTo(MessageHandlingOptions.MQTTMessagePerTag);
        protocolAdapterEntity.getNorthboundMappings().clear();
        protocolAdapterEntity
                .getNorthboundMappings()
                .add(new NorthboundMappingEntity(
                        northboundMappingEntity.getTagName(),
                        northboundMappingEntity.getTopic(),
                        northboundMappingEntity.getMaxQoS(),
                        null,
                        northboundMappingEntity.isIncludeTagNames(),
                        northboundMappingEntity.isIncludeTimestamp(),
                        northboundMappingEntity.getUserProperties(),
                        northboundMappingEntity.getMessageExpiryInterval()));
        final File newConfigFile = new File(tempDir, "new-conf.xml");
        try (final FileWriter fileWriter = new FileWriter(newConfigFile)) {
            configFileReader.writeConfigToXML(fileWriter);
        }
        final String newConfigString = Files.readString(newConfigFile.toPath());
        // If nillable = true is set on messageHandlingOptions, the output XML would contain xsi:nil="true".
        // This is a bug that can cause the config to be invalid.
        // @XmlElement(name = "messageHandlingOptions", defaultValue = "MQTTMessagePerTag", nillable = true)
        // private final @Nullable MessageHandlingOptions messageHandlingOptions;
        assertThat(newConfigString)
                .as("<messageHandlingOptions/> shouldn't contain xsi:nil=\"true\"")
                .doesNotContain("<messageHandlingOptions xsi:nil=\"true\"/>");
    }

    @ParameterizedTest
    @CsvSource({"true,0", "true,1", "true,2", "false,-1", "false,3", "false,abc"})
    public void whenNorthboundMappingQoSIsProvided_thenApplyConfigShouldWorkAsExpected(
            final boolean valid, final @NotNull String maxQoS) throws IOException {
        final ConfigFileReaderWriter configFileReader =
                getConfigFileReaderWriter(StringTemplate.format("""
                <hivemq>
                  <protocol-adapters>
                    <protocol-adapter>
                      <adapterId>simInvalid</adapterId>
                      <protocolId>simulation</protocolId>
                      <configVersion>1</configVersion>
                      <config>
                        <pollingIntervalMillis>123</pollingIntervalMillis>
                        <timeout>10000</timeout>
                        <minDelay>0</minDelay>
                        <maxDelay>0</maxDelay>
                      </config>
                      <northboundMappings>
                        <northboundMapping>
                          <topic>MTConnect/my-steams</topic>
                          <tagName>tag1</tagName>
                          <maxQos>${maxQoS}</maxQos>
                        </northboundMapping>
                      </northboundMappings>
                      <tags>
                        <tag>
                          <name>tag1</name>
                          <description>description1</description>
                        </tag>
                      </tags>
                    </protocol-adapter>
                  </protocol-adapters>
                </hivemq>
                """, Map.of("maxQoS", maxQoS)));
        if (valid) {
            assertThat(configFileReader.applyConfig()).isNotNull();
        } else {
            assertThatThrownBy(configFileReader::applyConfig).isInstanceOf(UnrecoverableException.class);
        }
    }

    @ParameterizedTest
    @CsvSource({"true,true", "true,false", "false,abc"})
    public void whenNorthboundMappingIncludeTagNamesIsProvided_thenApplyConfigShouldWorkAsExpected(
            final boolean valid, final @NotNull String includeTagNames) throws IOException {
        final ConfigFileReaderWriter configFileReader =
                getConfigFileReaderWriter(StringTemplate.format("""
                <hivemq>
                  <protocol-adapters>
                    <protocol-adapter>
                      <adapterId>simInvalid</adapterId>
                      <protocolId>simulation</protocolId>
                      <configVersion>1</configVersion>
                      <config>
                        <pollingIntervalMillis>123</pollingIntervalMillis>
                        <timeout>10000</timeout>
                        <minDelay>0</minDelay>
                        <maxDelay>0</maxDelay>
                      </config>
                      <northboundMappings>
                        <northboundMapping>
                          <topic>MTConnect/my-steams</topic>
                          <tagName>tag1</tagName>
                          <includeTagNames>${includeTagNames}</includeTagNames>
                        </northboundMapping>
                      </northboundMappings>
                      <tags>
                        <tag>
                          <name>tag1</name>
                          <description>description1</description>
                        </tag>
                      </tags>
                    </protocol-adapter>
                  </protocol-adapters>
                </hivemq>
                """, Map.of("includeTagNames", includeTagNames)));
        if (valid) {
            assertThat(configFileReader.applyConfig()).isNotNull();
        } else {
            assertThatThrownBy(configFileReader::applyConfig).isInstanceOf(UnrecoverableException.class);
        }
    }

    @ParameterizedTest
    @CsvSource({"true,true", "true,false", "false,abc"})
    public void whenNorthboundMappingIncludeTimestampIsProvided_thenApplyConfigShouldWorkAsExpected(
            final boolean valid, final @NotNull String includeTimestamp) throws IOException {
        final ConfigFileReaderWriter configFileReader =
                getConfigFileReaderWriter(StringTemplate.format("""
                <hivemq>
                  <protocol-adapters>
                    <protocol-adapter>
                      <adapterId>simInvalid</adapterId>
                      <protocolId>simulation</protocolId>
                      <configVersion>1</configVersion>
                      <config>
                        <pollingIntervalMillis>123</pollingIntervalMillis>
                        <timeout>10000</timeout>
                        <minDelay>0</minDelay>
                        <maxDelay>0</maxDelay>
                      </config>
                      <northboundMappings>
                        <northboundMapping>
                          <topic>MTConnect/my-steams</topic>
                          <tagName>tag1</tagName>
                          <includeTimestamp>${includeTimestamp}</includeTimestamp>
                        </northboundMapping>
                      </northboundMappings>
                      <tags>
                        <tag>
                          <name>tag1</name>
                          <description>description1</description>
                        </tag>
                      </tags>
                    </protocol-adapter>
                  </protocol-adapters>
                </hivemq>
                """, Map.of("includeTimestamp", includeTimestamp)));
        if (valid) {
            assertThat(configFileReader.applyConfig()).isNotNull();
        } else {
            assertThatThrownBy(configFileReader::applyConfig).isInstanceOf(UnrecoverableException.class);
        }
    }

    @ParameterizedTest
    @CsvSource({"true,1", "true,123", "false,0", "false,-1", "false,abc"})
    public void whenNorthboundMappingMessageExpiryIntervalIsProvided_thenApplyConfigShouldWorkAsExpected(
            final boolean valid, final @NotNull String messageExpiryInterval) throws IOException {
        final ConfigFileReaderWriter configFileReader = getConfigFileReaderWriter(
                StringTemplate.format("""
                <hivemq>
                  <protocol-adapters>
                    <protocol-adapter>
                      <adapterId>simInvalid</adapterId>
                      <protocolId>simulation</protocolId>
                      <configVersion>1</configVersion>
                      <config>
                        <pollingIntervalMillis>123</pollingIntervalMillis>
                        <timeout>10000</timeout>
                        <minDelay>0</minDelay>
                        <maxDelay>0</maxDelay>
                      </config>
                      <northboundMappings>
                        <northboundMapping>
                          <topic>MTConnect/my-steams</topic>
                          <tagName>tag1</tagName>
                          <messageExpiryInterval>${messageExpiryInterval}</messageExpiryInterval>
                        </northboundMapping>
                      </northboundMappings>
                      <tags>
                        <tag>
                          <name>tag1</name>
                          <description>description1</description>
                        </tag>
                      </tags>
                    </protocol-adapter>
                  </protocol-adapters>
                </hivemq>
                """, Map.of("messageExpiryInterval", messageExpiryInterval)));
        if (valid) {
            assertThat(configFileReader.applyConfig()).isNotNull();
        } else {
            assertThatThrownBy(configFileReader::applyConfig).isInstanceOf(UnrecoverableException.class);
        }
    }

    @Test
    public void whenNoMappings_thenApplyConfigShouldPass() throws IOException {
        final ConfigFileReaderWriter configFileReader = getConfigFileReaderWriter("""
                <hivemq>
                  <protocol-adapters>
                    <protocol-adapter>
                      <adapterId>simInvalid</adapterId>
                      <protocolId>simulation</protocolId>
                      <configVersion>1</configVersion>
                      <config>
                        <pollingIntervalMillis>123</pollingIntervalMillis>
                        <timeout>10000</timeout>
                        <minDelay>0</minDelay>
                        <maxDelay>0</maxDelay>
                      </config>
                      <tags>
                        <tag>
                          <name>tag1</name>
                          <description>description1</description>
                        </tag>
                      </tags>
                    </protocol-adapter>
                  </protocol-adapters>
                </hivemq>
                """);
        assertThat(configFileReader.applyConfig()).isNotNull();
    }

    @Test
    public void whenNoMappingsNoTags_thenApplyConfigShouldPass() throws IOException {
        final ConfigFileReaderWriter configFileReader = getConfigFileReaderWriter("""
                <hivemq>
                  <protocol-adapters>
                    <protocol-adapter>
                      <adapterId>simInvalid</adapterId>
                      <protocolId>simulation</protocolId>
                      <configVersion>1</configVersion>
                      <config>
                        <pollingIntervalMillis>123</pollingIntervalMillis>
                        <timeout>10000</timeout>
                        <minDelay>0</minDelay>
                        <maxDelay>0</maxDelay>
                      </config>
                    </protocol-adapter>
                  </protocol-adapters>
                </hivemq>
                """);
        assertThat(configFileReader.applyConfig()).isNotNull();
    }

    @Test
    public void whenNoTags_thenApplyConfigShouldFail() throws IOException {
        final ConfigFileReaderWriter configFileReader = getConfigFileReaderWriter("""
                <hivemq>
                  <protocol-adapters>
                    <protocol-adapter>
                      <adapterId>simInvalid</adapterId>
                      <protocolId>simulation</protocolId>
                      <configVersion>1</configVersion>
                      <config>
                        <pollingIntervalMillis>123</pollingIntervalMillis>
                        <timeout>10000</timeout>
                        <minDelay>0</minDelay>
                        <maxDelay>0</maxDelay>
                      </config>
                      <northboundMappings>
                        <northboundMapping>
                          <topic>MTConnect/my-steams</topic>
                          <tagName>tag1</tagName>
                        </northboundMapping>
                      </northboundMappings>
                    </protocol-adapter>
                  </protocol-adapters>
                </hivemq>
                """);
        assertThatThrownBy(configFileReader::applyConfig).isInstanceOf(UnrecoverableException.class);
    }

    @Test
    public void whenMessageHandlingOptionsIsNillable_thenApplyConfigShouldFail() throws IOException {
        final ConfigFileReaderWriter configFileReader = getConfigFileReaderWriter("""
                <hivemq>
                  <protocol-adapters>
                    <protocol-adapter>
                      <adapterId>simInvalid</adapterId>
                      <protocolId>simulation</protocolId>
                      <configVersion>1</configVersion>
                      <config>
                        <pollingIntervalMillis>123</pollingIntervalMillis>
                        <timeout>10000</timeout>
                        <minDelay>0</minDelay>
                        <maxDelay>0</maxDelay>
                      </config>
                      <northboundMappings>
                        <northboundMapping>
                          <topic>MTConnect/my-steams</topic>
                          <messageHandlingOptions xsi:nil="true"/>
                        </northboundMapping>
                      </northboundMappings>
                    </protocol-adapter>
                  </protocol-adapters>
                </hivemq>
                """);
        // XML schema violation in line '16' and column '51' caused by:
        // "The prefix "xsi" for attribute "xsi:nil" associated with an element type "messageHandlingOptions" is not
        // bound."
        assertThatThrownBy(configFileReader::applyConfig).isInstanceOf(UnrecoverableException.class);
    }

    @Test
    public void whenUnexpectedXmlElementIsUnderHivemq_thenApplyConfigShouldFail() throws IOException {
        final ConfigFileReaderWriter configFileReader = getConfigFileReaderWriter("""
                <hivemq>
                  <adapterId>simInvalid</adapterId>
                  <protocolId>simulation</protocolId>
                  <configVersion>1</configVersion>
                  <config>
                    <pollingIntervalMillis>123</pollingIntervalMillis>
                    <timeout>10000</timeout>
                    <minDelay>0</minDelay>
                    <maxDelay>0</maxDelay>
                  </config>
                </hivemq>
                """);
        assertThatThrownBy(configFileReader::applyConfig).isInstanceOf(UnrecoverableException.class);
    }

    @Test
    public void whenNoMappingsNoTags_setConfigurationShouldReturnTrue() throws IOException {
        final ConfigFileReaderWriter configFileReader = getConfigFileReaderWriter();
        final HiveMQConfigEntity entity = configFileReader.applyConfig();
        assertThat(entity).isNotNull();
        final ProtocolAdapterEntity protocolAdapterEntity =
                new ProtocolAdapterEntity("adapterId", "protocolId", 1, Map.of(), List.of(), List.of(), List.of());
        entity.getProtocolAdapterConfig().add(protocolAdapterEntity);
        assertThat(configFileReader.internalApplyConfig(entity)).isTrue();
    }

    @Test
    public void whenNoMappings_setConfigurationShouldReturnTrue() throws IOException {
        final ConfigFileReaderWriter configFileReader = getConfigFileReaderWriter();
        final HiveMQConfigEntity entity = configFileReader.applyConfig();
        assertThat(entity).isNotNull();
        final ProtocolAdapterEntity protocolAdapterEntity = new ProtocolAdapterEntity(
                "adapterId",
                "protocolId",
                1,
                Map.of(),
                List.of(),
                List.of(),
                List.of(new TagEntity("abc", "def", Map.of())));
        entity.getProtocolAdapterConfig().add(protocolAdapterEntity);
        assertThat(configFileReader.internalApplyConfig(entity)).isTrue();
    }

    @Test
    public void whenNoTags_setConfigurationShouldReturnFalse() throws IOException {
        final ConfigFileReaderWriter configFileReader = getConfigFileReaderWriter();
        final HiveMQConfigEntity entity = configFileReader.applyConfig();
        assertThat(entity).isNotNull();
        final NorthboundMappingEntity northboundMappingEntity =
                new NorthboundMappingEntity("tagName", "topic", 1, null, false, true, List.of(), 100L);
        final ProtocolAdapterEntity protocolAdapterEntity = new ProtocolAdapterEntity(
                "adapterId", "protocolId", 1, Map.of(), List.of(northboundMappingEntity), List.of(), List.of());
        entity.getProtocolAdapterConfig().add(protocolAdapterEntity);
        assertThat(configFileReader.internalApplyConfig(entity)).isFalse();
    }

    @Test
    public void whenNorthboundMappingTagNameAreNotFound_setConfigurationShouldReturnFalse() throws IOException {
        final ConfigFileReaderWriter configFileReader = getConfigFileReaderWriter();
        final HiveMQConfigEntity entity = configFileReader.applyConfig();
        assertThat(entity).isNotNull();
        final NorthboundMappingEntity northboundMappingEntity = new NorthboundMappingEntity(
                "tagName", "topic", 1, MessageHandlingOptions.MQTTMessagePerSubscription, false, true, List.of(), 100L);
        final ProtocolAdapterEntity protocolAdapterEntity = new ProtocolAdapterEntity(
                "adapterId",
                "protocolId",
                1,
                Map.of(),
                List.of(northboundMappingEntity),
                List.of(),
                List.of(new TagEntity("abc", "def", Map.of())));
        entity.getProtocolAdapterConfig().add(protocolAdapterEntity);
        assertThat(configFileReader.internalApplyConfig(entity)).isFalse();
    }

    @ParameterizedTest
    @CsvSource({",topic", "tagName,"})
    public void whenNorthboundMappingTagNameOrTopicIsEmpty_setConfigurationShouldReturnFalse(
            final @NotNull String tagName, final @NotNull String topic) throws IOException {
        final ConfigFileReaderWriter configFileReader = getConfigFileReaderWriter();
        final HiveMQConfigEntity entity = configFileReader.applyConfig();
        assertThat(entity).isNotNull();
        final NorthboundMappingEntity northboundMappingEntity = new NorthboundMappingEntity(
                tagName, topic, 1, MessageHandlingOptions.MQTTMessagePerSubscription, false, true, List.of(), 100L);
        final ProtocolAdapterEntity protocolAdapterEntity = new ProtocolAdapterEntity(
                "adapterId", "protocolId", 1, Map.of(), List.of(northboundMappingEntity), List.of(), List.of());
        entity.getProtocolAdapterConfig().add(protocolAdapterEntity);
        assertThat(configFileReader.internalApplyConfig(entity)).isFalse();
    }

    @Test
    public void whenSouthboundMappingTagNameIsNotFound_setConfigurationShouldReturnFalse() throws IOException {
        final ConfigFileReaderWriter configFileReader = getConfigFileReaderWriter();
        final HiveMQConfigEntity entity = configFileReader.applyConfig();
        assertThat(entity).isNotNull();
        final SouthboundMappingEntity southboundMappingEntity =
                new SouthboundMappingEntity("tagName", "topicFilter", new FieldMappingEntity(), "schema");
        final ProtocolAdapterEntity protocolAdapterEntity = new ProtocolAdapterEntity(
                "adapterId",
                "protocolId",
                1,
                Map.of(),
                List.of(),
                List.of(southboundMappingEntity),
                List.of(new TagEntity("abc", "def", Map.of())));
        entity.getProtocolAdapterConfig().add(protocolAdapterEntity);
        assertThat(configFileReader.internalApplyConfig(entity)).isFalse();
    }

    @ParameterizedTest
    @CsvSource({",topicFilter,schema", "tagName,,schema", "tagName,topicFilter,"})
    public void whenSouthboundMappingTagNameOrTopicFilterOrSchemaIsEmpty_setConfigurationShouldReturnFalse(
            final @NotNull String tagName, final @NotNull String topicFilter, final @NotNull String schema)
            throws IOException {
        final ConfigFileReaderWriter configFileReader = getConfigFileReaderWriter();
        final HiveMQConfigEntity entity = configFileReader.applyConfig();
        assertThat(entity).isNotNull();
        final SouthboundMappingEntity southboundMappingEntity =
                new SouthboundMappingEntity(tagName, topicFilter, new FieldMappingEntity(), schema);
        final ProtocolAdapterEntity protocolAdapterEntity = new ProtocolAdapterEntity(
                "adapterId", "protocolId", 1, Map.of(), List.of(), List.of(southboundMappingEntity), List.of());
        entity.getProtocolAdapterConfig().add(protocolAdapterEntity);
        assertThat(configFileReader.internalApplyConfig(entity)).isFalse();
    }

    @Test
    public void whenNoDuplicatedTags_thenAddAdapterReturnsTrue() throws IOException {
        final ConfigFileReaderWriter configFileReader = getConfigFileReaderWriter();
        final ProtocolAdapterExtractor protocolAdapterExtractor = configFileReader.getProtocolAdapterExtractor();
        assertThat(protocolAdapterExtractor.addAdapter(new ProtocolAdapterEntity(
                        "adapterId",
                        "protocolId",
                        1,
                        Map.of(),
                        List.of(),
                        List.of(),
                        IntStream.range(0, 10)
                                .mapToObj(i -> new TagEntity("tag" + i, "description" + i, Map.of()))
                                .toList())))
                .isTrue();
    }

    @Test
    public void whenNoDuplicatedTags_thenUpdateAdapterReturnsTrue() throws IOException {
        final ConfigFileReaderWriter configFileReader = getConfigFileReaderWriter();
        final ProtocolAdapterExtractor protocolAdapterExtractor = configFileReader.getProtocolAdapterExtractor();
        assertThat(protocolAdapterExtractor.addAdapter(new ProtocolAdapterEntity(
                        "adapterId",
                        "protocolId",
                        1,
                        Map.of(),
                        List.of(),
                        List.of(),
                        IntStream.range(0, 5)
                                .mapToObj(i -> new TagEntity("tag" + i, "description" + i, Map.of()))
                                .toList())))
                .isTrue();
        assertThat(protocolAdapterExtractor.updateAdapter(new ProtocolAdapterEntity(
                        "adapterId",
                        "protocolId",
                        1,
                        Map.of(),
                        List.of(),
                        List.of(),
                        IntStream.range(0, 10)
                                .mapToObj(i -> new TagEntity("tag" + i, "description" + i, Map.of()))
                                .toList())))
                .isTrue();
    }

    @Test
    public void whenNoDuplicatedTags_thenUpdateAllAdaptersReturnsTrue() throws IOException {
        final ConfigFileReaderWriter configFileReader = getConfigFileReaderWriter();
        final ProtocolAdapterExtractor protocolAdapterExtractor = configFileReader.getProtocolAdapterExtractor();
        IntStream.range(0, 10).forEach(j -> assertThat(protocolAdapterExtractor.addAdapter(new ProtocolAdapterEntity(
                        "adapterId" + j,
                        "protocolId",
                        1,
                        Map.of(),
                        List.of(),
                        List.of(),
                        IntStream.range(0, 5)
                                .mapToObj(i -> new TagEntity("tag" + i, "description" + i, Map.of()))
                                .toList())))
                .isTrue());
        assertThat(protocolAdapterExtractor.updateAllAdapters(IntStream.range(0, 10)
                        .mapToObj(j -> new ProtocolAdapterEntity(
                                "adapterId" + j,
                                "protocolId",
                                1,
                                Map.of(),
                                List.of(),
                                List.of(),
                                IntStream.range(0, 10)
                                        .mapToObj(i -> new TagEntity("tag123" + i, "description" + i, Map.of()))
                                        .toList()))
                        .toList()))
                .isEqualTo(Configurator.ConfigResult.SUCCESS);
    }

    @Test
    public void whenDuplicatedTags_thenAddAdapterReturnsFalse() throws IOException {
        final ConfigFileReaderWriter configFileReader = getConfigFileReaderWriter();
        final ProtocolAdapterExtractor protocolAdapterExtractor = configFileReader.getProtocolAdapterExtractor();
        assertThat(protocolAdapterExtractor.addAdapter(new ProtocolAdapterEntity(
                        "adapterId",
                        "protocolId",
                        1,
                        Map.of(),
                        List.of(),
                        List.of(),
                        IntStream.range(0, 10)
                                .mapToObj(i -> new TagEntity("tag", "description", Map.of()))
                                .toList())))
                .isFalse();
    }

    @Test
    public void whenDuplicatedTags_thenUpdateAdapterReturnsFalse() throws IOException {
        final ConfigFileReaderWriter configFileReader = getConfigFileReaderWriter();
        final ProtocolAdapterExtractor protocolAdapterExtractor = configFileReader.getProtocolAdapterExtractor();
        assertThat(protocolAdapterExtractor.addAdapter(new ProtocolAdapterEntity(
                        "adapterId",
                        "protocolId",
                        1,
                        Map.of(),
                        List.of(),
                        List.of(),
                        IntStream.range(0, 5)
                                .mapToObj(i -> new TagEntity("tag" + i, "description" + i, Map.of()))
                                .toList())))
                .isTrue();
        assertThat(protocolAdapterExtractor.updateAdapter(new ProtocolAdapterEntity(
                        "adapterId",
                        "protocolId",
                        1,
                        Map.of(),
                        List.of(),
                        List.of(),
                        IntStream.range(0, 10)
                                .mapToObj(i -> new TagEntity("tag", "description", Map.of()))
                                .toList())))
                .isFalse();
    }

    @Test
    public void whenDuplicatedTags_thenUpdateAllAdaptersReturnsFalse() throws IOException {
        final ConfigFileReaderWriter configFileReader = getConfigFileReaderWriter();
        final ProtocolAdapterExtractor protocolAdapterExtractor = configFileReader.getProtocolAdapterExtractor();
        IntStream.range(0, 10).forEach(j -> assertThat(protocolAdapterExtractor.addAdapter(new ProtocolAdapterEntity(
                        "adapterId" + j,
                        "protocolId",
                        1,
                        Map.of(),
                        List.of(),
                        List.of(),
                        IntStream.range(0, 5)
                                .mapToObj(i -> new TagEntity("tag" + i, "description" + i, Map.of()))
                                .toList())))
                .isTrue());
        assertThat(protocolAdapterExtractor.updateAllAdapters(IntStream.range(0, 10)
                        .mapToObj(j -> new ProtocolAdapterEntity(
                                "adapterId" + j,
                                "protocolId",
                                1,
                                Map.of(),
                                List.of(),
                                List.of(),
                                IntStream.range(0, 10)
                                        .mapToObj(i -> new TagEntity("tag", "description", Map.of()))
                                        .toList()))
                        .toList()))
                .isEqualTo(Configurator.ConfigResult.ERROR);
    }

    @Test
    public void whenAdapterExists_thenDeleteAdapterReturnsTrue() throws IOException {
        final ConfigFileReaderWriter configFileReader = getConfigFileReaderWriter();
        final ProtocolAdapterExtractor protocolAdapterExtractor = configFileReader.getProtocolAdapterExtractor();
        assertThat(protocolAdapterExtractor.addAdapter(new ProtocolAdapterEntity(
                        "adapterId",
                        "protocolId",
                        1,
                        Map.of(),
                        List.of(),
                        List.of(),
                        IntStream.range(0, 5)
                                .mapToObj(i -> new TagEntity("tag" + i, "description" + i, Map.of()))
                                .toList())))
                .isTrue();
        assertThat(protocolAdapterExtractor.deleteAdapter("adapterId")).isTrue();
    }

    @Test
    public void whenAdapterDoesNotExist_thenDeleteAdapterReturnsFalse() throws IOException {
        final ConfigFileReaderWriter configFileReader = getConfigFileReaderWriter();
        final ProtocolAdapterExtractor protocolAdapterExtractor = configFileReader.getProtocolAdapterExtractor();
        assertThat(protocolAdapterExtractor.deleteAdapter("adapterId")).isFalse();
    }

    // ==================== Tests for (adapterId, tagName) deduplication ====================

    @Test
    public void whenSameTagNameInDifferentAdapters_thenAddAdapterReturnsTrue() throws IOException {
        final ConfigFileReaderWriter configFileReader = getConfigFileReaderWriter();
        final ProtocolAdapterExtractor protocolAdapterExtractor = configFileReader.getProtocolAdapterExtractor();

        // Add first adapter with tag "sharedTag"
        assertThat(protocolAdapterExtractor.addAdapter(new ProtocolAdapterEntity(
                        "adapter1",
                        "protocolId",
                        1,
                        Map.of(),
                        List.of(),
                        List.of(),
                        List.of(new TagEntity("sharedTag", "description1", Map.of())))))
                .isTrue();

        // Add second adapter with the same tag name "sharedTag" - should succeed
        // because deduplication is by (adapterId, tagName), not just tagName
        assertThat(protocolAdapterExtractor.addAdapter(new ProtocolAdapterEntity(
                        "adapter2",
                        "protocolId",
                        1,
                        Map.of(),
                        List.of(),
                        List.of(),
                        List.of(new TagEntity("sharedTag", "description2", Map.of())))))
                .isTrue();

        assertThat(protocolAdapterExtractor.getAllConfigs()).hasSize(2);
    }

    @Test
    public void whenSameTagNameInDifferentAdapters_thenUpdateAllAdaptersReturnsSuccess() throws IOException {
        final ConfigFileReaderWriter configFileReader = getConfigFileReaderWriter();
        final ProtocolAdapterExtractor protocolAdapterExtractor = configFileReader.getProtocolAdapterExtractor();

        // Update all adapters with multiple adapters having the same tag name
        assertThat(protocolAdapterExtractor.updateAllAdapters(List.of(
                        new ProtocolAdapterEntity(
                                "adapter1",
                                "protocolId",
                                1,
                                Map.of(),
                                List.of(),
                                List.of(),
                                List.of(new TagEntity("sharedTag", "description1", Map.of()))),
                        new ProtocolAdapterEntity(
                                "adapter2",
                                "protocolId",
                                1,
                                Map.of(),
                                List.of(),
                                List.of(),
                                List.of(new TagEntity("sharedTag", "description2", Map.of()))),
                        new ProtocolAdapterEntity(
                                "adapter3",
                                "protocolId",
                                1,
                                Map.of(),
                                List.of(),
                                List.of(),
                                List.of(new TagEntity("sharedTag", "description3", Map.of()))))))
                .isEqualTo(Configurator.ConfigResult.SUCCESS);

        assertThat(protocolAdapterExtractor.getAllConfigs()).hasSize(3);
    }

    @Test
    public void whenUpdateAdapterWithTagNameExistingInOtherAdapter_thenUpdateAdapterReturnsTrue() throws IOException {
        final ConfigFileReaderWriter configFileReader = getConfigFileReaderWriter();
        final ProtocolAdapterExtractor protocolAdapterExtractor = configFileReader.getProtocolAdapterExtractor();

        // Add first adapter with tag "existingTag"
        assertThat(protocolAdapterExtractor.addAdapter(new ProtocolAdapterEntity(
                        "adapter1",
                        "protocolId",
                        1,
                        Map.of(),
                        List.of(),
                        List.of(),
                        List.of(new TagEntity("existingTag", "description1", Map.of())))))
                .isTrue();

        // Add second adapter with different tag
        assertThat(protocolAdapterExtractor.addAdapter(new ProtocolAdapterEntity(
                        "adapter2",
                        "protocolId",
                        1,
                        Map.of(),
                        List.of(),
                        List.of(),
                        List.of(new TagEntity("otherTag", "description2", Map.of())))))
                .isTrue();

        // Update second adapter to use "existingTag" - should succeed
        // because (adapter2, existingTag) is different from (adapter1, existingTag)
        assertThat(protocolAdapterExtractor.updateAdapter(new ProtocolAdapterEntity(
                        "adapter2",
                        "protocolId",
                        1,
                        Map.of(),
                        List.of(),
                        List.of(),
                        List.of(new TagEntity("existingTag", "updated description", Map.of())))))
                .isTrue();
    }

    @Test
    public void whenDeleteAdapter_thenTagNamesAreFreedForOtherAdapters() throws IOException {
        final ConfigFileReaderWriter configFileReader = getConfigFileReaderWriter();
        final ProtocolAdapterExtractor protocolAdapterExtractor = configFileReader.getProtocolAdapterExtractor();

        // Add first adapter with tags
        assertThat(protocolAdapterExtractor.addAdapter(new ProtocolAdapterEntity(
                        "adapter1",
                        "protocolId",
                        1,
                        Map.of(),
                        List.of(),
                        List.of(),
                        List.of(
                                new TagEntity("tag1", "description1", Map.of()),
                                new TagEntity("tag2", "description2", Map.of())))))
                .isTrue();

        // Delete the adapter
        assertThat(protocolAdapterExtractor.deleteAdapter("adapter1")).isTrue();

        // Add another adapter with the same adapter ID and same tags - should succeed
        // because the old tags were removed from the dedup set
        assertThat(protocolAdapterExtractor.addAdapter(new ProtocolAdapterEntity(
                        "adapter1",
                        "protocolId",
                        1,
                        Map.of(),
                        List.of(),
                        List.of(),
                        List.of(
                                new TagEntity("tag1", "new description1", Map.of()),
                                new TagEntity("tag2", "new description2", Map.of())))))
                .isTrue();
    }

    @Test
    public void whenDuplicateTagNameWithinSameAdapterOnAdd_thenAddAdapterReturnsFalse() throws IOException {
        final ConfigFileReaderWriter configFileReader = getConfigFileReaderWriter();
        final ProtocolAdapterExtractor protocolAdapterExtractor = configFileReader.getProtocolAdapterExtractor();

        // Try to add adapter with duplicate tag names within the same adapter
        assertThat(protocolAdapterExtractor.addAdapter(new ProtocolAdapterEntity(
                        "adapter1",
                        "protocolId",
                        1,
                        Map.of(),
                        List.of(),
                        List.of(),
                        List.of(
                                new TagEntity("duplicateTag", "description1", Map.of()),
                                new TagEntity("duplicateTag", "description2", Map.of())))))
                .isFalse();

        assertThat(protocolAdapterExtractor.getAllConfigs()).isEmpty();
    }

    @Test
    public void whenDuplicateTagNameWithinSameAdapterOnUpdate_thenUpdateAdapterReturnsFalse() throws IOException {
        final ConfigFileReaderWriter configFileReader = getConfigFileReaderWriter();
        final ProtocolAdapterExtractor protocolAdapterExtractor = configFileReader.getProtocolAdapterExtractor();

        // Add adapter with unique tags
        assertThat(protocolAdapterExtractor.addAdapter(new ProtocolAdapterEntity(
                        "adapter1",
                        "protocolId",
                        1,
                        Map.of(),
                        List.of(),
                        List.of(),
                        List.of(
                                new TagEntity("tag1", "description1", Map.of()),
                                new TagEntity("tag2", "description2", Map.of())))))
                .isTrue();

        // Try to update with duplicate tag names within the same adapter
        assertThat(protocolAdapterExtractor.updateAdapter(new ProtocolAdapterEntity(
                        "adapter1",
                        "protocolId",
                        1,
                        Map.of(),
                        List.of(),
                        List.of(),
                        List.of(
                                new TagEntity("duplicateTag", "description1", Map.of()),
                                new TagEntity("duplicateTag", "description2", Map.of())))))
                .isFalse();

        // Original adapter should remain unchanged
        assertThat(protocolAdapterExtractor.getAllConfigs()).hasSize(1);
        assertThat(protocolAdapterExtractor.getAllConfigs().getFirst().getTags())
                .hasSize(2);
        assertThat(protocolAdapterExtractor
                        .getAllConfigs()
                        .getFirst()
                        .getTags()
                        .get(0)
                        .getName())
                .isEqualTo("tag1");
    }

    @Test
    public void whenDuplicateTagNameWithinSameAdapterOnUpdateAll_thenUpdateAllAdaptersReturnsError()
            throws IOException {
        final ConfigFileReaderWriter configFileReader = getConfigFileReaderWriter();
        final ProtocolAdapterExtractor protocolAdapterExtractor = configFileReader.getProtocolAdapterExtractor();

        // Try to update all adapters where one adapter has duplicate tag names
        assertThat(protocolAdapterExtractor.updateAllAdapters(List.of(
                        new ProtocolAdapterEntity(
                                "adapter1",
                                "protocolId",
                                1,
                                Map.of(),
                                List.of(),
                                List.of(),
                                List.of(new TagEntity("uniqueTag", "description1", Map.of()))),
                        new ProtocolAdapterEntity(
                                "adapter2",
                                "protocolId",
                                1,
                                Map.of(),
                                List.of(),
                                List.of(),
                                List.of(
                                        new TagEntity("duplicateTag", "description1", Map.of()),
                                        new TagEntity("duplicateTag", "description2", Map.of()))))))
                .isEqualTo(Configurator.ConfigResult.ERROR);
    }

    @Test
    public void whenMultipleAdaptersWithMixedTags_thenOperationsWorkCorrectly() throws IOException {
        final ConfigFileReaderWriter configFileReader = getConfigFileReaderWriter();
        final ProtocolAdapterExtractor protocolAdapterExtractor = configFileReader.getProtocolAdapterExtractor();

        // Add adapter1 with tags [tag1, tag2]
        assertThat(protocolAdapterExtractor.addAdapter(new ProtocolAdapterEntity(
                        "adapter1",
                        "protocolId",
                        1,
                        Map.of(),
                        List.of(),
                        List.of(),
                        List.of(new TagEntity("tag1", "desc", Map.of()), new TagEntity("tag2", "desc", Map.of())))))
                .isTrue();

        // Add adapter2 with tags [tag1, tag3] - tag1 is shared, should succeed
        assertThat(protocolAdapterExtractor.addAdapter(new ProtocolAdapterEntity(
                        "adapter2",
                        "protocolId",
                        1,
                        Map.of(),
                        List.of(),
                        List.of(),
                        List.of(new TagEntity("tag1", "desc", Map.of()), new TagEntity("tag3", "desc", Map.of())))))
                .isTrue();

        // Add adapter3 with tags [tag2, tag3] - both shared, should succeed
        assertThat(protocolAdapterExtractor.addAdapter(new ProtocolAdapterEntity(
                        "adapter3",
                        "protocolId",
                        1,
                        Map.of(),
                        List.of(),
                        List.of(),
                        List.of(new TagEntity("tag2", "desc", Map.of()), new TagEntity("tag3", "desc", Map.of())))))
                .isTrue();

        assertThat(protocolAdapterExtractor.getAllConfigs()).hasSize(3);

        // Update adapter1 to have tags [tag1, tag3] - tag3 exists in other adapters, should succeed
        assertThat(protocolAdapterExtractor.updateAdapter(new ProtocolAdapterEntity(
                        "adapter1",
                        "protocolId",
                        1,
                        Map.of(),
                        List.of(),
                        List.of(),
                        List.of(new TagEntity("tag1", "desc", Map.of()), new TagEntity("tag3", "desc", Map.of())))))
                .isTrue();

        // Delete adapter2
        assertThat(protocolAdapterExtractor.deleteAdapter("adapter2")).isTrue();
        assertThat(protocolAdapterExtractor.getAllConfigs()).hasSize(2);

        // Re-add adapter2 with all shared tags - should succeed
        assertThat(protocolAdapterExtractor.addAdapter(new ProtocolAdapterEntity(
                        "adapter2",
                        "protocolId",
                        1,
                        Map.of(),
                        List.of(),
                        List.of(),
                        List.of(
                                new TagEntity("tag1", "desc", Map.of()),
                                new TagEntity("tag2", "desc", Map.of()),
                                new TagEntity("tag3", "desc", Map.of())))))
                .isTrue();

        assertThat(protocolAdapterExtractor.getAllConfigs()).hasSize(3);
    }

    @Test
    public void whenConfigFileHasSameTagNameInDifferentAdapters_thenApplyConfigSucceeds() throws IOException {
        final ConfigFileReaderWriter configFileReader = getConfigFileReaderWriter("""
                <hivemq>
                  <protocol-adapters>
                    <protocol-adapter>
                      <adapterId>adapter1</adapterId>
                      <protocolId>simulation</protocolId>
                      <configVersion>1</configVersion>
                      <config>
                        <pollingIntervalMillis>1000</pollingIntervalMillis>
                      </config>
                      <tags>
                        <tag>
                          <name>sharedTag</name>
                          <description>Shared tag in adapter1</description>
                        </tag>
                      </tags>
                    </protocol-adapter>
                    <protocol-adapter>
                      <adapterId>adapter2</adapterId>
                      <protocolId>simulation</protocolId>
                      <configVersion>1</configVersion>
                      <config>
                        <pollingIntervalMillis>1000</pollingIntervalMillis>
                      </config>
                      <tags>
                        <tag>
                          <name>sharedTag</name>
                          <description>Shared tag in adapter2</description>
                        </tag>
                      </tags>
                    </protocol-adapter>
                  </protocol-adapters>
                </hivemq>
                """);

        assertThat(configFileReader.applyConfig()).isNotNull();
        final ProtocolAdapterExtractor protocolAdapterExtractor = configFileReader.getProtocolAdapterExtractor();
        assertThat(protocolAdapterExtractor.getAllConfigs()).hasSize(2);

        // Verify both adapters have the same tag name
        assertThat(protocolAdapterExtractor.getAdapterByAdapterId("adapter1"))
                .isPresent()
                .hasValueSatisfying(adapter ->
                        assertThat(adapter.getTags().getFirst().getName()).isEqualTo("sharedTag"));
        assertThat(protocolAdapterExtractor.getAdapterByAdapterId("adapter2"))
                .isPresent()
                .hasValueSatisfying(adapter ->
                        assertThat(adapter.getTags().getFirst().getName()).isEqualTo("sharedTag"));
    }

    @Test
    public void whenConfigFileHasDuplicateTagNameWithinSameAdapter_thenApplyConfigFails() throws IOException {
        final ConfigFileReaderWriter configFileReader = getConfigFileReaderWriter("""
                <hivemq>
                  <protocol-adapters>
                    <protocol-adapter>
                      <adapterId>adapter1</adapterId>
                      <protocolId>simulation</protocolId>
                      <configVersion>1</configVersion>
                      <config>
                        <pollingIntervalMillis>1000</pollingIntervalMillis>
                      </config>
                      <tags>
                        <tag>
                          <name>duplicateTag</name>
                          <description>First occurrence</description>
                        </tag>
                        <tag>
                          <name>duplicateTag</name>
                          <description>Second occurrence</description>
                        </tag>
                      </tags>
                    </protocol-adapter>
                  </protocol-adapters>
                </hivemq>
                """);

        assertThatThrownBy(configFileReader::applyConfig).isInstanceOf(UnrecoverableException.class);
    }
}
