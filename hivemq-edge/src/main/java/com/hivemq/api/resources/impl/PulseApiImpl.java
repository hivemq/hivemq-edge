/*
 *  Copyright 2019-present HiveMQ GmbH
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.hivemq.api.resources.impl;

import com.google.common.collect.Sets;
import com.hivemq.api.errors.AlreadyExistsError;
import com.hivemq.api.errors.ConfigWritingDisabled;
import com.hivemq.api.errors.InternalServerError;
import com.hivemq.api.errors.pulse.ActivationTokenAlreadyDeletedError;
import com.hivemq.api.errors.pulse.ActivationTokenInvalidError;
import com.hivemq.api.errors.pulse.ActivationTokenNotDeletedError;
import com.hivemq.api.errors.pulse.AssetMapperNotFoundError;
import com.hivemq.api.errors.pulse.AssetMapperReferencedError;
import com.hivemq.api.errors.pulse.DuplicatedManagedAssetIdError;
import com.hivemq.api.errors.pulse.EmptyMappingsForAssetMapperError;
import com.hivemq.api.errors.pulse.EmptySourcesForAssetMapperError;
import com.hivemq.api.errors.pulse.InvalidDataIdentifierReferenceForAssetMapperError;
import com.hivemq.api.errors.pulse.InvalidManagedAssetMappingIdError;
import com.hivemq.api.errors.pulse.InvalidManagedAssetSchemaError;
import com.hivemq.api.errors.pulse.InvalidManagedAssetTopicError;
import com.hivemq.api.errors.pulse.ManagedAssetAlreadyExistsError;
import com.hivemq.api.errors.pulse.ManagedAssetNotFoundError;
import com.hivemq.api.errors.pulse.MissingEntityTypePulseAgentForAssetMapperError;
import com.hivemq.api.errors.pulse.PulseAgentDeactivatedError;
import com.hivemq.api.errors.pulse.PulseAgentNotConnectedError;
import com.hivemq.api.model.ItemsResponse;
import com.hivemq.combining.model.DataCombiner;
import com.hivemq.combining.model.EntityType;
import com.hivemq.configuration.entity.pulse.PulseAssetEntity;
import com.hivemq.configuration.entity.pulse.PulseEntity;
import com.hivemq.configuration.info.SystemInformation;
import com.hivemq.configuration.reader.AssetMappingExtractor;
import com.hivemq.configuration.reader.PulseExtractor;
import com.hivemq.edge.api.PulseApi;
import com.hivemq.edge.api.model.Combiner;
import com.hivemq.edge.api.model.CombinerList;
import com.hivemq.edge.api.model.DataCombining;
import com.hivemq.edge.api.model.DataCombiningList;
import com.hivemq.edge.api.model.Instruction;
import com.hivemq.edge.api.model.ManagedAsset;
import com.hivemq.edge.api.model.ManagedAssetList;
import com.hivemq.edge.api.model.PulseActivationToken;
import com.hivemq.edge.api.model.PulseStatus;
import com.hivemq.pulse.asset.AssetProviderRegistry;
import com.hivemq.pulse.asset.PulseAgentAsset;
import com.hivemq.pulse.asset.PulseAgentAssetMapping;
import com.hivemq.pulse.asset.PulseAgentAssets;
import com.hivemq.pulse.converters.PulseAgentAssetMappingStatusConverter;
import com.hivemq.pulse.converters.PulseAgentAssetSchemaConverter;
import com.hivemq.pulse.converters.PulseAgentAssetsConverter;
import com.hivemq.pulse.converters.PulseAgentStatusConverter;
import com.hivemq.pulse.status.Status;
import com.hivemq.pulse.status.StatusProvider;
import com.hivemq.pulse.status.StatusProviderRegistry;
import com.hivemq.pulse.utils.PulseAgentAssetUtils;
import com.hivemq.util.ErrorResponseUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.core.Response;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * The type Pulse api.
 */
