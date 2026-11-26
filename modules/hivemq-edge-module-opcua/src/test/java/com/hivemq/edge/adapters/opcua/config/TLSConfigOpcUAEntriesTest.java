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

import static com.hivemq.edge.adapters.opcua.Constants.PROTOCOL_ID_OPCUA;
import static com.hivemq.protocols.ProtocolAdapterUtils.createProtocolAdapterMapper;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hivemq.adapter.sdk.api.factories.ProtocolAdapterFactoryInput;
import com.hivemq.configuration.entity.HiveMQConfigEntity;
import com.hivemq.configuration.info.SystemInformation;
import com.hivemq.configuration.reader.ConfigFileReaderWriter;
import com.hivemq.configuration.reader.ConfigurationFile;
import com.hivemq.edge.adapters.opcua.OpcUaProtocolAdapterFactory;
import com.hivemq.edge.adapters.opcua.client.Failure;
import com.hivemq.edge.adapters.opcua.client.ParsedConfig;
import com.hivemq.edge.adapters.opcua.client.Result;
import com.hivemq.edge.adapters.opcua.client.Success;
import com.hivemq.protocols.ProtocolAdapterConfig;
import com.hivemq.protocols.ProtocolAdapterConfigConverter;
import com.hivemq.protocols.ProtocolAdapterFactoryManager;
import com.hivemq.security.tls.FileSet;
import com.hivemq.security.tls.MaterialRef;
import com.hivemq.security.tls.Secret;
import com.hivemq.security.tls.TLSPolicy;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import util.KeyChain;

/// The TLS configuration of an OPC UA adapter as the configuration file and the superseded elements
/// shape it: single list entries, entries without a location, the superseded single-path elements,
/// entries that name nothing, and the integrity-check waiver.
class TLSConfigOpcUAEntriesTest {

    private static final @NotNull ObjectMapper MAPPER = new ObjectMapper();

    private static final @NotNull String STORE_PASSWORD = "store-pw";

    @TempDir
    @NotNull
    Path tempDir;

    private static @NotNull ProtocolAdapterConfigConverter converter() {
        final ProtocolAdapterFactoryInput input = mock(ProtocolAdapterFactoryInput.class);
        when(input.isWritingEnabled()).thenReturn(true);
        final OpcUaProtocolAdapterFactory factory = new OpcUaProtocolAdapterFactory(input);
        final ProtocolAdapterFactoryManager manager = mock(ProtocolAdapterFactoryManager.class);
        when(manager.get(PROTOCOL_ID_OPCUA)).thenReturn(Optional.of(factory));
        return new ProtocolAdapterConfigConverter(manager, createProtocolAdapterMapper(new ObjectMapper()));
    }

    private static @NotNull FileSet filesOf(final @NotNull Optional<MaterialRef> material) {
        return inlineOf(material).files();
    }

    /// The material a resolved element names, which these elements always write inline.
    private static @NotNull MaterialRef.Inline inlineOf(final @NotNull Optional<MaterialRef> material) {
        if (material.orElseThrow() instanceof MaterialRef.Inline inline) {
            return inline;
        }
        return fail("the element must resolve to inline material [material=" + material + "]");
    }

    /// The superseded spelling, as existing configurations write it; a `null` argument leaves its
    /// element out. A truststore is written with `STORE_PASSWORD`.
    private static @NotNull OpcUaSpecificAdapterConfig legacyConfig(
            final @NotNull String preset,
            final @Nullable Path truststore,
            final @Nullable String allowListPath,
            final @Nullable String revocationListPath)
            throws Exception {
        final ObjectNode root = MAPPER.createObjectNode().put("uri", "opc.tcp://machine.local:4840");
        final ObjectNode tls = root.putObject("tls").put("enabled", true).put("tlsChecks", preset);
        if (truststore != null) {
            tls.putObject("truststore").put("path", truststore.toString()).put("password", STORE_PASSWORD);
        }
        if (allowListPath != null) {
            tls.putObject("allowList").put("path", allowListPath);
        }
        if (revocationListPath != null) {
            tls.putObject("revocationList").put("path", revocationListPath);
        }
        return MAPPER.treeToValue(root, OpcUaSpecificAdapterConfig.class);
    }

