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
package com.hivemq.protocols;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.adapter.sdk.api.config.ProtocolSpecificAdapterConfig;
import com.hivemq.adapter.sdk.api.factories.ProtocolAdapterFactory;
import com.hivemq.adapter.sdk.api.tag.TagDefinition;
import com.hivemq.configuration.entity.adapter.ProtocolAdapterEntity;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

/**
 * The converter's own mapper, which is the one an operator's configuration file goes through.
 *
 * <p>Worth pinning separately from {@link ProtocolAdapterUtilsTest}: the converter holds the
 * application-wide mapper directly rather than the one {@code createProtocolAdapterMapper} returns, so a
 * handler wired only into the latter never runs on this path. That distinction is invisible from a unit
 * test of the helper alone — it was fixed once in the wrong place and the helper's tests stayed green.
 */
class ProtocolAdapterConfigConverterTest {

    @Test
    void unknownSettingsInAConfigurationFile_areReportedRatherThanDiscardedInSilence() {
        // The application-wide mapper disables FAIL_ON_UNKNOWN_PROPERTIES, so this is the configuration
        // the product actually runs with.
        final ObjectMapper applicationMapper =
                new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        final ProtocolAdapterFactory<?> factory = mock(ProtocolAdapterFactory.class);
        final ProtocolAdapterFactoryManager manager = mock(ProtocolAdapterFactoryManager.class);
        when(manager.get("test-protocol")).thenReturn(Optional.of(factory));

        final ProtocolAdapterConfigConverter converter = new ProtocolAdapterConfigConverter(manager, applicationMapper);

        final ProtocolAdapterEntity entity = mock(ProtocolAdapterEntity.class);
        when(entity.getAdapterId()).thenReturn("adapter-1");
        when(entity.getProtocolId()).thenReturn("test-protocol");
        when(entity.getConfig()).thenReturn(Map.of("hostame", "NONE"));
        when(entity.getTags()).thenReturn(List.of());
        when(entity.getNorthboundMappings()).thenReturn(List.of());
        when(entity.getSouthboundMappings()).thenReturn(List.of());

        // The factory is what turns the raw map into a config object; drive the converter's mapper
        // through it exactly as a real factory does.
        when(factory.convertConfigObject(
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.anyBoolean()))
                .thenAnswer(invocation -> {
                    final ObjectMapper mapper = invocation.getArgument(0);
                    final Map<String, Object> config = invocation.getArgument(1);
                    return mapper.convertValue(config, ExampleAdapterConfig.class);
                });

        final List<ILoggingEvent> events = whileCapturing(() -> converter.fromEntity(entity));

        assertThat(events)
                .as("the mapper handed to the factory must be the reporting one")
                .anySatisfy(event -> {
                    assertThat(event.getLevel()).isEqualTo(Level.WARN);
                    assertThat(event.getFormattedMessage())
                            .contains("'hostame'")
                            .contains("has been ignored");
                });
    }

    @Test
    void theConvertedConfigCarriesTheEntitysOwnAdapterId() {
        // ProtocolAdapterManager.convertConfigs keys its duplicate-id arithmetic on two different
        // sources: the catch branch has only the entity's id, because the converted config is what
        // failed to materialise, while the success branch uses the converted config's id. Reviewers
        // read that as two id spaces. It is one, and this single line in fromEntity is what makes it
        // one - if it ever stops being the entity's id, the duplicate arithmetic develops a hole that
        // nothing else would catch, because both branches would keep looking correct in isolation.
        final ProtocolAdapterFactory<?> factory = mock(ProtocolAdapterFactory.class);
        final ProtocolAdapterFactoryManager manager = mock(ProtocolAdapterFactoryManager.class);
        when(manager.get("test-protocol")).thenReturn(Optional.of(factory));

        final ProtocolAdapterConfigConverter converter =
                new ProtocolAdapterConfigConverter(manager, new ObjectMapper());

        final ProtocolAdapterEntity entity = mock(ProtocolAdapterEntity.class);
        when(entity.getAdapterId()).thenReturn("adapter-1");
        when(entity.getProtocolId()).thenReturn("test-protocol");
        when(entity.getConfig()).thenReturn(Map.of("hostname", "machine-1"));
        when(entity.getTags()).thenReturn(List.of());
        when(entity.getNorthboundMappings()).thenReturn(List.of());
        when(entity.getSouthboundMappings()).thenReturn(List.of());
        when(factory.convertConfigObject(
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.anyBoolean()))
                .thenAnswer(invocation -> {
                    final ObjectMapper mapper = invocation.getArgument(0);
                    final Map<String, Object> config = invocation.getArgument(1);
                    return mapper.convertValue(config, ExampleAdapterConfig.class);
                });

        final ProtocolAdapterConfig converted = converter.fromEntity(entity);

        assertThat(converted.getAdapterId()).isEqualTo(entity.getAdapterId());
    }

