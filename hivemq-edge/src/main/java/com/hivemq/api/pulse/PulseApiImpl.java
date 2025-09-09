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

package com.hivemq.api.pulse;

import com.hivemq.api.errors.InternalServerError;
import com.hivemq.api.errors.pulse.ActivationTokenAlreadyDeletedError;
import com.hivemq.api.errors.pulse.ActivationTokenInvalidError;
import com.hivemq.api.errors.pulse.ActivationTokenNotDeletedError;
import com.hivemq.api.errors.pulse.ManagedAssetAlreadyExistsError;
import com.hivemq.api.errors.pulse.ManagedAssetNotFoundError;
import com.hivemq.api.errors.pulse.PulseAgentDeactivatedError;
import com.hivemq.api.errors.pulse.PulseAgentNotConnectedError;
import com.hivemq.configuration.entity.pulse.PulseEntity;
import com.hivemq.configuration.reader.PulseExtractor;
import com.hivemq.configuration.service.ConfigurationService;
import com.hivemq.edge.api.PulseApi;
import com.hivemq.edge.api.model.Combiner;
import com.hivemq.edge.api.model.ManagedAsset;
import com.hivemq.edge.api.model.ManagedAssetList;
import com.hivemq.edge.api.model.PulseActivationToken;
import com.hivemq.edge.api.model.PulseStatus;
import com.hivemq.pulse.asset.AssetProviderRegistry;
import com.hivemq.pulse.asset.PulseAgentAsset;
import com.hivemq.pulse.asset.PulseAgentAssetMapping;
import com.hivemq.pulse.asset.PulseAgentAssets;
import com.hivemq.pulse.converters.PulseAgentAssetConverter;
import com.hivemq.pulse.converters.PulseAgentAssetMappingStatusConverter;
import com.hivemq.pulse.converters.PulseAgentAssetsConverter;
import com.hivemq.pulse.converters.PulseAgentStatusConverter;
import com.hivemq.pulse.status.Status;
import com.hivemq.pulse.status.StatusProvider;
import com.hivemq.pulse.status.StatusProviderRegistry;
import com.hivemq.util.ErrorResponseUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.core.Response;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.stream.IntStream;

/**
 * The type Pulse api.
 */
@Singleton
public class PulseApiImpl implements PulseApi {
    private static final @NotNull Logger LOGGER = LoggerFactory.getLogger(PulseApiImpl.class);
    private final @NotNull ConfigurationService configurationService;
    private final @NotNull AssetProviderRegistry assetProviderRegistry;
    private final @NotNull StatusProviderRegistry statusProviderRegistry;

    @Inject
    public PulseApiImpl(
            final @NotNull ConfigurationService configurationService,
            final @NotNull AssetProviderRegistry assetProviderRegistry,
            final @NotNull StatusProviderRegistry statusProviderRegistry) {
        this.assetProviderRegistry = assetProviderRegistry;
        this.configurationService = configurationService;
        this.statusProviderRegistry = statusProviderRegistry;
    }

    @Override
    public @NotNull Response addAssetMapper(final @NotNull Combiner combiner) {
        final Optional<Response> optionalResponse = checkStatus();
        if (optionalResponse.isPresent()) {
            return optionalResponse.get();
        }
        // TODO
        return Response.ok().build();
    }

    @Override
    public @NotNull Response addManagedAsset(final @NotNull ManagedAsset managedAsset) {
        final Optional<Response> optionalResponse = checkStatus();
        if (optionalResponse.isPresent()) {
            return optionalResponse.get();
        }
        final PulseExtractor pulseExtractor = configurationService.pulseExtractor();
        final PulseEntity existingPulseEntity = pulseExtractor.getPulseEntity();
        synchronized (existingPulseEntity.getLock()) {
            final PulseAgentAssets assets =
                    PulseAgentAssets.fromPersistence(existingPulseEntity.getPulseAssetsEntity());
            final OptionalInt optionalAssetIndex = IntStream.range(0, assets.size())
                    .filter(i -> Objects.equals(assets.get(i).getId(), managedAsset.getId()))
                    .findAny();
            if (optionalAssetIndex.isEmpty()) {
                return ErrorResponseUtil.errorResponse(new ManagedAssetNotFoundError());
            }
            final PulseAgentAsset asset = assets.get(optionalAssetIndex.getAsInt());
            if (asset.getMapping().getId() != null) {
                return ErrorResponseUtil.errorResponse(new ManagedAssetAlreadyExistsError());
            }
            final PulseAgentAsset newAsset = asset.withDescription(managedAsset.getDescription())
                    .withMapping(PulseAgentAssetMapping.builder()
                            .id(asset.getId())
                            .status(PulseAgentAssetMappingStatusConverter.INSTANCE.toInternalEntity(managedAsset.getMapping()
                                    .getStatus()))
                            .build());
            assets.set(optionalAssetIndex.getAsInt(), newAsset);
            final PulseEntity newPulseEntity = new PulseEntity(assets.toPersistence());
            pulseExtractor.setPulseEntity(newPulseEntity);
            return Response.ok(PulseAgentAssetConverter.INSTANCE.toRestEntity(newAsset)).build();
        }
    }

