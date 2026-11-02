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

import com.hivemq.configuration.entity.HiveMQConfigEntity;
import com.hivemq.configuration.entity.tls.TlsMaterialEntity;
import com.hivemq.exceptions.UnrecoverableException;
import com.hivemq.security.tls.FileSet;
import com.hivemq.security.tls.MaterialRef;
import com.hivemq.security.tls.Secret;
import com.hivemq.security.tls.TLSMaterialRegistry;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

/// That `tls-material` written in a configuration file reaches the registry a reference resolves
/// against, and that a declaration a lookup could not resolve is refused when the registry is built
/// rather than when a surface connects.
///
/// Parses through the real reader rather than a hand-built JAXB context, since the element names and
/// the wrapper are exactly what could be wrong.
class TlsMaterialRegistryBuilderTest extends AbstractConfigurationTest {

    @Test
    void whenMaterialIsDeclared_thenEveryTypeReachesTheRegistry() throws Exception {
        final TLSMaterialRegistry registry = registryFrom("""
                <tls-materials>
                    <tls-material>
                        <name>plant</name>
                        <tls-identity>
                            <name>gateway</name>
                            <format>p12</format>
                            <file>/etc/edge/gateway.p12</file>
                            <password>secret</password>
                            <key-entry>gateway-key</key-entry>
                        </tls-identity>
                        <tls-truststore>
                            <name>authorities</name>
                            <format>jks</format>
                            <file>/etc/edge/plant-ca.jks</file>
                            <password-file>/run/secrets/truststore</password-file>
                            <builtin>JVM-cacerts</builtin>
                        </tls-truststore>
                        <tls-allowlist>
                            <name>pinned</name>
                            <file>/etc/edge/pinned.txt</file>
                        </tls-allowlist>
                        <tls-revocationlist>
                            <name>revoked</name>
                            <directory>
                                <location>/etc/edge/crl.d</location>
                                <may-be-empty>true</may-be-empty>
                            </directory>
                        </tls-revocationlist>
                    </tls-material>
                </tls-materials>
                """);

        final MaterialRef.Inline identity =
                registry.resolve(new MaterialRef.ByName("plant", "gateway"), TLSMaterialRegistry.Kind.IDENTITY);
        assertThat(identity.format()).isEqualTo("p12");
        assertThat(identity.files().files()).containsExactly("/etc/edge/gateway.p12");
        assertThat(identity.password()).isEqualTo(new Secret.Literal("secret"));
        assertThat(identity.entry()).isEqualTo("gateway-key");

        final MaterialRef.Inline truststore =
                registry.resolve(new MaterialRef.ByName("plant", "authorities"), TLSMaterialRegistry.Kind.TRUSTSTORE);
        assertThat(truststore.files().files()).containsExactly("/etc/edge/plant-ca.jks");
        assertThat(truststore.files().builtins()).containsExactly(FileSet.JVM_CACERTS);
        assertThat(truststore.password()).isEqualTo(new Secret.FromFile("/run/secrets/truststore"));

        final MaterialRef.Inline allowList =
                registry.resolve(new MaterialRef.ByName("plant", "pinned"), TLSMaterialRegistry.Kind.ALLOW_LIST);
        assertThat(allowList.files().files()).containsExactly("/etc/edge/pinned.txt");

        final MaterialRef.Inline revocation =
                registry.resolve(new MaterialRef.ByName("plant", "revoked"), TLSMaterialRegistry.Kind.REVOCATION_LIST);
        assertThat(revocation.files().directories()).containsExactly(new FileSet.Directory("/etc/edge/crl.d", true));
    }

    @Test
    void whenADirectoryOmitsMayBeEmpty_thenItMustHoldAFile() throws Exception {
        final TLSMaterialRegistry registry = registryFrom("""
                <tls-materials>
                    <tls-material>
                        <name>plant</name>
                        <tls-truststore>
                            <name>authorities</name>
                            <directory><location>/etc/edge/ca.d</location></directory>
                        </tls-truststore>
                    </tls-material>
                </tls-materials>
                """);

        final MaterialRef.Inline truststore =
                registry.resolve(new MaterialRef.ByName("plant", "authorities"), TLSMaterialRegistry.Kind.TRUSTSTORE);
        assertThat(truststore.files().directories()).containsExactly(new FileSet.Directory("/etc/edge/ca.d", false));
    }

