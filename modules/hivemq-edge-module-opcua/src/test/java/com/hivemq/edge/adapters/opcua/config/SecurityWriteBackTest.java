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

import static com.hivemq.protocols.ProtocolAdapterUtils.createProtocolAdapterMapper;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.adapter.sdk.api.factories.ProtocolAdapterFactoryInput;
import com.hivemq.edge.adapters.opcua.OpcUaProtocolAdapterFactory;
import java.util.HashMap;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Reading an adapter over the API and writing it straight back must not change its configuration.
 *
 * <p>{@code GET /api/v1/management/protocol-adapters/adapters} serialises the <em>parsed</em>
 * configuration, and {@code PUT} persists whatever the caller submits, verbatim. So any value the
 * model invents on read is handed to the caller as if they had written it, and the next save — the UI
 * does exactly this round trip on every edit — puts it in {@code config.xml}. This is the
 * {@link TlsWriteBackTest} property applied to {@code <security>}, whose {@code messageSecurityMode}
 * was resolved to {@code IGNORED} at parse time and so appeared in every response.
 *
 * <p>These go through {@code convertConfigObject} / {@code unconvertConfigObject} on the real factory
 * with the production adapter mapper rather than a bare {@link ObjectMapper}: the inclusion settings
 * that decide whether an unset field is emitted belong to that mapper, so a plain one would prove
 * nothing about the endpoint.
 */
class SecurityWriteBackTest {

    private static final @NotNull String URI = "opc.tcp://machine.local:4840";

    @Test
    void unsetMode_isNotWrittenBack() {
        final Map<String, Object> written = writeBack(Map.of("security", Map.of("policy", "NONE")));

        assertThat(security(written))
                .as("a mode the operator never configured must not come back from a read")
                .doesNotContainKey("messageSecurityMode");
    }

    @Test
    void absentSecurityElement_doesNotSproutAMode() {
        // The adapter whose XML has no <security> at all: the configuration still resolves a Security
        // to carry the default policy, and that object must not carry a mode either.
        final Map<String, Object> written = writeBack(Map.of());

        assertThat(security(written)).doesNotContainKey("messageSecurityMode");
    }

    @Test
    void explicitlyIgnoredMode_survivesTheRoundTrip() {
        // The other direction, and the reason the fix is in the parse rather than in the serialisation:
        // IGNORED means the same thing as unset, but an operator who wrote it must not find it
        // silently deleted from their file. Suppressing it on write would trade one churn for another.
        final Map<String, Object> written =
                writeBack(Map.of("security", Map.of("policy", "NONE", "messageSecurityMode", "IGNORED")));

        assertThat(security(written)).containsEntry("messageSecurityMode", "IGNORED");
    }

    @ParameterizedTest
    @ValueSource(strings = {"NONE", "SIGN", "SIGN_AND_ENCRYPT"})
    void configuredMode_isPreservedVerbatim(final @NotNull String mode) {
        final Map<String, Object> written =
                writeBack(Map.of("security", Map.of("policy", "BASIC256SHA256", "messageSecurityMode", mode)));

        assertThat(security(written)).containsEntry("messageSecurityMode", mode);
    }

    @Test
    void readingAndSavingWithoutEditingLeavesTheAdapterUnchanged() {
        // Guards the fix rather than the defect: this held before it too. ProtocolAdapterManager skips
        // the restart when the reloaded configuration equals the running one, and that comparison
        // reaches Security.equals - so making the mode nullable is only safe while every path that
        // builds a Security agrees on how "unset" is spelled. Miss one and a saved adapter bounces.
        final Map<String, Object> declared = Map.of("security", Map.of("policy", "NONE"));

        final OpcUaSpecificAdapterConfig running = parse(declared);
        final OpcUaSpecificAdapterConfig reloaded = parse(writeBack(declared));

        assertThat(reloaded).isEqualTo(running);
        assertThat(reloaded.hashCode()).isEqualTo(running.hashCode());
    }

    @Test
    void unsetModeIsNullOnTheRecord() {
        // The representation the two properties above rest on: the record keeps "not configured" as
        // null instead of collapsing it onto IGNORED, which is what left NON_NULL nothing to suppress.
        assertThat(parse(Map.of("security", Map.of("policy", "NONE")))
                        .getSecurity()
                        .messageSecurityMode())
                .isNull();
        assertThat(parse(Map.of()).getSecurity().messageSecurityMode()).isNull();
    }

    /** The REST read path: parsed configuration back out as the map {@code GET} serialises. */
    private static @NotNull Map<String, Object> writeBack(final @NotNull Map<String, Object> adapterConfig) {
        return factory().unconvertConfigObject(createProtocolAdapterMapper(new ObjectMapper()), parse(adapterConfig));
    }

    /** The REST write path: a submitted map back into the configuration object. */
    private static @NotNull OpcUaSpecificAdapterConfig parse(final @NotNull Map<String, Object> adapterConfig) {
        final Map<String, Object> withUri = new HashMap<>(adapterConfig);
        withUri.put("uri", URI);
        return (OpcUaSpecificAdapterConfig)
                factory().convertConfigObject(createProtocolAdapterMapper(new ObjectMapper()), withUri, true);
    }

    private static @NotNull OpcUaProtocolAdapterFactory factory() {
        final ProtocolAdapterFactoryInput input = mock(ProtocolAdapterFactoryInput.class);
        when(input.isWritingEnabled()).thenReturn(true);
        return new OpcUaProtocolAdapterFactory(input);
    }

    @SuppressWarnings("unchecked")
    private static @NotNull Map<String, @Nullable Object> security(final @NotNull Map<String, Object> written) {
        assertThat(written).containsKey("security");
        return (Map<String, Object>) written.get("security");
    }
}