    private static @NotNull Tls legacyTls(
            final @NotNull String preset,
            final @Nullable Path truststore,
            final @Nullable String allowListPath,
            final @Nullable String revocationListPath)
            throws Exception {
        return legacyConfig(preset, truststore, allowListPath, revocationListPath)
                .getTls();
    }

    /// The same two steps the adapter takes at start: the configuration is resolved, then the policy
    /// reads what it names. The `NONE` preset still builds a certification path (chain trust), which
    /// is where revocation lists are read.
    private static @NotNull TLSPolicy loadPolicy(final @NotNull Tls tls) throws Exception {
        return new TLSPolicy(new TLSConfigOpcUA(tls));
    }

    private static @NotNull Tls supersedingTruststore(final @NotNull TlsTruststore truststore) {
        return Tls.superseding(true, TlsChecks.STANDARD, null, null, truststore, null, null);
    }

    /// The whole adapter configuration, read the way `readTls` reads its `tls` element.
    private @NotNull OpcUaSpecificAdapterConfig readConfig(final @NotNull String tlsChildren) {
        final String xml = "<hivemq><protocol-adapters><protocol-adapter>"
                + "<adapterId>opcua-1</adapterId><protocolId>opcua</protocolId>"
                + "<config><uri>opc.tcp://machine.local:4840</uri>"
                + "<tls><enabled>true</enabled>"
                + tlsChildren
                + "</tls></config></protocol-adapter></protocol-adapters></hivemq>";
        final Path file = tempDir.resolve("config.xml");
        try {
            Files.writeString(file, xml);
        } catch (final IOException e) {
            throw new IllegalStateException("could not write the configuration file [path=" + file + "]", e);
        }
        final HiveMQConfigEntity entity = new ConfigFileReaderWriter(
                        mock(SystemInformation.class), new ConfigurationFile(file.toFile()), List.of())
                .applyConfig();
        final ProtocolAdapterConfig adapter =
                converter().fromEntity(entity.getProtocolAdapterConfig().getFirst());
        return (OpcUaSpecificAdapterConfig) adapter.getAdapterConfig();
    }

    /// The `tls` element of one adapter, through the path an operator's file takes: the XML reader
    /// (through `ArbitraryValuesMapAdapter`), then the converter that binds the adapter's map.
    private @NotNull Tls readTls(final @NotNull String tlsChildren) {
        return readConfig(tlsChildren).getTls();
    }

    /// A JKS truststore holding `ca`, protected by `STORE_PASSWORD`.
    private @NotNull Path writeTruststore(final @NotNull X509Certificate ca) throws Exception {
        final KeyStore store = KeyStore.getInstance("JKS");
        store.load(null, null);
        store.setCertificateEntry("ca", ca);
        final Path file = tempDir.resolve("ca.jks");
        try (final OutputStream out = Files.newOutputStream(file)) {
            store.store(out, STORE_PASSWORD.toCharArray());
        }
        return file;
    }

    /// A root certificate authority in a JKS truststore, and one DER revocation list it signed,
    /// written to `crlFile`.
    private @NotNull Path writeTruststoreAndRevocationList(final @NotNull Path crlFile) throws Exception {
        final KeyChain chain = KeyChain.createKeyChain();
        chain.writeRootCrl(crlFile, List.of());
        return writeTruststore(chain.getRootCertificate());
    }

    /// One XML element where the model holds a list binds as a one-element list.
    @Nested
    class SingleValuedListEntries {

        /// A single `<builtin>` binds as a one-element list of builtins.
        @Test
        void givenOneBuiltin_whenTheConfigurationFileIsRead_thenItBindsAsAList() {
            final Tls tls = readTls("<tls-truststore><builtin>JVM-cacerts</builtin></tls-truststore>");

            assertThat(tls.tlsTruststore()).isNotNull();
            assertThat(tls.tlsTruststore().builtins()).containsExactly("JVM-cacerts");
        }

