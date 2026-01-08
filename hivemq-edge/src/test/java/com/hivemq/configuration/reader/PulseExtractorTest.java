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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.hivemq.configuration.entity.HiveMQConfigEntity;
import com.hivemq.configuration.entity.pulse.PulseAssetEntity;
import com.hivemq.configuration.entity.pulse.PulseAssetMappingEntity;
import com.hivemq.configuration.entity.pulse.PulseAssetMappingStatus;
import com.hivemq.configuration.entity.pulse.PulseAssetsEntity;
import com.hivemq.configuration.info.SystemInformation;
import com.hivemq.exceptions.UnrecoverableException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import util.LogbackCapturingAppender;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
public class PulseExtractorTest {
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

    @AfterEach
    public void tearDown() throws Exception {
        LogbackCapturingAppender.Factory.cleanUp();
    }

    protected @NotNull ConfigFileReaderWriter getConfigFileReaderWriter() throws IOException {
        return getConfigFileReaderWriter(null);
    }

    protected @NotNull ConfigFileReaderWriter getConfigFileReaderWriter(@Nullable String xmlString) throws IOException {
        if (xmlString == null) {
            xmlString = "<hivemq></hivemq>";
        }
        final File tempFile = new File(tempDir, "conf.xml");
        Files.writeString(tempFile.toPath(), xmlString);
        return new ConfigFileReaderWriter(systemInformation, new ConfigurationFile(tempFile), List.of());
    }

    @Test
    public void whenPulseIsAbsent_thenApplyConfigPasses() throws IOException {
        final ConfigFileReaderWriter configFileReader = getConfigFileReaderWriter();
        final HiveMQConfigEntity configEntity = configFileReader.applyConfig();
        extractPulseAssetEntities(configEntity, 0);
    }

    @Test
    public void whenPulseExistsButManagedAssetsIsAbsent_thenApplyConfigFails() throws IOException {
        final ConfigFileReaderWriter configFileReader = getConfigFileReaderWriter("""
                <hivemq>
                    <pulse/>
                </hivemq>
                """);
        assertThatThrownBy(configFileReader::applyConfig).isInstanceOf(UnrecoverableException.class);
    }

    @Test
    public void whenPulseExistsAndManagedAssetsIsEmpty_thenApplyConfigPasses() throws IOException {
        final ConfigFileReaderWriter configFileReader = getConfigFileReaderWriter("""
                <hivemq>
                    <pulse>
                        <managed-assets/>
                    </pulse>
                </hivemq>
                """);
        final HiveMQConfigEntity configEntity = configFileReader.applyConfig();
        extractPulseAssetEntities(configEntity, 0);
    }

    @Test
    public void whenPulseExistsAndManagedAssetsIsEmpty_thenSetConfigurationPasses() throws IOException {
        final ConfigFileReaderWriter configFileReader = getConfigFileReaderWriter();
        final HiveMQConfigEntity configEntity = configFileReader.applyConfig();
        configEntity.getPulseEntity().setPulseAssetsEntity(new PulseAssetsEntity());
        assertThat(configFileReader.internalApplyConfig(configEntity)).isTrue();
    }

