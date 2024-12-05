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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.ImmutableList;
import com.hivemq.adapter.sdk.api.ProtocolAdapterCapability;
import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.config.ProtocolSpecificAdapterConfig;
import com.hivemq.adapter.sdk.api.discovery.ProtocolAdapterDiscoveryInput;
import com.hivemq.adapter.sdk.api.tag.Tag;
import com.hivemq.adapter.sdk.api.writing.WritingProtocolAdapter;
import com.hivemq.api.AbstractApi;
import com.hivemq.api.adapters.AdapterConfigModel;
import com.hivemq.api.json.CustomConfigSchemaGenerator;
import com.hivemq.api.model.ApiConstants;
import com.hivemq.api.model.ApiErrorMessages;
import com.hivemq.api.model.adapters.Adapter;
import com.hivemq.api.model.adapters.AdapterStatusModelConversionUtils;
import com.hivemq.api.model.adapters.AdaptersList;
import com.hivemq.api.model.adapters.ProtocolAdapter;
import com.hivemq.api.model.adapters.ProtocolAdaptersList;
import com.hivemq.api.model.adapters.ValuesTree;
import com.hivemq.api.model.mappings.northbound.NorthboundMappingListModel;
import com.hivemq.api.model.mappings.northbound.NorthboundMappingModel;
import com.hivemq.api.model.mappings.southbound.SouthboundMappingListModel;
import com.hivemq.api.model.mappings.southbound.SouthboundMappingModel;
import com.hivemq.api.model.status.Status;
import com.hivemq.api.model.status.StatusList;
import com.hivemq.api.model.status.StatusTransitionCommand;
import com.hivemq.api.model.status.StatusTransitionResult;
import com.hivemq.api.model.tags.DomainTagModel;
import com.hivemq.api.model.tags.DomainTagModelList;
import com.hivemq.api.model.tags.TagSchema;
import com.hivemq.api.resources.ProtocolAdaptersApi;
import com.hivemq.api.utils.ApiErrorUtils;
import com.hivemq.configuration.service.ConfigurationService;
import com.hivemq.edge.HiveMQEdgeConstants;
import com.hivemq.edge.HiveMQEdgeRemoteService;
import com.hivemq.edge.VersionProvider;
import com.hivemq.edge.modules.adapters.impl.ProtocolAdapterDiscoveryOutputImpl;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterValidationFailure;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterValidator;
import com.hivemq.http.HttpStatus;
import com.hivemq.persistence.domain.DomainTag;
import com.hivemq.persistence.domain.DomainTagAddResult;
import com.hivemq.persistence.domain.DomainTagDeleteResult;
import com.hivemq.persistence.domain.DomainTagUpdateResult;
import com.hivemq.persistence.mappings.NorthboundMapping;
import com.hivemq.persistence.mappings.SouthboundMapping;
import com.hivemq.protocols.InternalProtocolAdapterWritingService;
import com.hivemq.protocols.ProtocolAdapterConfig;
import com.hivemq.protocols.ProtocolAdapterConfigConverter;
import com.hivemq.protocols.ProtocolAdapterManager;
import com.hivemq.protocols.ProtocolAdapterSchemaManager;
import com.hivemq.protocols.ProtocolAdapterUtils;
import com.hivemq.protocols.ProtocolAdapterWrapper;
import com.hivemq.protocols.params.NodeTreeImpl;
import com.hivemq.protocols.tag.TagSchemaCreationInputImpl;
import com.hivemq.protocols.tag.TagSchemaCreationOutputImpl;
import com.hivemq.util.ErrorResponseUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.Response;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Singleton
public class ProtocolAdaptersResourceImpl extends AbstractApi implements ProtocolAdaptersApi {

    private static final @NotNull Logger log = LoggerFactory.getLogger(ProtocolAdaptersResourceImpl.class);

    private final @NotNull HiveMQEdgeRemoteService remoteService;
    private final @NotNull ConfigurationService configurationService;
    private final @NotNull ProtocolAdapterManager protocolAdapterManager;
    private final @NotNull InternalProtocolAdapterWritingService protocolAdapterWritingService;
    private final @NotNull ProtocolAdapterConfigConverter configConverter;
    private final @NotNull ObjectMapper objectMapper;
    private final @NotNull VersionProvider versionProvider;
    private final @NotNull CustomConfigSchemaGenerator customConfigSchemaGenerator = new CustomConfigSchemaGenerator();