        /// A single `<directory>` binds as a one-element list, its location kept.
        @Test
        void givenOneDirectory_whenTheConfigurationFileIsRead_thenItBindsAsAList() {
            final Tls tls = readTls("<tls-truststore><format>pem</format>"
                    + "<directory><location>/etc/edge/authorities</location></directory>"
                    + "<skip-integrity-check>true</skip-integrity-check></tls-truststore>");

            assertThat(tls.tlsTruststore()).isNotNull();
            assertThat(tls.tlsTruststore().directories())
                    .extracting(FileLocation::location)
                    .containsExactly("/etc/edge/authorities");
        }

        /// A single `<pattern>` binds as a one-element list, its location and `may-be-empty` kept.
        @Test
        void givenOnePattern_whenTheConfigurationFileIsRead_thenItBindsAsAList() {
            final Tls tls = readTls("<tls-truststore><format>pem</format>"
                    + "<pattern><location>/etc/edge/authorities/*.pem</location>"
                    + "<may-be-empty>true</may-be-empty></pattern>"
                    + "<skip-integrity-check>true</skip-integrity-check></tls-truststore>");

            assertThat(tls.tlsTruststore()).isNotNull();
            assertThat(tls.tlsTruststore().patterns()).singleElement().satisfies(pattern -> {
                assertThat(pattern.location()).isEqualTo("/etc/edge/authorities/*.pem");
                assertThat(pattern.isMayBeEmpty()).isTrue();
            });
        }

        /// Two `<directory>` siblings bind as a two-element list in document order.
        @Test
        void givenTwoDirectories_whenTheConfigurationFileIsRead_thenBothBind() {
            final Tls tls = readTls("<tls-truststore><format>pem</format>"
                    + "<directory><location>/etc/edge/a</location></directory>"
                    + "<directory><location>/etc/edge/b</location></directory>"
                    + "<skip-integrity-check>true</skip-integrity-check></tls-truststore>");

            assertThat(tls.tlsTruststore()).isNotNull();
            assertThat(tls.tlsTruststore().directories())
                    .extracting(FileLocation::location)
                    .containsExactly("/etc/edge/a", "/etc/edge/b");
        }

        /// A single `<file>` inside `<tls-allowlist>` binds as a one-element list.
        @Test
        void givenOneAllowListFile_whenTheConfigurationFileIsRead_thenItBindsAsAList() {
            final Tls tls = readTls("<tlsChecks>SELF_SIGNED</tlsChecks>"
                    + "<tls-allowlist><file>/etc/edge/fingerprints.txt</file></tls-allowlist>");

            assertThat(tls.tlsAllowlist()).isNotNull();
            assertThat(tls.tlsAllowlist().files()).containsExactly("/etc/edge/fingerprints.txt");
        }

        /// A single `<directory>` inside `<tls-allowlist>` binds as a one-element list.
        @Test
        void givenOneAllowListDirectory_whenTheConfigurationFileIsRead_thenItBindsAsAList() {
            final Tls tls = readTls("<tlsChecks>SELF_SIGNED</tlsChecks>"
                    + "<tls-allowlist><directory><location>/etc/edge/fingerprints</location></directory>"
                    + "</tls-allowlist>");

            assertThat(tls.tlsAllowlist()).isNotNull();
            assertThat(tls.tlsAllowlist().directories())
                    .extracting(FileLocation::location)
                    .containsExactly("/etc/edge/fingerprints");
        }

        /// One `<file>` beside one `<pattern>` in `<tls-revocationlist>` binds each as its own one-element list.
        @Test
        void givenOneRevocationListFileAndOnePattern_whenTheConfigurationFileIsRead_thenBothBindAsLists() {
            final Tls tls = readTls("<tls-revocationlist><file>/etc/edge/plant-ca.crl</file>"
                    + "<pattern><location>/etc/edge/crls/*.crl</location></pattern></tls-revocationlist>");

            assertThat(tls.tlsRevocationlist()).isNotNull();
            assertThat(tls.tlsRevocationlist().files()).containsExactly("/etc/edge/plant-ca.crl");
            assertThat(tls.tlsRevocationlist().patterns())
                    .extracting(FileLocation::location)
                    .containsExactly("/etc/edge/crls/*.crl");
        }

