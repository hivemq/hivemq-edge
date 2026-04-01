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
package com.hivemq.edge.knappogue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hivemq.configuration.entity.HiveMQConfigEntity;
import com.hivemq.configuration.entity.adapter.ProtocolAdapterEntity;
import com.hivemq.configuration.reader.ConfigFileReaderWriter;
import com.hivemq.edge.compiler.EdgeCompiler;
import com.hivemq.edge.compiler.lib.model.CompiledConfig;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CompiledConfigApplierTest {

    private ConfigFileReaderWriter configFileReaderWriter;
    private CompiledConfigApplier applier;
    private HiveMQConfigEntity configEntity;

    @BeforeEach
    void setUp() {
        configFileReaderWriter = mock(ConfigFileReaderWriter.class);
        configEntity = mock(HiveMQConfigEntity.class);
        final List<ProtocolAdapterEntity> adapterList = new ArrayList<>();
        when(configEntity.getProtocolAdapterConfig()).thenReturn(adapterList);
        when(configFileReaderWriter.getCurrentConfigEntity()).thenReturn(configEntity);
        when(configFileReaderWriter.applyCompiledConfig(any())).thenReturn(true);

        applier = new CompiledConfigApplier(configFileReaderWriter);
    }

    @Test
    void applyKnappogueExampleProducesTwoAdapters() throws Exception {
        final CompiledConfig config = compileFixture();

        final boolean success = applier.apply(config);

        assertThat(success).isTrue();
        verify(configFileReaderWriter).applyCompiledConfig(configEntity);
        assertThat(configEntity.getProtocolAdapterConfig()).hasSize(2);
    }

    @Test
    void opcUaAdapterHasCorrectFieldsAfterTranslation() throws Exception {
        final CompiledConfig config = compileFixture();
        applier.apply(config);

        final ProtocolAdapterEntity extruder = adapterById("extruder-01");
        assertThat(extruder.getProtocolId()).isEqualTo("opcua");
        assertThat(extruder.getConfig())
                .containsEntry("uri", "opc.tcp://192.168.1.10:4840")
                .doesNotContainKey("host")
                .doesNotContainKey("port")
                .doesNotContainKey("protocol");
        assertThat(extruder.getTags()).hasSize(2);
        final var nozzle = extruder.getTags().stream()
                .filter(t -> t.getName().equals("NozzlePressure"))
                .findFirst()
                .orElseThrow();
        assertThat(nozzle.getDefinition()).containsEntry("node", "ns=2;i=1003");
        assertThat(extruder.getNorthboundMappings()).hasSize(1);
        assertThat(extruder.getNorthboundMappings().get(0).getTagName()).isEqualTo("NozzlePressure");
        assertThat(extruder.getNorthboundMappings().get(0).getTopic())
                .isEqualTo("factory/berlin/extruder-01/nozzle-pressure");
    }

    @Test
    void bacNetAdapterHasCorrectFieldsAfterTranslation() throws Exception {
        final CompiledConfig config = compileFixture();
        applier.apply(config);

        final ProtocolAdapterEntity hvac = adapterById("hvac-01");
        assertThat(hvac.getProtocolId()).isEqualTo("bacnetip");
        assertThat(hvac.getTags()).hasSize(1);
        final var zoneTemp = hvac.getTags().get(0);
        assertThat(zoneTemp.getDefinition())
                .containsEntry("deviceInstanceNumber", 1)
                .containsEntry("objectInstanceNumber", 0)
                .containsEntry("objectType", "ANALOG_INPUT")
                .containsEntry("propertyType", "PRESENT_VALUE")
                .doesNotContainKey("address");
    }

    @Test
    void wrongNoticeFieldIsRejected() {
        final CompiledConfig badConfig = new CompiledConfig(
                "WRONG NOTICE",
                CompiledConfig.SIGNATURE_UNSIGNED,
                CompiledConfig.FORMAT_VERSION,
                "2.5",
                List.of(),
                List.of());

        final boolean success = applier.apply(badConfig);

        assertThat(success).isFalse();
        verify(configFileReaderWriter, org.mockito.Mockito.never()).applyCompiledConfig(any());
    }

    @Test
    void northboundMappingDefaultsArePreserved() throws Exception {
        final CompiledConfig config = compileFixture();
        applier.apply(config);

        final ProtocolAdapterEntity extruder = adapterById("extruder-01");
        final var mapping = extruder.getNorthboundMappings().get(0);
        assertThat(mapping.isIncludeTagNames()).isFalse();
        assertThat(mapping.isIncludeTimestamp()).isTrue();
        assertThat(mapping.isIncludeMetadata()).isFalse();
        assertThat(mapping.getMessageExpiryInterval()).isEqualTo(Long.MAX_VALUE);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private CompiledConfig compileFixture() throws Exception {
        final URL url = getClass().getClassLoader().getResource("fixtures/knappogue-example");
        assertThat(url).as("fixture not found").isNotNull();
        final EdgeCompiler compiler = new EdgeCompiler();
        final EdgeCompiler.Result result = compiler.compile(Paths.get(url.toURI()));
        assertThat(result.diagnostics().errors())
                .as("unexpected compile errors")
                .isEmpty();
        return result.compiledConfig();
    }

    private ProtocolAdapterEntity adapterById(final String adapterId) {
        return configEntity.getProtocolAdapterConfig().stream()
                .filter(a -> a.getAdapterId().equals(adapterId))
                .findFirst()
                .orElseThrow(() -> new AssertionError("adapter not found: " + adapterId));
    }
}