@Singleton
public class PulseApiImpl implements PulseApi {
    private static final @NotNull Logger LOGGER = LoggerFactory.getLogger(PulseApiImpl.class);
    private final @NotNull AssetProviderRegistry assetProviderRegistry;
    private final @NotNull AssetMappingExtractor assetMappingExtractor;
    private final @NotNull PulseExtractor pulseExtractor;
    private final @NotNull StatusProviderRegistry statusProviderRegistry;
    private final @NotNull SystemInformation systemInformation;

    @Inject
    public PulseApiImpl(
            final @NotNull SystemInformation systemInformation,
            final @NotNull AssetMappingExtractor assetMappingExtractor,
            final @NotNull PulseExtractor pulseExtractor,
            final @NotNull AssetProviderRegistry assetProviderRegistry,
            final @NotNull StatusProviderRegistry statusProviderRegistry) {
        this.assetProviderRegistry = assetProviderRegistry;
        this.assetMappingExtractor = assetMappingExtractor;
        this.pulseExtractor = pulseExtractor;
        this.statusProviderRegistry = statusProviderRegistry;
        this.systemInformation = systemInformation;
    }

    @Override
    public @NotNull Response addAssetMapper(final @NotNull Combiner combiner) {
        final Optional<Response> optionalResponse = check(this::checkConfigWritable);
        if (optionalResponse.isPresent()) {
            return optionalResponse.get();
        }
        final PulseEntity existingPulseEntity = pulseExtractor.getPulseEntity();
        synchronized (existingPulseEntity.getLock()) {
            final UUID id = combiner.getId();
            final @NotNull Optional<DataCombiner> optionalDataCombiner = assetMappingExtractor.getCombinerById(id);
            if (optionalDataCombiner.isPresent()) {
                return ErrorResponseUtil.errorResponse(new AlreadyExistsError(String.format(
                        "Asset mapper already exists '%s'",
                        id)));
            }
            final DataCombiner dataCombiner = DataCombiner.fromModel(combiner);
            final Map<String, PulseAssetEntity> assetEntityMap =
                    PulseAgentAssetUtils.toAssetEntityMap(pulseExtractor.getPulseEntity());
            final Optional<Response> optionalResponseDataCombiner =
                    checkAssetMapper(dataCombiner, null, assetEntityMap);
            if (optionalResponseDataCombiner.isPresent()) {
                return optionalResponseDataCombiner.get();
            }
            final PulseAgentAssets assets =
                    PulseAgentAssets.fromPersistence(existingPulseEntity.getPulseAssetsEntity());
            final Map<String, com.hivemq.combining.model.DataCombining> dataCombiningMap = dataCombiner.dataCombinings()
                    .stream()
                    .collect(Collectors.toMap(dataCombining -> dataCombining.destination().assetId(),
                            Function.identity()));
            final PulseAgentAssets newAssets = new PulseAgentAssets();
            final AtomicBoolean assetUpdated = new AtomicBoolean(false);
            assets.stream()
                    .map(asset -> Optional.ofNullable(dataCombiningMap.get(asset.getId().toString()))
                            .map(dataCombining -> {
                                if (asset.getMapping().getId() == null) {
                                    assetUpdated.set(true);
                                    return asset.withMapping(asset.getMapping().withId(dataCombining.id()));
                                }
                                return asset;
                            })
                            .orElse(asset))
                    .forEach(newAssets::add);
            if (assetUpdated.get()) {
                final PulseEntity newPulseEntity = new PulseEntity(newAssets.toPersistence());
                pulseExtractor.setPulseEntity(newPulseEntity);
            }
            try {
                assetMappingExtractor.addDataCombiner(dataCombiner);
            } catch (final Exception e) {
                final Throwable cause = e.getCause();
                if (cause instanceof IllegalArgumentException) {
                    LOGGER.warn("Exception occurred during addition of data combining '{}':",
                            combiner.getName(),
                            cause);
                }
                return ErrorResponseUtil.errorResponse(new InternalServerError("Exception during add of data combiner."));
            }
        }
        return Response.ok().build();
    }