        /// Two `<file>` siblings bind as a two-element list in document order.
        @Test
        void givenTwoFileElements_whenTheConfigurationFileIsRead_thenBothPathsBind() {
            final Tls tls = readTls("<tls-truststore><format>pem</format>"
                    + "<file>/etc/edge/plant-ca.pem</file><file>/etc/edge/site-ca.pem</file>"
                    + "<skip-integrity-check>true</skip-integrity-check></tls-truststore>");

            assertThat(tls.tlsTruststore()).isNotNull();
            assertThat(tls.tlsTruststore().files()).containsExactly("/etc/edge/plant-ca.pem", "/etc/edge/site-ca.pem");
        }

        /// The documented `<tls-truststore>` with a single `<file>` binds: the XML reader hands one
        /// element over as a scalar, which the list-valued field must accept as a one-element list.
        @Test
        void givenOneFileElement_whenTheConfigurationFileIsRead_thenThePathBinds() {
            final String children = "<tls-truststore><format>pem</format><file>/etc/edge/plant-ca.pem</file>"
                    + "<skip-integrity-check>true</skip-integrity-check></tls-truststore>";

            assertThatCode(() -> readTls(children))
                    .as("a single <file> inside <tls-truststore> must bind to the list-valued field")
                    .doesNotThrowAnyException();
            final Tls tls = readTls(children);
            assertThat(tls.tlsTruststore()).isNotNull();
            assertThat(tls.tlsTruststore().files())
                    .as("the one file must be held")
                    .containsExactly("/etc/edge/plant-ca.pem");
        }

        /// The first example of the OPC UA document, as written there.
        @Test
        void givenTheDocumentedTruststoreAndRevocationListExample_whenItIsRead_thenItResolves() throws Exception {
            final Tls tls = readTls("<tlsChecks>ALL</tlsChecks>"
                    + "<tls-truststore><format>pem</format><file>/opt/hivemq/conf/plant-ca.pem</file>"
                    + "<skip-integrity-check>true</skip-integrity-check></tls-truststore>"
                    + "<tls-revocationlist><file>/opt/hivemq/conf/plant-ca.crl</file></tls-revocationlist>");

            final TLSConfigOpcUA effective = new TLSConfigOpcUA(tls);

            assertThat(filesOf(effective.truststore())).isEqualTo(FileSet.of("/opt/hivemq/conf/plant-ca.pem"));
            assertThat(filesOf(effective.revocationList())).isEqualTo(FileSet.of("/opt/hivemq/conf/plant-ca.crl"));
        }

        /// The identity example of the OPC UA document, as written there.
        @Test
        void givenTheDocumentedIdentityAndAllowListExample_whenItIsRead_thenItResolves() throws Exception {
            final Tls tls = readTls("<tlsChecks>SELF_SIGNED</tlsChecks>"
                    + "<tls-allowlist><file>/opt/hivemq/conf/opcua-fingerprints.txt</file></tls-allowlist>"
                    + "<tls-identity><format>p12</format><file>/opt/hivemq/conf/edge-client.p12</file>"
                    + "<password-file>/run/secrets/edge-keystore-password</password-file>"
                    + "<key-entry>edge-client</key-entry></tls-identity>");

            final TLSConfigOpcUA effective = new TLSConfigOpcUA(tls);

            assertThat(filesOf(effective.allowList())).isEqualTo(FileSet.of("/opt/hivemq/conf/opcua-fingerprints.txt"));
            assertThat(filesOf(effective.identity())).isEqualTo(FileSet.of("/opt/hivemq/conf/edge-client.p12"));
        }
    }

    /// A directory or pattern entry without a location is refused, not dropped.
    @Nested
    class EntriesWithoutALocation {