    @Test
    void whenTwoPartiesUseOneEntryName_thenEachReferenceFindsItsOwn() throws Exception {
        final TLSMaterialRegistry registry = registryFrom("""
                <tls-materials>
                    <tls-material>
                        <name>plant</name>
                        <tls-truststore><name>corporate-ca</name><file>/plant/ca.jks</file><password>a</password></tls-truststore>
                    </tls-material>
                    <tls-material>
                        <name>cloud</name>
                        <tls-truststore><name>corporate-ca</name><file>/cloud/ca.jks</file><password>b</password></tls-truststore>
                    </tls-material>
                </tls-materials>
                """);

        assertThat(registry.resolve(
                                new MaterialRef.ByName("plant", "corporate-ca"), TLSMaterialRegistry.Kind.TRUSTSTORE)
                        .files()
                        .files())
                .containsExactly("/plant/ca.jks");
        assertThat(registry.resolve(
                                new MaterialRef.ByName("cloud", "corporate-ca"), TLSMaterialRegistry.Kind.TRUSTSTORE)
                        .files()
                        .files())
                .containsExactly("/cloud/ca.jks");
    }

    @Test
    void whenOneNameIsUsedByTwoTypes_thenEachResolvesToItsOwn() throws Exception {
        // A reference carries the type the surface asked for, so one name serves both.
        final TLSMaterialRegistry registry = registryFrom("""
                <tls-materials>
                    <tls-material>
                        <name>plant</name>
                        <tls-truststore><name>shared</name><file>/ca.jks</file><password>a</password></tls-truststore>
                        <tls-allowlist><name>shared</name><file>/pinned.txt</file></tls-allowlist>
                    </tls-material>
                </tls-materials>
                """);

        assertThat(registry.resolve(new MaterialRef.ByName("plant", "shared"), TLSMaterialRegistry.Kind.TRUSTSTORE)
                        .files()
                        .files())
                .containsExactly("/ca.jks");
        assertThat(registry.resolve(new MaterialRef.ByName("plant", "shared"), TLSMaterialRegistry.Kind.ALLOW_LIST)
                        .files()
                        .files())
                .containsExactly("/pinned.txt");
    }

    @Test
    void whenReferencedByUuid_thenTheMaterialIsFound() throws Exception {
        final TLSMaterialRegistry registry = registryFrom("""
                <tls-materials>
                    <tls-material>
                        <name>plant</name>
                        <tls-truststore>
                            <name>authorities</name>
                            <uuid>0d6c1b3e-aaaa-4bbb-8ccc-1d2e3f405162</uuid>
                            <file>/ca.jks</file>
                            <password>a</password>
                        </tls-truststore>
                    </tls-material>
                </tls-materials>
                """);

        assertThat(registry.resolve(
                                new MaterialRef.ByUuid("0d6c1b3e-aaaa-4bbb-8ccc-1d2e3f405162"),
                                TLSMaterialRegistry.Kind.TRUSTSTORE)
                        .files()
                        .files())
                .containsExactly("/ca.jks");
    }

    @Test
    void whenAUuidNamesAnotherType_thenItIsRefusedNamingBoth() throws Exception {
        final TLSMaterialRegistry registry = registryFrom("""
                <tls-materials>
                    <tls-material>
                        <name>plant</name>
                        <tls-allowlist>
                            <name>pinned</name>
                            <uuid>0d6c1b3e-aaaa-4bbb-8ccc-1d2e3f405162</uuid>
                            <file>/pinned.txt</file>
                        </tls-allowlist>
                    </tls-material>
                </tls-materials>
                """);

        assertThatThrownBy(() -> registry.resolve(
                        new MaterialRef.ByUuid("0d6c1b3e-aaaa-4bbb-8ccc-1d2e3f405162"),
                        TLSMaterialRegistry.Kind.TRUSTSTORE))
                .isInstanceOf(TLSMaterialRegistry.InvalidReferenceException.class)
                .hasMessageContaining("tls-allowlist")
                .hasMessageContaining("tls-truststore");
    }

