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
import com.hivemq.combining.model.DataCombiningSources;
import com.hivemq.combining.model.DataIdentifierReference;
import com.hivemq.configuration.entity.adapter.ProtocolAdapterEntity;
import com.hivemq.configuration.entity.adapter.TagEntity;
import com.hivemq.configuration.reader.ConfigFileReaderWriter;
import com.hivemq.persistence.mappings.fieldmapping.Instruction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Migrates legacy combiner configurations that have TAG references without scope.
 * <p>
 * For legacy configs where primary or instruction TAG references have null scope:
 * <ul>
 *   <li>If the tag exists in exactly one adapter, backfill the scope with that adapter's ID</li>
 *   <li>If the tag exists in multiple adapters, log a warning</li>
 *   <li>If the tag doesn't exist in any adapter, log a warning</li>
 * </ul>
 * <p>
 * This migrator is invoked as a post-apply callback on {@link ConfigFileReaderWriter},
 * so it runs both at startup and on every hot reload. When tags are migrated,
 * the corrected config is persisted to disk via the extractors.
 */
public class DataCombiningScopeMigrator {

    private static final @NotNull Logger log = LoggerFactory.getLogger(DataCombiningScopeMigrator.class);

    /**
     * Migrates unscoped TAG references in all data combiners.
     * Reads adapters and combiners from the given {@link ConfigFileReaderWriter},
     * and writes back migrated combiners (which triggers persistence to disk).
     */
    public void migrateUnscopedTags(final @NotNull ConfigFileReaderWriter configFileReaderWriter) {
        final Map<String, List<String>> tagToAdapters = buildTagToAdaptersMap(
                configFileReaderWriter.getProtocolAdapterExtractor().getAllConfigs());

        log.debug(
                "Starting migration of unscoped TAG references. Found {} unique tags across all adapters.",
                tagToAdapters.size());

        final var dataCombiningExtractor = configFileReaderWriter.getDataCombiningExtractor();
        final var assetMappingExtractor = configFileReaderWriter.getAssetMappingExtractor();

        migrateExtractor(
                dataCombiningExtractor::getAllCombiners,
                dataCombiningExtractor::updateDataCombiners,
                "DataCombiningExtractor",
                tagToAdapters);

        migrateExtractor(
                assetMappingExtractor::getAllCombiners,
                assetMappingExtractor::updateDataCombiners,
                "AssetMappingExtractor",
                tagToAdapters);
    }

    /**
     * Builds a map of tag name to list of adapter IDs that define that tag.
     */
    private @NotNull Map<String, List<String>> buildTagToAdaptersMap(
            final @NotNull List<ProtocolAdapterEntity> adapters) {
        final Map<String, List<String>> tagToAdapters = new HashMap<>();
        for (final ProtocolAdapterEntity adapter : adapters) {
            for (final TagEntity tag : adapter.getTags()) {
                tagToAdapters
                        .computeIfAbsent(tag.getName(), k -> new ArrayList<>())
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
            if (!migratedCombiner.equals(combiner)) {
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
            final @NotNull DataCombiner combiner, final @NotNull Map<String, List<String>> tagToAdapters) {

        boolean anyChanges = false;
        final List<DataCombining> migratedDataCombinings = new ArrayList<>();

        for (final DataCombining dataCombining : combiner.dataCombinings()) {
            final DataCombining migratedDataCombining = migrateDataCombining(combiner, dataCombining, tagToAdapters);
            migratedDataCombinings.add(migratedDataCombining);
            if (!migratedDataCombining.equals(dataCombining)) {
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
     * Migrates both the primary reference and instruction references.
     */
    private @NotNull DataCombining migrateDataCombining(
            final @NotNull DataCombiner combiner,
            final @NotNull DataCombining dataCombining,
            final @NotNull Map<String, List<String>> tagToAdapters) {

        boolean anyChanges = false;

        // Migrate the primary reference
        final DataIdentifierReference primaryRef = dataCombining.sources().primaryReference();
        final DataIdentifierReference migratedPrimaryRef = migrateTagReference(combiner, primaryRef, tagToAdapters);
        final boolean primaryChanged = !Objects.equals(primaryRef, migratedPrimaryRef);
        if (primaryChanged) {
            anyChanges = true;
        }

        // Migrate instructions
        final List<Instruction> migratedInstructions = new ArrayList<>();
        for (final Instruction instruction : dataCombining.instructions()) {
            final Instruction migratedInstruction = migrateInstruction(combiner, instruction, tagToAdapters);
            migratedInstructions.add(migratedInstruction);
            if (!migratedInstruction.equals(instruction)) {
                anyChanges = true;
            }
        }

        if (anyChanges) {
            final DataCombiningSources migratedSources = primaryChanged
                    ? new DataCombiningSources(
                            migratedPrimaryRef,
                            dataCombining.sources().tags(),
                            dataCombining.sources().topicFilters())
                    : dataCombining.sources();
            return new DataCombining(
                    dataCombining.id(), migratedSources, dataCombining.destination(), migratedInstructions);
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

        final DataIdentifierReference dataIdentifierReference = instruction.dataIdentifierReference();
        if (dataIdentifierReference == null) {
            return instruction;
        }

        final DataIdentifierReference migratedRef =
                migrateTagReference(combiner, dataIdentifierReference, tagToAdapters);
        if (Objects.equals(dataIdentifierReference, migratedRef)) {
            return instruction;
        }
        return new Instruction(instruction.sourceFieldName(), instruction.destinationFieldName(), migratedRef);
    }

    /**
     * Migrates a single TAG reference by backfilling scope if it's missing.
     * Returns the original reference if no migration is needed.
     */
    private @Nullable DataIdentifierReference migrateTagReference(
            final @NotNull DataCombiner combiner,
            final @Nullable DataIdentifierReference dataIdentifierReference,
            final @NotNull Map<String, List<String>> tagToAdapters) {

        // Skip if not a TAG type or already has scope
        if (dataIdentifierReference == null
                || dataIdentifierReference.type() != DataIdentifierReference.Type.TAG
                || (dataIdentifierReference.scope() != null
                        && !dataIdentifierReference.scope().isBlank())) {
            return dataIdentifierReference;
        }

        final String tagName = dataIdentifierReference.id();
        final List<String> adapters = tagToAdapters.get(tagName);

        if (adapters == null || adapters.isEmpty()) {
            log.warn(
                    "Legacy combiner '{}' ({}) has TAG reference '{}' without scope, "
                            + "and tag not found in any adapter. The combiner may not function correctly.",
                    combiner.name(),
                    combiner.id(),
                    tagName);
            return dataIdentifierReference;
        }

        if (adapters.size() == 1) {
            final String adapterId = adapters.getFirst();
            log.info(
                    "Migrated TAG reference '{}' in combiner '{}' ({}) to scope '{}'.",
                    tagName,
                    combiner.name(),
                    combiner.id(),
                    adapterId);
            return new DataIdentifierReference(dataIdentifierReference.id(), dataIdentifierReference.type(), adapterId);
        }

        log.warn(
                "Legacy combiner '{}' ({}) has TAG reference '{}' without scope, "
                        + "but tag exists in multiple adapters: {}. "
                        + "Please update the combiner via UI/API to specify the correct adapter.",
                combiner.name(),
                combiner.id(),
                tagName,
                adapters);
        return dataIdentifierReference;
    }
}