    @Override
    public @NotNull Response deleteAssetMapper(final @NotNull UUID combinerId) {
        final Optional<Response> optionalResponse = checkStatus();
        if (optionalResponse.isPresent()) {
            return optionalResponse.get();
        }
        // TODO
        return Response.ok().build();
    }

    @Override
    public @NotNull Response deleteManagedAsset(final @NotNull UUID assetId) {
        final Optional<Response> optionalResponse = checkStatus();
        if (optionalResponse.isPresent()) {
            return optionalResponse.get();
        }
        final PulseExtractor pulseExtractor = configurationService.pulseExtractor();
        final PulseEntity existingPulseEntity = pulseExtractor.getPulseEntity();
        synchronized (existingPulseEntity.getLock()) {
            final PulseAgentAssets assets =
                    PulseAgentAssets.fromPersistence(existingPulseEntity.getPulseAssetsEntity());
            final OptionalInt optionalAssetIndex = IntStream.range(0, assets.size())
                    .filter(i -> Objects.equals(assets.get(i).getId(), assetId))
                    .findAny();
            if (optionalAssetIndex.isEmpty()) {
                return ErrorResponseUtil.errorResponse(new ManagedAssetNotFoundError());
            }
            assets.remove(optionalAssetIndex.getAsInt());
            final PulseEntity newPulseEntity = new PulseEntity(assets.toPersistence());
            pulseExtractor.setPulseEntity(newPulseEntity);
            return Response.ok().build();
        }
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
        final Optional<Response> optionalResponse = checkStatus();
        if (optionalResponse.isPresent()) {
            return optionalResponse.get();
        }
        // TODO
        return Response.ok().build();
    }

    @Override
    public @NotNull Response getAssetMapperInstructions(final @NotNull UUID combinerId, final @NotNull UUID mappingId) {
        final Optional<Response> optionalResponse = checkStatus();
        if (optionalResponse.isPresent()) {
            return optionalResponse.get();
        }
        // TODO
        return Response.ok().build();
    }

    @Override
    public @NotNull Response getAssetMapperMappings(final @NotNull UUID combinerId) {
        final Optional<Response> optionalResponse = checkStatus();
        if (optionalResponse.isPresent()) {
            return optionalResponse.get();
        }
        // TODO
        return Response.ok().build();
    }

    @Override
    public @NotNull Response getAssetMappers() {
        final Optional<Response> optionalResponse = checkStatus();
        if (optionalResponse.isPresent()) {
            return optionalResponse.get();
        }
        // TODO
        return Response.ok().build();
    }

    @Override
    public @NotNull Response getManagedAssets() {
        final Optional<Response> optionalResponse = checkStatus();
        if (optionalResponse.isPresent()) {
            return optionalResponse.get();
        }
        final PulseExtractor pulseExtractor = configurationService.pulseExtractor();
        final PulseEntity pulseEntity = pulseExtractor.getPulseEntity();
        final PulseAgentAssets assets = PulseAgentAssets.fromPersistence(pulseEntity.getPulseAssetsEntity());
        final ManagedAssetList managedAssetList = PulseAgentAssetsConverter.INSTANCE.toRestEntity(assets);
        return Response.ok(managedAssetList).build();
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
        final Optional<Response> optionalResponse = checkStatus();
        if (optionalResponse.isPresent()) {
            return optionalResponse.get();
        }
        // TODO
        return Response.ok().build();
    }

    @Override
    public @NotNull Response updateManagedAsset(final @NotNull UUID assetId, final @NotNull ManagedAsset managedAsset) {
        final Optional<Response> optionalResponse = checkStatus();
        if (optionalResponse.isPresent()) {
            return optionalResponse.get();
        }
        final PulseExtractor pulseExtractor = configurationService.pulseExtractor();
        final PulseEntity existingPulseEntity = pulseExtractor.getPulseEntity();
        synchronized (existingPulseEntity.getLock()) {
            final PulseAgentAssets assets =
                    PulseAgentAssets.fromPersistence(existingPulseEntity.getPulseAssetsEntity());
            final OptionalInt optionalAssetIndex = IntStream.range(0, assets.size())
                    .filter(i -> Objects.equals(assets.get(i).getId(), assetId))
                    .findAny();
            if (optionalAssetIndex.isEmpty()) {
                return ErrorResponseUtil.errorResponse(new ManagedAssetNotFoundError());
            }
            final PulseAgentAsset asset = assets.get(optionalAssetIndex.getAsInt());
            assets.set(optionalAssetIndex.getAsInt(),
                    asset.withDescription(managedAsset.getDescription())
                            .withMapping(asset.getMapping()
                                    .withId(asset.getId())
                                    .withStatus(PulseAgentAssetMappingStatusConverter.INSTANCE.toInternalEntity(
                                            managedAsset.getMapping().getStatus()))));
            final PulseEntity newPulseEntity = new PulseEntity(assets.toPersistence());
            pulseExtractor.setPulseEntity(newPulseEntity);
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
}