    @Override
    public @NotNull Response addManagedAsset(final @NotNull ManagedAsset managedAsset) {
        final Optional<Response> optionalResponse = check(this::checkStatus, this::checkConfigWritable);
        if (optionalResponse.isPresent()) {
            return optionalResponse.get();
        }
        final UUID mappingId = managedAsset.getMapping().getMappingId();
        if (mappingId == null) {
            return ErrorResponseUtil.errorResponse(new InvalidManagedAssetMappingIdError("null"));
        }
        final PulseEntity existingPulseEntity = pulseExtractor.getPulseEntity();
        synchronized (existingPulseEntity.getLock()) {
            final PulseAgentAssets assets =
                    PulseAgentAssets.fromPersistence(existingPulseEntity.getPulseAssetsEntity());
            final OptionalInt optionalAssetIndex = IntStream.range(0, assets.size())
                    .filter(i -> Objects.equals(assets.get(i).getId(), managedAsset.getId()))
                    .findAny();
            if (optionalAssetIndex.isEmpty()) {
                return ErrorResponseUtil.errorResponse(new ManagedAssetNotFoundError(managedAsset.getId()));
            }
            if (assets.getMappingIdSet().contains(mappingId.toString())) {
                return ErrorResponseUtil.errorResponse(new InvalidManagedAssetMappingIdError(mappingId));
            }
            if (assetMappingExtractor.getMappingIdSet().contains(mappingId.toString())) {
                return ErrorResponseUtil.errorResponse(new InvalidManagedAssetMappingIdError(mappingId));
            }
            final PulseAgentAsset asset = assets.get(optionalAssetIndex.getAsInt());
            if (asset.getMapping().getId() != null) {
                return ErrorResponseUtil.errorResponse(new ManagedAssetAlreadyExistsError());
            }
            final PulseAgentAsset newAsset = asset.withMapping(PulseAgentAssetMapping.builder()
                    .id(mappingId)
                    .status(PulseAgentAssetMappingStatusConverter.INSTANCE.toInternalEntity(managedAsset.getMapping()
                            .getStatus()))
                    .build());
            assets.set(optionalAssetIndex.getAsInt(), newAsset);
            final PulseEntity newPulseEntity = new PulseEntity(assets.toPersistence());
            pulseExtractor.setPulseEntity(newPulseEntity);
            notifyAssetMapper(newAsset);
            return Response.ok().build();
        }
    }

    @Override
    public @NotNull Response deleteAssetMapper(final @NotNull UUID combinerId) {
        final Optional<Response> optionalResponse = check(this::checkConfigWritable);
        if (optionalResponse.isPresent()) {
            return optionalResponse.get();
        }
        final PulseEntity existingPulseEntity = pulseExtractor.getPulseEntity();
        synchronized (existingPulseEntity.getLock()) {
            final @NotNull Optional<DataCombiner> optionalDataCombiner =
                    assetMappingExtractor.getCombinerById(combinerId);
            if (optionalDataCombiner.isEmpty()) {
                return ErrorResponseUtil.errorResponse(new AssetMapperNotFoundError(combinerId));
            }
            final DataCombiner dataCombiner = optionalDataCombiner.get();
            final PulseAgentAssets assets =
                    PulseAgentAssets.fromPersistence(existingPulseEntity.getPulseAssetsEntity());
            final Set<String> mappingIdSet = dataCombiner.getMappingIdSet();
            final PulseAgentAssets newAssets = new PulseAgentAssets();
            final AtomicBoolean updated = new AtomicBoolean(false);
            assets.stream().map(asset -> {
                final UUID mappingId = asset.getMapping().getId();
                if (mappingId != null && mappingIdSet.contains(mappingId.toString())) {
                    updated.set(true);
                    // Reset the asset mapping to mappingId = null and status = UNMAPPED.
                    return asset.withMapping(PulseAgentAssetMapping.builder().build());
                }
                return asset;
            }).forEach(newAssets::add);
            try {
                if (updated.get()) {
                    final PulseEntity newPulseEntity = new PulseEntity(newAssets.toPersistence());
                    pulseExtractor.setPulseEntity(newPulseEntity);
                }
                assetMappingExtractor.deleteDataCombiner(combinerId);
            } catch (final Exception e) {
                final Throwable cause = e.getCause();
                LOGGER.warn("Exception occurred during deletion of data combining '{}':", combinerId, cause);
                return ErrorResponseUtil.errorResponse(new InternalServerError(
                        "Exception during deletion of data combiner."));
            }
        }
        return Response.ok().build();
    }

