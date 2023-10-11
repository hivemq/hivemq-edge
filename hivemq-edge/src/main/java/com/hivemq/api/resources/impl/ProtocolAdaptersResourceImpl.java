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
package com.hivemq.api.resources.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.google.common.collect.ImmutableList;
import com.hivemq.api.AbstractApi;
import com.hivemq.api.model.ApiConstants;
import com.hivemq.api.model.ApiErrorMessages;
import com.hivemq.api.model.adapters.Adapter;
import com.hivemq.api.model.adapters.AdapterStatusModelConversionUtils;
import com.hivemq.api.model.adapters.AdaptersList;
import com.hivemq.api.model.adapters.ProtocolAdapter;
import com.hivemq.api.model.adapters.ProtocolAdaptersList;
import com.hivemq.api.model.adapters.ValuesTree;
import com.hivemq.api.model.status.Status;
import com.hivemq.api.model.status.StatusList;
import com.hivemq.api.model.status.StatusTransitionCommand;
import com.hivemq.api.model.status.StatusTransitionResult;
import com.hivemq.api.resources.ProtocolAdaptersApi;
import com.hivemq.api.utils.ApiErrorUtils;
import com.hivemq.configuration.service.ConfigurationService;
import com.hivemq.edge.HiveMQEdgeConstants;
import com.hivemq.edge.HiveMQEdgeRemoteService;
import com.hivemq.edge.modules.adapters.impl.ProtocolAdapterDiscoveryOutputImpl;
import com.hivemq.edge.modules.adapters.model.ProtocolAdapterDiscoveryInput;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterCapability;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterInformation;
import com.hivemq.edge.modules.api.adapters.model.ProtocolAdapterValidationFailure;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.protocols.ProtocolAdapterWrapper;
import com.hivemq.protocols.ProtocolAdapterManager;
import com.hivemq.protocols.ProtocolAdapterUtils;
import com.hivemq.protocols.params.NodeTreeImpl;
import com.networknt.schema.ValidationMessage;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class ProtocolAdaptersResourceImpl extends AbstractApi implements ProtocolAdaptersApi {

    private final @NotNull HiveMQEdgeRemoteService remoteService;
    private final @NotNull ConfigurationService configurationService;
    private final @NotNull ProtocolAdapterManager protocolAdapterManager;
    private final @NotNull ObjectMapper objectMapper;

    @Inject
    public ProtocolAdaptersResourceImpl(
            final @NotNull HiveMQEdgeRemoteService remoteService,
            final @NotNull ConfigurationService configurationService,
            final @NotNull ProtocolAdapterManager protocolAdapterManager,
            final @NotNull ObjectMapper objectMapper) {
        this.remoteService = remoteService;
        this.configurationService = configurationService;
        this.protocolAdapterManager = protocolAdapterManager;
        this.objectMapper = ProtocolAdapterUtils.createProtocolAdapterMapper(objectMapper);
    }

    @Override
    public @NotNull Response getAdapterTypes() {

        //-- Obtain the adapters installed by the runtime (these will be marked as installed = true).
        Set<ProtocolAdapter> installedAdapters = protocolAdapterManager.getAllAvailableAdapterTypes().
                values().
                stream().
                map(installedAdapter -> ProtocolAdapterApiUtils.convertInstalledAdapterType(
                        objectMapper, protocolAdapterManager, installedAdapter, configurationService)).
                collect(Collectors.toSet());

        //-- Obtain the remote modules and perform a selective union on the two sets
        remoteService.getConfiguration().getModules().stream().
                map(m -> ProtocolAdapterApiUtils.convertModuleAdapterType(m, configurationService)).
                filter(p -> !installedAdapters.contains(p)).forEach(installedAdapters::add);

        return Response.status(200).entity(new ProtocolAdaptersList(
                new ArrayList<>(installedAdapters))).build();
    }

    @Override
    public @NotNull Response getAdapters() {

        final List<Adapter> adapters = protocolAdapterManager.getProtocolAdapters()
                .values()
                .stream()
                .map(this::convertToAdapter)
                .collect(Collectors.toUnmodifiableList());

        return Response.status(200).entity(new AdaptersList(adapters)).build();
    }

    @Override
    public @NotNull Response getAdaptersForType(@NotNull final String adapterType) {

        Optional<ProtocolAdapterInformation> protocolAdapterType =
                protocolAdapterManager.getAdapterTypeById(adapterType);
        if (protocolAdapterType.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        final List<Adapter> adapters = protocolAdapterManager.getProtocolAdapters()
                .values()
                .stream()
                .filter(adapterInstance -> adapterInstance.getAdapterInformation().getProtocolId().equals(adapterType))
                .map(this::convertToAdapter)
                .collect(Collectors.toUnmodifiableList());

        return Response.status(200).entity(new AdaptersList(adapters)).build();
    }

    @Override
    public @NotNull Response getAdapter(final @NotNull String adapterId) {
        Optional<ProtocolAdapterWrapper> instance = protocolAdapterManager.getAdapterById(adapterId);
        if (!instance.isPresent()) {
            return ApiErrorUtils.notFound("Adapter not found");
        }
        return Response.status(200).entity(convertToAdapter(instance.get())).build();
    }


    private @NotNull Adapter convertToAdapter(final @NotNull ProtocolAdapterWrapper value) {
        final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        Map<String, Object> configObject;
        try {
            Thread.currentThread().setContextClassLoader(value.getAdapterFactory().getClass().getClassLoader());
            configObject = value.getAdapterFactory().unconvertConfigObject(objectMapper, value.getConfigObject());
        } finally {
            Thread.currentThread().setContextClassLoader(contextClassLoader);
        }
        return new Adapter(value.getAdapter().getId(),
                value.getAdapterInformation().getProtocolId(),
                configObject,
                getStatusInternal(value.getAdapter().getId()));
    }

    @Override
    public @NotNull Response discoverValues(
            @NotNull final String adapterId, final @Nullable String rootNode, final @Nullable Integer depth) {

        Optional<ProtocolAdapterWrapper> instance = protocolAdapterManager.getAdapterById(adapterId);
        if (!instance.isPresent()) {
            return ApiErrorUtils.notFound("Adapter not found");
        }

        final ProtocolAdapterWrapper adapterInstance = instance.get();
        if(!ProtocolAdapterCapability.supportsCapability(adapterInstance.getAdapterInformation(),
                ProtocolAdapterCapability.DISCOVER)){
            return ApiErrorUtils.badRequest("Adapter does not support discovery");
        }
        final ProtocolAdapterDiscoveryOutputImpl output = new ProtocolAdapterDiscoveryOutputImpl();
        final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread()
                    .setContextClassLoader(adapterInstance.getAdapterFactory().getClass().getClassLoader());
            adapterInstance.getAdapter().discoverValues(new ProtocolAdapterDiscoveryInput() {
                @Override
                public @Nullable String getRootNode() {
                    return rootNode;
                }

                @Override
                public int getDepth() {
                    return (depth != null && depth > 0) ? depth : 1;
                }

            }, output);
        } finally {
            Thread.currentThread().setContextClassLoader(contextClassLoader);
        }

        final NodeTreeImpl nodeTree = output.getNodeTree();
        final List<NodeTreeImpl.ObjectNode> children = nodeTree.getRootNode().getChildren();
        return Response.status(200).entity(new ValuesTree(children)).build();
    }

    @Override
    public Response addAdapter(final String adapterType, final Adapter adapter) {
        Optional<ProtocolAdapterInformation> protocolAdapterType =
                protocolAdapterManager.getAdapterTypeById(adapterType);
        if (protocolAdapterType.isEmpty()) {
            return ApiErrorUtils.notFound("Adapter Type not found by adapterType");
        }

        ApiErrorMessages errorMessages = ApiErrorUtils.createErrorContainer();
        Optional<ProtocolAdapterWrapper> instance = protocolAdapterManager.getAdapterById(adapter.getId());
        if (instance.isPresent()) {
            ApiErrorUtils.addValidationError(errorMessages, "id", "Adapter ID must be unique in system");
            return ApiErrorUtils.badRequest(errorMessages);
        }

        validateAdapterSchema(errorMessages, adapter);
        if (ApiErrorUtils.hasRequestErrors(errorMessages)) {
            return ApiErrorUtils.badRequest(errorMessages);
        }
        try {
            protocolAdapterManager.addAdapter(adapterType, adapter.getId(), adapter.getConfig());
        } catch(IllegalArgumentException e){
            if(e.getCause() instanceof UnrecognizedPropertyException){
                ApiErrorUtils.addValidationError(errorMessages,
                        ((UnrecognizedPropertyException)e.getCause()).getPropertyName(), "Unknown field on adapter configuration");
            }

            return ApiErrorUtils.badRequest(errorMessages);
        }

        logger.info("Added protocol adapter of type {} with id {}", adapterType, adapter.getId());
        return Response.ok().build();
    }

    @Override
    public Response updateAdapter(final String adapterId, final Adapter adapter) {
        Optional<ProtocolAdapterWrapper> instance = protocolAdapterManager.getAdapterById(adapterId);
        if (!instance.isPresent()) {
            return ApiErrorUtils.notFound("Cannot update an adapter that does not exist");
        }
        protocolAdapterManager.updateAdapter(adapterId, adapter.getConfig());
        return Response.ok().build();
    }

    @Override
    public Response deleteAdapter(final String adapterId) {
        Optional<ProtocolAdapterWrapper> instance = protocolAdapterManager.getAdapterById(adapterId);
        if (!instance.isPresent()) {
            return ApiErrorUtils.notFound("Adapter not found");
        }
        logger.info("deleting adapter {}", adapterId);
        protocolAdapterManager.deleteAdapter(adapterId);
        return Response.ok().build();
    }

    @Override
    public Response changeStatus(final String adapterId, final StatusTransitionCommand command) {
        ApiErrorMessages errorMessages = ApiErrorUtils.createErrorContainer();
        ApiErrorUtils.validateRequiredField(errorMessages, "id", adapterId, false);
        ApiErrorUtils.validateRequiredFieldRegex(errorMessages, "id", adapterId, HiveMQEdgeConstants.ID_REGEX);
        ApiErrorUtils.validateRequiredEntity(errorMessages, "command", command);
        if (!protocolAdapterManager.getAdapterById(adapterId).isPresent()) {
            return ApiErrorUtils.notFound(String.format("Adapter not found by id '%s'", adapterId));
        }
        if (ApiErrorUtils.hasRequestErrors(errorMessages)) {
            return ApiErrorUtils.badRequest(errorMessages);
        } else {
            switch (command.getCommand()) {
                case START:
                    protocolAdapterManager.start(adapterId);
                    break;
                case STOP:
                    protocolAdapterManager.stop(adapterId);
                    break;
                case RESTART:
                    protocolAdapterManager.stop(adapterId).thenRun(() ->
                            protocolAdapterManager.start(adapterId));
                    break;
            }
            return Response.ok(
                    StatusTransitionResult.pending(ApiConstants.ADAPTER_TYPE, adapterId,
                            ApiConstants.DEFAULT_TRANSITION_WAIT_TIMEOUT)).build();
        }
    }

    @Override
    public Response getStatus(final @NotNull String adapterId) {

        ApiErrorMessages errorMessages = ApiErrorUtils.createErrorContainer();
        ApiErrorUtils.validateRequiredField(errorMessages, "id", adapterId, false);
        ApiErrorUtils.validateRequiredFieldRegex(errorMessages, "id", adapterId, HiveMQEdgeConstants.ID_REGEX);
        if (!protocolAdapterManager.getAdapterById(adapterId).isPresent()) {
            return ApiErrorUtils.notFound(String.format("Adapter not found by id '%s'", adapterId));
        }
        if (ApiErrorUtils.hasRequestErrors(errorMessages)) {
            return ApiErrorUtils.badRequest(errorMessages);
        } else {
            return Response.status(200).entity(
                    getStatusInternal(adapterId)).build();
        }
    }

    protected Status getStatusInternal(final @NotNull String adapterId) {
        Optional<ProtocolAdapterWrapper> optionalAdapterInstance = protocolAdapterManager.getAdapterById(adapterId);
        if (optionalAdapterInstance.isPresent()) {
            return AdapterStatusModelConversionUtils.getAdapterStatus(
                    optionalAdapterInstance.get().getAdapter());
        }
        else {
            return Status.unknown(Status.RUNTIME_STATUS.STOPPED, ApiConstants.ADAPTER_TYPE, adapterId);
        }
    }

    protected void validateAdapterSchema(
            final @NotNull ApiErrorMessages apiErrorMessages, final @NotNull Adapter adapter) {
        ProtocolAdapterInformation information =
                protocolAdapterManager.getAllAvailableAdapterTypes().get(adapter.getProtocolAdapterType());
        if (information == null) {
            ApiErrorUtils.addValidationError(apiErrorMessages,
                    "config",
                    "Unable to find adapter type by supplied adapterTypeId");
            return;
        }
        if (adapter.getConfig() == null) {
            ApiErrorUtils.addValidationError(apiErrorMessages, "config", "Config must be supplied on the adapter");
            return;
        }

        List<ProtocolAdapterValidationFailure> errors =
                protocolAdapterManager.getProtocolAdapterFactory(
                        information.getProtocolId()).getValidator().validateConfiguration(objectMapper, adapter.getConfig());

        errors.stream()
                .forEach(e -> ApiErrorUtils.addValidationError(apiErrorMessages,
                        e.getFieldName(), e.getMessage()));
    }

    @Override
    public Response status() {
        ImmutableList.Builder<Status> builder = new ImmutableList.Builder<>();
        Map<String, ProtocolAdapterWrapper> adapters = protocolAdapterManager.getProtocolAdapters();
        for (ProtocolAdapterWrapper instance : adapters.values()) {
            builder.add(AdapterStatusModelConversionUtils.getAdapterStatus(instance.getAdapter()));
        }
        return Response.status(200).entity(new StatusList(builder.build())).build();
    }
}
