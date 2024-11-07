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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.google.common.collect.ImmutableList;
import com.hivemq.adapter.sdk.api.ProtocolAdapterCapability;
import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.discovery.ProtocolAdapterDiscoveryInput;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterWritingService;
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
import com.hivemq.api.model.mapping.FieldMappingsModel;
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
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.http.HttpStatus;
import com.hivemq.persistence.domain.DomainTag;
import com.hivemq.persistence.domain.DomainTagAddResult;
import com.hivemq.persistence.domain.DomainTagDeleteResult;
import com.hivemq.persistence.domain.DomainTagUpdateResult;
import com.hivemq.protocols.ProtocolAdapterManager;
import com.hivemq.protocols.ProtocolAdapterSchemaManager;
import com.hivemq.protocols.ProtocolAdapterUtils;
import com.hivemq.protocols.ProtocolAdapterWrapper;
import com.hivemq.protocols.params.NodeTreeImpl;
import com.hivemq.protocols.tag.TagSchemaCreationInputImpl;
import com.hivemq.protocols.tag.TagSchemaCreationOutputImpl;
import com.hivemq.util.ErrorResponseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.Response;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
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
    private final @NotNull ProtocolAdapterWritingService protocolAdapterWritingService;
    private final @NotNull ObjectMapper objectMapper;
    private final @NotNull VersionProvider versionProvider;
    private final @NotNull CustomConfigSchemaGenerator customConfigSchemaGenerator = new CustomConfigSchemaGenerator();

    @Inject
    public ProtocolAdaptersResourceImpl(
            final @NotNull HiveMQEdgeRemoteService remoteService,
            final @NotNull ConfigurationService configurationService,
            final @NotNull ProtocolAdapterManager protocolAdapterManager,
            final @NotNull ProtocolAdapterWritingService protocolAdapterWritingService,
            final @NotNull ObjectMapper objectMapper,
            final @NotNull VersionProvider versionProvider) {
        this.remoteService = remoteService;
        this.configurationService = configurationService;
        this.protocolAdapterManager = protocolAdapterManager;
        this.objectMapper = ProtocolAdapterUtils.createProtocolAdapterMapper(objectMapper);
        this.versionProvider = versionProvider;
        this.protocolAdapterWritingService = protocolAdapterWritingService;
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
    public @NotNull Response getAdaptersForType(@NotNull final String adapterType) {
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
        final Optional<ProtocolAdapterWrapper<? extends com.hivemq.adapter.sdk.api.ProtocolAdapter>> instance =
                protocolAdapterManager.getAdapterById(adapterId);
        if (instance.isEmpty()) {
            return ApiErrorUtils.notFound("Adapter not found");
        }
        return Response.status(200).entity(convertToAdapter(instance.get())).build();
    }


    private @NotNull Adapter convertToAdapter(final @NotNull ProtocolAdapterWrapper<? extends com.hivemq.adapter.sdk.api.ProtocolAdapter> value) {
        final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        Map<String, Object> configObject;
        try {
            Thread.currentThread().setContextClassLoader(value.getAdapterFactory().getClass().getClassLoader());
            configObject = value.getAdapterFactory().unconvertConfigObject(objectMapper, value.getConfigObject());
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
            @NotNull final String adapterId, final @Nullable String rootNode, final @Nullable Integer depth) {

        final Optional<ProtocolAdapterWrapper<? extends com.hivemq.adapter.sdk.api.ProtocolAdapter>> instance =
                protocolAdapterManager.getAdapterById(adapterId);
        if (instance.isEmpty()) {
            return ApiErrorUtils.notFound("Adapter not found");
        }

        final ProtocolAdapterWrapper<? extends com.hivemq.adapter.sdk.api.ProtocolAdapter> adapterInstance =
                instance.get();
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
            return ApiErrorUtils.notFound("Adapter Type not found by adapterType");
        }
        final ApiErrorMessages errorMessages = ApiErrorUtils.createErrorContainer();
        final Optional<ProtocolAdapterWrapper<? extends com.hivemq.adapter.sdk.api.ProtocolAdapter>> instance =
                protocolAdapterManager.getAdapterById(adapter.getId());
        if (instance.isPresent()) {
            ApiErrorUtils.addValidationError(errorMessages, "id", "Adapter ID must be unique in system");
            return ApiErrorUtils.badRequest(errorMessages);
        }
        validateAdapterSchema(errorMessages, adapter);
        if (ApiErrorUtils.hasRequestErrors(errorMessages)) {
            return ApiErrorUtils.badRequest(errorMessages);
        }
        try {
            final Map<String, Object> config =
                    objectMapper.convertValue(adapter.getConfig(), new TypeReference<>() {
                    });
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
        final Optional<ProtocolAdapterWrapper<? extends com.hivemq.adapter.sdk.api.ProtocolAdapter>> instance =
                protocolAdapterManager.getAdapterById(adapterId);
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
        final Map<String, Object> config =
                objectMapper.convertValue(adapter.getConfig(), new TypeReference<>() {
                });
        protocolAdapterManager.updateAdapterConfig(adapterId, config);
        return Response.ok().build();
    }

    @Override
    public @NotNull Response deleteAdapter(final @NotNull String adapterId) {
        final Optional<ProtocolAdapterWrapper<? extends com.hivemq.adapter.sdk.api.ProtocolAdapter>> instance =
                protocolAdapterManager.getAdapterById(adapterId);
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
        final Optional<ProtocolAdapterWrapper<? extends com.hivemq.adapter.sdk.api.ProtocolAdapter>>
                optionalAdapterInstance = protocolAdapterManager.getAdapterById(adapterId);
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

        final ProtocolAdapterSchemaManager protocolAdapterSchemaManager =
                new ProtocolAdapterSchemaManager(objectMapper, protocolAdapterWritingService.writingEnabled() ? information.configurationClassWriting() : information.configurationClassReading());
        final ProtocolAdapterValidator validator =
                (objectMapper, config) -> protocolAdapterSchemaManager.validateObject(config);
        final List<ProtocolAdapterValidationFailure> errors =
                validator.validateConfiguration(objectMapper, adapter.getConfig());
        errors.forEach(e -> ApiErrorUtils.addValidationError(apiErrorMessages, e.getFieldName(), e.getMessage()));
    }

    @Override
    public @NotNull Response status() {
        final ImmutableList.Builder<Status> builder = new ImmutableList.Builder<>();
        final Map<String, ProtocolAdapterWrapper<? extends com.hivemq.adapter.sdk.api.ProtocolAdapter>> adapters =
                protocolAdapterManager.getProtocolAdapters();
        for (final ProtocolAdapterWrapper<? extends com.hivemq.adapter.sdk.api.ProtocolAdapter> instance : adapters.values()) {
            builder.add(AdapterStatusModelConversionUtils.getAdapterStatus(instance));
        }
        return Response.status(200).entity(new StatusList(builder.build())).build();
    }


    @Override
    public @NotNull Response getDomainTagsForAdapter(@NotNull final String adapterId) {
        return protocolAdapterManager
                .getTagsForAdapter(adapterId)
                .map(tags -> {
                    if(tags.isEmpty()) {
                        return Response.ok().build();
                    } else {
                        final List<DomainTagModel> domainTagModels =
                                tags.stream().map(DomainTagModel::fromDomainTag).collect(Collectors.toList());
                        final DomainTagModelList domainTagModelList = new DomainTagModelList(domainTagModels);
                        return Response.ok().entity(domainTagModelList).build();
                    }
                })
                .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }

    @Override
    public @NotNull Response addAdapterDomainTag(
            @NotNull final String adapterId, @NotNull final DomainTagModel domainTag) {
        final DomainTagAddResult domainTagAddResult =
                protocolAdapterManager.addDomainTag(adapterId, DomainTag.fromDomainTagEntity(domainTag, adapterId));
        switch (domainTagAddResult.getDomainTagPutStatus()) {
            case SUCCESS:
                return Response.ok().build();
            case ALREADY_EXISTS:
                final @NotNull String tagId = domainTag.getTag();
                return ErrorResponseUtil.alreadyExists("The tag '" +
                        tagId +
                        "' cannot be created since another item already exists with the same id.");
            case ADAPTER_MISSING:
                return ErrorResponseUtil.errorResponse(HttpStatus.NOT_FOUND_404,"Adapter not found", "The adapter named '" +
                        domainTag.getProtocolId() +
                        "' does not exist.");
            default:
                log.error("Unhandled PUT-statud: {}", domainTagAddResult.getDomainTagPutStatus());
        }
        return Response.serverError().build();
    }

    @Override
    public @NotNull Response deleteDomainTag(
            @NotNull final String adapterId, @NotNull final String tagIdBase64Encoded) {
        final byte[] decoded = Base64.getDecoder().decode(tagIdBase64Encoded);
        final String tagId = new String(decoded, StandardCharsets.UTF_8);

        final DomainTagDeleteResult domainTagDeleteResult = protocolAdapterManager.deleteDomainTag(adapterId, tagId);
        switch (domainTagDeleteResult.getDomainTagDeleteStatus()) {
            case SUCCESS:
                return Response.ok().build();
            case NOT_FOUND:
                return ErrorResponseUtil.notFound("Tag", tagId);
        }
        return Response.serverError().build();
    }

    @Override
    public @NotNull Response updateDomainTag(
            final @NotNull String adapterId, @NotNull final String tagId, final @NotNull DomainTagModel domainTag) {
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
                final @NotNull String tagId = domainTagUpdateResult.getErrorMessage();
                return ErrorResponseUtil.alreadyExists("The tag '" +
                        tagId +
                        "' cannot be created since another item already exists with the same id.");
            case INTERNAL_ERROR:
                return Response.serverError().build();
        }
        return Response.serverError().build();
    }

    @Override
    public @NotNull Response getDomainTags() {
        final List<DomainTag> domainTags = protocolAdapterManager.getDomainTags();
        if (domainTags.isEmpty()) {
            // empty list is also 200 as discussed.
            return Response.ok().build();
        }
        final List<DomainTagModel> domainTagModels =
                domainTags.stream().map(DomainTagModel::fromDomainTag).collect(Collectors.toList());
        return Response.ok().entity(new DomainTagModelList(domainTagModels)).build();
    }

    @Override
    public Response getDomainTag(final String tagName) {
        return protocolAdapterManager
                .getDomainTagByName(tagName)
                .map(tag -> Response.ok().entity(DomainTagModel.fromDomainTag(tag)).build())
                .orElse(ErrorResponseUtil.notFound("Tag", tagName));
    }

    @Override
    public Response getTagSchema(String protocolId) {
        return protocolAdapterManager
                .getAdapterTypeById(protocolId)
                .map(info -> Response
                                .status(200)
                                .entity(new TagSchema(protocolId, customConfigSchemaGenerator.generateJsonSchema(info.tagConfigurationClass())))
                                .build())
                .orElseGet(() -> ErrorResponseUtil
                        .errorResponse(404,
                                "Missing protocol adapter with id: " + protocolId,
                                "No protocol adapter with id " + protocolId +" exists"));
    }

    @Override
    public @NotNull Response getWritingSchema(@NotNull final String adapterId, @NotNull final String tagName) {
        final Optional<ProtocolAdapterWrapper<? extends com.hivemq.adapter.sdk.api.ProtocolAdapter>>
                optionalProtocolAdapterWrapper = protocolAdapterManager.getAdapterById(adapterId);
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
        adapter.createTagSchema(new TagSchemaCreationInputImpl(tagName), tagSchemaCreationOutput);

        try {
            final JsonNode jsonSchemaRootNode = tagSchemaCreationOutput.getFuture().get();
            return Response.ok().entity(jsonSchemaRootNode).build();
        } catch (final InterruptedException e) {
            log.warn("Creation of json schema for writing to PLCs were interrupted.");
            log.debug("Original exception: ", e);
            return Response.serverError().build();
        } catch (final ExecutionException e) {
            if(e.getCause() instanceof UnsupportedOperationException){
                return ErrorResponseUtil.errorResponse(404, "Operation not supported", e.getCause().getMessage());
            } else if(e.getCause() instanceof IllegalStateException){
                return ErrorResponseUtil.errorResponse(404, "Adapter not started", e.getCause().getMessage());
            } else {
                log.warn("Exception was raised during creation of json schema for writing to PLCs.");
                log.debug("Original exception: ", e);
                return Response.serverError().build();
            }
        }
    }

    @Override
    public Response addCompleteAdapter(
            final String adapterType,
            final String adapterName,
            final AdapterConfigModel adapter) {
        final Optional<ProtocolAdapterInformation> protocolAdapterType =
                protocolAdapterManager.getAdapterTypeById(adapterType);
        if (protocolAdapterType.isEmpty()) {
            return ApiErrorUtils.notFound("Adapter Type not found by adapterType");
        }
        final ApiErrorMessages errorMessages = ApiErrorUtils.createErrorContainer();
        final String adapterId = adapter.getAdapter().getId();
        final Optional<ProtocolAdapterWrapper<? extends com.hivemq.adapter.sdk.api.ProtocolAdapter>> instance =
                protocolAdapterManager.getAdapterById(adapterId);
        if (instance.isPresent()) {
            ApiErrorUtils.addValidationError(errorMessages, "id", "Adapter ID must be unique in system");
            return ApiErrorUtils.badRequest(errorMessages);
        }
        validateAdapterSchema(errorMessages, adapter.getAdapter());
        if (ApiErrorUtils.hasRequestErrors(errorMessages)) {
            return ApiErrorUtils.badRequest(errorMessages);
        }
        try {

            //TODO: we need to move all these conversions into a common place
            final Map<String, Object> config =
                    objectMapper.convertValue(adapter.getAdapter(), new TypeReference<>() {
                    });

            final List<Map<String, Object>> domainTags = adapter.getDomainTagModels()
                    .stream()
                    .map(dtm -> DomainTag.fromDomainTagEntity(dtm, adapterId))
                    .map(DomainTag::toTagMap)
                    .collect(Collectors.toList());
            protocolAdapterManager.addAdapter(adapterType, adapterId, config, domainTags);
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
    public @NotNull Response addFieldMapping(
            @NotNull final String adapterId,
            @NotNull final FieldMappingsModel fieldMappingsModel) {




        return null;
    }
}
