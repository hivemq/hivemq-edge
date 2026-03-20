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
package com.hivemq.configuration.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hivemq.combining.model.DataCombiner;
import com.hivemq.combining.model.DataCombining;
import com.hivemq.combining.model.DataCombiningDestination;
import com.hivemq.combining.model.DataCombiningSources;
import com.hivemq.combining.model.DataIdentifierReference;
import com.hivemq.configuration.entity.adapter.ProtocolAdapterEntity;
import com.hivemq.configuration.entity.adapter.TagEntity;
import com.hivemq.configuration.reader.AssetMappingExtractor;
import com.hivemq.configuration.reader.DataCombiningExtractor;
import com.hivemq.configuration.reader.ProtocolAdapterExtractor;
import com.hivemq.persistence.mappings.fieldmapping.Instruction;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class DataCombiningScopeMigratorTest {

    private final @NotNull ProtocolAdapterExtractor protocolAdapterExtractor = mock(ProtocolAdapterExtractor.class);
    private final @NotNull DataCombiningExtractor dataCombiningExtractor = mock(DataCombiningExtractor.class);
    private final @NotNull AssetMappingExtractor assetMappingExtractor = mock(AssetMappingExtractor.class);

    private @NonNull DataCombiningScopeMigrator migrator;

    @BeforeEach
    void setUp() {
        migrator =
                new DataCombiningScopeMigrator(protocolAdapterExtractor, dataCombiningExtractor, assetMappingExtractor);
    }

    // --- Primary reference migration tests ---

    @Test
    void migrateUnscopedTags_primaryTagWithoutScope_singleAdapter_migratesPrimary() {
        final ProtocolAdapterEntity adapter = adapterWithTags("adapter-1", "temperature");
        when(protocolAdapterExtractor.getAllConfigs()).thenReturn(List.of(adapter));

        final DataCombining dataCombining = dataCombiningWithPrimary(unscopedTagRef("temperature"), List.of());
        final DataCombiner combiner = combinerWith(dataCombining);

        when(dataCombiningExtractor.getAllCombiners()).thenReturn(List.of(combiner));
        when(dataCombiningExtractor.updateDataCombiners(anyList())).thenReturn(1);
        when(assetMappingExtractor.getAllCombiners()).thenReturn(List.of());

        migrator.migrateUnscopedTags();

        final ArgumentCaptor<List<DataCombiner>> captor = combinersCaptor();
        verify(dataCombiningExtractor).updateDataCombiners(captor.capture());

        final DataCombining migrated = captor.getValue().get(0).dataCombinings().get(0);
        assertThat(migrated.sources().primaryReference().scope()).isEqualTo("adapter-1");
    }

    @Test
    void migrateUnscopedTags_primaryTagWithoutScope_multipleAdapters_doesNotMigrate() {
        final ProtocolAdapterEntity adapter1 = adapterWithTags("adapter-1", "temperature");
        final ProtocolAdapterEntity adapter2 = adapterWithTags("adapter-2", "temperature");
        when(protocolAdapterExtractor.getAllConfigs()).thenReturn(List.of(adapter1, adapter2));

        final DataCombining dataCombining = dataCombiningWithPrimary(unscopedTagRef("temperature"), List.of());
        final DataCombiner combiner = combinerWith(dataCombining);

        when(dataCombiningExtractor.getAllCombiners()).thenReturn(List.of(combiner));
        when(assetMappingExtractor.getAllCombiners()).thenReturn(List.of());

        migrator.migrateUnscopedTags();

        verify(dataCombiningExtractor, never()).updateDataCombiners(anyList());
    }

    @Test
    void migrateUnscopedTags_primaryTagWithoutScope_tagNotFound_doesNotMigrate() {
        when(protocolAdapterExtractor.getAllConfigs()).thenReturn(List.of());

        final DataCombining dataCombining = dataCombiningWithPrimary(unscopedTagRef("nonexistent"), List.of());
        final DataCombiner combiner = combinerWith(dataCombining);

        when(dataCombiningExtractor.getAllCombiners()).thenReturn(List.of(combiner));
        when(assetMappingExtractor.getAllCombiners()).thenReturn(List.of());

        migrator.migrateUnscopedTags();

        verify(dataCombiningExtractor, never()).updateDataCombiners(anyList());
    }

    @Test
    void migrateUnscopedTags_primaryTagAlreadyHasScope_noMigration() {
        final ProtocolAdapterEntity adapter = adapterWithTags("adapter-1", "temperature");
        when(protocolAdapterExtractor.getAllConfigs()).thenReturn(List.of(adapter));

        final DataCombining dataCombining =
                dataCombiningWithPrimary(scopedTagRef("temperature", "adapter-1"), List.of());
        final DataCombiner combiner = combinerWith(dataCombining);

        when(dataCombiningExtractor.getAllCombiners()).thenReturn(List.of(combiner));
        when(assetMappingExtractor.getAllCombiners()).thenReturn(List.of());

        migrator.migrateUnscopedTags();

        verify(dataCombiningExtractor, never()).updateDataCombiners(anyList());
    }

    @Test
    void migrateUnscopedTags_primaryIsTopicFilter_noMigration() {
        final ProtocolAdapterEntity adapter = adapterWithTags("adapter-1", "temperature");
        when(protocolAdapterExtractor.getAllConfigs()).thenReturn(List.of(adapter));

        final DataCombining dataCombining = dataCombiningWithPrimary(
                new DataIdentifierReference("my/topic", DataIdentifierReference.Type.TOPIC_FILTER), List.of());
        final DataCombiner combiner = combinerWith(dataCombining);

        when(dataCombiningExtractor.getAllCombiners()).thenReturn(List.of(combiner));
        when(assetMappingExtractor.getAllCombiners()).thenReturn(List.of());

        migrator.migrateUnscopedTags();

        verify(dataCombiningExtractor, never()).updateDataCombiners(anyList());
    }

    // --- Combined primary + instruction migration tests ---

    @Test
    void migrateUnscopedTags_bothPrimaryAndInstructionUnscoped_migratesBoth() {
        final ProtocolAdapterEntity adapter = adapterWithTags("adapter-1", "temperature", "humidity");
        when(protocolAdapterExtractor.getAllConfigs()).thenReturn(List.of(adapter));

        final Instruction unscopedInstruction = new Instruction("$.value", "$.output", unscopedTagRef("humidity"));
        final DataCombining dataCombining =
                dataCombiningWithPrimary(unscopedTagRef("temperature"), List.of(unscopedInstruction));
        final DataCombiner combiner = combinerWith(dataCombining);

        when(dataCombiningExtractor.getAllCombiners()).thenReturn(List.of(combiner));
        when(dataCombiningExtractor.updateDataCombiners(anyList())).thenReturn(1);
        when(assetMappingExtractor.getAllCombiners()).thenReturn(List.of());

        migrator.migrateUnscopedTags();

        final ArgumentCaptor<List<DataCombiner>> captor = combinersCaptor();
        verify(dataCombiningExtractor).updateDataCombiners(captor.capture());

        final DataCombining migrated = captor.getValue().get(0).dataCombinings().get(0);
        assertThat(migrated.sources().primaryReference().scope()).isEqualTo("adapter-1");
        assertThat(migrated.instructions().get(0).dataIdentifierReference().scope())
                .isEqualTo("adapter-1");
    }

    @Test
    void migrateUnscopedTags_onlyPrimaryUnscoped_instructionAlreadyScoped_migratesOnlyPrimary() {
        final ProtocolAdapterEntity adapter = adapterWithTags("adapter-1", "temperature", "humidity");
        when(protocolAdapterExtractor.getAllConfigs()).thenReturn(List.of(adapter));

        final Instruction scopedInstruction =
                new Instruction("$.value", "$.output", scopedTagRef("humidity", "adapter-1"));
        final DataCombining dataCombining =
                dataCombiningWithPrimary(unscopedTagRef("temperature"), List.of(scopedInstruction));
        final DataCombiner combiner = combinerWith(dataCombining);

        when(dataCombiningExtractor.getAllCombiners()).thenReturn(List.of(combiner));
        when(dataCombiningExtractor.updateDataCombiners(anyList())).thenReturn(1);
        when(assetMappingExtractor.getAllCombiners()).thenReturn(List.of());

        migrator.migrateUnscopedTags();

        final ArgumentCaptor<List<DataCombiner>> captor = combinersCaptor();
        verify(dataCombiningExtractor).updateDataCombiners(captor.capture());

        final DataCombining migrated = captor.getValue().get(0).dataCombinings().get(0);
        assertThat(migrated.sources().primaryReference().scope()).isEqualTo("adapter-1");
        // instruction was already scoped, should remain unchanged
        assertThat(migrated.instructions().get(0).dataIdentifierReference().scope())
                .isEqualTo("adapter-1");
    }

    @Test
    void migrateUnscopedTags_primaryScoped_instructionUnscoped_migratesOnlyInstruction() {
        final ProtocolAdapterEntity adapter = adapterWithTags("adapter-1", "temperature", "humidity");
        when(protocolAdapterExtractor.getAllConfigs()).thenReturn(List.of(adapter));

        final Instruction unscopedInstruction = new Instruction("$.value", "$.output", unscopedTagRef("humidity"));
        final DataCombining dataCombining =
                dataCombiningWithPrimary(scopedTagRef("temperature", "adapter-1"), List.of(unscopedInstruction));
        final DataCombiner combiner = combinerWith(dataCombining);

        when(dataCombiningExtractor.getAllCombiners()).thenReturn(List.of(combiner));
        when(dataCombiningExtractor.updateDataCombiners(anyList())).thenReturn(1);
        when(assetMappingExtractor.getAllCombiners()).thenReturn(List.of());

        migrator.migrateUnscopedTags();

        final ArgumentCaptor<List<DataCombiner>> captor = combinersCaptor();
        verify(dataCombiningExtractor).updateDataCombiners(captor.capture());

        final DataCombining migrated = captor.getValue().get(0).dataCombinings().get(0);
        // primary was already scoped, should remain unchanged
        assertThat(migrated.sources().primaryReference().scope()).isEqualTo("adapter-1");
        assertThat(migrated.instructions().get(0).dataIdentifierReference().scope())
                .isEqualTo("adapter-1");
    }

    @Test
    void migrateUnscopedTags_primaryMigrated_preservesSourcesTagsAndTopicFilters() {
        final ProtocolAdapterEntity adapter = adapterWithTags("adapter-1", "temperature");
        when(protocolAdapterExtractor.getAllConfigs()).thenReturn(List.of(adapter));

        final DataCombiningSources sources =
                new DataCombiningSources(unscopedTagRef("temperature"), List.of("temperature"), List.of("my/topic"));
        final DataCombining dataCombining = new DataCombining(
                UUID.randomUUID(), sources, new DataCombiningDestination(null, "dest/topic", null), List.of());
        final DataCombiner combiner = combinerWith(dataCombining);

        when(dataCombiningExtractor.getAllCombiners()).thenReturn(List.of(combiner));
        when(dataCombiningExtractor.updateDataCombiners(anyList())).thenReturn(1);
        when(assetMappingExtractor.getAllCombiners()).thenReturn(List.of());

        migrator.migrateUnscopedTags();

        final ArgumentCaptor<List<DataCombiner>> captor = combinersCaptor();
        verify(dataCombiningExtractor).updateDataCombiners(captor.capture());

        final DataCombiningSources migratedSources =
                captor.getValue().get(0).dataCombinings().get(0).sources();
        assertThat(migratedSources.primaryReference().scope()).isEqualTo("adapter-1");
        assertThat(migratedSources.tags()).containsExactly("temperature");
        assertThat(migratedSources.topicFilters()).containsExactly("my/topic");
    }

    // --- Instruction-only migration tests (existing behavior) ---

    @Test
    void migrateUnscopedTags_instructionWithNullRef_noMigration() {
        final ProtocolAdapterEntity adapter = adapterWithTags("adapter-1", "temperature");
        when(protocolAdapterExtractor.getAllConfigs()).thenReturn(List.of(adapter));

        final Instruction noRefInstruction = new Instruction("$.value", "$.output", null);
        final DataCombining dataCombining =
                dataCombiningWithPrimary(scopedTagRef("temperature", "adapter-1"), List.of(noRefInstruction));
        final DataCombiner combiner = combinerWith(dataCombining);

        when(dataCombiningExtractor.getAllCombiners()).thenReturn(List.of(combiner));
        when(assetMappingExtractor.getAllCombiners()).thenReturn(List.of());

        migrator.migrateUnscopedTags();

        verify(dataCombiningExtractor, never()).updateDataCombiners(anyList());
    }

    @Test
    void migrateUnscopedTags_assetMappingExtractorAlsoMigrated() {
        final ProtocolAdapterEntity adapter = adapterWithTags("adapter-1", "temperature");
        when(protocolAdapterExtractor.getAllConfigs()).thenReturn(List.of(adapter));

        final DataCombining dataCombining = dataCombiningWithPrimary(unscopedTagRef("temperature"), List.of());
        final DataCombiner combiner = combinerWith(dataCombining);

        when(dataCombiningExtractor.getAllCombiners()).thenReturn(List.of());
        when(assetMappingExtractor.getAllCombiners()).thenReturn(List.of(combiner));
        when(assetMappingExtractor.updateDataCombiners(anyList())).thenReturn(1);

        migrator.migrateUnscopedTags();

        final ArgumentCaptor<List<DataCombiner>> captor = combinersCaptor();
        verify(assetMappingExtractor).updateDataCombiners(captor.capture());

        final DataCombining migrated = captor.getValue().get(0).dataCombinings().get(0);
        assertThat(migrated.sources().primaryReference().scope()).isEqualTo("adapter-1");
    }

    // --- Helper methods ---

    private static @NotNull DataIdentifierReference unscopedTagRef(final @NotNull String tagName) {
        return new DataIdentifierReference(tagName, DataIdentifierReference.Type.TAG);
    }

    private static @NotNull DataIdentifierReference scopedTagRef(
            final @NotNull String tagName, final @NotNull String scope) {
        return new DataIdentifierReference(tagName, DataIdentifierReference.Type.TAG, scope);
    }

    private static @NotNull ProtocolAdapterEntity adapterWithTags(
            final @NotNull String adapterId, final @NotNull String... tagNames) {
        final List<TagEntity> tags = new java.util.ArrayList<>();
        for (final String tagName : tagNames) {
            tags.add(new TagEntity(tagName, "", Map.of()));
        }
        return new ProtocolAdapterEntity(adapterId, "test-protocol", 1, Map.of(), List.of(), List.of(), tags);
    }

    private static @NotNull DataCombining dataCombiningWithPrimary(
            final @NotNull DataIdentifierReference primaryRef, final @NotNull List<Instruction> instructions) {
        return new DataCombining(
                UUID.randomUUID(),
                new DataCombiningSources(primaryRef, List.of(), List.of()),
                new DataCombiningDestination(null, "dest/topic", null),
                instructions);
    }

    private static @NotNull DataCombiner combinerWith(final @NotNull DataCombining... dataCombinings) {
        return new DataCombiner(
                UUID.randomUUID(), "test-combiner", "test description", List.of(), List.of(dataCombinings));
    }

    @SuppressWarnings("unchecked")
    private static @NotNull ArgumentCaptor<List<DataCombiner>> combinersCaptor() {
        return ArgumentCaptor.forClass(List.class);
    }
}