    @Test
    void whenAReferenceNamesNothing_thenItIsRefusedWithNoFallback() throws Exception {
        final TLSMaterialRegistry registry = registryFrom("""
                <tls-materials>
                    <tls-material>
                        <name>plant</name>
                        <tls-truststore><name>authorities</name><file>/ca.jks</file><password>a</password></tls-truststore>
                    </tls-material>
                </tls-materials>
                """);

        assertThatThrownBy(() -> registry.resolve(
                        new MaterialRef.ByName("plant", "authoritys"), TLSMaterialRegistry.Kind.TRUSTSTORE))
                .isInstanceOf(TLSMaterialRegistry.InvalidReferenceException.class)
                .hasMessageContaining("authoritys")
                .hasMessageContaining("plant");
    }

    @Test
    void whenTwoEntriesOfOneTypeShareAName_thenTheDeclarationIsRefused() {
        assertThatThrownBy(() -> registryFrom("""
                <tls-materials>
                    <tls-material>
                        <name>plant</name>
                        <tls-truststore><name>authorities</name><file>/a.jks</file><password>a</password></tls-truststore>
                        <tls-truststore><name>authorities</name><file>/b.jks</file><password>b</password></tls-truststore>
                    </tls-material>
                </tls-materials>
                """))
                .isInstanceOf(TLSMaterialRegistry.InvalidReferenceException.class)
                .hasMessageContaining("authorities");
    }

    @Test
    void whenTwoMaterialsShareAName_thenTheDeclarationIsRefused() {
        assertThatThrownBy(() -> registryFrom("""
                <tls-materials>
                    <tls-material><name>plant</name></tls-material>
                    <tls-material><name>plant</name></tls-material>
                </tls-materials>
                """))
                .isInstanceOf(TLSMaterialRegistry.InvalidReferenceException.class)
                .hasMessageContaining("plant");
    }

    @Test
    void whenOneUuidIsUsedTwice_thenTheDeclarationIsRefused() {
        assertThatThrownBy(() -> registryFrom("""
                <tls-materials>
                    <tls-material>
                        <name>plant</name>
                        <tls-truststore>
                            <name>a</name><uuid>same-uuid</uuid><file>/a.jks</file><password>a</password>
                        </tls-truststore>
                        <tls-allowlist>
                            <name>b</name><uuid>same-uuid</uuid><file>/pinned.txt</file>
                        </tls-allowlist>
                    </tls-material>
                </tls-materials>
                """))
                .isInstanceOf(TLSMaterialRegistry.InvalidReferenceException.class)
                .hasMessageContaining("same-uuid");
    }

    @Test
    void whenAnEntryHasNoName_thenTheDeclarationIsRefused() {
        assertThatThrownBy(() -> registryFrom("""
                <tls-materials>
                    <tls-material>
                        <name>plant</name>
                        <tls-truststore><file>/ca.jks</file><password>a</password></tls-truststore>
                    </tls-material>
                </tls-materials>
                """))
                .isInstanceOf(TLSMaterialRegistry.InvalidReferenceException.class)
                .hasMessageContaining("no name");
    }

    @Test
    void whenAMaterialHasNoName_thenTheDeclarationIsRefused() {
        assertThatThrownBy(() -> registryFrom("""
                <tls-materials>
                    <tls-material>
                        <tls-truststore><name>authorities</name><file>/ca.jks</file><password>a</password></tls-truststore>
                    </tls-material>
                </tls-materials>
                """))
                .isInstanceOf(TLSMaterialRegistry.InvalidReferenceException.class)
                .hasMessageContaining("no name");
    }

    @Test
    void whenANameCarriesTheSeparator_thenTheDeclarationIsRefused() {
        assertThatThrownBy(() -> registryFrom("""
                <tls-materials>
                    <tls-material>
                        <name>plant::east</name>
                        <tls-truststore><name>authorities</name><file>/ca.jks</file><password>a</password></tls-truststore>
                    </tls-material>
                </tls-materials>
                """))
                .isInstanceOf(TLSMaterialRegistry.InvalidReferenceException.class)
                .hasMessageContaining("::");
    }

    @Test
    void whenADeclaredEntryIsItselfAReference_thenItIsRefused() {
        // A declaration pointing at a declaration would be a chain to follow, with a cycle to
        // detect, for no case anybody has.
        assertThatThrownBy(() -> registryFrom("""
                <tls-materials>
                    <tls-material>
                        <name>plant</name>
                        <tls-truststore>
                            <name>authorities</name>
                            <reference-name>other::authorities</reference-name>
                        </tls-truststore>
                    </tls-material>
                </tls-materials>
                """))
                .isInstanceOf(TLSMaterialRegistry.InvalidReferenceException.class)
                .hasMessageContaining("itself a reference");
    }

