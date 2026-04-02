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

import com.hivemq.combining.model.DataIdentifierReference;
import com.hivemq.combining.model.EntityType;
import com.hivemq.configuration.entity.HiveMQConfigEntity;
import com.hivemq.configuration.entity.adapter.ProtocolAdapterEntity;
import com.hivemq.configuration.entity.combining.DataCombinerEntity;
import com.hivemq.configuration.entity.combining.DataCombiningEntity;
import com.hivemq.configuration.entity.combining.EntityReferenceEntity;
import com.hivemq.configuration.reader.ConfigFileReaderWriter;
import com.hivemq.edge.compiler.EdgeCompiler;
import com.hivemq.edge.compiler.lib.model.CompiledConfig;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
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
        final List<DataCombinerEntity> combinerList = new ArrayList<>();
        when(configEntity.getProtocolAdapterConfig()).thenReturn(adapterList);
        when(configEntity.getDataCombinerEntities()).thenReturn(combinerList);
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

    @Test
    void combinerIsTranslatedFromFixture() throws Exception {
        final CompiledConfig config = compileFixture();
        applier.apply(config);

        assertThat(configEntity.getDataCombinerEntities()).hasSize(1);
        final DataCombinerEntity combiner =
                configEntity.getDataCombinerEntities().get(0);
        assertThat(combiner.getName()).isEqualTo("ExtruderStatus");
        assertThat(combiner.getDescription())
                .isEqualTo("Combines extruder sensor readings into a unified status payload");
        // ID is deterministically derived from name
        assertThat(combiner.getId())
                .isEqualTo(UUID.nameUUIDFromBytes("ExtruderStatus".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void combinerHasCorrectEntityReferences() throws Exception {
        final CompiledConfig config = compileFixture();
        applier.apply(config);

        final DataCombinerEntity combiner =
                configEntity.getDataCombinerEntities().get(0);
        // extruder-01 appears in trigger and instructions; should be deduplicated to one reference
        assertThat(combiner.getEntityReferenceEntities())
                .hasSize(1)
                .containsExactly(new EntityReferenceEntity(EntityType.ADAPTER, "extruder-01"));
    }

    @Test
    void combinerMappingHasCorrectTriggerAndOutput() throws Exception {
        final CompiledConfig config = compileFixture();
        applier.apply(config);

        final DataCombiningEntity mapping = combinerMapping(0);
        // trigger: tag extruder-01::NozzlePressure → primaryReference TAG type, scope=extruder-01, id=NozzlePressure
        final var primary = mapping.getSources().getPrimaryIdentifier();
        assertThat(primary.getType()).isEqualTo(DataIdentifierReference.Type.TAG);
        assertThat(primary.getId()).isEqualTo("NozzlePressure");
        assertThat(primary.getScope()).isEqualTo("extruder-01");
        // destination topic
        assertThat(mapping.getDestination().getTopic()).isEqualTo("factory/berlin/assets/extruder-status");
        assertThat(mapping.getDestination().getSchema()).isEqualTo("");
    }

    @Test
    void combinerMappingHasCorrectInstructions() throws Exception {
        final CompiledConfig config = compileFixture();
        applier.apply(config);

        final DataCombiningEntity mapping = combinerMapping(0);
        assertThat(mapping.getInstructions()).hasSize(3);

        // instruction 0: tag source
        final var instr0 = mapping.getInstructions().get(0);
        assertThat(instr0.getSourceFieldName()).isEqualTo("$.value");
        assertThat(instr0.getDestinationFieldName()).isEqualTo("$.pressure");
        assertThat(instr0.getOrigin()).isNotNull();
        assertThat(instr0.getOrigin().getType()).isEqualTo(DataIdentifierReference.Type.TAG);
        assertThat(instr0.getOrigin().getId()).isEqualTo("NozzlePressure");
        assertThat(instr0.getOrigin().getScope()).isEqualTo("extruder-01");

        // instruction 2: topic filter source
        final var instr2 = mapping.getInstructions().get(2);
        assertThat(instr2.getSourceFieldName()).isEqualTo("$.zoneTemp");
        assertThat(instr2.getDestinationFieldName()).isEqualTo("$.ambientTemp");
        assertThat(instr2.getOrigin()).isNotNull();
        assertThat(instr2.getOrigin().getType()).isEqualTo(DataIdentifierReference.Type.TOPIC_FILTER);
        assertThat(instr2.getOrigin().getId()).isEqualTo("sensors/hvac/zone/+");
        assertThat(instr2.getOrigin().getScope()).isNull();
    }

    @Test
    void combinerMappingIdIsDeterministicallyDerived() throws Exception {
        final CompiledConfig config = compileFixture();
        applier.apply(config);

        final DataCombiningEntity mapping = combinerMapping(0);
        final UUID expectedMappingId =
                UUID.nameUUIDFromBytes("ExtruderStatus::NozzleTrigger".getBytes(StandardCharsets.UTF_8));
        assertThat(mapping.getId()).isEqualTo(expectedMappingId);
    }

    @Test
    void combinerSourcesTagsAndTopicFiltersAreEmpty() throws Exception {
        final CompiledConfig config = compileFixture();
        applier.apply(config);

        final var sources = combinerMapping(0).getSources();
        assertThat(sources.getTags()).isEmpty();
        assertThat(sources.getTopicFilters()).isEmpty();
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

    private DataCombiningEntity combinerMapping(final int mappingIndex) {
        assertThat(configEntity.getDataCombinerEntities()).isNotEmpty();
        final DataCombinerEntity combiner =
                configEntity.getDataCombinerEntities().get(0);
        assertThat(combiner.getDataCombiningEntities()).hasSizeGreaterThan(mappingIndex);
        return combiner.getDataCombiningEntities().get(mappingIndex);
    }
}
