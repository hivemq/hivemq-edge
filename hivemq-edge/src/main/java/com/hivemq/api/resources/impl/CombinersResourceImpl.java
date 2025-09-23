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

import com.hivemq.api.errors.AlreadyExistsError;
import com.hivemq.api.errors.ConfigWritingDisabled;
import com.hivemq.api.errors.InternalServerError;
import com.hivemq.api.errors.adapters.DataCombinerNotFoundError;
import com.hivemq.api.model.ItemsResponse;
import com.hivemq.combining.model.DataCombiner;
import com.hivemq.combining.model.DataCombining;
import com.hivemq.configuration.info.SystemInformation;
import com.hivemq.configuration.reader.DataCombiningExtractor;
import com.hivemq.edge.api.CombinersApi;
import com.hivemq.edge.api.model.Combiner;
import com.hivemq.edge.api.model.CombinerList;
import com.hivemq.edge.api.model.DataCombiningList;
import com.hivemq.persistence.mappings.fieldmapping.Instruction;
import com.hivemq.util.ErrorResponseUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.core.Response;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Singleton
public class CombinersResourceImpl implements CombinersApi {


    private static final Logger log = LoggerFactory.getLogger(CombinersResourceImpl.class);

    private final @NotNull SystemInformation systemInformation;
    private final @NotNull DataCombiningExtractor dataCombiningExtractor;

    @Inject
    public CombinersResourceImpl(
            final @NotNull SystemInformation systemInformation,
            final @NotNull DataCombiningExtractor dataCombiningExtractor) {
        this.systemInformation = systemInformation;
        this.dataCombiningExtractor = dataCombiningExtractor;
    }

    @Override
    public @NotNull Response addCombiner(final @NotNull Combiner combiner) {
        if (!systemInformation.isConfigWriteable()) {
            return ErrorResponseUtil.errorResponse(new ConfigWritingDisabled());
        }

        final @NotNull Optional<DataCombiner> instance = dataCombiningExtractor.getCombinerById(combiner.getId());
        if (instance.isPresent()) {
            return ErrorResponseUtil.errorResponse(new AlreadyExistsError(String.format(
                    "DataCombiner already exists '%s'",
                    combiner.getId())));
        }


        final DataCombiner dataCombiner = DataCombiner.fromModel(combiner);
        try {
            dataCombiningExtractor.addDataCombiner(dataCombiner);
        } catch (final Exception e) {
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

        final @NotNull Optional<DataCombiner> instance = dataCombiningExtractor.getCombinerById(combiner.getId());
        if (instance.isEmpty()) {
            return ErrorResponseUtil.errorResponse(new DataCombinerNotFoundError(combiner.getId().toString()));
        }

        final DataCombiner dataCombiner = DataCombiner.fromModel(combiner);

        final boolean updated = dataCombiningExtractor.updateDataCombiner(dataCombiner);
        if (updated) {
            return Response.ok().build();
        } else {
            return ErrorResponseUtil.errorResponse(new DataCombinerNotFoundError(combiner.getId().toString()));
        }
    }

    @Override
    public @NotNull Response deleteCombiner(final @NotNull UUID combinerId) {
        if (!systemInformation.isConfigWriteable()) {
            return ErrorResponseUtil.errorResponse(new ConfigWritingDisabled());
        }
        final @NotNull Optional<DataCombiner> instance = dataCombiningExtractor.getCombinerById(combinerId);
        if (instance.isEmpty()) {
            return ErrorResponseUtil.errorResponse(new DataCombinerNotFoundError(combinerId.toString()));
        }

        try {
            dataCombiningExtractor.deleteDataCombiner(combinerId);
        } catch (final Exception e) {
            final Throwable cause = e.getCause();
            log.warn("Exception occurred during deletion of data combining '{}':", combinerId, cause);
            return ErrorResponseUtil.errorResponse(new InternalServerError("Exception during deletion of data combiner."));
        }

        return Response.ok().build();
    }

    @Override
    public @NotNull Response getCombiners() {
        final Collection<DataCombiner> allCombiners = dataCombiningExtractor.getAllCombiners();
        final List<Combiner> combiners = allCombiners.stream().map(DataCombiner::toModel).toList();
        final CombinerList combinerList = new CombinerList().items(combiners);
        return Response.ok().entity(combinerList).build();
    }

    @Override
    public @NotNull Response getCombinersById(final @NotNull UUID combinerId) {
        final @NotNull Optional<DataCombiner> instance = dataCombiningExtractor.getCombinerById(combinerId);
        if (instance.isEmpty()) {
            return ErrorResponseUtil.errorResponse(new DataCombinerNotFoundError(combinerId.toString()));
        }
        return Response.ok().entity(instance.get().toModel()).build();
    }

    @Override
    public @NotNull Response getCombinerMappings(final @NotNull UUID combinerId) {
        final @NotNull Optional<DataCombiner> instance = dataCombiningExtractor.getCombinerById(combinerId);
        if (instance.isEmpty()) {
            return ErrorResponseUtil.errorResponse(new DataCombinerNotFoundError(combinerId.toString()));
        }
        final List<com.hivemq.edge.api.model.DataCombining> dataCombinings =
                instance.get().dataCombinings().stream().map(DataCombining::toModel).toList();
        return Response.ok().entity(new DataCombiningList().items(dataCombinings)).build();
    }

    @Override
    public @NotNull Response getMappingInstructions(final @NotNull UUID combinerId, final @NotNull UUID mappingId) {
        final @NotNull Optional<DataCombiner> instance = dataCombiningExtractor.getCombinerById(combinerId);
        if (instance.isEmpty()) {
            return ErrorResponseUtil.errorResponse(new DataCombinerNotFoundError(combinerId.toString()));
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