    @Test
    void whenNoMaterialIsDeclared_thenTheRegistryIsEmptyRatherThanAbsent() throws Exception {
        final TLSMaterialRegistry registry = registryFrom("");

        assertThatThrownBy(() ->
                        registry.resolve(new MaterialRef.ByName("plant", "ca"), TLSMaterialRegistry.Kind.TRUSTSTORE))
                .isInstanceOf(TLSMaterialRegistry.InvalidReferenceException.class);
    }

    @Test
    void whenMaterialIsWrittenInline_thenItResolvesToItself() throws Exception {
        // Every surface writes its material inline today, and such a config names no tls-material.
        final MaterialRef.Inline inline =
                new MaterialRef.Inline("jks", FileSet.of("/ca.jks"), new Secret.Literal("a"), null, null);

        assertThat(TLSMaterialRegistry.empty().resolve(inline, TLSMaterialRegistry.Kind.TRUSTSTORE))
                .isSameAs(inline);
    }

    @Test
    void whenAReferenceIsParsed_thenBothHalvesAreTaken() throws Exception {
        assertThat(TLSMaterialRegistry.parseReference("plant::authorities"))
                .isEqualTo(new MaterialRef.ByName("plant", "authorities"));
    }

    @Test
    void whenAParsedReferenceNamesNoMaterial_thenItIsRefused() {
        assertThatThrownBy(() -> TLSMaterialRegistry.parseReference("authorities"))
                .isInstanceOf(TLSMaterialRegistry.InvalidReferenceException.class)
                .hasMessageContaining("names no tls-material");
    }

    @Test
    void whenAParsedReferenceHasAnEmptyHalf_thenItIsRefused() {
        assertThatThrownBy(() -> TLSMaterialRegistry.parseReference("plant::"))
                .isInstanceOf(TLSMaterialRegistry.InvalidReferenceException.class)
                .hasMessageContaining("empty half");
    }

    @Test
    void whenAnElementIsMisspelled_thenTheSchemaRefusesTheFile() {
        // Pins that the schema names these elements rather than admitting anything, so a typo
        // reaches the operator instead of silently leaving material unconfigured.
        assertThatThrownBy(() -> registryFrom("""
                <tls-materials>
                    <tls-material>
                        <name>plant</name>
                        <tls-truststore><name>authorities</name><fiel>/ca.jks</fiel></tls-truststore>
                    </tls-material>
                </tls-materials>
                """)).isInstanceOf(UnrecoverableException.class);
    }

    @Test
    void whenABuiltinIsNotTheOneName_thenTheSchemaRefusesTheFile() {
        assertThatThrownBy(() -> registryFrom("""
                <tls-materials>
                    <tls-material>
                        <name>plant</name>
                        <tls-truststore><name>authorities</name><builtin>system-roots</builtin></tls-truststore>
                    </tls-material>
                </tls-materials>
                """)).isInstanceOf(UnrecoverableException.class);
    }

    @Test
    void whenAFormatIsNotOneOfTheThree_thenTheSchemaRefusesTheFile() {
        assertThatThrownBy(() -> registryFrom("""
                <tls-materials>
                    <tls-material>
                        <name>plant</name>
                        <tls-identity><name>gateway</name><format>pkcs8</format><file>/k.p12</file></tls-identity>
                    </tls-material>
                </tls-materials>
                """)).isInstanceOf(UnrecoverableException.class);
    }

    /// The registry the given `tls-materials` block forms, parsed through the real reader so the
    /// element names and the wrapper are exercised.
    private @NotNull TLSMaterialRegistry registryFrom(final @NotNull String materials) throws Exception {
        Files.writeString(xmlFile.toPath(), """
                <?xml version="1.0" encoding="UTF-8" ?>
                <hivemq>
                %s
                </hivemq>
                """.formatted(materials), StandardCharsets.UTF_8);
        final HiveMQConfigEntity config = reader.applyConfig();
        final List<TlsMaterialEntity> declared = config.getTlsMaterials();
        return TlsMaterialRegistryBuilder.registryOf(declared);
    }
}
