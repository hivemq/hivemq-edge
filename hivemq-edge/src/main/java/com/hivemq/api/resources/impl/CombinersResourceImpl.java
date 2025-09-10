package com.hivemq.api.resources.impl;

import com.hivemq.edge.api.CombinersApi;
import com.hivemq.edge.api.PulseApi;
import com.hivemq.edge.api.model.Combiner;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.core.Response;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;


@Singleton
public class CombinersResourceImpl implements CombinersApi {
    private final @NotNull PulseApi pulseApi;

    @Inject
    public CombinersResourceImpl(final @NotNull PulseApi pulseApi) {
        this.pulseApi = pulseApi;
    }

    @Override
    public @NotNull Response addCombiner(final @NotNull Combiner combiner) {
        return pulseApi.addAssetMapper(combiner);
    }

    @Override
    public @NotNull Response updateCombiner(final @NotNull UUID combinerId, final @NotNull Combiner combiner) {
        return pulseApi.updateAssetMapper(combinerId, combiner);
    }

    @Override
    public @NotNull Response deleteCombiner(final @NotNull UUID combinerId) {
        return pulseApi.deleteAssetMapper(combinerId);
    }

    @Override
    public @NotNull Response getCombiners() {
        return pulseApi.getAssetMappers();
    }

    @Override
    public @NotNull Response getCombinersById(final @NotNull UUID combinerId) {
        return pulseApi.getAssetMapper(combinerId);
    }

    @Override
    public @NotNull Response getCombinerMappings(final @NotNull UUID combinerId) {
        return pulseApi.getAssetMapperMappings(combinerId);
    }

    @Override
    public @NotNull Response getMappingInstructions(final @NotNull UUID combinerId, final @NotNull UUID mappingId) {
        return pulseApi.getAssetMapperInstructions(combinerId, mappingId);
    }
}
