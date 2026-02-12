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

import com.hivemq.combining.model.DataCombiner;
import com.hivemq.combining.model.DataCombining;
import com.hivemq.combining.model.DataIdentifierReference;
import com.hivemq.configuration.entity.adapter.ProtocolAdapterEntity;
import com.hivemq.configuration.entity.adapter.TagEntity;
import com.hivemq.configuration.reader.AssetMappingExtractor;
import com.hivemq.configuration.reader.DataCombiningExtractor;
import com.hivemq.configuration.reader.ProtocolAdapterExtractor;
import com.hivemq.persistence.mappings.fieldmapping.Instruction;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Migrates legacy combiner configurations that have TAG references without scope.
 * <p>
 * For legacy configs where instructions have TAG references with null scope:
 * <ul>
 *   <li>If the tag exists in exactly one adapter, backfill the scope with that adapter's ID</li>
 *   <li>If the tag exists in multiple adapters, log a warning</li>
 *   <li>If the tag doesn't exist in any adapter, log a warning</li>
 * </ul>
 */
@Singleton
public class DataCombiningScopeMigrator {

    private static final @NotNull Logger log = LoggerFactory.getLogger(DataCombiningScopeMigrator.class);

    private final @NotNull ProtocolAdapterExtractor protocolAdapterExtractor;
    private final @NotNull DataCombiningExtractor dataCombiningExtractor;
    private final @NotNull AssetMappingExtractor assetMappingExtractor;

    @Inject
    public DataCombiningScopeMigrator(
            final @NotNull ProtocolAdapterExtractor protocolAdapterExtractor,
            final @NotNull DataCombiningExtractor dataCombiningExtractor,
            final @NotNull AssetMappingExtractor assetMappingExtractor) {
        this.protocolAdapterExtractor = protocolAdapterExtractor;
        this.dataCombiningExtractor = dataCombiningExtractor;
        this.assetMappingExtractor = assetMappingExtractor;
    }

    /**
     * Migrates unscoped TAG references in all data combiners from both extractors.
     * This method should be called once at startup after all configs are loaded.
     */
    public void migrateUnscopedTags() {
        final Map<String, List<String>> tagToAdapters = buildTagToAdaptersMap();

        log.debug("Starting migration of unscoped TAG references. Found {} unique tags across all adapters.",
                tagToAdapters.size());

        // Migrate combiners from DataCombiningExtractor
        migrateExtractor(
                dataCombiningExtractor::getAllCombiners,
                dataCombiningExtractor::updateDataCombiners,
                "DataCombiningExtractor",
                tagToAdapters);

        // Migrate combiners from AssetMappingExtractor
        migrateExtractor(
                assetMappingExtractor::getAllCombiners,
                assetMappingExtractor::updateDataCombiners,
                "AssetMappingExtractor",
                tagToAdapters);
    }

    /**
     * Builds a map of tag name to list of adapter IDs that define that tag.
     */
    private @NotNull Map<String, List<String>> buildTagToAdaptersMap() {
        final Map<String, List<String>> tagToAdapters = new HashMap<>();
        for (final ProtocolAdapterEntity adapter : protocolAdapterExtractor.getAllConfigs()) {
            for (final TagEntity tag : adapter.getTags()) {
                tagToAdapters.computeIfAbsent(tag.getName(), k -> new ArrayList<>())
                        .add(adapter.getAdapterId());
            }
        }
        return tagToAdapters;
    }

    /**
     * Migrates combiners from a specific extractor.
     */
    private void migrateExtractor(
            final @NotNull Supplier<List<DataCombiner>> getAllCombiners,
            final @NotNull Function<List<DataCombiner>, Integer> updateCombiners,
            final @NotNull String extractorName,
            final @NotNull Map<String, List<String>> tagToAdapters) {
        final List<DataCombiner> migratedCombiners = new ArrayList<>();
        for (final DataCombiner combiner : getAllCombiners.get()) {
            final DataCombiner migratedCombiner = migrateCombiner(combiner, tagToAdapters);
            if (migratedCombiner != combiner) {
                migratedCombiners.add(migratedCombiner);
            }
        }
        if (!migratedCombiners.isEmpty()) {
            final int updatedCount = updateCombiners.apply(migratedCombiners);
            log.info("Migrated {} combiner(s) from {} with TAG scopes.", updatedCount, extractorName);
        }
    }