    @Test
    void emptyOptionalTagFields_areLeftOutOfTheJsonTheFrontendReceives() {
        // The frontend validates a tag against its schema, where an optional field is typed as string,
        // integer or enum. An explicit null fails that check ("must be string") and blocks saving the
        // tag, so an unset field must reach the frontend as an absent key. OPC UA, HTTP and CIP tags all
        // carry such fields, which is why this is pinned on the shared conversion, not on one adapter.
        final ProtocolAdapterConfigConverter converter =
                new ProtocolAdapterConfigConverter(mock(ProtocolAdapterFactoryManager.class), new ObjectMapper());

        final JsonNode json = converter.convertTagDefinitionToJsonNode(
                new ExampleTagDefinition("ns=1;i=1004", null, null, null, Map.of("unit", "rpm")));

        assertThat(json.has("node")).isTrue();
        assertThat(json.has("notifierNode")).as("unset string").isFalse();
        assertThat(json.has("filterType")).as("unset enum").isFalse();
        assertThat(json.has("bitIndex")).as("unset integer").isFalse();
        assertThat(json.get("labels").get("unit").asText()).isEqualTo("rpm");
    }

    @Test
    void aTagDefinitionWithoutItsEmptyFields_readsBackToTheSameDefinition() {
        // Leaving a key out must mean exactly what the null meant, or a tag would change on a
        // read/write cycle through the UI.
        final ProtocolAdapterConfigConverter converter =
                new ProtocolAdapterConfigConverter(mock(ProtocolAdapterFactoryManager.class), new ObjectMapper());
        final ExampleTagDefinition original = new ExampleTagDefinition("ns=1;i=1004", null, null, 3, Map.of());

        final ExampleTagDefinition readBack = new ObjectMapper()
                .convertValue(converter.convertTagDefinitionToJsonNode(original), ExampleTagDefinition.class);

        assertThat(readBack).usingRecursiveComparison().isEqualTo(original);
    }

    enum ExampleFilterType {
        ALARM,
        EVENT
    }

    static class ExampleTagDefinition implements TagDefinition {
        public @NotNull String node;
        public @Nullable String notifierNode;
        public @Nullable ExampleFilterType filterType;
        public @Nullable Integer bitIndex;
        public @Nullable Map<String, String> labels;

        @SuppressWarnings("unused") // Jackson
        ExampleTagDefinition() {
            this.node = "";
        }

        ExampleTagDefinition(
                final @NotNull String node,
                final @Nullable String notifierNode,
                final @Nullable ExampleFilterType filterType,
                final @Nullable Integer bitIndex,
                final @Nullable Map<String, String> labels) {
            this.node = node;
            this.notifierNode = notifierNode;
            this.filterType = filterType;
            this.bitIndex = bitIndex;
            this.labels = labels;
        }
    }

    static class ExampleAdapterConfig implements ProtocolSpecificAdapterConfig {
        public @org.jetbrains.annotations.Nullable String hostname;
    }

    private static @NotNull List<ILoggingEvent> whileCapturing(final @NotNull Runnable body) {
        final Logger logger = (Logger) LoggerFactory();
        final ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            body.run();
            return List.copyOf(appender.list);
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    private static @NotNull org.slf4j.Logger LoggerFactory() {
        return org.slf4j.LoggerFactory.getLogger(ProtocolAdapterUtils.class);
    }
}