    @Override
    public @NotNull Response deleteManagedAsset(final @NotNull UUID assetId) {
        final Optional<Response> optionalResponse = check(this::checkStatus, this::checkConfigWritable);
        if (optionalResponse.isPresent()) {
            return optionalResponse.get();
        }
        final PulseEntity existingPulseEntity = pulseExtractor.getPulseEntity();
        synchronized (existingPulseEntity.getLock()) {
            final PulseAgentAssets assets =
                    PulseAgentAssets.fromPersistence(existingPulseEntity.getPulseAssetsEntity());
            final OptionalInt optionalAssetIndex = IntStream.range(0, assets.size())
                    .filter(i -> Objects.equals(assets.get(i).getId(), assetId))
                    .findAny();
            if (optionalAssetIndex.isEmpty()) {
                return ErrorResponseUtil.errorResponse(new ManagedAssetNotFoundError(assetId));
            }
            final PulseAgentAsset asset = assets.get(optionalAssetIndex.getAsInt());
            final UUID mappingId = asset.getMapping().getId();
            if (mappingId != null && assetMappingExtractor.getMappingIdSet().contains(mappingId.toString())) {
                return ErrorResponseUtil.errorResponse(new AssetMapperReferencedError(assetId));
            }
            assets.remove(optionalAssetIndex.getAsInt());
            final PulseEntity newPulseEntity = new PulseEntity(assets.toPersistence());
            pulseExtractor.setPulseEntity(newPulseEntity);
            notifyAssetMapper(asset);
        }
        return Response.ok().build();
    }

    @Override
    public synchronized @NotNull Response deletePulseActivationToken() {
        final Optional<StatusProvider> optionalStatusProvider =
                statusProviderRegistry.getStatusProviders().stream().findFirst();
        if (optionalStatusProvider.isEmpty()) {
            return ErrorResponseUtil.errorResponse(new InternalServerError(null));
        }
        final StatusProvider statusProvider = optionalStatusProvider.get();
        if (statusProvider.getStatus().activationStatus() == Status.ActivationStatus.DEACTIVATED) {
            return ErrorResponseUtil.errorResponse(new ActivationTokenAlreadyDeletedError());
        }
        try {
            statusProvider.deactivatePulse();
        } catch (final Exception e) {
            return ErrorResponseUtil.errorResponse(new InternalServerError(e.getMessage()));
        }
        switch (statusProvider.getStatus().activationStatus()) {
            case DEACTIVATED -> {
                return Response.ok().build();
            }
            case ERROR -> {
                return ErrorResponseUtil.errorResponse(new ActivationTokenNotDeletedError());
            }
            default -> {
                return ErrorResponseUtil.errorResponse(new InternalServerError(null));
            }
        }
    }

    @Override
    public @NotNull Response getAssetMapper(final @NotNull UUID combinerId) {
        final PulseEntity existingPulseEntity = pulseExtractor.getPulseEntity();
        synchronized (existingPulseEntity.getLock()) {
            final @NotNull Optional<DataCombiner> optionalDataCombiner =
                    assetMappingExtractor.getCombinerById(combinerId);
            if (optionalDataCombiner.isEmpty()) {
                return ErrorResponseUtil.errorResponse(new AssetMapperNotFoundError(combinerId));
            }
            return Response.ok().entity(optionalDataCombiner.get().toModel()).build();
        }
    }

