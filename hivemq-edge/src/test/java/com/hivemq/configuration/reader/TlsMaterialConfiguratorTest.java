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
import com.hivemq.security.tls.MaterialRef;
import com.hivemq.security.tls.TLSMaterialProvider;
import com.hivemq.security.tls.TLSMaterialRegistry;
import jakarta.xml.bind.JAXBContext;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import javax.xml.transform.stream.StreamSource;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

/// That material declared in a configuration file reaches the provider a starting surface reads,
/// and that a declaration which cannot be indexed does not replace one that could.
class TlsMaterialConfiguratorTest extends AbstractConfigurationTest {

    private final @NotNull TLSMaterialProvider provider = new TLSMaterialProvider();
    private final @NotNull TlsMaterialConfigurator configurator = new TlsMaterialConfigurator(provider);

    @Test
    void whenNothingHasBeenRead_thenTheProviderHoldsNoMaterial() throws Exception {
        // A surface starting before the configuration is read resolves no reference rather than
        // seeing a half-built registry.
        assertThatThrownBy(() -> provider.registry()
                        .resolve(new MaterialRef.ByName("plant", "authorities"), TLSMaterialRegistry.Kind.TRUSTSTORE))
                .isInstanceOf(TLSMaterialRegistry.InvalidReferenceException.class);
    }

    @Test
    void whenMaterialIsDeclared_thenItReachesTheProvider() throws Exception {
        final Configurator.ConfigResult result = configurator.applyConfig(configWith("""
                <tls-materials>
                    <tls-material>
                        <name>plant</name>
                        <tls-truststore>
                            <name>authorities</name>
                            <file>/plant/ca.jks</file>
                            <password>pw</password>
                        </tls-truststore>
                    </tls-material>
                </tls-materials>
                """));

        assertThat(result).isEqualTo(Configurator.ConfigResult.SUCCESS);
        final MaterialRef.Inline resolved = provider.registry()
                .resolve(new MaterialRef.ByName("plant", "authorities"), TLSMaterialRegistry.Kind.TRUSTSTORE);
        assertThat(resolved.files().files()).containsExactly("/plant/ca.jks");
    }

    @Test
    void whenTheConfigurationIsReadAgain_thenTheProviderHoldsTheNewMaterial() throws Exception {
        // The configuration is re-read while Edge runs, so renaming declared material must not need
        // a node restart.
        configurator.applyConfig(configWith("""
                <tls-materials>
                    <tls-material>
                        <name>plant</name>
                        <tls-truststore><name>authorities</name><file>/old.jks</file><password>pw</password></tls-truststore>
                    </tls-material>
                </tls-materials>
                """));

        configurator.applyConfig(configWith("""
                <tls-materials>
                    <tls-material>
                        <name>plant</name>
                        <tls-truststore><name>authorities</name><file>/new.jks</file><password>pw</password></tls-truststore>
                    </tls-material>
                </tls-materials>
                """));

        assertThat(provider.registry()
                        .resolve(new MaterialRef.ByName("plant", "authorities"), TLSMaterialRegistry.Kind.TRUSTSTORE)
                        .files()
                        .files())
                .containsExactly("/new.jks");
    }

    @Test
    void whenADeclarationCannotBeIndexed_thenTheLastUsableOneIsKept() throws Exception {
        // Reported as an error, and the previous declaration left in place: replacing it with
        // nothing would refuse every reference and turn one bad declaration into every surface
        // failing to start.
        configurator.applyConfig(configWith("""
                <tls-materials>
                    <tls-material>
                        <name>plant</name>
                        <tls-truststore><name>authorities</name><file>/good.jks</file><password>pw</password></tls-truststore>
                    </tls-material>
                </tls-materials>
                """));

        final Configurator.ConfigResult result = configurator.applyConfig(unreadConfigWith("""
                <tls-materials>
                    <tls-material>
                        <name>plant</name>
                        <tls-truststore><name>clash</name><file>/a.jks</file><password>pw</password></tls-truststore>
                        <tls-truststore><name>clash</name><file>/b.jks</file><password>pw</password></tls-truststore>
                    </tls-material>
                </tls-materials>
                """));

        assertThat(result).isEqualTo(Configurator.ConfigResult.ERROR);
        assertThat(provider.registry()
                        .resolve(new MaterialRef.ByName("plant", "authorities"), TLSMaterialRegistry.Kind.TRUSTSTORE)
                        .files()
                        .files())
                .containsExactly("/good.jks");
    }

    @Test
    void whenNoMaterialIsDeclared_thenTheProviderHoldsAnEmptyRegistry() throws Exception {
        final Configurator.ConfigResult result = configurator.applyConfig(configWith(""));

        assertThat(result).isEqualTo(Configurator.ConfigResult.SUCCESS);
        assertThatThrownBy(() -> provider.registry()
                        .resolve(new MaterialRef.ByName("plant", "authorities"), TLSMaterialRegistry.Kind.TRUSTSTORE))
                .isInstanceOf(TLSMaterialRegistry.InvalidReferenceException.class);
    }

    @Test
    void whenMaterialChanges_thenNoNodeRestartIsDemanded() throws Exception {
        // A surface reads its material when it starts, so a changed declaration reaches it then.
        assertThat(configurator.needsRestartWithConfig(configWith("""
                        <tls-materials>
                            <tls-material>
                                <name>plant</name>
                                <tls-truststore><name>a</name><file>/a.jks</file><password>pw</password></tls-truststore>
                            </tls-material>
                        </tls-materials>
                        """))).isFalse();
    }

    /// The configuration entity the given `tls-materials` block parses to, through the real reader.
    private @NotNull HiveMQConfigEntity configWith(final @NotNull String materials) throws Exception {
        Files.writeString(xmlFile.toPath(), """
                <?xml version="1.0" encoding="UTF-8" ?>
                <hivemq>
                %s
                </hivemq>
                """.formatted(materials), StandardCharsets.UTF_8);
        return reader.applyConfig();
    }

    /// The configuration as unmarshalled, without the checks the reader makes before applying it: the
    /// reader refuses a declaration that cannot be indexed, so this is how one reaches the configurator,
    /// whose own refusal is the defence under test.
    private static @NotNull HiveMQConfigEntity unreadConfigWith(final @NotNull String materials) throws Exception {
        final String xml = "<hivemq>%s</hivemq>".formatted(materials);
        return JAXBContext.newInstance(HiveMQConfigEntity.class)
                .createUnmarshaller()
                .unmarshal(new StreamSource(new StringReader(xml)), HiveMQConfigEntity.class)
                .getValue();
    }
}