        /// A truststore `<directory>` without a `<location>` is refused, naming the element and the entry.
        @Test
        void givenATruststoreDirectoryWithoutALocation_whenResolved_thenItIsRefused() {
            final Tls tls = readTls("<tls-truststore><format>pem</format>"
                    + "<directory><may-be-empty>true</may-be-empty></directory>"
                    + "<skip-integrity-check>true</skip-integrity-check></tls-truststore>");

            assertThatThrownBy(() -> new TLSConfigOpcUA(tls))
                    .isInstanceOf(TLSConfigOpcUA.InvalidTlsConfigException.class)
                    .hasMessageContaining("'tls-truststore'")
                    .hasMessageContaining("'directory' entry without a 'location'");
        }

        /// A truststore pattern whose location is blank is refused like one without a location.
        @Test
        void givenATruststorePatternWithABlankLocation_whenResolved_thenItIsRefused() {
            final Tls tls = Tls.superseding(
                    true,
                    TlsChecks.STANDARD,
                    null,
                    null,
                    new TlsTruststore(
                            MaterialFormat.PEM,
                            null,
                            null,
                            List.of(new FileLocation("  ")),
                            null,
                            null,
                            null,
                            true,
                            null,
                            null),
                    null,
                    null);

            assertThatThrownBy(() -> new TLSConfigOpcUA(tls))
                    .isInstanceOf(TLSConfigOpcUA.InvalidTlsConfigException.class)
                    .hasMessageContaining("'tls-truststore'")
                    .hasMessageContaining("'pattern' entry without a 'location'");
        }

        /// A location-less directory is refused even when a `<file>` beside it names usable material.
        @Test
        void givenATruststoreDirectoryWithoutALocationBesideAFile_whenResolved_thenItIsStillRefused() {
            final Tls tls = readTls("<tls-truststore><format>pem</format><file>/etc/edge/plant-ca.pem</file>"
                    + "<directory><may-be-empty>true</may-be-empty></directory>"
                    + "<skip-integrity-check>true</skip-integrity-check></tls-truststore>");

            assertThatThrownBy(() -> new TLSConfigOpcUA(tls))
                    .isInstanceOf(TLSConfigOpcUA.InvalidTlsConfigException.class)
                    .hasMessageContaining("'directory' entry without a 'location'");
        }

        /// A `<tls-revocationlist>` directory without a location is refused, naming that element.
        @Test
        void givenARevocationListDirectoryWithoutALocation_whenResolved_thenItIsRefused() {
            final Tls tls = readTls(
                    "<tls-revocationlist><directory><may-be-empty>true</may-be-empty></directory></tls-revocationlist>");

            assertThatThrownBy(() -> new TLSConfigOpcUA(tls))
                    .isInstanceOf(TLSConfigOpcUA.InvalidTlsConfigException.class)
                    .hasMessageContaining("'tls-revocationlist'")
                    .hasMessageContaining("'directory' entry without a 'location'");
        }

        /// A `<tls-allowlist>` pattern without a location is refused, naming that element.
        @Test
        void givenAnAllowListPatternWithoutALocation_whenResolved_thenItIsRefused() {
            final Tls tls = readTls("<tlsChecks>SELF_SIGNED</tlsChecks>"
                    + "<tls-allowlist><pattern><may-be-empty>true</may-be-empty></pattern></tls-allowlist>");

            assertThatThrownBy(() -> new TLSConfigOpcUA(tls))
                    .isInstanceOf(TLSConfigOpcUA.InvalidTlsConfigException.class)
                    .hasMessageContaining("'tls-allowlist'")
                    .hasMessageContaining("'pattern' entry without a 'location'");
        }

        /// A directory with a location resolves into the truststore's file set.
        @Test
        void givenATruststoreDirectoryWithALocation_whenResolved_thenItIsKept() throws Exception {
            final Tls tls = readTls("<tls-truststore><format>pem</format>"
                    + "<directory><location>/etc/edge/authorities</location></directory>"
                    + "<skip-integrity-check>true</skip-integrity-check></tls-truststore>");

            final TLSConfigOpcUA effective = new TLSConfigOpcUA(tls);

            assertThat(filesOf(effective.truststore()).directories())
                    .containsExactly(new FileSet.Directory("/etc/edge/authorities"));
        }
    }