    /**
     * Migrates a single combiner, returning the updated combiner if any changes were made,
     * or the original combiner if no changes were needed.
     */
    private @NotNull DataCombiner migrateCombiner(
            final @NotNull DataCombiner combiner,
            final @NotNull Map<String, List<String>> tagToAdapters) {

        boolean anyChanges = false;
        final List<DataCombining> migratedDataCombinings = new ArrayList<>();

        for (final DataCombining dataCombining : combiner.dataCombinings()) {
            final DataCombining migratedDataCombining = migrateDataCombining(combiner, dataCombining, tagToAdapters);
            migratedDataCombinings.add(migratedDataCombining);
            if (migratedDataCombining != dataCombining) {
                anyChanges = true;
            }
        }

        if (anyChanges) {
            return new DataCombiner(
                    combiner.id(),
                    combiner.name(),
                    combiner.description(),
                    combiner.entityReferences(),
                    migratedDataCombinings);
        }
        return combiner;
    }

    /**
     * Migrates a single DataCombining, returning the updated version if any changes were made.
     */
    private @NotNull DataCombining migrateDataCombining(
            final @NotNull DataCombiner combiner,
            final @NotNull DataCombining dataCombining,
            final @NotNull Map<String, List<String>> tagToAdapters) {

        boolean anyChanges = false;
        final List<Instruction> migratedInstructions = new ArrayList<>();

        for (final Instruction instruction : dataCombining.instructions()) {
            final Instruction migratedInstruction = migrateInstruction(combiner, instruction, tagToAdapters);
            migratedInstructions.add(migratedInstruction);
            if (migratedInstruction != instruction) {
                anyChanges = true;
            }
        }

        if (anyChanges) {
            return new DataCombining(
                    dataCombining.id(),
                    dataCombining.sources(),
                    dataCombining.destination(),
                    migratedInstructions);
        }
        return dataCombining;
    }

    /**
     * Migrates a single instruction, returning the updated version if the TAG reference
     * was migrated with a scope, or the original instruction if no changes were needed.
     */
    private @NotNull Instruction migrateInstruction(
            final @NotNull DataCombiner combiner,
            final @NotNull Instruction instruction,
            final @NotNull Map<String, List<String>> tagToAdapters) {

        final DataIdentifierReference ref = instruction.dataIdentifierReference();

        // Skip if no reference, not a TAG type, or already has scope
        if (ref == null ||
                ref.type() != DataIdentifierReference.Type.TAG ||
                (ref.scope() != null && !ref.scope().isBlank())) {
            return instruction;
        }

        final String tagName = ref.id();
        final List<String> adapters = tagToAdapters.get(tagName);

        if (adapters == null || adapters.isEmpty()) {
            // Case C: Tag not found in any adapter
            log.warn("Legacy combiner '{}' ({}) has TAG reference '{}' without scope, " +
                            "and tag not found in any adapter. The combiner may not function correctly.",
                    combiner.name(), combiner.id(), tagName);
            return instruction;
        }

        if (adapters.size() == 1) {
            // Case A: Tag found in exactly one adapter - backfill scope
            final String adapterId = adapters.get(0);
            log.info("Migrated TAG reference '{}' in combiner '{}' ({}) to scope '{}'.",
                    tagName, combiner.name(), combiner.id(), adapterId);
            return new Instruction(
                    instruction.sourceFieldName(),
                    instruction.destinationFieldName(),
                    new DataIdentifierReference(ref.id(), ref.type(), adapterId));
        }

        // Case B: Tag found in multiple adapters
        log.warn("Legacy combiner '{}' ({}) has TAG reference '{}' without scope, " +
                        "but tag exists in multiple adapters: {}. " +
                        "Please update the combiner via UI/API to specify the correct adapter.",
                combiner.name(), combiner.id(), tagName, adapters);
        return instruction;
    }
}
