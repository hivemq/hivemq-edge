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

import com.hivemq.adapter.sdk.api.config.MessageHandlingOptions;
import com.hivemq.configuration.entity.HiveMQConfigEntity;
import com.hivemq.configuration.entity.adapter.NorthboundMappingEntity;
import com.hivemq.configuration.entity.adapter.ProtocolAdapterEntity;
import com.hivemq.configuration.entity.adapter.SouthboundMappingEntity;
import com.hivemq.configuration.entity.adapter.TagEntity;
import com.hivemq.configuration.entity.adapter.fieldmapping.FieldMappingEntity;
import com.hivemq.exceptions.UnrecoverableException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
        return new ConfigFileReaderWriter(new ConfigurationFile(tempFile), List.of());
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

    @ParameterizedTest
    @CsvSource({"true,0", "true,1", "true,2", "false,-1", "false,3", "false,abc"})
    public void whenNorthboundMappingQoSIsProvided_thenApplyConfigShouldWorkAsExpected(
            final boolean valid,
            final @NotNull String maxQoS)
            throws IOException {
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
                """.replace("${maxQoS}", maxQoS));
        if (valid) {
            assertThat(configFileReader.applyConfig()).isNotNull();
        } else {
            assertThatThrownBy(configFileReader::applyConfig).isInstanceOf(UnrecoverableException.class);
        }
    }

    @ParameterizedTest
    @CsvSource({"true,MQTTMessagePerTag", "true,MQTTMessagePerSubscription", "false,abc"})
    public void whenNorthboundMappingMessageHandlingOptionsIsProvided_thenApplyConfigShouldWorkAsExpected(
            final boolean valid,
            final @NotNull String messageHandlingOptions)
            throws IOException {
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
                          <messageHandlingOptions>${messageHandlingOptions}</messageHandlingOptions>
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
                """.replace("${messageHandlingOptions}", messageHandlingOptions));
        if (valid) {
            assertThat(configFileReader.applyConfig()).isNotNull();
        } else {
            assertThatThrownBy(configFileReader::applyConfig).isInstanceOf(UnrecoverableException.class);
        }
    }

    @ParameterizedTest
    @CsvSource({"true,true", "true,false", "false,abc"})
    public void whenNorthboundMappingIncludeTagNamesIsProvided_thenApplyConfigShouldWorkAsExpected(
            final boolean valid,
            final @NotNull String includeTagNames)
            throws IOException {
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
                """.replace("${includeTagNames}", includeTagNames));
        if (valid) {
            assertThat(configFileReader.applyConfig()).isNotNull();
        } else {
            assertThatThrownBy(configFileReader::applyConfig).isInstanceOf(UnrecoverableException.class);
        }
    }

    @ParameterizedTest
    @CsvSource({"true,true", "true,false", "false,abc"})
    public void whenNorthboundMappingIncludeTimestampIsProvided_thenApplyConfigShouldWorkAsExpected(
            final boolean valid,
            final @NotNull String includeTimestamp)
            throws IOException {
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
                """.replace("${includeTimestamp}", includeTimestamp));
        if (valid) {
            assertThat(configFileReader.applyConfig()).isNotNull();
        } else {
            assertThatThrownBy(configFileReader::applyConfig).isInstanceOf(UnrecoverableException.class);
        }
    }

    @ParameterizedTest
    @CsvSource({"true,1", "true,123", "false,0", "false,-1", "false,abc"})
    public void whenNorthboundMappingMessageExpiryIntervalIsProvided_thenApplyConfigShouldWorkAsExpected(
            final boolean valid,
            final @NotNull String messageExpiryInterval)
            throws IOException {
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
                """.replace("${messageExpiryInterval}", messageExpiryInterval));
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
        assertThat(configFileReader.setConfiguration(entity)).isTrue();
    }

    @Test
    public void whenNoMappings_setConfigurationShouldReturnTrue() throws IOException {
        final ConfigFileReaderWriter configFileReader = getConfigFileReaderWriter();
        final HiveMQConfigEntity entity = configFileReader.applyConfig();
        assertThat(entity).isNotNull();
        final ProtocolAdapterEntity protocolAdapterEntity = new ProtocolAdapterEntity("adapterId",
                "protocolId",
                1,
                Map.of(),
                List.of(),
                List.of(),
                List.of(new TagEntity("abc", "def", Map.of())));
        entity.getProtocolAdapterConfig().add(protocolAdapterEntity);
        assertThat(configFileReader.setConfiguration(entity)).isTrue();
    }

    @Test
    public void whenNoTags_setConfigurationShouldReturnFalse() throws IOException {
        final ConfigFileReaderWriter configFileReader = getConfigFileReaderWriter();
        final HiveMQConfigEntity entity = configFileReader.applyConfig();
        assertThat(entity).isNotNull();
        final NorthboundMappingEntity northboundMappingEntity = new NorthboundMappingEntity("tagName",
                "topic",
                1,
                MessageHandlingOptions.MQTTMessagePerTag,
                false,
                true,
                List.of(),
                100);
        final ProtocolAdapterEntity protocolAdapterEntity = new ProtocolAdapterEntity("adapterId",
                "protocolId",
                1,
                Map.of(),
                List.of(northboundMappingEntity),
                List.of(),
                List.of());
        entity.getProtocolAdapterConfig().add(protocolAdapterEntity);
        assertThat(configFileReader.setConfiguration(entity)).isFalse();
    }

    @Test
    public void whenNorthboundMappingTagNameAreNotFound_setConfigurationShouldReturnFalse() throws IOException {
        final ConfigFileReaderWriter configFileReader = getConfigFileReaderWriter();
        final HiveMQConfigEntity entity = configFileReader.applyConfig();
        assertThat(entity).isNotNull();
        final NorthboundMappingEntity northboundMappingEntity = new NorthboundMappingEntity("tagName",
                "topic",
                1,
                MessageHandlingOptions.MQTTMessagePerTag,
                false,
                true,
                List.of(),
                100);
        final ProtocolAdapterEntity protocolAdapterEntity = new ProtocolAdapterEntity("adapterId",
                "protocolId",
                1,
                Map.of(),
                List.of(northboundMappingEntity),
                List.of(),
                List.of(new TagEntity("abc", "def", Map.of())));
        entity.getProtocolAdapterConfig().add(protocolAdapterEntity);
        assertThat(configFileReader.setConfiguration(entity)).isFalse();
    }

    @ParameterizedTest
    @CsvSource({",topic", "tagName,"})
    public void whenNorthboundMappingTagNameOrTopicIsEmpty_setConfigurationShouldReturnFalse(
            final @NotNull String tagName,
            final @NotNull String topic)
            throws IOException {
        final ConfigFileReaderWriter configFileReader = getConfigFileReaderWriter();
        final HiveMQConfigEntity entity = configFileReader.applyConfig();
        assertThat(entity).isNotNull();
        final NorthboundMappingEntity northboundMappingEntity = new NorthboundMappingEntity(tagName,
                topic,
                1,
                MessageHandlingOptions.MQTTMessagePerTag,
                false,
                true,
                List.of(),
                100);
        final ProtocolAdapterEntity protocolAdapterEntity = new ProtocolAdapterEntity("adapterId",
                "protocolId",
                1,
                Map.of(),
                List.of(northboundMappingEntity),
                List.of(),
                List.of());
        entity.getProtocolAdapterConfig().add(protocolAdapterEntity);
        assertThat(configFileReader.setConfiguration(entity)).isFalse();
    }

    @Test
    public void whenSouthboundMappingTagNameIsNotFound_setConfigurationShouldReturnFalse() throws IOException {
        final ConfigFileReaderWriter configFileReader = getConfigFileReaderWriter();
        final HiveMQConfigEntity entity = configFileReader.applyConfig();
        assertThat(entity).isNotNull();
        final SouthboundMappingEntity southboundMappingEntity =
                new SouthboundMappingEntity("tagName", "topicFilter", new FieldMappingEntity(), "schema");
        final ProtocolAdapterEntity protocolAdapterEntity = new ProtocolAdapterEntity("adapterId",
                "protocolId",
                1,
                Map.of(),
                List.of(),
                List.of(southboundMappingEntity),
                List.of(new TagEntity("abc", "def", Map.of())));
        entity.getProtocolAdapterConfig().add(protocolAdapterEntity);
        assertThat(configFileReader.setConfiguration(entity)).isFalse();
    }

    @ParameterizedTest
    @CsvSource({",topicFilter,schema", "tagName,,schema", "tagName,topicFilter,"})
    public void whenSouthboundMappingTagNameOrTopicFilterOrSchemaIsEmpty_setConfigurationShouldReturnFalse(
            final @NotNull String tagName,
            final @NotNull String topicFilter,
            final @NotNull String schema)
            throws IOException {
        final ConfigFileReaderWriter configFileReader = getConfigFileReaderWriter();
        final HiveMQConfigEntity entity = configFileReader.applyConfig();
        assertThat(entity).isNotNull();
        final SouthboundMappingEntity southboundMappingEntity =
                new SouthboundMappingEntity(tagName, topicFilter, new FieldMappingEntity(), schema);
        final ProtocolAdapterEntity protocolAdapterEntity = new ProtocolAdapterEntity("adapterId",
                "protocolId",
                1,
                Map.of(),
                List.of(),
                List.of(southboundMappingEntity),
                List.of());
        entity.getProtocolAdapterConfig().add(protocolAdapterEntity);
        assertThat(configFileReader.setConfiguration(entity)).isFalse();
    }
}