    /// The two superseded single-path elements keep what each always read.
    @Nested
    class SupersededPaths {

        /// The superseded `revocationList.path` resolves to a legacy path, which may name a file or a directory.
        @Test
        void givenALegacyRevocationListPath_whenResolved_thenItMayNameAFileOrADirectory() throws Exception {
            final Tls tls = legacyTls("NONE", null, null, "/etc/edge/crls");

            final TLSConfigOpcUA effective = new TLSConfigOpcUA(tls);

            assertThat(filesOf(effective.revocationList())).isEqualTo(FileSet.ofLegacyPath("/etc/edge/crls"));
        }

        /// The superseded `revocationList.path` naming one DER file: the policy reads that list.
        @Test
        void givenALegacyRevocationListPathNamingAFile_whenThePolicyIsLoaded_thenTheListIsRead() throws Exception {
            final Path crlFile = tempDir.resolve("ca.crl");
            final Path truststore = writeTruststoreAndRevocationList(crlFile);

            final TLSPolicy policy = loadPolicy(legacyTls("NONE", truststore, null, crlFile.toString()));

            assertThat(policy.revocationLists()).hasSize(1);
        }

        /// The same element naming a directory still loads, as it did before the element was
        /// superseded, and the policy reads the one DER list inside it.
        @Test
        void givenALegacyRevocationListPathNamingADirectory_whenThePolicyIsLoaded_thenTheListIsRead() throws Exception {
            final Path crlDirectory = Files.createDirectory(tempDir.resolve("crls"));
            final Path truststore = writeTruststoreAndRevocationList(crlDirectory.resolve("ca.crl"));
            final Tls tls = legacyTls("NONE", truststore, null, crlDirectory.toString());

            assertThatCode(() -> loadPolicy(tls))
                    .as("a legacy revocationList.path naming a directory must still load, as it did before")
                    .doesNotThrowAnyException();
            assertThat(loadPolicy(tls).revocationLists())
                    .as("the directory's one DER revocation list must be read")
                    .hasSize(1);
        }

        /// The superseded `allowList.path` resolves to exactly one file.
        @Test
        void givenALegacyAllowListPath_whenResolved_thenItNamesOneFile() throws Exception {
            final Tls tls = legacyTls("SELF_SIGNED", null, "/etc/edge/fingerprints.txt", null);

            final TLSConfigOpcUA effective = new TLSConfigOpcUA(tls);

            assertThat(filesOf(effective.allowList())).isEqualTo(FileSet.of("/etc/edge/fingerprints.txt"));
        }

        /// A superseded `allowList.path` naming a directory refuses the start with a message that names
        /// the directory and the `<tls-allowlist><directory>` element to write instead.
        @Test
        void givenALegacyAllowListPathNamingADirectory_whenTheAdapterStarts_thenTheMessageNamesTheFix()
                throws Exception {
            final Path directory = Files.createDirectory(tempDir.resolve("fingerprints"));
            Files.writeString(directory.resolve("server.txt"), "00".repeat(32) + "\n");

            final Result<ParsedConfig, String> result =
                    ParsedConfig.fromConfig(legacyConfig("SELF_SIGNED", null, directory.toString(), null));

            final String refusal =
                    switch (result) {
                        case Failure<ParsedConfig, String>(final String failure) -> failure;
                        case Success<ParsedConfig, String>(final ParsedConfig parsed) ->
                            fail("the start must be refused [parsed=" + parsed + "]");
                    };
            assertThat(refusal)
                    .contains("allowList path '" + directory + "' is a directory")
                    .contains("<tls-allowlist><directory><location>" + directory + "</location>");
        }

        /// Under chain trust the allow-list is never read, so a directory there stays as inert as it
        /// was before rather than becoming a reason to refuse the adapter.
        @Test
        void givenALegacyAllowListDirectoryUnderChainTrust_whenTheAdapterStarts_thenItIsNotRefusedForIt()
                throws Exception {
            final Path directory = Files.createDirectory(tempDir.resolve("unused-fingerprints"));

            final Result<ParsedConfig, String> result =
                    ParsedConfig.fromConfig(legacyConfig("NONE", null, directory.toString(), null));

            assertThat(result).isInstanceOf(Success.class);
        }
    }