    @Inject
    public ProtocolAdaptersResourceImpl(
            final @NotNull HiveMQEdgeRemoteService remoteService,
            final @NotNull ConfigurationService configurationService,
            final @NotNull ProtocolAdapterManager protocolAdapterManager,
            final @NotNull InternalProtocolAdapterWritingService protocolAdapterWritingService,
            final @NotNull ObjectMapper objectMapper,
            final @NotNull VersionProvider versionProvider,
            final @NotNull ProtocolAdapterConfigConverter configConverter) {
        this.remoteService = remoteService;
        this.configurationService = configurationService;
        this.protocolAdapterManager = protocolAdapterManager;
        this.objectMapper = ProtocolAdapterUtils.createProtocolAdapterMapper(objectMapper);
        this.versionProvider = versionProvider;
        this.protocolAdapterWritingService = protocolAdapterWritingService;
        this.configConverter = configConverter;
    }

    @Override
    public @NotNull Response getAdapterTypes() {

        //-- Obtain the adapters installed by the runtime (these will be marked as installed = true).
        final Set<ProtocolAdapter> installedAdapters =
                protocolAdapterManager.getAllAvailableAdapterTypes().values().stream().map(installedAdapter -> {
                    try {
                        return ProtocolAdapterApiUtils.convertInstalledAdapterType(objectMapper,
                                protocolAdapterManager,
                                installedAdapter,
                                configurationService,
                                versionProvider);
                    } catch (final Throwable t) {
                        if (logger.isWarnEnabled()) {
                            logger.warn("Not able to properly load protocol adapter.", t);
                        }
                        return null;
                    }
                }).filter(Objects::nonNull).collect(Collectors.toSet());

        //-- Obtain the remote modules and perform a selective union on the two sets
        remoteService.getConfiguration()
                .getModules()
                .stream()
                .map(m -> ProtocolAdapterApiUtils.convertModuleAdapterType(m, configurationService))
                .filter(p -> !installedAdapters.contains(p))
                .forEach(installedAdapters::add);

        return Response.status(200).entity(new ProtocolAdaptersList(new ArrayList<>(installedAdapters))).build();
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
    public @NotNull Response getAdaptersForType(final @NotNull String adapterType) {
        final Optional<ProtocolAdapterInformation> protocolAdapterType =
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
        final Optional<ProtocolAdapterWrapper> instance = protocolAdapterManager.getAdapterById(adapterId);
        if (instance.isEmpty()) {
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
            configObject.put("id", value.getId());
        } finally {
            Thread.currentThread().setContextClassLoader(contextClassLoader);
        }
        return new Adapter(value.getId(),
                value.getAdapterInformation().getProtocolId(),
                objectMapper.valueToTree(configObject),
                getStatusInternal(value.getId()));
    }

    @Override
    public @NotNull Response discoverValues(
            final @NotNull String adapterId, final @Nullable String rootNode, final @Nullable Integer depth) {

        final Optional<ProtocolAdapterWrapper> instance = protocolAdapterManager.getAdapterById(adapterId);
        if (instance.isEmpty()) {
            return ApiErrorUtils.notFound("Adapter not found");
        }

        final ProtocolAdapterWrapper adapterInstance = instance.get();
        if (!adapterInstance.getAdapterInformation().getCapabilities().contains(ProtocolAdapterCapability.DISCOVER)) {
            return ApiErrorUtils.badRequest("Adapter does not support discovery");
        }
        final ProtocolAdapterDiscoveryOutputImpl output = new ProtocolAdapterDiscoveryOutputImpl();
        final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread()
                    .setContextClassLoader(adapterInstance.getAdapterFactory().getClass().getClassLoader());
            adapterInstance.discoverValues(new ProtocolAdapterDiscoveryInput() {
                @Override
                public @Nullable String getRootNode() {
                    return rootNode;
                }

                @Override
                public int getDepth() {
                    return (depth != null && depth > 0) ? depth : 1;
                }

            }, output);
            output.getOutputFuture().orTimeout(1, TimeUnit.MINUTES).get();
        } catch (final ExecutionException e) {
            final Throwable cause = e.getCause();
            log.warn("Exception occurred during discovery for adapter '{}'", adapterId, cause);
            return ErrorResponseUtil.genericError("Exception during discovery.");
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Thread was interrupted during discovery for adapter '{}'", adapterId);
            return ErrorResponseUtil.genericError("Exception during discovery.");
        } catch (final Exception e) {
            log.warn("Exception was thrown during discovery for adapter '{}'.", adapterId);
            return ErrorResponseUtil.genericError("Exception during discovery.");
        } finally {
            Thread.currentThread().setContextClassLoader(contextClassLoader);
        }
        final NodeTreeImpl nodeTree = output.getNodeTree();
        final List<NodeTreeImpl.ObjectNode> children = nodeTree.getRootNode().getChildren();
        return Response.status(200).entity(new ValuesTree(children)).build();
    }

