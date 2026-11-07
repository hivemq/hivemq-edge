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
package com.hivemq.edge.adapters.opcua.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.hivemq.adapter.sdk.api.ProtocolAdapterConnectionDirection;
import com.hivemq.adapter.sdk.api.factories.ProtocolAdapterFactoryInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartOutput;
import com.hivemq.adapter.sdk.api.services.ModuleServices;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterMetricsService;
import com.hivemq.adapter.sdk.api.streaming.ProtocolAdapterTagStreamingService;
import com.hivemq.configuration.info.SystemInformation;
import com.hivemq.configuration.reader.ConfigFileReaderWriter;
import com.hivemq.configuration.reader.ConfigurationFile;
import com.hivemq.configuration.reader.TlsMaterialConfigurator;
import com.hivemq.edge.adapters.opcua.FakeEventService;
import com.hivemq.edge.adapters.opcua.OpcUaProtocolAdapter;
import com.hivemq.edge.adapters.opcua.OpcUaProtocolAdapterFactory;
import com.hivemq.edge.adapters.opcua.config.OpcUaSpecificAdapterConfig;
import com.hivemq.edge.adapters.opcua.config.Tls;
import com.hivemq.edge.adapters.opcua.config.TlsChecks;
import com.hivemq.edge.adapters.opcua.config.TlsTruststore;
import com.hivemq.edge.modules.adapters.impl.ProtocolAdapterStateImpl;
import com.hivemq.protocols.ProtocolAdapterInputImpl;
import com.hivemq.security.tls.FileSet;
import com.hivemq.security.tls.MaterialRef;
import com.hivemq.security.tls.TLSMaterialProvider;
import com.hivemq.security.tls.TLSMaterialRegistry;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import util.KeyChain;

/// Shared TLS material declared in the configuration file reaches the OPC UA adapter's start path:
/// a truststore written as a reference (`plant::ca`) resolves against the registry the node built.
class ParsedConfigSharedMaterialTest {

    private static final @NotNull String ADAPTER_ID = "opcua-1";

    private static final @NotNull String REFERENCE = "plant::ca";

    @TempDir
    @NotNull
    Path tempDir;

    /// Given the registry that declares the material, the parse resolves the reference.
    @Test
    void givenAReferenceToDeclaredMaterial_whenTheRegistryIsPassed_thenTheReferenceResolves() throws Exception {
        final TLSMaterialRegistry declared = declaringTruststore(writeCaCertificate());

        final Result<ParsedConfig, String> result = ParsedConfig.fromConfig(configReferencing(REFERENCE), declared);

        assertThat(result).isInstanceOf(Success.class);
    }

    /// The adapter's own start resolves the reference too: it must parse against the node's registry,
    /// not an empty one, or every adapter naming shared material refuses to start.
    @Test
    void givenAReferenceToDeclaredMaterial_whenTheProductionEntryPointParsesIt_thenTheReferenceResolves()
            throws Exception {
        // The adapter's start, reached the way Edge reaches it: plant::ca declared in a configuration
        // file and read by the configurator Edge installs, the adapter built by its factory from Edge's
        // own input, which carries the node's provider. The outcome is what the start parsed.
        final Result<ParsedConfig, Throwable> result = startedThroughEdge(configReferencing(REFERENCE));

        assertThat(result)
                .as("the adapter's start path must resolve a reference to declared shared material")
                .isInstanceOf(Success.class);
    }

    private static @NotNull OpcUaSpecificAdapterConfig configReferencing(final @NotNull String reference) {
        final TlsTruststore truststore =
                new TlsTruststore(null, null, null, null, null, null, null, null, reference, null);
        return new OpcUaSpecificAdapterConfig(
                "opc.tcp://machine.local:4840",
                false,
                null,
                null,
                Tls.superseding(true, TlsChecks.STANDARD, null, null, truststore, null, null),
                null,
                null,
                null);
    }

