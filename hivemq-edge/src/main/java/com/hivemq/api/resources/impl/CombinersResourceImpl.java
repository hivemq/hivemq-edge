package com.hivemq.api.resources.impl;

import com.hivemq.api.errors.AlreadyExistsError;
import com.hivemq.api.errors.ConfigWritingDisabled;
import com.hivemq.api.errors.InternalServerError;
import com.hivemq.api.errors.adapters.AdapterNotFoundError;
import com.hivemq.api.model.ItemsResponse;
import com.hivemq.combining.model.DataCombiner;
import com.hivemq.combining.model.DataCombining;
import com.hivemq.combining.runtime.DataCombinerManager;
import com.hivemq.configuration.info.SystemInformation;
import com.hivemq.edge.api.CombinersApi;
import com.hivemq.edge.api.model.Combiner;
import com.hivemq.edge.api.model.CombinerList;
import com.hivemq.edge.api.model.DataCombiningList;
import com.hivemq.persistence.mappings.fieldmapping.Instruction;
import com.hivemq.util.ErrorResponseUtil;
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
    private final @NotNull DataCombinerManager dataCombinerManager;

    @Inject
    public CombinersResourceImpl(
            final @NotNull SystemInformation systemInformation,
            final @NotNull DataCombinerManager dataCombinerManager) {
        this.systemInformation = systemInformation;
        this.dataCombinerManager = dataCombinerManager;
    }

    @Override
    public @NotNull Response addCombiner(final @NotNull Combiner combiner) {
        if (!systemInformation.isConfigWriteable()) {
            return ErrorResponseUtil.errorResponse(new ConfigWritingDisabled());
        }

        final @NotNull Optional<DataCombiner> instance = dataCombinerManager.getCombinerById(combiner.getId());
        if (instance.isPresent()) {
            return ErrorResponseUtil.errorResponse(new AlreadyExistsError(String.format(
                    "DataCombiner already exists '%s'",
                    combiner.getId())));
        }


        final DataCombiner dataCombiner = DataCombiner.fromModel(combiner);
        try {
            dataCombinerManager.addDataCombiner(dataCombiner).get();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Thread was interrupted while data combiner was being added. '{}'", combiner.getName());
            return ErrorResponseUtil.errorResponse(new InternalServerError("Exception during add of data combiner."));
        } catch (final ExecutionException e) {
            final Throwable cause = e.getCause();
            if (cause instanceof IllegalArgumentException) {
                log.warn("Exception occurred during addition of data combining '{}':", combiner.getName(), cause);
            }
            return ErrorResponseUtil.errorResponse(new InternalServerError("Exception during add of data combiner."));
        }
        return Response.ok().build();
    }

    @Override
    public @NotNull Response updateCombiner(final @NotNull UUID combinerId, final @NotNull Combiner combiner) {
        if (!systemInformation.isConfigWriteable()) {
            return ErrorResponseUtil.errorResponse(new ConfigWritingDisabled());
        }

        final @NotNull Optional<DataCombiner> instance = dataCombinerManager.getCombinerById(combiner.getId());
        if (instance.isEmpty()) {
            return ErrorResponseUtil.errorResponse(new AdapterNotFoundError(String.format("DataCombiner not found '%s'",
                    combiner.getId())));
        }

        final DataCombiner dataCombiner = DataCombiner.fromModel(combiner);

        final boolean updated = dataCombinerManager.updateDataCombiner(dataCombiner);
        if (updated) {
            return Response.ok().build();
        } else {
            return ErrorResponseUtil.errorResponse(new AdapterNotFoundError(String.format("DataCombiner not found '%s'",
                    combiner.getId())));
        }
    }

    @Override
    public @NotNull Response deleteCombiner(final @NotNull UUID combinerId) {
        if (!systemInformation.isConfigWriteable()) {
            return ErrorResponseUtil.errorResponse(new ConfigWritingDisabled());
        }
        final @NotNull Optional<DataCombiner> instance = dataCombinerManager.getCombinerById(combinerId);
        if (instance.isEmpty()) {
            return ErrorResponseUtil.errorResponse(new AdapterNotFoundError(String.format("DataCombiner not found '%s'",
                    combinerId)));
        }

        try {
            dataCombinerManager.deleteDataCombiner(combinerId).get();
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
        final Collection<DataCombiner> allCombiners = dataCombinerManager.getAllCombiners();
        final List<Combiner> combiners = allCombiners.stream().map(DataCombiner::toModel).toList();
        final CombinerList combinerList = new CombinerList().items(combiners);
        return Response.ok().entity(combinerList).build();
    }

    @Override
    public @NotNull Response getCombinersById(final @NotNull UUID combinerId) {
        final @NotNull Optional<DataCombiner> instance = dataCombinerManager.getCombinerById(combinerId);
        if (instance.isEmpty()) {
            return ErrorResponseUtil.errorResponse(new AdapterNotFoundError(String.format("DataCombiner not found '%s'",
                    combinerId)));
        }
        return Response.ok().entity(instance.get().toModel()).build();
    }

    @Override
    public @NotNull Response getCombinerMappings(final @NotNull UUID combinerId) {
        final @NotNull Optional<DataCombiner> instance = dataCombinerManager.getCombinerById(combinerId);
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
        final @NotNull Optional<DataCombiner> instance = dataCombinerManager.getCombinerById(combinerId);
        if (instance.isEmpty()) {
            return ErrorResponseUtil.errorResponse(new AdapterNotFoundError(String.format("DataCombiner not found '%s'",
                    combinerId)));
        }

        final List<com.hivemq.edge.api.model.Instruction> instructions = instance.get()
                .dataCombinings()
                .stream()
                .filter(c -> c.id().equals(mappingId))
                .flatMap(dataCombining -> dataCombining.instructions().stream())
                .map(Instruction::toModel)
                .toList();
        return Response.ok().entity(new InstructionList(instructions)).build();
    }


    public static class InstructionList extends ItemsResponse<com.hivemq.edge.api.model.Instruction> {
        public InstructionList(final @NotNull List<com.hivemq.edge.api.model.Instruction> items) {
            super(items);
        }
    }
}