    @Override
    public @NotNull Response addAdapter(final @NotNull String adapterType, final @NotNull Adapter adapter) {
        final Optional<ProtocolAdapterInformation> protocolAdapterType =
                protocolAdapterManager.getAdapterTypeById(adapterType);
        if (protocolAdapterType.isEmpty()) {
            return ApiErrorUtils.notFound("No Adapter Type was found by the given adapterType '" + adapterType + "'.");
        }
        final ApiErrorMessages errorMessages = ApiErrorUtils.createErrorContainer();
        final Optional<ProtocolAdapterWrapper> instance = protocolAdapterManager.getAdapterById(adapter.getId());
        if (instance.isPresent()) {
            ApiErrorUtils.addValidationError(errorMessages, "id", "Adapter ID must be unique in system");
            return ApiErrorUtils.badRequest(errorMessages);
        }
        validateAdapterSchema(errorMessages, adapter);
        if (ApiErrorUtils.hasRequestErrors(errorMessages)) {
            return ApiErrorUtils.badRequest(errorMessages);
        }
        try {
            final Map<String, Object> config = configConverter.convertConfigToMaps(adapter.getConfig());
            protocolAdapterManager.addAdapterWithoutTags(adapterType, adapter.getId(), config);
        } catch (final IllegalArgumentException e) {
            if (e.getCause() instanceof UnrecognizedPropertyException) {
                ApiErrorUtils.addValidationError(errorMessages,
                        ((UnrecognizedPropertyException) e.getCause()).getPropertyName(),
                        "Unknown field on adapter configuration");
            }
            return ApiErrorUtils.badRequest(errorMessages);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Added protocol adapter of type {} with ID {}.", adapterType, adapter.getId());
        }
        return Response.ok().build();
    }

