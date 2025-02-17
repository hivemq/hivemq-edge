package com.hivemq.api.resources.impl;

import com.hivemq.combining.DataCombiner;
import com.hivemq.edge.api.CombinersApi;
import com.hivemq.edge.api.model.Combiner;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.Response;
import java.util.UUID;


@Singleton
public class CombinersResourceImpl implements CombinersApi {


    @Inject
    public CombinersResourceImpl() {
    }

    @Override
    public @NotNull Response addCombiner(final @NotNull Combiner combiner) {
        final DataCombiner dataCombiner = DataCombiner.fromModel(combiner);



        return null;
    }

    @Override
    public @NotNull Response deleteCombiner(final @NotNull UUID combinerId) {



        return null;
    }

    @Override
    public @NotNull Response getCombinerMappings(final @NotNull UUID combinerId) {



        return null;
    }

    @Override
    public @NotNull Response getCombiners() {


        return null;
    }

    @Override
    public @NotNull Response getCombinersById(final @NotNull UUID combinerId) {


        return null;
    }

    @Override
    public @NotNull Response getMappingInstructions(final @NotNull UUID combinerId, final @NotNull UUID mappingId) {



        return null;
    }

    @Override
    public @NotNull Response updateCombiner(final @NotNull UUID combinerId, final @NotNull Combiner combiner) {



        return null;
    }
}
