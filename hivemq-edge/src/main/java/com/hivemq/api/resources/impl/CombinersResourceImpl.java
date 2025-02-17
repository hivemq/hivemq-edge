package com.hivemq.api.resources.impl;

import com.hivemq.api.errors.ConfigWritingDisabled;
import com.hivemq.api.errors.InternalServerError;
import com.hivemq.api.errors.adapters.AdapterNotFoundError;
import com.hivemq.combining.CombiningManager;
import com.hivemq.combining.DataCombiner;
import com.hivemq.combining.DataCombining;
import com.hivemq.configuration.info.SystemInformation;
import com.hivemq.edge.api.CombinersApi;
import com.hivemq.edge.api.model.Combiner;
import com.hivemq.edge.api.model.CombinerList;
import com.hivemq.edge.api.model.DataCombiningList;
import com.hivemq.persistence.mappings.fieldmapping.Instruction;
import com.hivemq.util.ErrorResponseUtil;
import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.Response;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;


@Singleton
public class CombinersResourceImpl implements CombinersApi {


    private static final Logger log = LoggerFactory.getLogger(CombinersResourceImpl.class);

    private final @NotNull SystemInformation systemInformation;
    private final @NotNull CombiningManager combiningManager;

    @Inject
    public CombinersResourceImpl(
            final @NotNull SystemInformation systemInformation, final @NotNull CombiningManager combiningManager) {
        this.systemInformation = systemInformation;
        this.combiningManager = combiningManager;
    }

    @Override
    public @NotNull Response addCombiner(final @NotNull Combiner combiner) {
        if (!systemInformation.isConfigWriteable()) {
            return ErrorResponseUtil.errorResponse(new ConfigWritingDisabled());
        }
        final DataCombiner dataCombiner = DataCombiner.fromModel(combiner);
        try {
            combiningManager.addDataCombiner(dataCombiner).get();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Thread was interrupted while data combiner was being added. '{}'", combiner.getName());
            return ErrorResponseUtil.errorResponse(new InternalServerError("Exception during add of data combiner."));
        } catch (final ExecutionException e) {
            final Throwable cause = e.getCause();
            log.warn("Exception occurred during addition of data combining '{}':", combiner.getName(), cause);
            return ErrorResponseUtil.errorResponse(new InternalServerError("Exception during add of data combiner."));
        }
        return Response.ok().build();
    }

    @Override
    public @NotNull Response updateCombiner(final @NotNull UUID combinerId, final @NotNull Combiner combiner) {
        if (!systemInformation.isConfigWriteable()) {
            return ErrorResponseUtil.errorResponse(new ConfigWritingDisabled());
        }

        final @NotNull Optional<DataCombiner> instance = combiningManager.getCombinerById(combiner.getId());
        if (instance.isEmpty()) {
            return ErrorResponseUtil.errorResponse(new AdapterNotFoundError(String.format("DataCombiner not found '%s'",
                    combiner.getId())));
        }

        final DataCombiner dataCombiner = DataCombiner.fromModel(combiner);
        try {
            combiningManager.updateDataCombiner(dataCombiner).get();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Thread was interrupted while data combiner was being updated. '{}'", combiner.getName());
            return ErrorResponseUtil.errorResponse(new InternalServerError("Exception during update of data combiner."));
        } catch (final ExecutionException e) {
            final Throwable cause = e.getCause();
            log.warn("Exception occurred during update of data combining '{}':", combiner.getName(), cause);
            return ErrorResponseUtil.errorResponse(new InternalServerError("Exception during update of data combiner."));
        }
        return Response.ok().build();
    }

    @Override
    public @NotNull Response deleteCombiner(final @NotNull UUID combinerId) {
        if (!systemInformation.isConfigWriteable()) {
            return ErrorResponseUtil.errorResponse(new ConfigWritingDisabled());
        }
        final @NotNull Optional<DataCombiner> instance = combiningManager.getCombinerById(combinerId);
        if (instance.isEmpty()) {
            return ErrorResponseUtil.errorResponse(new AdapterNotFoundError(String.format("DataCombiner not found '%s'",
                    combinerId)));
        }

        try {
            combiningManager.deleteDataCombiner(combinerId).get();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Thread was interrupted while data combiner was being deleted. '{}'", combinerId);
            return ErrorResponseUtil.errorResponse(new InternalServerError("Exception during deletion of data combiner."));
        } catch (final ExecutionException e) {
            final Throwable cause = e.getCause();
            log.warn("Exception occurred during deletion of data combining '{}':", combinerId, cause);
            return ErrorResponseUtil.errorResponse(new InternalServerError("Exception during deletion of data combiner."));
        }

        return Response.ok().build();
    }

    @Override
    public @NotNull Response getCombiners() {
        final Collection<DataCombiner> allCombiners = combiningManager.getAllCombiners();
        final List<Combiner> combiners = allCombiners.stream().map(DataCombiner::toModel).toList();
        final CombinerList combinerList = new CombinerList().items(combiners);
        return Response.ok().entity(combinerList).build();
    }

    @Override
    public @NotNull Response getCombinersById(final @NotNull UUID combinerId) {
        final @NotNull Optional<DataCombiner> instance = combiningManager.getCombinerById(combinerId);
        if (instance.isEmpty()) {
            return ErrorResponseUtil.errorResponse(new AdapterNotFoundError(String.format("DataCombiner not found '%s'",
                    combinerId)));
        }
        return Response.ok().entity(instance.get().toModel()).build();
    }

    @Override
    public @NotNull Response getCombinerMappings(final @NotNull UUID combinerId) {
        final @NotNull Optional<DataCombiner> instance = combiningManager.getCombinerById(combinerId);
        if (instance.isEmpty()) {
            return ErrorResponseUtil.errorResponse(new AdapterNotFoundError(String.format("DataCombiner not found '%s'",
                    combinerId)));
        }
        final List<com.hivemq.edge.api.model.DataCombining> dataCombinings =
                instance.get().dataCombinings().stream().map(DataCombining::toModel).toList();
        return Response.ok().entity(new DataCombiningList().items(dataCombinings)).build();
    }

    @Override
    public @NotNull Response getMappingInstructions(final @NotNull UUID combinerId, final @NotNull UUID mappingId) {
        final @NotNull Optional<DataCombiner> instance = combiningManager.getCombinerById(combinerId);
        if (instance.isEmpty()) {
            return ErrorResponseUtil.errorResponse(new AdapterNotFoundError(String.format("DataCombiner not found '%s'",
                    combinerId)));
        }

        final List<com.hivemq.edge.api.model.Instruction> instructions = instance.get()
                .dataCombinings()
                .stream()
                .filter(c -> c.id().equals(mappingId))
                .flatMap(dataCombining -> dataCombining.instructions().stream())
                .map(Instruction::toModel).toList();


        // TODO open api update needed (InstructionList is missing)
        throw new NotImplementedException();
       // return Response.ok().entity().build();
    }
}
