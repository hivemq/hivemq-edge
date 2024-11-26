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
package com.hivemq.edge.adapters.http.config.legacy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.adapter.sdk.api.factories.ProtocolAdapterFactoryInput;
import com.hivemq.configuration.entity.HiveMQConfigEntity;
import com.hivemq.configuration.entity.adapter.ProtocolAdapterEntity;
import com.hivemq.configuration.migration.ConfigurationMigrator;
import com.hivemq.configuration.reader.ConfigFileReaderWriter;
import com.hivemq.configuration.reader.ConfigurationFile;
import com.hivemq.edge.adapters.http.HttpProtocolAdapterFactory;
import com.hivemq.edge.modules.ModuleLoader;
import com.hivemq.protocols.ProtocolAdapterConfig;
import com.hivemq.protocols.ProtocolAdapterConfigConverter;
import com.hivemq.protocols.ProtocolAdapterFactoryManager;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.hivemq.protocols.ProtocolAdapterUtils.createProtocolAdapterMapper;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
public class LegacyHttpProtocolAdapterConfigTest {

    private final @NotNull ObjectMapper mapper = createProtocolAdapterMapper(new ObjectMapper());

    @Test
    public void convertConfigObject_defaults() throws Exception {
        final URL resource = getClass().getResource("/legacy-http-config-minimal.xml");

        final ConfigurationMigrator migrator = new ConfigurationMigrator(
                new ConfigurationFile(new File(resource.toURI())),
                mock(ModuleLoader.class));
        final ProtocolAdapterFactoryInput mockInput = mock(ProtocolAdapterFactoryInput.class);
        when(mockInput.isWritingEnabled()).thenReturn(true);

        assertThat(migrator.migrateIfNeeded(Map.of("http", new HttpProtocolAdapterFactory(mockInput))))
                .isNotEmpty()
                .get()
                .satisfies(cfg -> {
                    assertThat(cfg.getProtocolAdapterConfig())
                            .hasSize(1)
                            .allSatisfy(entity -> {
                                assertThat(entity.getProtocolId()).isEqualTo("http");
                                assertThat(entity.getAdapterId()).isEqualTo("my-protocol-adapter");
                                assertThat(entity.getTags())
                                        .hasSize(1)
                                        .allSatisfy(tag -> {
                                                assertThat(tag.getName()).startsWith("my-protocol-adapter-");
                                                assertThat(tag.getDefinition())
                                                        .extracting("httpRequestBody", "httpRequestBodyContentType", "httpRequestMethod", "httpRequestTimeoutSeconds", "url")
                                                        .containsExactly(null, "JSON", "GET", 5, "http://192.168.0.02:777/?asdasd=asdasd");
                                        });
                                assertThat(entity.getFromEdgeMappingEntities())
                                        .hasSize(1)
                                        .allSatisfy(mapping -> {
                                            assertThat(mapping.getMaxQoS()).isEqualTo(1);
                                            assertThat(mapping.getTagName()).startsWith("my-protocol-adapter-");
                                            assertThat(mapping.getTopic()).isEqualTo("my/destination");
                                            assertThat(mapping.getUserProperties()).isEmpty();
                                        });
                                assertThat(entity.getFieldMappings()).isEmpty();
                                assertThat(entity.getToEdgeMappingEntities()).isEmpty();
                            });
                });
    }

    @Test
    public void convertConfigObject_full() throws Exception {
        final URL resource = getClass().getResource("/legacy-http-config-with-headers.xml");

        final ConfigurationMigrator migrator = new ConfigurationMigrator(
                new ConfigurationFile(new File(resource.toURI())),
                mock(ModuleLoader.class));
        final ProtocolAdapterFactoryInput mockInput = mock(ProtocolAdapterFactoryInput.class);
        when(mockInput.isWritingEnabled()).thenReturn(true);

        assertThat(migrator.migrateIfNeeded(Map.of("http", new HttpProtocolAdapterFactory(mockInput))))
                .isNotEmpty()
                .get()
                .satisfies(cfg -> {
                    assertThat(cfg.getProtocolAdapterConfig())
                            .hasSize(1)
                            .allSatisfy(entity -> {
                                assertThat(entity.getProtocolId()).isEqualTo("http");
                                assertThat(entity.getAdapterId()).isEqualTo("my-protocol-adapter");
                                assertThat(entity.getTags())
                                        .hasSize(1)
                                        .allSatisfy(tag -> {
                                            assertThat(tag.getName()).startsWith("my-protocol-adapter-");
                                            assertThat(tag.getDefinition())
                                                    .extracting("httpHeaders", "httpRequestBody", "httpRequestBodyContentType", "httpRequestMethod", "httpRequestTimeoutSeconds", "url")
                                                    .containsExactly(
                                                            List.of(
                                                                Map.of("name", "foo 1", "value", "bar 1"),
                                                                Map.of("name", "foo 2", "value", "bar 2")),
                                                            "my-body",
                                                            "YAML",
                                                            "GET",
                                                            50,
                                                            "http://192.168.0.02:777/?asdasd=asdasd");
                                        });
                                assertThat(entity.getFromEdgeMappingEntities())
                                        .hasSize(1)
                                        .allSatisfy(mapping -> {
                                            assertThat(mapping.getMaxQoS()).isEqualTo(0);
                                            assertThat(mapping.getTagName()).startsWith("my-protocol-adapter-");
                                            assertThat(mapping.getTopic()).isEqualTo("my/destination");
                                            assertThat(mapping.getUserProperties()).isEmpty();
                                        });
                                assertThat(entity.getFieldMappings()).isEmpty();
                                assertThat(entity.getToEdgeMappingEntities()).isEmpty();
                            });
                });
    }

    private @NotNull ProtocolAdapterConfig getProtocolAdapterConfig(final @NotNull URL resource) throws URISyntaxException {
        final File path = Path.of(resource.toURI()).toFile();

        final HiveMQConfigEntity configEntity = loadConfig(path);
        final ProtocolAdapterEntity adapterEntity = configEntity.getProtocolAdapterConfig().get(0);

        final ProtocolAdapterConfigConverter converter = createConverter();

        return converter.fromEntity(adapterEntity);
    }

    private @NotNull ProtocolAdapterConfigConverter createConverter() {
        final ProtocolAdapterFactoryInput mockInput = mock(ProtocolAdapterFactoryInput.class);
        when(mockInput.isWritingEnabled()).thenReturn(true);

        HttpProtocolAdapterFactory httpProtocolAdapterFactory = new HttpProtocolAdapterFactory(mockInput);
        ProtocolAdapterFactoryManager manager = mock(ProtocolAdapterFactoryManager.class);
        when(manager.get("http")).thenReturn(Optional.of(httpProtocolAdapterFactory));
        ProtocolAdapterConfigConverter converter = new ProtocolAdapterConfigConverter(manager, mapper);
        return converter;
    }

    private @NotNull HiveMQConfigEntity loadConfig(final @NotNull File configFile) {
        final ConfigFileReaderWriter readerWriter = new ConfigFileReaderWriter(new ConfigurationFile(configFile),
                mock(),
                mock(),
                mock(),
                mock(),
                mock(),
                mock(),
                mock(),
                mock(),
                mock(),
                mock(),
                mock(),
                mock(),
                mock(),
                mock());
        return readerWriter.applyConfig();
    }
}
