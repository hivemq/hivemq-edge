/*
 * Copyright 2023-present HiveMQ GmbH
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
package com.hivemq.edge.adapters.opcua.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

/**
 * How the enclosing settings of the adapter configuration are read.
 *
 * <p>The load-bearing property here is that a misspelled enclosing setting can never loosen
 * security. The application-wide handling for unknown adapter settings warns and drops, which for
 * these settings replaces whole blocks with their insecure defaults: a misspelled {@code <tlls>}
 * runs without TLS, a misspelled {@code <securtiy>} runs with policy NONE, a misspelled
 * {@code <aut>} connects anonymously. The configuration's any-setter runs ahead of that handler and
 * rejects the conversion instead, naming the entry — contained to the one adapter, with a running
 * instance left unchanged. The one exception is the retired {@code includeMetadata} setting, which
 * released versions accepted and an upgrade may still carry: it is reported and ignored, never
 * refused.
 */
class UnknownSettingsParsingTest {

    private static final @NotNull ObjectMapper MAPPER = new ObjectMapper();

    private static final @NotNull String URI = "\"uri\":\"opc.tcp://machine.local:4840\"";

    @Test
    void aMisspelledTlsSettingIsRejectedNamingTheEntryAndTheKnownSettings() {
        assertThatThrownBy(() -> MAPPER.readValue(
                        "{" + URI + ",\"tlls\":{\"enabled\":false}}", OpcUaSpecificAdapterConfig.class))
                .hasMessageContaining("'tlls'")
                .hasMessageContaining("Known settings")
                .hasMessageContaining("tls, security");
    }

    @Test
    void aMisspelledSecuritySettingIsRejectedNamingTheEntry() {
        assertThatThrownBy(() -> MAPPER.readValue(
                        "{" + URI + ",\"securtiy\":{\"policy\":\"BASIC256SHA256\"}}", OpcUaSpecificAdapterConfig.class))
                .hasMessageContaining("'securtiy'")
                .hasMessageContaining("Known settings");
    }

    @Test
    void aMisspelledAuthSettingIsRejectedNamingTheEntry() {
        assertThatThrownBy(() -> MAPPER.readValue(
                        "{" + URI + ",\"aut\":{\"basic\":{\"username\":\"u\",\"password\":\"p\"}}}",
                        OpcUaSpecificAdapterConfig.class))
                .hasMessageContaining("'aut'")
                .hasMessageContaining("Known settings");
    }

    @Test
    void theRetiredIncludeMetadataSettingIsAcceptedWithAWarning() throws Exception {
        // includeMetadata existed in released versions; refusing it would break an upgrade. It is
        // the one entry that is reported and dropped rather than rejected.
        final ListAppender<ILoggingEvent> appender = attachAppender(OpcUaSpecificAdapterConfig.class);
        try {
            final OpcUaSpecificAdapterConfig config =
                    MAPPER.readValue("{" + URI + ",\"includeMetadata\":true}", OpcUaSpecificAdapterConfig.class);

            assertThat(config.getUri()).isEqualTo("opc.tcp://machine.local:4840");
            assertThat(appender.list).anySatisfy(event -> {
                assertThat(event.getLevel()).isEqualTo(Level.WARN);
                assertThat(event.getFormattedMessage())
                        .contains("includeMetadata")
                        .contains("retired");
            });
        } finally {
            detachAppender(OpcUaSpecificAdapterConfig.class, appender);
        }
    }

    @Test
    void theBidirectionalSubtypeInheritsTheRejection() {
        // The any-setter is a method on the base class, so the subtype used for northbound-and-
        // southbound deployments carries the same trap without declaring anything.
        assertThatThrownBy(() ->
                        MAPPER.readValue("{" + URI + ",\"aut\":{}}", BidirectionalOpcUaSpecificAdapterConfig.class))
                .hasMessageContaining("'aut'")
                .hasMessageContaining("Known settings");
    }

    @Test
    void aSubtypesOwnSettingNeverReachesTheInheritedRejection() throws Exception {
        // A subtype's declared settings are known to its deserializer and are bound before the
        // any-setter is consulted - only genuinely unknown entries arrive there.
        final SubtypeWithOwnSetting config =
                MAPPER.readValue("{" + URI + ",\"mqttToOpcua\":{\"maxBatchSize\":5}}", SubtypeWithOwnSetting.class);

        assertThat(config.mqttToOpcua).containsEntry("maxBatchSize", 5);

        assertThatThrownBy(() ->
                        MAPPER.readValue("{" + URI + ",\"tlls\":{\"enabled\":true}}", SubtypeWithOwnSetting.class))
                .hasMessageContaining("'tlls'");
    }

    /** The shape of the commercial subtype: extra settings of its own on top of the base ones. */
    static final class SubtypeWithOwnSetting extends OpcUaSpecificAdapterConfig {

        final @Nullable Map<String, Object> mqttToOpcua;

        @JsonCreator
        SubtypeWithOwnSetting(
                @JsonProperty(value = "uri", required = true) final @NotNull String uri,
                @JsonProperty("mqttToOpcua") final @Nullable Map<String, Object> mqttToOpcua) {
            super(uri, null, null, null, null, null, null, null);
            this.mqttToOpcua = mqttToOpcua;
        }
    }

    private static @NotNull ListAppender<ILoggingEvent> attachAppender(final @NotNull Class<?> type) {
        final Logger logger = (Logger) LoggerFactory.getLogger(type);
        final ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        return appender;
    }

    private static void detachAppender(
            final @NotNull Class<?> type, final @NotNull ListAppender<ILoggingEvent> appender) {
        ((Logger) LoggerFactory.getLogger(type)).detachAppender(appender);
        appender.stop();
    }
}