    @Override
    public @NotNull Response getAssetMapperInstructions(final @NotNull UUID combinerId, final @NotNull UUID mappingId) {
        final PulseEntity existingPulseEntity = pulseExtractor.getPulseEntity();
        synchronized (existingPulseEntity.getLock()) {
            final @NotNull Optional<DataCombiner> optionalDataCombiner =
                    assetMappingExtractor.getCombinerById(combinerId);
            if (optionalDataCombiner.isEmpty()) {
                return ErrorResponseUtil.errorResponse(new AssetMapperNotFoundError(combinerId));
            }
            final List<Instruction> instructions = optionalDataCombiner.get()
                    .dataCombinings()
                    .stream()
                    .filter(dataCombining -> Objects.equals(dataCombining.id(), mappingId))
                    .flatMap(dataCombining -> dataCombining.instructions().stream())
                    .map(com.hivemq.persistence.mappings.fieldmapping.Instruction::toModel)
                    .toList();
            return Response.ok().entity(new InstructionList(instructions)).build();
        }
    }

    @Override
    public @NotNull Response getAssetMapperMappings(final @NotNull UUID combinerId) {
        final PulseEntity existingPulseEntity = pulseExtractor.getPulseEntity();
        synchronized (existingPulseEntity.getLock()) {
            final @NotNull Optional<DataCombiner> optionalDataCombiner =
                    assetMappingExtractor.getCombinerById(combinerId);
            if (optionalDataCombiner.isEmpty()) {
                return ErrorResponseUtil.errorResponse(new AssetMapperNotFoundError(combinerId));
            }
            final List<DataCombining> dataCombiningList = optionalDataCombiner.get()
                    .dataCombinings()
                    .stream()
                    .map(com.hivemq.combining.model.DataCombining::toModel)
                    .toList();
            return Response.ok().entity(new DataCombiningList().items(dataCombiningList)).build();
        }
    }

    @Override
    public @NotNull Response getAssetMappers() {
        final PulseEntity existingPulseEntity = pulseExtractor.getPulseEntity();
        synchronized (existingPulseEntity.getLock()) {
            final List<DataCombiner> allCombiners = assetMappingExtractor.getAllCombiners();
            final List<Combiner> combiners = allCombiners.stream().map(DataCombiner::toModel).toList();
            final CombinerList combinerList = new CombinerList().items(combiners);
            return Response.ok().entity(combinerList).build();
        }
    }

    @Override
    public @NotNull Response getManagedAssets() {
        final Optional<Response> optionalResponse = check(this::checkStatus);
        if (optionalResponse.isPresent()) {
            return optionalResponse.get();
        }
        final PulseEntity pulseEntity = pulseExtractor.getPulseEntity();
        synchronized (pulseEntity.getLock()) {
            final PulseAgentAssets assets = PulseAgentAssets.fromPersistence(pulseEntity.getPulseAssetsEntity());
            final ManagedAssetList managedAssetList = PulseAgentAssetsConverter.INSTANCE.toRestEntity(assets);
            return Response.ok(managedAssetList).build();
        }
    }

    @Override
    public @NotNull Response getPulseStatus() {
        final Optional<StatusProvider> optionalStatusProvider =
                statusProviderRegistry.getStatusProviders().stream().findFirst();
        if (optionalStatusProvider.isEmpty()) {
            return ErrorResponseUtil.errorResponse(new InternalServerError(null));
        }
        final StatusProvider statusProvider = optionalStatusProvider.get();
        final Status status = statusProvider.getStatus();
        final PulseStatus pulseStatus = PulseAgentStatusConverter.INSTANCE.toRestEntity(status);
        return Response.ok(pulseStatus).build();
    }