    @Test
    public void whenAllElementsAreCorrect_thenApplyConfigPasses() throws IOException {
        final ConfigFileReaderWriter configFileReader = getConfigFileReaderWriter("""
                <hivemq>
                    <pulse>
                        <managed-assets>
                            <managed-asset id="123e4567-e89b-12d3-a456-426614174000" name="asset1" topic="topic1">
                                <schema>{}</schema>
                                <mapping status="UNMAPPED"/>
                            </managed-asset>
                            <managed-asset id="123e4567-e89b-12d3-a456-426614174001" name="asset2" topic="topic2">
                                <schema>{}</schema>
                                <mapping id="123e4567-e89b-12d3-a456-426614174001" status="STREAMING"/>
                            </managed-asset>
                        </managed-assets>
                    </pulse>
                </hivemq>
                """);
        final HiveMQConfigEntity configEntity = configFileReader.applyConfig();
        final List<PulseAssetEntity> entities = extractPulseAssetEntities(configEntity, 2);
        {
            final PulseAssetEntity entity = entities.get(0);
            assertThat(entity.getId()).isEqualTo(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));
            assertThat(entity.getName()).isEqualTo("asset1");
            assertThat(entity.getTopic()).isEqualTo("topic1");
            assertThat(entity.getSchema()).isEqualTo("{}");
            assertThat(entity.getMapping()).isNotNull();
            assertThat(entity.getMapping().getId()).isNull();
            assertThat(entity.getMapping().getStatus()).isEqualTo(PulseAssetMappingStatus.UNMAPPED);
        }
        {
            final PulseAssetEntity entity = entities.get(1);
            assertThat(entity.getId()).isEqualTo(UUID.fromString("123e4567-e89b-12d3-a456-426614174001"));
            assertThat(entity.getName()).isEqualTo("asset2");
            assertThat(entity.getTopic()).isEqualTo("topic2");
            assertThat(entity.getSchema()).isEqualTo("{}");
            assertThat(entity.getMapping()).isNotNull();
            assertThat(entity.getMapping().getId()).isEqualTo(UUID.fromString("123e4567-e89b-12d3-a456-426614174001"));
            assertThat(entity.getMapping().getStatus()).isEqualTo(PulseAssetMappingStatus.STREAMING);
        }
    }

    @Test
    public void whenAllElementsAreCorrect_thenSetConfigurationPasses() throws IOException {
        final ConfigFileReaderWriter configFileReader = getConfigFileReaderWriter();
        final HiveMQConfigEntity configEntity = configFileReader.applyConfig();
        configEntity.getPulseEntity()
                .setPulseAssetsEntity(PulseAssetsEntity.builder()
                        .addPulseAssetEntities(List.of(PulseAssetEntity.builder()
                                        .id(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"))
                                        .name("asset1")
                                        .description("description1")
                                        .topic("topic1")
                                        .schema("{}")
                                        .mapping(PulseAssetMappingEntity.builder()
                                                .id(null)
                                                .status(PulseAssetMappingStatus.UNMAPPED)
                                                .build())
                                        .build(),
                                PulseAssetEntity.builder()
                                        .id(UUID.fromString("123e4567-e89b-12d3-a456-426614174001"))
                                        .name("asset2")
                                        .description("description2")
                                        .topic("topic2")
                                        .schema("{}")
                                        .mapping(PulseAssetMappingEntity.builder()
                                                .id(UUID.fromString("123e4567-e89b-12d3-a456-426614174001"))
                                                .status(PulseAssetMappingStatus.UNMAPPED)
                                                .build())
                                        .build()))
                        .build());
        assertThat(configFileReader.internalApplyConfig(configEntity)).isTrue();
    }

    @Test
    public void whenIdIsMissing_thenApplyConfigFails() throws IOException {
        final ConfigFileReaderWriter configFileReader = getConfigFileReaderWriter("""
                <hivemq>
                    <pulse>
                        <managed-assets>
                            <managed-asset name="asset1" topic="topic1">
                                <schema>{}</schema>
                                <mapping status="UNMAPPED"/>
                            </managed-asset>
                        </managed-assets>
                    </pulse>
                </hivemq>
                """);
        assertThatThrownBy(configFileReader::applyConfig).isInstanceOf(UnrecoverableException.class);
        assertThat(logCapture.isLogCaptured()).isTrue();
        assertThat(logCapture.getLastCapturedLog().getLevel()).isEqualTo(Level.ERROR);
        assertThat(logCapture.getLastCapturedLog().getFormattedMessage()).contains(
                "Attribute 'id' must appear on element 'managed-asset'.");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "abc", "   "})
    public void whenIdIsInvalid_thenApplyConfigFails(final @NotNull String idString) throws IOException {
        final ConfigFileReaderWriter configFileReader = getConfigFileReaderWriter("""
                <hivemq>
                    <pulse>
                        <managed-assets>
                            <managed-asset id="%s" name="asset1" topic="topic1">
                                <schema>{}</schema>
                                <mapping status="UNMAPPED"/>
                            </managed-asset>
                        </managed-assets>
                    </pulse>
                </hivemq>
                """.formatted(idString));
        assertThatThrownBy(configFileReader::applyConfig).isInstanceOf(UnrecoverableException.class);
        assertThat(logCapture.isLogCaptured()).isTrue();
        assertThat(logCapture.getLastCapturedLog().getLevel()).isEqualTo(Level.ERROR);
        assertThat(logCapture.getLastCapturedLog().getFormattedMessage()).contains(
                "Value '%s' is not facet-valid with respect to pattern".formatted(idString));
        assertThat(logCapture.getLastCapturedLog().getFormattedMessage()).contains(
                "The value '%s' of attribute 'id' on element 'managed-asset' is not valid with respect to its type, 'uuidType'.".formatted(
                        idString));
        assertThat(logCapture.getLastCapturedLog().getFormattedMessage()).contains("Invalid UUID string");
    }

    @Test
    public void whenNameIsMissing_thenApplyConfigFails() throws IOException {
        final ConfigFileReaderWriter configFileReader = getConfigFileReaderWriter("""
                <hivemq>
                    <pulse>
                        <managed-assets>
                            <managed-asset id="123e4567-e89b-12d3-a456-426614174000" topic="topic1">
                                <schema>{}</schema>
                                <mapping status="UNMAPPED"/>
                            </managed-asset>
                        </managed-assets>
                    </pulse>
                </hivemq>
                """);
        assertThatThrownBy(configFileReader::applyConfig).isInstanceOf(UnrecoverableException.class);
        assertThat(logCapture.isLogCaptured()).isTrue();
        assertThat(logCapture.getLastCapturedLog().getLevel()).isEqualTo(Level.ERROR);
        assertThat(logCapture.getLastCapturedLog().getFormattedMessage()).contains(
                "Attribute 'name' must appear on element 'managed-asset'.");
    }

    @Test
    public void whenNameIsEmpty_thenApplyConfigFails() throws IOException {
        final ConfigFileReaderWriter configFileReader = getConfigFileReaderWriter("""
                <hivemq>
                    <pulse>
                        <managed-assets>
                            <managed-asset id="123e4567-e89b-12d3-a456-426614174000" name="" topic="topic1">
                                <schema>{}</schema>
                                <mapping status="UNMAPPED"/>
                            </managed-asset>
                        </managed-assets>
                    </pulse>
                </hivemq>
                """);
        assertThatThrownBy(configFileReader::applyConfig).isInstanceOf(UnrecoverableException.class);
        assertThat(logCapture.isLogCaptured()).isTrue();
        assertThat(logCapture.getLastCapturedLog().getLevel()).isEqualTo(Level.ERROR);
        assertThat(logCapture.getCapturedLogs()
                .stream()
                .map(ILoggingEvent::getFormattedMessage)
                .collect(Collectors.joining("\n"))).contains("Pulse config error: name is missing");
    }

    @Test
    public void whenTopicIsMissing_thenApplyConfigFails() throws IOException {
        final ConfigFileReaderWriter configFileReader = getConfigFileReaderWriter("""
                <hivemq>
                    <pulse>
                        <managed-assets>
                            <managed-asset id="123e4567-e89b-12d3-a456-426614174000" name="asset1">
                                <schema>{}</schema>
                                <mapping status="UNMAPPED"/>
                            </managed-asset>
                        </managed-assets>
                    </pulse>
                </hivemq>
                """);
        assertThatThrownBy(configFileReader::applyConfig).isInstanceOf(UnrecoverableException.class);
        assertThat(logCapture.isLogCaptured()).isTrue();
        assertThat(logCapture.getLastCapturedLog().getLevel()).isEqualTo(Level.ERROR);
        assertThat(logCapture.getLastCapturedLog().getFormattedMessage()).contains(
                "Attribute 'topic' must appear on element 'managed-asset'.");
    }

    @Test
    public void whenTopicIsEmpty_thenApplyConfigFails() throws IOException {
        final ConfigFileReaderWriter configFileReader = getConfigFileReaderWriter("""
                <hivemq>
                    <pulse>
                        <managed-assets>
                            <managed-asset id="123e4567-e89b-12d3-a456-426614174000" name="asset1" topic="">
                                <schema>{}</schema>
                                <mapping status="UNMAPPED"/>
                            </managed-asset>
                        </managed-assets>
                    </pulse>
                </hivemq>
                """);
        assertThatThrownBy(configFileReader::applyConfig).isInstanceOf(UnrecoverableException.class);
        assertThat(logCapture.isLogCaptured()).isTrue();
        assertThat(logCapture.getLastCapturedLog().getLevel()).isEqualTo(Level.ERROR);
        assertThat(logCapture.getCapturedLogs()
                .stream()
                .map(ILoggingEvent::getFormattedMessage)
                .collect(Collectors.joining("\n"))).contains("Pulse config error: topic is missing");
    }

    @Test
    public void whenSchemaIsMissing_thenApplyConfigFails() throws IOException {
        final ConfigFileReaderWriter configFileReader = getConfigFileReaderWriter("""
                <hivemq>
                    <pulse>
                        <managed-assets>
                            <managed-asset id="123e4567-e89b-12d3-a456-426614174000" name="asset1" topic="topic1">
                                <mapping status="UNMAPPED"/>
                            </managed-asset>
                        </managed-assets>
                    </pulse>
                </hivemq>
                """);
        assertThatThrownBy(configFileReader::applyConfig).isInstanceOf(UnrecoverableException.class);
        assertThat(logCapture.isLogCaptured()).isTrue();
        assertThat(logCapture.getLastCapturedLog().getLevel()).isEqualTo(Level.ERROR);
        // With xs:all content model, the error message format differs from xs:sequence
        assertThat(logCapture.getLastCapturedLog().getFormattedMessage()).contains("One of '{schema}' is expected.");
    }

    @Test
    public void whenSchemaIsEmpty_thenApplyConfigFails() throws IOException {
        final ConfigFileReaderWriter configFileReader = getConfigFileReaderWriter("""
                <hivemq>
                    <pulse>
                        <managed-assets>
                            <managed-asset id="123e4567-e89b-12d3-a456-426614174000" name="asset1" topic="topic1">
                                <schema></schema>
                                <mapping status="UNMAPPED"/>
                            </managed-asset>
                        </managed-assets>
                    </pulse>
                </hivemq>
                """);
        assertThatThrownBy(configFileReader::applyConfig).isInstanceOf(UnrecoverableException.class);
        assertThat(logCapture.isLogCaptured()).isTrue();
        assertThat(logCapture.getLastCapturedLog().getLevel()).isEqualTo(Level.ERROR);
        assertThat(logCapture.getCapturedLogs()
                .stream()
                .map(ILoggingEvent::getFormattedMessage)
                .collect(Collectors.joining("\n"))).contains("Pulse config error: schema is missing");
    }

    @Test
    public void whenMappingIsMissing_thenApplyConfigFails() throws IOException {
        final ConfigFileReaderWriter configFileReader = getConfigFileReaderWriter("""
                <hivemq>
                    <pulse>
                        <managed-assets>
                            <managed-asset id="123e4567-e89b-12d3-a456-426614174000" name="asset1" topic="topic1">
                                <schema>{}</schema>
                            </managed-asset>
                        </managed-assets>
                    </pulse>
                </hivemq>
                """);
        assertThatThrownBy(configFileReader::applyConfig).isInstanceOf(UnrecoverableException.class);
        assertThat(logCapture.isLogCaptured()).isTrue();
        assertThat(logCapture.getLastCapturedLog().getLevel()).isEqualTo(Level.ERROR);
        assertThat(logCapture.getLastCapturedLog().getFormattedMessage()).contains("One of '{mapping}' is expected.");
    }

    @Test
    public void whenStatusIsMissing_thenApplyConfigFails() throws IOException {
        final ConfigFileReaderWriter configFileReader = getConfigFileReaderWriter("""
                <hivemq>
                    <pulse>
                        <managed-assets>
                            <managed-asset id="123e4567-e89b-12d3-a456-426614174000" name="asset1" topic="topic1">
                                <schema>{}</schema>
                                <mapping/>
                            </managed-asset>
                        </managed-assets>
                    </pulse>
                </hivemq>
                """);
        assertThatThrownBy(configFileReader::applyConfig).isInstanceOf(UnrecoverableException.class);
        assertThat(logCapture.isLogCaptured()).isTrue();
        assertThat(logCapture.getLastCapturedLog().getLevel()).isEqualTo(Level.ERROR);
        assertThat(logCapture.getLastCapturedLog().getFormattedMessage()).contains(
                "Attribute 'status' must appear on element 'mapping'.");
    }

    @Test
    public void whenStatusIsInvalid_thenApplyConfigFails() throws IOException {
        final ConfigFileReaderWriter configFileReader = getConfigFileReaderWriter("""
                <hivemq>
                    <pulse>
                        <managed-assets>
                            <managed-asset id="123e4567-e89b-12d3-a456-426614174000" name="asset1" topic="topic1">
                                <schema>{}</schema>
                                <mapping status="INVALID"/>
                            </managed-asset>
                        </managed-assets>
                    </pulse>
                </hivemq>
                """);
        assertThatThrownBy(configFileReader::applyConfig).isInstanceOf(UnrecoverableException.class);
        assertThat(logCapture.isLogCaptured()).isTrue();
        assertThat(logCapture.getLastCapturedLog().getLevel()).isEqualTo(Level.ERROR);
        assertThat(logCapture.getLastCapturedLog().getFormattedMessage()).contains(
                "Value 'INVALID' is not facet-valid with respect to enumeration '[DRAFT, MISSING, REQUIRES_REMAPPING, STREAMING, UNMAPPED]'. It must be a value from the enumeration.");
        // The type name is generated from the XSD, which uses a named type 'pulseAssetMappingStatus'
        assertThat(logCapture.getLastCapturedLog().getFormattedMessage()).contains(
                "The value 'INVALID' of attribute 'status' on element 'mapping' is not valid with respect to its type, 'pulseAssetMappingStatus'.");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "abc", "   "})
    public void whenMappingIdIsInvalid_thenApplyConfigFails(final @NotNull String idString) throws IOException {
        final ConfigFileReaderWriter configFileReader = getConfigFileReaderWriter("""
                <hivemq>
                    <pulse>
                        <managed-assets>
                            <managed-asset id="123e4567-e89b-12d3-a456-426614174000" name="asset1" topic="topic1">
                                <schema>{}</schema>
                                <mapping id="%s" status="UNMAPPED"/>
                            </managed-asset>
                        </managed-assets>
                    </pulse>
                </hivemq>
                """.formatted(idString));
        assertThatThrownBy(configFileReader::applyConfig).isInstanceOf(UnrecoverableException.class);
        assertThat(logCapture.isLogCaptured()).isTrue();
        assertThat(logCapture.getLastCapturedLog().getLevel()).isEqualTo(Level.ERROR);
        assertThat(logCapture.getLastCapturedLog().getFormattedMessage()).contains(
                "Value '%s' is not facet-valid with respect to pattern".formatted(idString));
        assertThat(logCapture.getLastCapturedLog().getFormattedMessage()).contains(
                "The value '%s' of attribute 'id' on element 'mapping' is not valid with respect to its type, 'uuidType'.".formatted(
                        idString));
        assertThat(logCapture.getLastCapturedLog().getFormattedMessage()).contains("Invalid UUID string");
    }

    protected @NotNull List<PulseAssetEntity> extractPulseAssetEntities(
            final @NotNull HiveMQConfigEntity configEntity,
            final int expectedSize) {
        assertThat(configEntity).isNotNull();
        assertThat(configEntity.getPulseEntity()).isNotNull();
        assertThat(configEntity.getPulseEntity().getPulseAssetsEntity()).isNotNull();
        assertThat(configEntity.getPulseEntity().getPulseAssetsEntity().getPulseAssetEntities()).hasSize(expectedSize);
        return configEntity.getPulseEntity().getPulseAssetsEntity().getPulseAssetEntities();
    }
}
