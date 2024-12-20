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
import com.google.common.collect.ImmutableList;
import com.hivemq.adapter.sdk.api.ProtocolAdapterCapability;
import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.config.ProtocolSpecificAdapterConfig;
import com.hivemq.adapter.sdk.api.discovery.ProtocolAdapterDiscoveryInput;
import com.hivemq.adapter.sdk.api.tag.Tag;
import com.hivemq.adapter.sdk.api.writing.WritingProtocolAdapter;
import com.hivemq.api.AbstractApi;
import com.hivemq.api.adapters.AdapterConfigModel;
import com.hivemq.api.errors.InvalidInputError;
import com.hivemq.api.format.DataUrl;
import com.hivemq.api.errors.AlreadyExistsError;
import com.hivemq.api.errors.BadRequestError;
import com.hivemq.api.errors.InternalServerError;
import com.hivemq.api.errors.adapters.AdapterFailedSchemaValidationError;
import com.hivemq.api.errors.adapters.AdapterFailedValidationError;
import com.hivemq.api.errors.adapters.AdapterNotFound403Error;
import com.hivemq.api.errors.adapters.AdapterNotFoundError;
import com.hivemq.api.errors.adapters.AdapterOperationNotSupportedError;
import com.hivemq.api.errors.adapters.AdapterTypeNotFoundError;
import com.hivemq.api.errors.adapters.AdapterTypeReadOnlyError;
import com.hivemq.api.errors.adapters.DomainTagNotFoundError;
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
import com.hivemq.persistence.domain.DomainTag;
import com.hivemq.persistence.domain.DomainTagAddResult;
import com.hivemq.persistence.domain.DomainTagDeleteResult;
import com.hivemq.persistence.domain.DomainTagUpdateResult;
import com.hivemq.persistence.mappings.NorthboundMapping;
import com.hivemq.persistence.mappings.SouthboundMapping;
import com.hivemq.persistence.topicfilter.TopicFilter;
import com.hivemq.persistence.topicfilter.TopicFilterPersistence;
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
import java.util.Base64;
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
    private final @NotNull TopicFilterPersistence topicFilterPersistence;
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
            final @NotNull ProtocolAdapterConfigConverter configConverter,
            final @NotNull TopicFilterPersistence topicFilterPersistence) {
        this.remoteService = remoteService;
        this.configurationService = configurationService;
        this.protocolAdapterManager = protocolAdapterManager;
        this.objectMapper = ProtocolAdapterUtils.createProtocolAdapterMapper(objectMapper);
        this.versionProvider = versionProvider;
        this.protocolAdapterWritingService = protocolAdapterWritingService;
        this.configConverter = configConverter;
        this.topicFilterPersistence = topicFilterPersistence;
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

        return Response.ok(new ProtocolAdaptersList(new ArrayList<>(installedAdapters))).build();
    }

    @Override
    public @NotNull Response getAdapters() {
        final List<Adapter> adapters = protocolAdapterManager.getProtocolAdapters()
                .values()
                .stream()
                .map(this::convertToAdapter)
                .collect(Collectors.toUnmodifiableList());
        return Response.ok(new AdaptersList(adapters)).build();
    }

    @Override
    public @NotNull Response getAdaptersForType(final @NotNull String adapterType) {
        final Optional<ProtocolAdapterInformation> protocolAdapterType =
                protocolAdapterManager.getAdapterTypeById(adapterType);
        if (protocolAdapterType.isEmpty()) {
            return ErrorResponseUtil.errorResponse(new AdapterTypeNotFoundError(String.format("Adapter not found '%s'",
                    adapterType)));
        }
        final List<Adapter> adapters = protocolAdapterManager.getProtocolAdapters()
                .values()
                .stream()
                .filter(adapterInstance -> adapterInstance.getAdapterInformation().getProtocolId().equals(adapterType))
                .map(this::convertToAdapter)
                .collect(Collectors.toUnmodifiableList());

        return Response.ok(new AdaptersList(adapters)).build();
    }

    @Override
    public @NotNull Response getAdapter(final @NotNull String adapterId) {
        final Optional<ProtocolAdapterWrapper> instance = protocolAdapterManager.getAdapterById(adapterId);
        if (instance.isEmpty()) {
            return ErrorResponseUtil.errorResponse(new AdapterNotFoundError(String.format("Adapter not found '%s'",
                    adapterId)));
        }
        return Response.ok(convertToAdapter(instance.get())).build();
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
            return ErrorResponseUtil.errorResponse(new AdapterNotFoundError(String.format("Adapter not found '%s'",
                    adapterId)));
        }

        final ProtocolAdapterWrapper adapterInstance = instance.get();
        if (!adapterInstance.getAdapterInformation().getCapabilities().contains(ProtocolAdapterCapability.DISCOVER)) {
            return ErrorResponseUtil.errorResponse(new AdapterFailedValidationError("Adapter does not support discovery"));
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
            return ErrorResponseUtil.errorResponse(new InternalServerError("Exception during discovery."));
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Thread was interrupted during discovery for adapter '{}'", adapterId);
            return ErrorResponseUtil.errorResponse(new InternalServerError("Exception during discovery."));
        } catch (final Exception e) {
            log.warn("Exception was thrown during discovery for adapter '{}'.", adapterId);
            return ErrorResponseUtil.errorResponse(new InternalServerError("Exception during discovery."));
        } finally {
            Thread.currentThread().setContextClassLoader(contextClassLoader);
        }
        final NodeTreeImpl nodeTree = output.getNodeTree();
        final List<NodeTreeImpl.ObjectNode> children = nodeTree.getRootNode().getChildren();
        return Response.ok(new ValuesTree(children)).build();
    }

    @Override
    public @NotNull Response addAdapter(final @NotNull String adapterType, final @NotNull Adapter adapter) {
        final Optional<ProtocolAdapterInformation> protocolAdapterType =
                protocolAdapterManager.getAdapterTypeById(adapterType);
        if (protocolAdapterType.isEmpty()) {
            return ErrorResponseUtil.errorResponse(new AdapterTypeNotFoundError(String.format("Adapter not found '%s'",
                    adapterType)));
        }
        final ApiErrorMessages errorMessages = ApiErrorUtils.createErrorContainer();
        final Optional<ProtocolAdapterWrapper> instance = protocolAdapterManager.getAdapterById(adapter.getId());
        if (instance.isPresent()) {
            ApiErrorUtils.addValidationError(errorMessages, "id", "Adapter ID must be unique in system");
            return ErrorResponseUtil.errorResponse(new AdapterFailedSchemaValidationError(errorMessages.toErrorList()));
        }
        validateAdapterSchema(errorMessages, adapter);
        if (ApiErrorUtils.hasRequestErrors(errorMessages)) {
            return ErrorResponseUtil.errorResponse(new AdapterFailedSchemaValidationError(errorMessages.toErrorList()));
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
            return ErrorResponseUtil.errorResponse(new AdapterFailedSchemaValidationError(errorMessages.toErrorList()));
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
            return ErrorResponseUtil.errorResponse(new AdapterNotFoundError(String.format("Adapter not found '%s'",
                    adapterId)));
        }
        final ApiErrorMessages errorMessages = ApiErrorUtils.createErrorContainer();
        validateAdapterSchema(errorMessages, adapter);
        if (ApiErrorUtils.hasRequestErrors(errorMessages)) {
            return ErrorResponseUtil.errorResponse(new AdapterFailedSchemaValidationError(errorMessages.toErrorList()));
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
            return ErrorResponseUtil.errorResponse(new InternalServerError(null));
        }
        return Response.ok().build();
    }

    @Override
    public @NotNull Response deleteAdapter(final @NotNull String adapterId) {
        final Optional<ProtocolAdapterWrapper> instance = protocolAdapterManager.getAdapterById(adapterId);
        if (instance.isEmpty()) {
            return ErrorResponseUtil.errorResponse(new AdapterNotFoundError(String.format("Adapter not found '%s'",
                    adapterId)));
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
            return ErrorResponseUtil.errorResponse(new AdapterNotFoundError(String.format("Adapter not found '%s'",
                    adapterId)));
        }
        if (ApiErrorUtils.hasRequestErrors(errorMessages)) {
            return ErrorResponseUtil.errorResponse(new AdapterFailedSchemaValidationError(errorMessages.toErrorList()));
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
        if (ApiErrorUtils.hasRequestErrors(errorMessages)) {
            return ErrorResponseUtil.errorResponse(new AdapterFailedSchemaValidationError(errorMessages.toErrorList()));
        }
        if (protocolAdapterManager.getAdapterById(adapterId).isEmpty()) {
            return ErrorResponseUtil.errorResponse(new AdapterNotFoundError(String.format("Adapter not found '%s'",
                    adapterId)));
        }
        return Response.ok(getStatusInternal(adapterId)).build();
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
                        information.configurationClassNorthAndSouthbound() :
                        information.configurationClassNorthbound());
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
        return Response.ok(new StatusList(builder.build())).build();
    }


    @Override
    public @NotNull Response getDomainTagsForAdapter(final @NotNull String adapterId) {
        return protocolAdapterManager.getTagsForAdapter(adapterId)
                .map(tags -> {
                    if (tags.isEmpty()) {
                        return Response.ok(new DomainTagModelList(List.of())).build();
                    } else {
                        final List<DomainTagModel> domainTagModels =
                                tags.stream().map(DomainTagModel::fromDomainTag).collect(Collectors.toList());
                        return Response.ok(new DomainTagModelList(domainTagModels)).build();
                    }
                })
                .orElse(ErrorResponseUtil.errorResponse(new AdapterNotFoundError(String.format("Adapter not found '%s'",
                        adapterId))));
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
                return ErrorResponseUtil.errorResponse(new AlreadyExistsError("The tag '" +
                        tagName +
                        "' cannot be created since another item already exists with the same id."));
            case ADAPTER_MISSING:
                log.warn("Tags could not be added for adapter '{}' because the adapter was not found.", adapterId);
                return ErrorResponseUtil.errorResponse(new AdapterNotFoundError(String.format("Adapter not found '%s'",
                        adapterId)));
            default:
                log.error("Unhandled PUT-status: {}", domainTagAddResult.getDomainTagPutStatus());
                return ErrorResponseUtil.errorResponse(new InternalServerError(null));
        }
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
                return ErrorResponseUtil.errorResponse(new DomainTagNotFoundError(decodedTagName));
            default:
                log.error("Unhandled DELETE-status: {}", domainTagDeleteResult.getDomainTagDeleteStatus());
                return ErrorResponseUtil.errorResponse(new InternalServerError(null));
        }
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
                return ErrorResponseUtil.errorResponse(new AdapterNotFound403Error("Adapter not found"));
            case INTERNAL_ERROR:
            default:
                log.error("Unhandled UPDATE-status: {}", domainTagUpdateResult.getDomainTagUpdateStatus());
                return ErrorResponseUtil.errorResponse(new InternalServerError(null));
        }
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
                return ErrorResponseUtil.errorResponse(new AdapterNotFoundError("Adapter not found"));
            case ALREADY_USED_BY_ANOTHER_ADAPTER:
                //noinspection DataFlowIssue cant be null here.
                final @NotNull String tagName = domainTagUpdateResult.getErrorMessage();
                return ErrorResponseUtil.errorResponse(new AlreadyExistsError("The tag '" +
                        tagName +
                        "' cannot be created since another item already exists with the same id."));
            case INTERNAL_ERROR:
            default:
                log.error("Unhandled UPDATE-status: {}", domainTagUpdateResult.getDomainTagUpdateStatus());
                return ErrorResponseUtil.errorResponse(new InternalServerError(null));
        }
    }

    @Override
    public @NotNull Response getDomainTags() {
        final List<DomainTag> domainTags = protocolAdapterManager.getDomainTags();
        if (domainTags.isEmpty()) {
            // empty list is also 200 as discussed.
            return Response.ok(new DomainTagModelList(List.of())).build();
        }
        final List<DomainTagModel> domainTagModels =
                domainTags.stream().map(DomainTagModel::fromDomainTag).collect(Collectors.toList());
        return Response.ok(new DomainTagModelList(domainTagModels)).build();
    }

    @Override
    public @NotNull Response getDomainTag(final @NotNull String tagName) {
        final String decodedTagName = URLDecoder.decode(tagName, StandardCharsets.UTF_8);
        return protocolAdapterManager.getDomainTagByName(decodedTagName)
                .map(tag -> Response.ok(DomainTagModel.fromDomainTag(tag)).build())
                .orElse(ErrorResponseUtil.errorResponse(new DomainTagNotFoundError(decodedTagName)));
    }

    @Override
    public @NotNull Response getTagSchema(final @NotNull String protocolId) {
        return protocolAdapterManager.getAdapterTypeById(protocolId)
                .map(info -> Response.ok(new TagSchema(protocolId,
                        customConfigSchemaGenerator.generateJsonSchema(info.tagConfigurationClass()))).build())
                .orElseGet(() -> {
                    log.warn(
                            "Json Schema for tags for protocols of type '{}' could not be generated because the protocol id is unknown ton this edge instance.",
                            protocolId);
                    return ErrorResponseUtil.errorResponse(new AdapterTypeNotFoundError(String.format(
                            "Adapter not found '%s'",
                            protocolId)));
                });
    }

    @Override
    public @NotNull Response getWritingSchema(final @NotNull String adapterId, final @NotNull String tagName) {
        final String decodedTagName = URLDecoder.decode(tagName, StandardCharsets.UTF_8);

        final Optional<ProtocolAdapterWrapper> optionalProtocolAdapterWrapper =
                protocolAdapterManager.getAdapterById(adapterId);
        if (optionalProtocolAdapterWrapper.isEmpty()) {
            log.warn("The Json Schema for an adapter '{}' was requested, but the adapter does not exist.", adapterId);
            return ErrorResponseUtil.errorResponse(new AdapterNotFoundError(String.format("Adapter not found '%s'",
                    adapterId)));
        }

        final com.hivemq.adapter.sdk.api.ProtocolAdapter adapter = optionalProtocolAdapterWrapper.get().getAdapter();

        if (!(adapter instanceof WritingProtocolAdapter)) {
            log.warn("The Json Schema for an adapter '{}' was requested, which does not support writing to PLCs.",
                    adapterId);
            return ErrorResponseUtil.errorResponse(new AdapterTypeReadOnlyError("The adapter with id '" +
                    adapterId +
                    "' exists, but it does not support writing to PLCs."));
        }

        final TagSchemaCreationOutputImpl tagSchemaCreationOutput = new TagSchemaCreationOutputImpl();
        adapter.createTagSchema(new TagSchemaCreationInputImpl(decodedTagName), tagSchemaCreationOutput);

        try {
            final JsonNode jsonSchemaRootNode = tagSchemaCreationOutput.getFuture().get();
            return Response.ok(jsonSchemaRootNode).build();
        } catch (final InterruptedException e) {
            log.warn("Creation of json schema for writing to PLCs were interrupted.");
            log.debug("Original exception: ", e);
            return ErrorResponseUtil.errorResponse(new InternalServerError(null));
        } catch (final ExecutionException e) {
            switch (tagSchemaCreationOutput.getStatus()) {
                case NOT_SUPPORTED:
                    return ErrorResponseUtil.errorResponse(new AdapterOperationNotSupportedError(String.format(
                            "Operation not supported '%s'",
                            e.getCause().getMessage())));
                case ADAPTER_NOT_STARTED:
                    return ErrorResponseUtil.errorResponse(new AdapterOperationNotSupportedError(String.format(
                            "Adapter not started '%s'",
                            e.getCause().getMessage())));
                case TAG_NOT_FOUND:
                    return ErrorResponseUtil.errorResponse(new DomainTagNotFoundError(tagName));
                default:
                    log.warn("Exception was raised during creation of json schema for writing to PLCs.");
                    log.debug("Original exception: ", e);
                    return ErrorResponseUtil.errorResponse(new InternalServerError(null));
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
            return ErrorResponseUtil.errorResponse(new AdapterTypeNotFoundError(String.format("Adapter not found '%s'",
                    adapterType)));
        }
        final ApiErrorMessages errorMessages = ApiErrorUtils.createErrorContainer();
        final String adapterId = adapter.getAdapter().getId();
        final Optional<ProtocolAdapterWrapper> instance = protocolAdapterManager.getAdapterById(adapterId);
        if (instance.isPresent()) {
            ApiErrorUtils.addValidationError(errorMessages, "id", "Adapter ID must be unique in system");
            return ErrorResponseUtil.errorResponse(new AdapterFailedSchemaValidationError(errorMessages.toErrorList()));
        }
        validateAdapterSchema(errorMessages, adapter.getAdapter());
        if (ApiErrorUtils.hasRequestErrors(errorMessages)) {
            return ErrorResponseUtil.errorResponse(new AdapterFailedSchemaValidationError(errorMessages.toErrorList()));
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

            final List<? extends Tag> tags;
            try {
                tags = configConverter.mapsToTags(adapterType, domainTags);
            } catch (final IllegalArgumentException illegalArgumentException) {
                log.warn("Unable to parse tags for adapter '{}'", adapterName);
                log.debug("Original Exception: ", illegalArgumentException);
                return ErrorResponseUtil.errorResponse(new InvalidInputError(
                        "Exception during parsing of tags for the adapter. See log for further information."));
            }

            final List<NorthboundMapping> northboundMappings = adapter.getNorthboundMappingModels()
                    .stream()
                    .map(NorthboundMappingModel::to)
                    .collect(Collectors.toList());


            final List<SouthboundMapping> southboundMappings = adapter.getSouthboundMappingModels()
                    .stream()
                    .map(this::parseAndEnrichWithSchema)
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
            return ErrorResponseUtil.errorResponse(new AdapterFailedSchemaValidationError(errorMessages.toErrorList()));
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Added protocol adapter of type {} with ID {}.", adapterType, adapter.getAdapter().getId());
        }
        return Response.ok().build();
    }


    @Override
    public @NotNull Response getNorthboundMappingsForAdapter(final @NotNull String adapterId) {
        return protocolAdapterManager.getAdapterById(adapterId)
                .map(adapter -> adapter.getFromEdgeMappings()
                        .stream()
                        .map(NorthboundMappingModel::from)
                        .collect(Collectors.toList()))
                .map(NorthboundMappingListModel::new)
                .map(mappingsList -> Response.ok(mappingsList).build())
                .orElseGet(() -> ErrorResponseUtil.errorResponse(new AdapterNotFoundError(String.format(
                        "Adapter not found '%s'",
                        adapterId))));
    }


    @Override
    public @NotNull Response getAllNorthboundMappings() {
        final List<NorthboundMappingModel> northboundMappingListModels = protocolAdapterManager.getProtocolAdapters()
                .values()
                .stream()
                .flatMap(adapter -> adapter.getFromEdgeMappings().stream().map(NorthboundMappingModel::from))
                .collect(Collectors.toList());
        return Response.status(200).entity(new NorthboundMappingListModel(northboundMappingListModels)).build();
    }


    @Override
    public @NotNull Response getAllSouthboundMappings() {
        final List<SouthboundMappingModel> southboundMappingModels = protocolAdapterManager.getProtocolAdapters()
                .values()
                .stream()
                .flatMap(adapter -> adapter.getToEdgeMappings().stream().map(SouthboundMappingModel::from))
                .collect(Collectors.toList());
        return Response.status(200).entity(new SouthboundMappingListModel(southboundMappingModels)).build();
    }

    @Override
    public Response updateNorthboundMappingsForAdapter(
            final @NotNull String adapterId, final @NotNull NorthboundMappingListModel northboundMappingListModel) {
        return protocolAdapterManager.getAdapterById(adapterId)
                .map(adapter -> {
                    final Set<String> requiredTags = new HashSet<>();
                    final List<NorthboundMapping> converted =
                            northboundMappingListModel.getItems().stream().map(mapping -> {
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
                            log.info("Successfully updated northbound mappings for adapter '{}'.", adapterId);
                            return Response.ok(northboundMappingListModel).build();
                        } else {
                            log.error("Something went wrong updating the adapter {}", adapterId);
                            return ErrorResponseUtil.errorResponse(new InternalServerError(null));
                        }
                    } else {
                        log.error(
                                "The following tags were missing for updating the northbound mappings for adapter {}: {}",
                                adapterId,
                                requiredTags);
                        return ErrorResponseUtil.errorResponse(new BadRequestError(
                                "Tags were missing for updating the northbound mappings" + requiredTags));
                    }
                })
                .orElseGet(() -> ErrorResponseUtil.errorResponse(new AdapterNotFoundError(String.format(
                        "Adapter not found '%s'",
                        adapterId))));
    }

    @Override
    public @NotNull Response getSouthboundMappingsForAdapter(final @NotNull String adapterId) {
        return protocolAdapterManager.getAdapterById(adapterId)
                .map(adapter -> adapter.getToEdgeMappings()
                        .stream()
                        .map(SouthboundMappingModel::from)
                        .collect(Collectors.toList()))
                .map(SouthboundMappingListModel::new)
                .map(mappingsList -> Response.ok(mappingsList).build())
                .orElseGet(() -> ErrorResponseUtil.errorResponse(new AdapterNotFoundError(String.format(
                        "Adapter not found '%s'",
                        adapterId))));
    }

    @Override
    public @NotNull Response updateSouthboundMappingsForAdapter(
            final @NotNull String adapterId, final @NotNull SouthboundMappingListModel southboundMappingListModel) {
        return protocolAdapterManager.getAdapterById(adapterId)
                .map(adapter -> {
                    final Set<String> requiredTags = new HashSet<>();
                    final List<SouthboundMapping> converted =
                            southboundMappingListModel.getItems().stream().map(mapping -> {
                                requiredTags.add(mapping.getTagName());
                                return parseAndEnrichWithSchema(mapping);
                            }).collect(Collectors.toList());
                    adapter.getTags().forEach(tag -> requiredTags.remove(tag.getName()));

                    if (requiredTags.isEmpty()) {
                        if (protocolAdapterManager.updateAdapterToMappings(adapterId, converted)) {
                            log.info("Successfully updated fromMappings for adapter '{}'.", adapterId);
                            return Response.ok(southboundMappingListModel).build();
                        } else {
                            log.error("Something went wrong updating the adapter {}", adapterId);
                            return ErrorResponseUtil.errorResponse(new InternalServerError(null));
                        }
                    } else {
                        log.error(
                                "The following tags were missing for updating the southbound mappings for adapter {}: {}",
                                adapterId,
                                requiredTags);
                        return ErrorResponseUtil.errorResponse(new BadRequestError(
                                "Tags were missing for updating the southbound mappings" + requiredTags));
                    }
                })
                .orElseGet(() -> ErrorResponseUtil.errorResponse(new AdapterNotFoundError(String.format(
                        "Adapter not found '%s'",
                        adapterId))));
    }


    private @NotNull SouthboundMapping parseAndEnrichWithSchema(final @NotNull SouthboundMappingModel model) {
        final TopicFilter topicFilter = topicFilterPersistence.getTopicFilter(model.getTopicFilter());
        if (topicFilter == null) {
            throw new IllegalStateException("Southbound mapping contained a topic filter '" +
                    model.getTopicFilter() +
                    "', which is unknown to Edge. Southbound mapping can not be created.");
        }

        final DataUrl schemaAsDataUrl = topicFilter.getSchema();
        if (schemaAsDataUrl == null) {
            throw new IllegalStateException("Southbound mapping contained a topic filter '" +
                    model.getTopicFilter() +
                    "', which has no schema attached. Southbound mapping can not be created.");
        }

        final String schema = new String(Base64.getDecoder().decode(schemaAsDataUrl.getData()));
        return model.toToEdgeMapping(schema);
    }
}