    @Override
    public @NotNull Response updateAssetMapper(final @NotNull UUID combinerId, final @NotNull Combiner combiner) {
        final Optional<Response> optionalResponse = check(this::checkConfigWritable);
        if (optionalResponse.isPresent()) {
            return optionalResponse.get();
        }
        final PulseEntity existingPulseEntity = pulseExtractor.getPulseEntity();
        synchronized (existingPulseEntity.getLock()) {
            final @NotNull Optional<DataCombiner> optionalDataCombiner =
                    assetMappingExtractor.getCombinerById(combiner.getId());
            if (optionalDataCombiner.isEmpty()) {
                return ErrorResponseUtil.errorResponse(new AssetMapperNotFoundError(combiner.getId()));
            }
            final DataCombiner newDataCombiner = DataCombiner.fromModel(combiner);
            final DataCombiner oldDataCombiner = optionalDataCombiner.get();
            final Map<String, PulseAssetEntity> assetEntityMap =
                    PulseAgentAssetUtils.toAssetEntityMap(pulseExtractor.getPulseEntity());
            final Optional<Response> optionalResponseDataCombiner =
                    checkAssetMapper(newDataCombiner, oldDataCombiner, assetEntityMap);
            if (optionalResponseDataCombiner.isPresent()) {
                return optionalResponseDataCombiner.get();
            }
            final Set<String> oldMappingIdSet = oldDataCombiner.getMappingIdSet();
            final Set<String> newMappingIdSet = newDataCombiner.getMappingIdSet();
            final Set<String> toBeRemovedMappingIdSet = Sets.difference(oldMappingIdSet, newMappingIdSet);
            if (!toBeRemovedMappingIdSet.isEmpty()) {
                final PulseAgentAssets assets =
                        PulseAgentAssets.fromPersistence(existingPulseEntity.getPulseAssetsEntity());
                final PulseAgentAssets newAssets = new PulseAgentAssets();
                assets.stream()
                        .map(asset -> Optional.ofNullable(asset.getMapping().getId())
                                .filter(mappingId -> toBeRemovedMappingIdSet.contains(mappingId.toString()))
                                .map(mappingId -> asset.withMapping(PulseAgentAssetMapping.builder().build()))
                                .orElse(asset))
                        .forEach(newAssets::add);
                final PulseEntity newPulseEntity = new PulseEntity(newAssets.toPersistence());
                pulseExtractor.setPulseEntity(newPulseEntity);
            }
            assetMappingExtractor.updateDataCombiner(newDataCombiner);
            return Response.ok().build();
        }
    }

    @Override
    public @NotNull Response updateManagedAsset(final @NotNull UUID assetId, final @NotNull ManagedAsset managedAsset) {
        final Optional<Response> optionalResponse = check(this::checkStatus, this::checkConfigWritable);
        if (optionalResponse.isPresent()) {
            return optionalResponse.get();
        }
        final PulseEntity existingPulseEntity = pulseExtractor.getPulseEntity();
        synchronized (existingPulseEntity.getLock()) {
            final PulseAgentAssets assets =
                    PulseAgentAssets.fromPersistence(existingPulseEntity.getPulseAssetsEntity());
            final OptionalInt optionalAssetIndex = IntStream.range(0, assets.size())
                    .filter(i -> Objects.equals(assets.get(i).getId(), assetId))
                    .findAny();
            if (optionalAssetIndex.isEmpty()) {
                return ErrorResponseUtil.errorResponse(new ManagedAssetNotFoundError(assetId));
            }
            final PulseAgentAsset asset = assets.get(optionalAssetIndex.getAsInt());
            if (asset.getMapping().getId() == null) {
                // Existing mapping ID cannot be null.
                return ErrorResponseUtil.errorResponse(new InvalidManagedAssetMappingIdError("null"));
            }
            final PulseAgentAsset newAsset = asset.withMapping(asset.getMapping()
                    .withStatus(PulseAgentAssetMappingStatusConverter.INSTANCE.toInternalEntity(managedAsset.getMapping()
                            .getStatus())));
            assets.set(optionalAssetIndex.getAsInt(), newAsset);
            final PulseEntity newPulseEntity = new PulseEntity(assets.toPersistence());
            pulseExtractor.setPulseEntity(newPulseEntity);
            notifyAssetMapper(newAsset);
            return Response.ok().build();
        }
    }

