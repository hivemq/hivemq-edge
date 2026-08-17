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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.function.Supplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;

/**
 * How an adapter configuration handles a setting it does not know.
 *
 * <p>A misspelled element used to be discarded in complete silence — the operator wrote a setting, the
 * adapter started without it, and nothing said so. The adapter configs already refuse to do that for a
 * misspelled enum <em>value</em>; this closes the same gap for the name around it.
 */
class ProtocolAdapterUtilsTest {

    record ExampleConfig(
            @JsonProperty("hostname") @Nullable String hostname,
            @JsonProperty("port") @Nullable Integer port) {}

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void anUnknownSetting_isIgnoredAndReported(final boolean failOnUnknown) throws Exception {
        // Run both ways on purpose. The application-wide mapper disables FAIL_ON_UNKNOWN_PROPERTIES
        // while a test that builds its own ObjectMapper gets Jackson's default, which is the opposite -
        // so this used to behave differently in tests than in the product. A problem handler is
        // consulted before the feature is examined, which makes the two identical.
        final ObjectMapper base =
                new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, failOnUnknown);
        final ObjectMapper mapper = ProtocolAdapterUtils.createProtocolAdapterMapper(base);

        final List<ILoggingEvent> events =
                whileCapturing(() -> readValue(mapper, "{\"hostame\":\"NONE\",\"port\":4840}"));

        assertThat(events).anySatisfy(event -> {
            assertThat(event.getLevel()).isEqualTo(Level.WARN);
            assertThat(event.getFormattedMessage())
                    .contains("'hostame'")
                    .contains("ExampleConfig")
                    .contains("has been ignored")
                    .contains("hostname");
        });
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void anUnknownSetting_doesNotStopTheRestOfTheConfigurationFromBeingRead(final boolean failOnUnknown) {
        // Configurations written for another version of HiveMQ Edge may legitimately carry settings this
        // one does not know. Refusing to start the adapter over one is a worse outcome than running it
        // without the setting - the operator has been told either way.
        final ObjectMapper mapper = ProtocolAdapterUtils.createProtocolAdapterMapper(
                new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, failOnUnknown));

        final ExampleConfig config = readValue(mapper, "{\"hostame\":\"NONE\",\"port\":4840}");

        assertThat(config.port()).isEqualTo(4840);
        assertThat(config.hostname())
                .as("the misspelled setting is absent, not guessed at")
                .isNull();
    }

    @Test
    void anUnknownObjectSetting_isSkippedWhole() {
        // The value has to be consumed, not just its first token, or the rest of the object is read as
        // though those fields belonged to the parent.
        final ObjectMapper mapper = ProtocolAdapterUtils.createProtocolAdapterMapper(new ObjectMapper());

        final ExampleConfig config =
                readValue(mapper, "{\"extra\":{\"a\":1,\"b\":[1,2,3]},\"hostname\":\"machine-1\"}");

        assertThat(config.hostname()).isEqualTo("machine-1");
    }

    @Test
    void aKnownSetting_isNotReported() {
        // The control: warning about settings that are perfectly valid would train operators to ignore
        // the warning that matters.
        final ObjectMapper mapper = ProtocolAdapterUtils.createProtocolAdapterMapper(new ObjectMapper());

        final List<ILoggingEvent> events =
                whileCapturing(() -> readValue(mapper, "{\"hostname\":\"machine-1\",\"port\":4840}"));

        assertThat(events).isEmpty();
    }

    private static @NotNull ExampleConfig readValue(final @NotNull ObjectMapper mapper, final @NotNull String json) {
        try {
            return mapper.readValue(json, ExampleConfig.class);
        } catch (final Exception e) {
            throw new AssertionError("failed reading " + json, e);
        }
    }

    private static @NotNull List<ILoggingEvent> whileCapturing(final @NotNull Supplier<?> body) {
        final Logger logger = (Logger) LoggerFactory.getLogger(ProtocolAdapterUtils.class);
        final ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            final Object ignored = body.get();
            assertThat(ignored).isNotNull();
            return List.copyOf(appender.list);
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }
}