    @Override
    public @NotNull Response updateAdapter(final @NotNull String adapterId, final @NotNull Adapter adapter) {
        final Optional<ProtocolAdapterWrapper> instance = protocolAdapterManager.getAdapterById(adapterId);
        if (instance.isEmpty()) {
            return ApiErrorUtils.notFound("Cannot update an adapter that does not exist");
        }
        final ApiErrorMessages errorMessages = ApiErrorUtils.createErrorContainer();
        validateAdapterSchema(errorMessages, adapter);
        if (ApiErrorUtils.hasRequestErrors(errorMessages)) {
            return ApiErrorUtils.badRequest(errorMessages);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Updating adapter \"{}\".", adapterId);
        }
        final Map<String, Object> config = configConverter.convertConfigToMaps(adapter.getConfig());
        try {
            protocolAdapterManager.updateAdapterConfig(adapter.getProtocolAdapterType(), adapterId, config);
        } catch (final Exception e) {
            log.error("Exception during update of adapter '{}'.", adapterId);
            log.debug("Original Exception:", e);
            return Response.serverError().build();
        }
        return Response.ok().build();
    }

    @Override
    public @NotNull Response deleteAdapter(final @NotNull String adapterId) {
        final Optional<ProtocolAdapterWrapper> instance = protocolAdapterManager.getAdapterById(adapterId);
        if (instance.isEmpty()) {
            return ApiErrorUtils.notFound("Adapter not found");
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Deleting adapter \"{}\".", adapterId);
        }
        protocolAdapterManager.deleteAdapter(adapterId);

        return Response.ok().build();
    }

    @Override
    public @NotNull Response changeStatus(
            final @NotNull String adapterId, final @NotNull StatusTransitionCommand command) {
        final ApiErrorMessages errorMessages = ApiErrorUtils.createErrorContainer();
        ApiErrorUtils.validateRequiredField(errorMessages, "id", adapterId, false);
        ApiErrorUtils.validateRequiredFieldRegex(errorMessages, "id", adapterId, HiveMQEdgeConstants.ID_REGEX);
        ApiErrorUtils.validateRequiredEntity(errorMessages, "command", command);
        if (protocolAdapterManager.getAdapterById(adapterId).isEmpty()) {
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
                    protocolAdapterManager.stop(adapterId).thenRun(() -> protocolAdapterManager.start(adapterId));
                    break;
            }
            return Response.ok(StatusTransitionResult.pending(ApiConstants.ADAPTER_TYPE,
                    adapterId,
                    ApiConstants.DEFAULT_TRANSITION_WAIT_TIMEOUT)).build();
        }
    }

    @Override
    public @NotNull Response getStatus(final @NotNull String adapterId) {

        final ApiErrorMessages errorMessages = ApiErrorUtils.createErrorContainer();
        ApiErrorUtils.validateRequiredField(errorMessages, "id", adapterId, false);
        ApiErrorUtils.validateRequiredFieldRegex(errorMessages, "id", adapterId, HiveMQEdgeConstants.ID_REGEX);
        if (protocolAdapterManager.getAdapterById(adapterId).isEmpty()) {
            return ApiErrorUtils.notFound(String.format("Adapter not found by id '%s'", adapterId));
        }
        if (ApiErrorUtils.hasRequestErrors(errorMessages)) {
            return ApiErrorUtils.badRequest(errorMessages);
        } else {
            return Response.status(200).entity(getStatusInternal(adapterId)).build();
        }
    }

    protected @NotNull Status getStatusInternal(final @NotNull String adapterId) {
        final Optional<ProtocolAdapterWrapper> optionalAdapterInstance =
                protocolAdapterManager.getAdapterById(adapterId);
        return optionalAdapterInstance.map(AdapterStatusModelConversionUtils::getAdapterStatus)
                .orElseGet(() -> Status.unknown(Status.RUNTIME_STATUS.STOPPED, ApiConstants.ADAPTER_TYPE, adapterId));
    }

    protected void validateAdapterSchema(
            final @NotNull ApiErrorMessages apiErrorMessages, final @NotNull Adapter adapter) {
        final ProtocolAdapterInformation information =
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

        final ProtocolAdapterSchemaManager protocolAdapterSchemaManager = new ProtocolAdapterSchemaManager(objectMapper,
                protocolAdapterWritingService.writingEnabled() ?
                        information.configurationClassWritingAndReading() :
                        information.configurationClassReading());
        final ProtocolAdapterValidator validator =
                (objectMapper, config) -> protocolAdapterSchemaManager.validateObject(config);
        final List<ProtocolAdapterValidationFailure> errors =
                validator.validateConfiguration(objectMapper, adapter.getConfig());
        errors.forEach(e -> ApiErrorUtils.addValidationError(apiErrorMessages, e.getFieldName(), e.getMessage()));
    }

    @Override
    public @NotNull Response status() {
        final ImmutableList.Builder<Status> builder = new ImmutableList.Builder<>();
        final Map<String, ProtocolAdapterWrapper> adapters = protocolAdapterManager.getProtocolAdapters();
        for (final ProtocolAdapterWrapper instance : adapters.values()) {
            builder.add(AdapterStatusModelConversionUtils.getAdapterStatus(instance));
        }
        return Response.status(200).entity(new StatusList(builder.build())).build();
    }


    @Override
    public @NotNull Response getDomainTagsForAdapter(final @NotNull String adapterId) {
        return protocolAdapterManager.getTagsForAdapter(adapterId).map(tags -> {
            if (tags.isEmpty()) {
                return Response.ok(new DomainTagModelList(List.of())).build();
            } else {
                final List<DomainTagModel> domainTagModels =
                        tags.stream().map(DomainTagModel::fromDomainTag).collect(Collectors.toList());
                return Response.ok(new DomainTagModelList(domainTagModels)).build();
            }
        }).orElse(Response.status(Response.Status.NOT_FOUND).build());
    }

    @Override
    public @NotNull Response addAdapterDomainTag(
            final @NotNull String adapterId, final @NotNull DomainTagModel domainTag) {
        final DomainTagAddResult domainTagAddResult =
                protocolAdapterManager.addDomainTag(adapterId, DomainTag.fromDomainTagEntity(domainTag, adapterId));
        switch (domainTagAddResult.getDomainTagPutStatus()) {
            case SUCCESS:
                return Response.ok().build();
            case ALREADY_EXISTS:
                final @NotNull String tagName = domainTag.getName();
                return ErrorResponseUtil.alreadyExists("The tag '" +
                        tagName +
                        "' cannot be created since another item already exists with the same id.");
            case ADAPTER_MISSING:
                log.warn("Tags could not be added for adapter '{}' because the adapter was not found.", adapterId);
                return ErrorResponseUtil.errorResponse(HttpStatus.NOT_FOUND_404,
                        "Adapter not found",
                        "The adapter named '" + adapterId + "' does not exist.");
            default:
                log.error("Unhandled PUT-status: {}", domainTagAddResult.getDomainTagPutStatus());
        }
        return Response.serverError().build();
    }

    @Override
    public @NotNull Response deleteDomainTag(
            final @NotNull String adapterId, final @NotNull String tagName) {
        final String decodedTagName = URLDecoder.decode(tagName, StandardCharsets.UTF_8);

        final DomainTagDeleteResult domainTagDeleteResult =
                protocolAdapterManager.deleteDomainTag(adapterId, decodedTagName);
        switch (domainTagDeleteResult.getDomainTagDeleteStatus()) {
            case SUCCESS:
                return Response.ok().build();
            case NOT_FOUND:
                return ErrorResponseUtil.notFound("Tag", decodedTagName);
        }
        return Response.serverError().build();
    }

    @Override
    public @NotNull Response updateDomainTag(
            final @NotNull String adapterId, final @NotNull String tagName, final @NotNull DomainTagModel domainTag) {
        final String decodedTagName = URLDecoder.decode(tagName, StandardCharsets.UTF_8);
        log.info("Updating tag with name {}", decodedTagName);
        final DomainTagUpdateResult domainTagUpdateResult =
                protocolAdapterManager.updateDomainTag(DomainTag.fromDomainTagEntity(domainTag, adapterId));
        switch (domainTagUpdateResult.getDomainTagUpdateStatus()) {
            case SUCCESS:
                return Response.ok().build();
            case ADAPTER_NOT_FOUND:
                return Response.status(403).entity("").build();
            case INTERNAL_ERROR:
                return Response.serverError().build();
        }
        log.error("UpdateResult '{}' was not handled in the method.", domainTagUpdateResult.getDomainTagUpdateStatus());
        return Response.serverError().build();
    }

    @Override
    public @NotNull Response updateDomainTags(
            final @NotNull String adapterId, final @NotNull DomainTagModelList domainTagList) {
        final Set<DomainTag> domainTags = domainTagList.getItems()
                .stream()
                .map(e -> DomainTag.fromDomainTagEntity(e, adapterId))
                .collect(Collectors.toSet());
        final DomainTagUpdateResult domainTagUpdateResult =
                protocolAdapterManager.updateDomainTags(adapterId, domainTags);
        switch (domainTagUpdateResult.getDomainTagUpdateStatus()) {
            case SUCCESS:
                return Response.ok().build();
            case ADAPTER_NOT_FOUND:
                return ErrorResponseUtil.notFound("adapter", adapterId);
            case ALREADY_USED_BY_ANOTHER_ADAPTER:
                //noinspection DataFlowIssue cant be null here.
                final @NotNull String tagName = domainTagUpdateResult.getErrorMessage();
                return ErrorResponseUtil.alreadyExists("The tag '" +
                        tagName +
                        "' cannot be created since another item already exists with the same id.");
            case INTERNAL_ERROR:
                return Response.serverError().build();
        }
        log.error("UpdateResult '{}' was not handled in the method.", domainTagUpdateResult.getDomainTagUpdateStatus());
        return Response.serverError().build();
    }

    @Override
    public @NotNull Response getDomainTags() {
        final List<DomainTag> domainTags = protocolAdapterManager.getDomainTags();
        if (domainTags.isEmpty()) {
            // empty list is also 200 as discussed.
            return Response.ok().entity(new DomainTagModelList(List.of())).build();
        }
        final List<DomainTagModel> domainTagModels =
                domainTags.stream().map(DomainTagModel::fromDomainTag).collect(Collectors.toList());
        return Response.ok().entity(new DomainTagModelList(domainTagModels)).build();
    }

    @Override
    public @NotNull Response getDomainTag(final @NotNull String tagName) {
        final String decodedTagName = URLDecoder.decode(tagName, StandardCharsets.UTF_8);
        return protocolAdapterManager.getDomainTagByName(decodedTagName)
                .map(tag -> Response.ok().entity(DomainTagModel.fromDomainTag(tag)).build())
                .orElse(ErrorResponseUtil.notFound("Tag", tagName));
    }

    @Override
    public @NotNull Response getTagSchema(final @NotNull String protocolId) {
        return protocolAdapterManager.getAdapterTypeById(protocolId)
                .map(info -> Response.status(200)
                        .entity(new TagSchema(protocolId,
                                customConfigSchemaGenerator.generateJsonSchema(info.tagConfigurationClass())))
                        .build())
                .orElseGet(() -> {
                    log.warn(
                            "Json Schema for tags for protocols of type '{}' could not be generated because the protocol id is unknown ton this edge instance.",
                            protocolId);
                    return ErrorResponseUtil.errorResponse(404,
                            "Missing protocol adapter for protocol id: " + protocolId,
                            "No protocol adapter for protocol id " + protocolId + "is installed");
                });
    }

    @Override
    public @NotNull Response getWritingSchema(final @NotNull String adapterId, final @NotNull String tagName) {
        final String decodedTagName = URLDecoder.decode(tagName, StandardCharsets.UTF_8);

        final Optional<ProtocolAdapterWrapper> optionalProtocolAdapterWrapper =
                protocolAdapterManager.getAdapterById(adapterId);
        if (optionalProtocolAdapterWrapper.isEmpty()) {
            log.warn("The Json Schema for an adapter '{}' was requested, but the adapter does not exist.", adapterId);
            return ErrorResponseUtil.notFound("Adapter", adapterId);
        }

        final com.hivemq.adapter.sdk.api.ProtocolAdapter adapter = optionalProtocolAdapterWrapper.get().getAdapter();

        if (!(adapter instanceof WritingProtocolAdapter)) {
            log.warn("The Json Schema for an adapter '{}' was requested, which does not support writing to PLCs.",
                    adapterId);
            return ErrorResponseUtil.errorResponse(404,
                    "Operation not supported.",
                    "The adapter with id '" + adapterId + "' exists, but it does not support writing to PLCs.");
        }

        final TagSchemaCreationOutputImpl tagSchemaCreationOutput = new TagSchemaCreationOutputImpl();
        adapter.createTagSchema(new TagSchemaCreationInputImpl(decodedTagName), tagSchemaCreationOutput);

        try {
            final JsonNode jsonSchemaRootNode = tagSchemaCreationOutput.getFuture().get();
            return Response.ok().entity(jsonSchemaRootNode).build();
        } catch (final InterruptedException e) {
            log.warn("Creation of json schema for writing to PLCs were interrupted.");
            log.debug("Original exception: ", e);
            return Response.serverError().build();
        } catch (final ExecutionException e) {
            if (e.getCause() instanceof UnsupportedOperationException) {
                return ErrorResponseUtil.errorResponse(404, "Operation not supported", e.getCause().getMessage());
            } else if (e.getCause() instanceof IllegalStateException) {
                return ErrorResponseUtil.errorResponse(404, "Adapter not started", e.getCause().getMessage());
            } else {
                log.warn("Exception was raised during creation of json schema for writing to PLCs.");
                log.debug("Original exception: ", e);
                return Response.serverError().build();
            }
        }
    }


    @Override
    public @NotNull Response addCompleteAdapter(
            final @NotNull String adapterType,
            final @NotNull String adapterName,
            final @NotNull AdapterConfigModel adapter) {
        final Optional<ProtocolAdapterInformation> protocolAdapterInformation =
                protocolAdapterManager.getAdapterTypeById(adapterType);
        if (protocolAdapterInformation.isEmpty()) {
            return ApiErrorUtils.notFound("No Adapter Type was found by the given adapterType '" + adapterType + "'.");
        }
        final ApiErrorMessages errorMessages = ApiErrorUtils.createErrorContainer();
        final String adapterId = adapter.getAdapter().getId();
        final Optional<ProtocolAdapterWrapper> instance = protocolAdapterManager.getAdapterById(adapterId);
        if (instance.isPresent()) {
            ApiErrorUtils.addValidationError(errorMessages, "id", "Adapter ID must be unique in system");
            return ApiErrorUtils.badRequest(errorMessages);
        }
        validateAdapterSchema(errorMessages, adapter.getAdapter());
        if (ApiErrorUtils.hasRequestErrors(errorMessages)) {
            return ApiErrorUtils.badRequest(errorMessages);
        }
        try {
            final Map<String, Object> config = configConverter.convertConfigToMaps(adapter.getAdapter().getConfig());
            final ProtocolSpecificAdapterConfig protocolSpecificAdapterConfig =
                    configConverter.convertAdapterConfig(adapterType, config, protocolAdapterManager.writingEnabled());

            final List<Map<String, Object>> domainTags = adapter.getDomainTagModels()
                    .stream()
                    .map(dtm -> DomainTag.fromDomainTagEntity(dtm, adapterId))
                    .map(DomainTag::toTagMap)
                    .collect(Collectors.toList());

            final List<? extends Tag> tags = configConverter.mapsToTags(adapterType, domainTags);

            final List<NorthboundMapping> northboundMappings = adapter.getNorthboundMappingModels()
                    .stream()
                    .map(NorthboundMappingModel::to)
                    .collect(Collectors.toList());

            final List<SouthboundMapping> southboundMappings = adapter.getSouthboundMappingModels()
                    .stream()
                    .map(SouthboundMappingModel::toToEdgeMapping)
                    .collect(Collectors.toList());

            protocolAdapterManager.addAdapter(new ProtocolAdapterConfig(adapterId,
                    adapterType,
                    protocolSpecificAdapterConfig,
                    southboundMappings,
                    northboundMappings,
                    tags));
        } catch (final IllegalArgumentException e) {
            if (e.getCause() instanceof UnrecognizedPropertyException) {
                ApiErrorUtils.addValidationError(errorMessages,
                        ((UnrecognizedPropertyException) e.getCause()).getPropertyName(),
                        "Unknown field on adapter configuration");
            } else {
                log.error("Error processing incoming request", e);
            }
            return ApiErrorUtils.badRequest(errorMessages);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Added protocol adapter of type {} with ID {}.", adapterType, adapter.getAdapter().getId());
        }
        return Response.ok().build();
    }

    @Override
    public Response getNorthboundMappingsForAdapter(final @NotNull String adapterId) {
        return protocolAdapterManager.getAdapterById(adapterId)
                .map(adapter -> adapter.getFromEdgeMappings()
                        .stream()
                        .map(NorthboundMappingModel::from)
                        .collect(Collectors.toList()))
                .map(NorthboundMappingListModel::new)
                .map(mappingsList -> Response.status(200).entity(mappingsList).build())
                .orElseGet(() -> ApiErrorUtils.notFound("Adapter not found"));
    }


    @Override
    public @NotNull Response getAllNorthboundMappings() {
        final List<NorthboundMappingListModel> northboundMappingListModels =
                protocolAdapterManager.getProtocolAdapters()
                        .values()
                        .stream()
                        .map(adapter -> adapter.getFromEdgeMappings()
                                .stream()
                                .map(NorthboundMappingModel::from)
                                .collect(Collectors.toList()))
                        .map(NorthboundMappingListModel::new)
                        .collect(Collectors.toList());
        return Response.status(200).entity(northboundMappingListModels).build();
    }


    @Override
    public @NotNull Response getAllSouthboundMappings() {
        final List<SouthboundMappingListModel> southboundMappingListModels =
                protocolAdapterManager.getProtocolAdapters()
                        .values()
                        .stream()
                        .map(adapter -> adapter.getToEdgeMappings()
                                .stream()
                                .map(SouthboundMappingModel::from)
                                .collect(Collectors.toList()))
                        .map(SouthboundMappingListModel::new)
                        .collect(Collectors.toList());
        return Response.status(200).entity(southboundMappingListModels).build();
    }

    @Override
    public Response updateNorthboundMappingsForAdapter(
            final @NotNull String adapterId, final @NotNull NorthboundMappingListModel northboundMappingListModel) {
        return protocolAdapterManager.getAdapterById(adapterId).map(adapter -> {
            final Set<String> requiredTags = new HashSet<>();
            final List<NorthboundMapping> converted = northboundMappingListModel.getItems().stream().map(mapping -> {
                requiredTags.add(mapping.getTagName());
                return mapping.to();
            }).collect(Collectors.toList());
            adapter.getTags().forEach(tag -> requiredTags.remove(tag.getName()));

            // TODO for now simulation does not need tags
            if (adapter.getProtocolAdapterInformation().getProtocolId().equals("simulation")) {
                requiredTags.clear();
            }

            if (requiredTags.isEmpty()) {
                if (protocolAdapterManager.updateAdapterFromMappings(adapterId, converted)) {
                    log.info("Successfully updated fromMappings for adapter '{}'.", adapterId);
                    return Response.status(200).entity(northboundMappingListModel).build();
                } else {
                    log.error("Something went wrong updating the adapter {}", adapterId);
                    return Response.status(500).build();
                }
            } else {
                log.error("The following tags were missing for updating the fromMappings for adapter {}: {}",
                        adapterId,
                        requiredTags);
                return Response.status(500).build();
            }
        }).orElseGet(() -> ApiErrorUtils.notFound("Adapter not found"));
    }

    @Override
    public Response getSouthboundMappingsForAdapter(final @NotNull String adapterId) {
        return protocolAdapterManager.getAdapterById(adapterId)
                .map(adapter -> adapter.getToEdgeMappings()
                        .stream()
                        .map(SouthboundMappingModel::from)
                        .collect(Collectors.toList()))
                .map(SouthboundMappingListModel::new)
                .map(mappingsList -> Response.status(200).entity(mappingsList).build())
                .orElseGet(() -> ApiErrorUtils.notFound("Adapter not found"));
    }

    @Override
    public Response updateSouthboundMappingsForAdapter(
            final @NotNull String adapterId, final @NotNull SouthboundMappingListModel southboundMappingListModel) {
        return protocolAdapterManager.getAdapterById(adapterId).map(adapter -> {
            final Set<String> requiredTags = new HashSet<>();
            final List<SouthboundMapping> converted = southboundMappingListModel.getItems().stream().map(mapping -> {
                requiredTags.add(mapping.getTagName());
                return mapping.toToEdgeMapping();
            }).collect(Collectors.toList());
            adapter.getTags().forEach(tag -> requiredTags.remove(tag.getName()));

            if (requiredTags.isEmpty()) {
                if (protocolAdapterManager.updateAdapterToMappings(adapterId, converted)) {
                    log.info("Successfully updated fromMappings for adapter '{}'.", adapterId);
                    return Response.status(200).entity(southboundMappingListModel).build();
                } else {
                    log.error("Something went wrong updating the adapter {}", adapterId);
                    return Response.status(500).build();
                }
            } else {
                log.error("The following tags were missing for updating the fromMappings for adapter {}: {}",
                        adapterId,
                        requiredTags);
                return Response.status(500).build();
            }
        }).orElseGet(() -> ApiErrorUtils.notFound("Adapter not found"));
    }
}