    /*
     * Updates the pulse activation token and notifies all status providers to activate pulse with the new token.
     * This method is synchronized to ensure thread safety when updating the token.
     */
    @Override
    public synchronized @NotNull Response updatePulseActivationToken(final @NotNull PulseActivationToken pulseActivationToken) {
        final String token = pulseActivationToken.getToken();
        try {
            final Optional<StatusProvider> optionalStatusProvider =
                    statusProviderRegistry.getStatusProviders().stream().findFirst();
            if (optionalStatusProvider.isEmpty()) {
                return ErrorResponseUtil.errorResponse(new InternalServerError(null));
            }
            final StatusProvider statusProvider = optionalStatusProvider.get();
            final boolean tokenValid = statusProvider.activatePulse(token);
            if (!tokenValid) {
                return ErrorResponseUtil.errorResponse(new ActivationTokenInvalidError());
            }
            switch (statusProvider.getStatus().activationStatus()) {
                case ACTIVATED -> {
                    return Response.ok().build();
                }
                case DEACTIVATED -> {
                    return ErrorResponseUtil.errorResponse(new PulseAgentDeactivatedError());
                }
                default -> {
                    return ErrorResponseUtil.errorResponse(new InternalServerError(null));
                }
            }
        } catch (final Exception e) {
            return ErrorResponseUtil.errorResponse(new InternalServerError(e.getMessage()));
        }
    }

    @SafeVarargs
    private @NotNull Optional<Response> check(final Supplier<Optional<Response>>... suppliers) {
        for (final Supplier<Optional<Response>> supplier : suppliers) {
            final Optional<Response> response = supplier.get();
            if (response.isPresent()) {
                return response;
            }
        }
        return Optional.empty();
    }

    private @NotNull Optional<Response> checkStatus() {
        final Optional<StatusProvider> optionalStatusProvider =
                statusProviderRegistry.getStatusProviders().stream().findFirst();
        if (optionalStatusProvider.isEmpty()) {
            return Optional.of(ErrorResponseUtil.errorResponse(new InternalServerError(null)));
        }
        final StatusProvider statusProvider = optionalStatusProvider.get();
        switch (statusProvider.getStatus().activationStatus()) {
            case ACTIVATED -> {
                switch (statusProvider.getStatus().connectionStatus()) {
                    case CONNECTED -> {
                        return Optional.empty();
                    }
                    case DISCONNECTED -> {
                        return Optional.of(ErrorResponseUtil.errorResponse(new PulseAgentNotConnectedError()));
                    }
                    default -> {
                        return Optional.of(ErrorResponseUtil.errorResponse(new InternalServerError(null)));
                    }
                }
            }
            case DEACTIVATED -> {
                return Optional.of(ErrorResponseUtil.errorResponse(new PulseAgentDeactivatedError()));
            }
            default -> {
                return Optional.of(ErrorResponseUtil.errorResponse(new InternalServerError(null)));
            }
        }
    }

    private @NotNull Optional<Response> checkConfigWritable() {
        if (!systemInformation.isConfigWriteable()) {
            return Optional.of(ErrorResponseUtil.errorResponse(new ConfigWritingDisabled()));
        }
        return Optional.empty();
    }