    /// A `file` or `builtin` entry that names nothing is refused, like a location-less directory.
    @Nested
    class EmptyEntries {

        /// A truststore `<file>` that is blank is refused.
        @Test
        void givenATruststoreWithABlankFile_whenResolved_thenItIsRefused() {
            final Tls tls = supersedingTruststore(new TlsTruststore(
                    MaterialFormat.PEM, List.of("  "), null, null, null, null, null, true, null, null));

            assertThatThrownBy(() -> new TLSConfigOpcUA(tls))
                    .isInstanceOf(TLSConfigOpcUA.InvalidTlsConfigException.class)
                    .hasMessageContaining("The 'tls-truststore' element has an empty 'file' entry");
        }

        /// An empty `<builtin>` is refused even beside a usable `<file>`.
        @Test
        void givenATruststoreWithABlankBuiltinBesideAFile_whenResolved_thenItIsRefused() {
            final Tls tls = supersedingTruststore(new TlsTruststore(
                    MaterialFormat.PEM,
                    List.of("/etc/edge/plant-ca.pem"),
                    null,
                    null,
                    List.of(""),
                    null,
                    null,
                    true,
                    null,
                    null));

            assertThatThrownBy(() -> new TLSConfigOpcUA(tls))
                    .isInstanceOf(TLSConfigOpcUA.InvalidTlsConfigException.class)
                    .hasMessageContaining("The 'tls-truststore' element has an empty 'builtin' entry");
        }

        /// An allow-list `<file>` that is blank is refused.
        @Test
        void givenAnAllowListWithABlankFile_whenResolved_thenItIsRefused() {
            final Tls tls = Tls.superseding(
                    true,
                    TlsChecks.SELF_SIGNED,
                    null,
                    null,
                    null,
                    new TlsFileList(List.of(" "), null, null, null, null),
                    null);

            assertThatThrownBy(() -> new TLSConfigOpcUA(tls))
                    .isInstanceOf(TLSConfigOpcUA.InvalidTlsConfigException.class)
                    .hasMessageContaining("The 'tls-allowlist' element has an empty 'file' entry");
        }

        /// A revocation-list `<file>` that is empty is refused.
        @Test
        void givenARevocationListWithABlankFile_whenResolved_thenItIsRefused() {
            final Tls tls = Tls.superseding(
                    true,
                    TlsChecks.STANDARD,
                    null,
                    null,
                    null,
                    null,
                    new TlsFileList(List.of(""), null, null, null, null));

            assertThatThrownBy(() -> new TLSConfigOpcUA(tls))
                    .isInstanceOf(TLSConfigOpcUA.InvalidTlsConfigException.class)
                    .hasMessageContaining("The 'tls-revocationlist' element has an empty 'file' entry");
        }
    }

    /// `skip-integrity-check` is carried as the waiver, so a container opens unchecked rather than
    /// with an empty password; a container named with neither a password nor the waiver is refused,
    /// and so is a truststore naming both.
    @Nested
    class IntegrityCheck {

        /// `skip-integrity-check` without a password resolves to the waiver, not to an absent password.
        @Test
        void givenSkipAndNoPassword_whenResolved_thenTheWaiverIsCarried() throws Exception {
            final Tls tls = supersedingTruststore(new TlsTruststore(
                    MaterialFormat.P12, List.of("/etc/edge/ca.p12"), null, null, null, null, null, true, null, null));

            final MaterialRef.Inline truststore = inlineOf(new TLSConfigOpcUA(tls).truststore());

            assertThat(truststore.password()).isEqualTo(new Secret.Waived());
        }

        /// A JKS store written with a password, read without it: an empty password fails its
        /// integrity check, so only the waiver lets the adapter start.
        @Test
        void givenSkipAndNoPasswordOnAProtectedStore_whenTheAdapterStarts_thenTheStoreLoads() throws Exception {
            final Path store = writeTruststore(KeyChain.createKeyChain("plant").getRootCertificate());

            final Result<ParsedConfig, String> result = ParsedConfig.fromConfig(readConfig("<tls-truststore>"
                    + "<format>jks</format><file>" + store + "</file>"
                    + "<skip-integrity-check>true</skip-integrity-check></tls-truststore>"));

            assertThat(result).isInstanceOf(Success.class);
        }