    private static @NotNull TLSMaterialRegistry declaringTruststore(final @NotNull Path certificate) throws Exception {
        return new TLSMaterialRegistry.Builder()
                .entry(
                        "plant",
                        TLSMaterialRegistry.Kind.TRUSTSTORE,
                        "ca",
                        null,
                        new MaterialRef.Inline("pem", FileSet.of(certificate.toString()), null, null, null))
                .build();
    }

    /// What the adapter's start parsed: the configuration it stored when it started, or the failure
    /// it reported. The failure itself is kept, not its message, which may be `null`; a start that
    /// reports neither outcome fails the test.
    private @NotNull Result<ParsedConfig, Throwable> startedThroughEdge(
            final @NotNull OpcUaSpecificAdapterConfig config) throws Exception {
        final TLSMaterialProvider node = new TLSMaterialProvider();
        final Path file = Files.writeString(tempDir.resolve("config.xml"), """
                <hivemq>
                    <tls-materials>
                        <tls-material>
                            <name>plant</name>
                            <tls-truststore>
                                <name>ca</name>
                                <format>pem</format>
                                <file>%s</file>
                            </tls-truststore>
                        </tls-material>
                    </tls-materials>
                </hivemq>
                """.formatted(writeCaCertificate()));
        new ConfigFileReaderWriter(
                        mock(SystemInformation.class),
                        new ConfigurationFile(file.toFile()),
                        List.of(new TlsMaterialConfigurator(node)))
                .applyConfig();

        final FakeEventService events = new FakeEventService();
        final ModuleServices services = mock(ModuleServices.class);
        final ProtocolAdapterTagStreamingService streaming = mock(ProtocolAdapterTagStreamingService.class);
        when(services.eventService()).thenReturn(events);
        when(services.protocolAdapterTagStreamingService()).thenReturn(streaming);
        final ProtocolAdapterFactoryInput factoryInput = mock(ProtocolAdapterFactoryInput.class);
        when(factoryInput.isWritingEnabled()).thenReturn(true);
        final OpcUaProtocolAdapterFactory factory = new OpcUaProtocolAdapterFactory(factoryInput);
        final ProtocolAdapterInputImpl<OpcUaSpecificAdapterConfig> input = new ProtocolAdapterInputImpl<>(
                ADAPTER_ID,
                config,
                List.of(),
                List.of(),
                "1",
                new ProtocolAdapterStateImpl(events, ADAPTER_ID, "opcua"),
                services,
                mock(ProtocolAdapterMetricsService.class),
                node);
        final OpcUaProtocolAdapter adapter =
                (OpcUaProtocolAdapter) factory.createAdapter(factory.getInformation(), input);

        final AtomicBoolean started = new AtomicBoolean();
        final AtomicReference<Throwable> failure = new AtomicReference<>();
        final ProtocolAdapterStartInput startInput = mock(ProtocolAdapterStartInput.class);
        when(startInput.moduleServices()).thenReturn(services);
        final ProtocolAdapterStartOutput output = new ProtocolAdapterStartOutput() {
            @Override
            public void startedSuccessfully() {
                started.set(true);
            }

            @Override
            public void failStart(final @NotNull Throwable cause, final @Nullable String message) {
                failure.set(cause);
            }
        };
        try {
            adapter.start(ProtocolAdapterConnectionDirection.Northbound, startInput, output);
            if (failure.get() != null) {
                return Failure.of(failure.get());
            }
            if (!started.get()) {
                fail("start returned without reporting an outcome [adapterId=" + ADAPTER_ID + "]");
            }
            final Field parsed = OpcUaProtocolAdapter.class.getDeclaredField("parsedConfig");
            parsed.setAccessible(true);
            return Success.of((ParsedConfig) parsed.get(adapter));
        } finally {
            adapter.destroy();
        }
    }

    /// The root certificate authority of a fresh chain in PEM, the one thing `plant::ca` stands for.
    private @NotNull Path writeCaCertificate() throws Exception {
        final X509Certificate ca = KeyChain.createKeyChain().getRootCertificate();
        final String pem = "-----BEGIN CERTIFICATE-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII))
                        .encodeToString(ca.getEncoded())
                + "\n-----END CERTIFICATE-----\n";
        return Files.writeString(tempDir.resolve("ca.pem"), pem);
    }
}