    private @NotNull Optional<Response> checkAssetMapper(
            final @NotNull DataCombiner newDataCombiner,
            final @Nullable DataCombiner oldDataCombiner,
            final @NotNull Map<String, PulseAssetEntity> assetEntityMap) {
        if (newDataCombiner.entityReferences().isEmpty()) {
            return Optional.of(ErrorResponseUtil.errorResponse(new EmptySourcesForAssetMapperError()));
        }
        if (newDataCombiner.dataCombinings().isEmpty()) {
            return Optional.of(ErrorResponseUtil.errorResponse(new EmptyMappingsForAssetMapperError()));
        }
        if (newDataCombiner.entityReferences()
                .stream()
                .noneMatch(entityReference -> entityReference.type() == EntityType.PULSE_AGENT)) {
            return Optional.of(ErrorResponseUtil.errorResponse(new MissingEntityTypePulseAgentForAssetMapperError()));
        }
        final Set<String> oldAssetIdSet =
                Optional.ofNullable(oldDataCombiner).map(DataCombiner::getAssetIdSet).orElseGet(Set::of);
        final Set<String> allAssetIdSet = assetMappingExtractor.getAssetIdSet();
        final Set<String> unusableAssetIdSet = Sets.difference(allAssetIdSet, oldAssetIdSet);
        final Set<String> usedAssetIdSet = new HashSet<>();
        for (final var dataCombining : newDataCombiner.dataCombinings()) {
            final String assetId = dataCombining.destination().assetId();
            if (unusableAssetIdSet.contains(assetId)) {
                return Optional.of(ErrorResponseUtil.errorResponse(new DuplicatedManagedAssetIdError(assetId)));
            }
            if (!usedAssetIdSet.add(assetId)) {
                return Optional.of(ErrorResponseUtil.errorResponse(new DuplicatedManagedAssetIdError(assetId)));
            }
            final PulseAssetEntity asset = assetEntityMap.get(assetId);
            if (asset == null) {
                return Optional.of(ErrorResponseUtil.errorResponse(new ManagedAssetNotFoundError(assetId)));
            }
            if (!Objects.equals(dataCombining.destination().topic(), asset.getTopic())) {
                return Optional.of(ErrorResponseUtil.errorResponse(new InvalidManagedAssetTopicError(assetId)));
            }
            // There are some cases where the schema is still data-url encoded, so we need to decode it before comparing.
            final String schema =
                    PulseAgentAssetSchemaConverter.INSTANCE.toInternalEntity(dataCombining.destination().schema());
            if (!Objects.equals(schema, asset.getSchema())) {
                return Optional.of(ErrorResponseUtil.errorResponse(new InvalidManagedAssetSchemaError(assetId)));
            }
            final Optional<String> optionalDuplicateMappingId = dataCombining.getFirstDuplicateMappingId();
            if (optionalDuplicateMappingId.isPresent()) {
                return Optional.of(ErrorResponseUtil.errorResponse(new InvalidDataIdentifierReferenceForAssetMapperError(
                        optionalDuplicateMappingId.get())));
            }
            final UUID mappingId = asset.getMapping().getId();
            final UUID dataCombiningId = dataCombining.id();
            if (oldDataCombiner == null) {
                if (dataCombiningId == null) {
                    return Optional.of(ErrorResponseUtil.errorResponse(new InvalidManagedAssetMappingIdError("null")));
                }
                if (mappingId != null && !Objects.equals(dataCombiningId, mappingId)) {
                    return Optional.of(ErrorResponseUtil.errorResponse(new InvalidManagedAssetMappingIdError(
                            dataCombiningId)));
                }
            } else {
                if (mappingId == null) {
                    return Optional.of(ErrorResponseUtil.errorResponse(new InvalidManagedAssetMappingIdError("null")));
                }
                if (!Objects.equals(mappingId, dataCombiningId)) {
                    return Optional.of(ErrorResponseUtil.errorResponse(new InvalidManagedAssetMappingIdError(
                            dataCombiningId)));
                }
            }
        }
        return Optional.empty();
    }

    private void notifyAssetMapper(final @NotNull PulseAgentAsset asset) {
        assetMappingExtractor.getAllCombiners()
                .stream()
                .filter(dataCombiner -> dataCombiner.dataCombinings()
                        .stream()
                        .anyMatch(dataCombining -> Objects.equals(asset.getMapping().getId(), dataCombining.id())))
                .forEach(assetMappingExtractor::updateDataCombiner);
    }

    public static class InstructionList extends ItemsResponse<Instruction> {
        public InstructionList(final @NotNull List<com.hivemq.edge.api.model.Instruction> items) {
            super(items);
        }
    }
}