        /// `skip-integrity-check` beside a password is refused: the two contradict each other.
        @Test
        void givenSkipAndAPassword_whenResolved_thenItIsRefused() {
            final Tls tls = supersedingTruststore(new TlsTruststore(
                    MaterialFormat.P12, List.of("/etc/edge/ca.p12"), null, null, null, "pw", null, true, null, null));

            assertThatThrownBy(() -> new TLSConfigOpcUA(tls))
                    .isInstanceOf(TLSConfigOpcUA.InvalidTlsConfigException.class)
                    .hasMessageContaining("names both a password and skip-integrity-check");
        }

        /// `skip-integrity-check` beside a password file is refused like one beside a password.
        @Test
        void givenSkipAndAPasswordFile_whenResolved_thenItIsRefused() {
            final Tls tls = supersedingTruststore(new TlsTruststore(
                    MaterialFormat.JKS,
                    List.of("/etc/edge/ca.jks"),
                    null,
                    null,
                    null,
                    null,
                    "/run/secrets/store-pw",
                    true,
                    null,
                    null));

            assertThatThrownBy(() -> new TLSConfigOpcUA(tls))
                    .isInstanceOf(TLSConfigOpcUA.InvalidTlsConfigException.class)
                    .hasMessageContaining("names both a password and skip-integrity-check");
        }

        /// A p12 truststore with neither a password nor the waiver is refused.
        @Test
        void givenAP12WithNeitherPasswordNorSkip_whenResolved_thenItIsRefused() {
            final Tls tls = supersedingTruststore(new TlsTruststore(
                    MaterialFormat.P12, List.of("/etc/edge/ca.p12"), null, null, null, null, null, null, null, null));

            assertThatThrownBy(() -> new TLSConfigOpcUA(tls))
                    .isInstanceOf(TLSConfigOpcUA.InvalidTlsConfigException.class)
                    .hasMessageContaining("names a p12 container but neither a password nor skip-integrity-check");
        }

        /// A jks truststore with neither a password nor the waiver is refused; an explicit `false` waives nothing.
        @Test
        void givenAJksWithNeitherPasswordNorSkip_whenResolved_thenItIsRefused() {
            final Tls tls = supersedingTruststore(new TlsTruststore(
                    MaterialFormat.JKS, List.of("/etc/edge/ca.jks"), null, null, null, null, null, false, null, null));

            assertThatThrownBy(() -> new TLSConfigOpcUA(tls))
                    .isInstanceOf(TLSConfigOpcUA.InvalidTlsConfigException.class)
                    .hasMessageContaining("names a jks container but neither a password nor skip-integrity-check");
        }

        /// A PEM truststore needs neither: PEM has no integrity check, so no password is carried.
        @Test
        void givenAPemWithNeitherPasswordNorSkip_whenResolved_thenItIsAccepted() throws Exception {
            final Tls tls = supersedingTruststore(new TlsTruststore(
                    MaterialFormat.PEM, List.of("/etc/edge/ca.pem"), null, null, null, null, null, null, null, null));

            final MaterialRef.Inline truststore = inlineOf(new TLSConfigOpcUA(tls).truststore());

            assertThat(truststore.password()).isNull();
        }

        /// A p12 truststore's password is carried as a literal secret.
        @Test
        void givenAP12WithAPassword_whenResolved_thenThePasswordIsCarried() throws Exception {
            final Tls tls = supersedingTruststore(new TlsTruststore(
                    MaterialFormat.P12, List.of("/etc/edge/ca.p12"), null, null, null, "pw", null, null, null, null));

            final MaterialRef.Inline truststore = inlineOf(new TLSConfigOpcUA(tls).truststore());

            assertThat(truststore.password()).isEqualTo(new Secret.Literal("pw"));
        }
    }
}
