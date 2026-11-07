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
package com.hivemq.security.tls;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hivemq.configuration.entity.tls.TlsMaterialEntity;
import com.hivemq.configuration.reader.TlsMaterialRegistryBuilder;
import jakarta.xml.bind.JAXBContext;
import java.io.StringReader;
import java.util.List;
import javax.xml.transform.stream.StreamSource;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

/// How a declared material entry is accepted into the registry: an entry that names no file is
/// refused, and a password-protected container keeps the integrity waiver it declares or is refused
/// when it declares neither a password nor the waiver.
class TLSMaterialRegistryDeclarationTest {

    /// Proves that a declared truststore naming no file is refused, naming the entry.
    @Test
    void whenADeclaredTruststoreNamesNothing_thenTheRegistryIsRefused() {
        assertThatThrownBy(() -> registry("<tls-truststore><name>empty</name></tls-truststore>"))
                .isInstanceOf(TLSMaterialRegistry.InvalidReferenceException.class)
                .hasMessageContaining("empty")
                .hasMessageContaining("names no file");
    }

    /// Proves that a declaration waiving the integrity check resolves to a container whose password
    /// is the waiver.
    @Test
    void whenADeclarationWaivesTheCheck_thenTheRegistryKeepsTheWaiver() throws Exception {
        final TLSMaterialRegistry registry = registry("""
                <tls-truststore>
                    <name>authorities</name><format>jks</format><file>/ca.jks</file>
                    <skip-integrity-check>true</skip-integrity-check>
                </tls-truststore>
                """);

        final MaterialRef.Inline truststore = (MaterialRef.Inline)
                registry.resolve(new MaterialRef.ByName("plant", "authorities"), TLSMaterialRegistry.Kind.TRUSTSTORE);
        assertThat(truststore.password()).isEqualTo(new Secret.Waived());
    }

    /// Proves that a declared container with neither a password nor the waiver is refused.
    @Test
    void whenADeclaredContainerNamesNeitherPasswordNorWaiver_thenTheRegistryIsRefused() {
        assertThatThrownBy(() -> registry("<tls-truststore><name>authorities</name><format>jks</format>"
                        + "<file>/ca.jks</file></tls-truststore>"))
                .isInstanceOf(TLSMaterialRegistry.InvalidReferenceException.class)
                .hasMessageContaining("neither a password nor skip-integrity-check");
    }

    /// The registry built from one material set named `plant` holding the given entries.
    private static @NotNull TLSMaterialRegistry registry(final @NotNull String entries) throws Exception {
        final TlsMaterialEntity material = JAXBContext.newInstance(TlsMaterialEntity.class)
                .createUnmarshaller()
                .unmarshal(
                        new StreamSource(
                                new StringReader("<tls-material><name>plant</name>" + entries + "</tls-material>")),
                        TlsMaterialEntity.class)
                .getValue();
        return TlsMaterialRegistryBuilder.registryOf(List.of(material));
    }
}
