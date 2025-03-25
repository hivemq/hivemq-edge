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
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.hivemq.adapter.sdk.api.ProtocolAdapterCapability;
import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.discovery.ProtocolAdapterDiscoveryInput;
import com.hivemq.adapter.sdk.api.writing.WritingProtocolAdapter;
import com.hivemq.api.AbstractApi;
import com.hivemq.api.errors.AlreadyExistsError;
import com.hivemq.api.errors.BadRequestError;
import com.hivemq.api.errors.ConfigWritingDisabled;
import com.hivemq.api.errors.InternalServerError;
import com.hivemq.api.errors.adapters.AdapterCannotBeUpdatedError;
import com.hivemq.api.errors.adapters.AdapterFailedSchemaValidationError;
import com.hivemq.api.errors.adapters.AdapterFailedValidationError;
import com.hivemq.api.errors.adapters.AdapterNotFound403Error;
import com.hivemq.api.errors.adapters.AdapterNotFoundError;
import com.hivemq.api.errors.adapters.AdapterOperationNotSupportedError;
import com.hivemq.api.errors.adapters.AdapterTypeNotFoundError;
import com.hivemq.api.errors.adapters.AdapterTypeReadOnlyError;
import com.hivemq.api.errors.adapters.DomainTagNotFoundError;
import com.hivemq.api.format.DataUrl;
import com.hivemq.api.json.CustomConfigSchemaGenerator;
import com.hivemq.api.model.ApiConstants;
import com.hivemq.api.model.ApiErrorMessages;
import com.hivemq.api.model.adapters.AdapterStatusModelConversionUtils;
import com.hivemq.api.model.adapters.ProtocolAdapter;
import com.hivemq.api.model.adapters.ProtocolAdaptersList;
import com.hivemq.api.model.adapters.ValuesTree;
import com.hivemq.api.model.mappings.northbound.NorthboundMappingListModel;
import com.hivemq.api.model.mappings.northbound.NorthboundMappingModel;
import com.hivemq.api.utils.ApiErrorUtils;
import com.hivemq.configuration.entity.adapter.NorthboundMappingEntity;
import com.hivemq.configuration.entity.adapter.ProtocolAdapterEntity;
import com.hivemq.configuration.entity.adapter.SouthboundMappingEntity;
import com.hivemq.configuration.entity.adapter.TagEntity;
import com.hivemq.configuration.info.SystemInformation;
import com.hivemq.configuration.reader.ProtocolAdapterExtractor;
import com.hivemq.configuration.service.ConfigurationService;
import com.hivemq.edge.HiveMQEdgeConstants;
import com.hivemq.edge.HiveMQEdgeRemoteService;
import com.hivemq.edge.VersionProvider;
import com.hivemq.edge.api.ProtocolAdaptersApi;
import com.hivemq.edge.api.model.Adapter;
import com.hivemq.edge.api.model.AdapterConfig;
import com.hivemq.edge.api.model.AdaptersList;
import com.hivemq.edge.api.model.DomainTag;
import com.hivemq.edge.api.model.DomainTagList;
import com.hivemq.edge.api.model.NorthboundMappingList;
import com.hivemq.edge.api.model.SouthboundMappingList;
import com.hivemq.edge.api.model.Status;
import com.hivemq.edge.api.model.StatusList;
import com.hivemq.edge.api.model.StatusTransitionCommand;
import com.hivemq.edge.api.model.StatusTransitionResult;
import com.hivemq.edge.api.model.TagSchema;
import com.hivemq.edge.modules.adapters.impl.ProtocolAdapterDiscoveryOutputImpl;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterValidationFailure;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterValidator;
import com.hivemq.persistence.mappings.NorthboundMapping;
import com.hivemq.persistence.topicfilter.TopicFilterPersistence;
import com.hivemq.persistence.topicfilter.TopicFilterPojo;
import com.hivemq.protocols.InternalProtocolAdapterWritingService;
import com.hivemq.protocols.ProtocolAdapterManager;
import com.hivemq.protocols.ProtocolAdapterSchemaManager;
import com.hivemq.protocols.ProtocolAdapterUtils;
import com.hivemq.protocols.ProtocolAdapterWrapper;
import com.hivemq.protocols.params.NodeTreeImpl;
import com.hivemq.protocols.tag.TagSchemaCreationInputImpl;
import com.hivemq.protocols.tag.TagSchemaCreationOutputImpl;
import com.hivemq.util.ErrorResponseUtil;
import io.netty.handler.codec.base64.Base64Decoder;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Singleton
public class ProtocolAdaptersResourceImpl extends AbstractApi implements ProtocolAdaptersApi {

    private static final @NotNull Logger log = LoggerFactory.getLogger(ProtocolAdaptersResourceImpl.class);

    private final @NotNull HiveMQEdgeRemoteService remoteService;
    private final @NotNull ConfigurationService configurationService;
    private final @NotNull ProtocolAdapterManager protocolAdapterManager;
    private final @NotNull InternalProtocolAdapterWritingService protocolAdapterWritingService;
    private final @NotNull TopicFilterPersistence topicFilterPersistence;
    private final @NotNull ObjectMapper objectMapper;
    private final @NotNull VersionProvider versionProvider;
    private final @NotNull CustomConfigSchemaGenerator customConfigSchemaGenerator = new CustomConfigSchemaGenerator();
    private final @NotNull SystemInformation systemInformation;
    private final @NotNull ProtocolAdapterExtractor protocolAdapterConfig;

    @Inject
    public ProtocolAdaptersResourceImpl(
            final @NotNull HiveMQEdgeRemoteService remoteService,
            final @NotNull ConfigurationService configurationService,
            final @NotNull ProtocolAdapterManager protocolAdapterManager,
            final @NotNull InternalProtocolAdapterWritingService protocolAdapterWritingService,
            final @NotNull ObjectMapper objectMapper,
            final @NotNull VersionProvider versionProvider,
            final @NotNull TopicFilterPersistence topicFilterPersistence,
            final @NotNull SystemInformation systemInformation,
            final @NotNull ProtocolAdapterExtractor protocolAdapterConfig) {
        this.systemInformation = systemInformation;
        this.remoteService = remoteService;
        this.configurationService = configurationService;
        this.protocolAdapterManager = protocolAdapterManager;
        this.objectMapper = ProtocolAdapterUtils.createProtocolAdapterMapper(objectMapper);
        this.versionProvider = versionProvider;
        this.protocolAdapterWritingService = protocolAdapterWritingService;
        this.topicFilterPersistence = topicFilterPersistence;
        this.protocolAdapterConfig = protocolAdapterConfig;
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
        return Response.ok(new AdaptersList().items(adapters)).build();
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

        return Response.ok(new AdaptersList().items(adapters)).build();
    }

    @Override
    public @NotNull Response getAdapter(final @NotNull String adapterId) {
        final Optional<ProtocolAdapterWrapper> instance = protocolAdapterManager.getProtocolAdapterWrapperByAdapterId(adapterId);
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
        return new Adapter().id(value.getId())
                .type(value.getAdapterInformation().getProtocolId())
                .config(objectMapper.valueToTree(configObject))
                .status(getStatusInternal(value.getId()));
    }

    @Override
    public @NotNull Response discoverDataPoints(
            final @NotNull String adapterId, final @Nullable String rootNode, final @Nullable Integer depth) {

        final Optional<ProtocolAdapterWrapper> instance = protocolAdapterManager.getProtocolAdapterWrapperByAdapterId(adapterId);
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
        if (!systemInformation.isConfigWriteable()) {
            return ErrorResponseUtil.errorResponse(new ConfigWritingDisabled());
        }
        final var protocolAdapterType = protocolAdapterManager.getAdapterTypeById(adapterType);
        if (protocolAdapterType.isEmpty()) {
            return ErrorResponseUtil.errorResponse(new AdapterTypeNotFoundError(String.format("Adapter not found '%s'",
                    adapterType)));
        }
        final var errorMessages = ApiErrorUtils.createErrorContainer();
        if (protocolAdapterConfig.getAdapterByAdapterId(adapter.getId()).isPresent()) {
            ApiErrorUtils.addValidationError(errorMessages, "id", "Adapter ID must be unique in system");
            return ErrorResponseUtil.errorResponse(new AdapterFailedSchemaValidationError(errorMessages.toErrorList()));
        }
        validateAdapterSchema(errorMessages, adapter);
        if (ApiErrorUtils.hasRequestErrors(errorMessages)) {
            return ErrorResponseUtil.errorResponse(new AdapterFailedSchemaValidationError(errorMessages.toErrorList()));
        }
        try {
            protocolAdapterConfig.addAdapter(new ProtocolAdapterEntity(
                    adapter.getId(),
                    adapterType,
                    protocolAdapterType.get().getCurrentConfigVersion(),
                    (LinkedHashMap<String,Object>) adapter.getConfig(),
                    List.of(),
                    List.of(),
                    List.of()));
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
        if (!systemInformation.isConfigWriteable()) {
            return ErrorResponseUtil.errorResponse(new ConfigWritingDisabled());
        }

        return protocolAdapterConfig
                .getAdapterByAdapterId(adapterId)
                .map(oldInstance -> {
                    final ProtocolAdapterEntity newConfig = new ProtocolAdapterEntity(
                            oldInstance.getAdapterId(),
                            oldInstance.getProtocolId(),
                            oldInstance.getConfigVersion(),
                            (Map<String, Object>)adapter.getConfig(),
                            oldInstance.getNorthboundMappingEntities(),
                            oldInstance.getSouthboundMappingEntities(),
                            oldInstance.getTags());
                    protocolAdapterConfig.updateAdapter(newConfig);
                    return Response.ok().build();
                })
                .orElseGet(() -> {
                    log.info("Adapter '{}' does not exist.", adapterId);
                    return ErrorResponseUtil.errorResponse(new AdapterNotFoundError(adapterId));
                });
    }

    @Override
    public @NotNull Response deleteAdapter(final @NotNull String adapterId) {
        if (!systemInformation.isConfigWriteable()) {
            return ErrorResponseUtil.errorResponse(new ConfigWritingDisabled());
        }
        if (protocolAdapterConfig.getAdapterByAdapterId(adapterId).isEmpty()) {
            return ErrorResponseUtil.errorResponse(new AdapterNotFoundError(String.format("Adapter not found '%s'",
                    adapterId)));
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Deleting adapter \"{}\".", adapterId);
        }
        protocolAdapterConfig.deleteAdapter(adapterId);

        return Response.ok().build();
    }

    @Override
    public @NotNull Response transitionAdapterStatus(
            final @NotNull String adapterId, final @NotNull StatusTransitionCommand command) {
        final var errorMessages = ApiErrorUtils.createErrorContainer();
        ApiErrorUtils.validateRequiredField(errorMessages, "id", adapterId, false);
        ApiErrorUtils.validateRequiredFieldRegex(errorMessages, "id", adapterId, HiveMQEdgeConstants.ID_REGEX);
        ApiErrorUtils.validateRequiredEntity(errorMessages, "command", command);
        if (protocolAdapterConfig.getAdapterByAdapterId(adapterId).isEmpty()) {
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

            final var statusTransitionResult =
                    new StatusTransitionResult().status(StatusTransitionResult.StatusEnum.PENDING)
                            .type(ApiConstants.ADAPTER_TYPE)
                            .identifier(adapterId)
                            .status(StatusTransitionResult.StatusEnum.PENDING)
                            .callbackTimeoutMillis(ApiConstants.DEFAULT_TRANSITION_WAIT_TIMEOUT);
            return Response.ok(statusTransitionResult).build();
        }
    }

    @Override
    public @NotNull Response getAdapterStatus(final @NotNull String adapterId) {

        final var errorMessages = ApiErrorUtils.createErrorContainer();
        ApiErrorUtils.validateRequiredField(errorMessages, "id", adapterId, false);
        ApiErrorUtils.validateRequiredFieldRegex(errorMessages, "id", adapterId, HiveMQEdgeConstants.ID_REGEX);
        if (ApiErrorUtils.hasRequestErrors(errorMessages)) {
            return ErrorResponseUtil.errorResponse(new AdapterFailedSchemaValidationError(errorMessages.toErrorList()));
        }
        if (protocolAdapterConfig.getAdapterByAdapterId(adapterId).isEmpty()) {
            return ErrorResponseUtil.errorResponse(new AdapterNotFoundError(String.format("Adapter not found '%s'",
                    adapterId)));
        }
        return Response.ok(getStatusInternal(adapterId)).build();
    }

    protected @NotNull Status getStatusInternal(final @NotNull String adapterId) {
        final Optional<ProtocolAdapterWrapper> optionalAdapterInstance =
                protocolAdapterManager.getProtocolAdapterWrapperByAdapterId(adapterId);
        return optionalAdapterInstance.map(AdapterStatusModelConversionUtils::getAdapterStatus)
                .orElseGet(() -> unknown(Status.RuntimeEnum.STOPPED, ApiConstants.ADAPTER_TYPE, adapterId));
    }

    public static @NotNull Status unknown(
            final @NotNull Status.RuntimeEnum runtimeStatus,
            final @NotNull String connectionType,
            final @NotNull String entityId) {
        Preconditions.checkNotNull(connectionType);
        Preconditions.checkNotNull(entityId);
        return new Status().runtime(runtimeStatus)
                .connection(Status.ConnectionEnum.UNKNOWN)
                .id(entityId)
                .type(connectionType);
    }


    protected void validateAdapterSchema(
            final @NotNull ApiErrorMessages apiErrorMessages, final @NotNull Adapter adapter) {
        final ProtocolAdapterInformation information =
                protocolAdapterManager.getAllAvailableAdapterTypes().get(adapter.getType());
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
    public @NotNull Response getAdaptersStatus() {
        final ImmutableList.Builder<Status> builder = new ImmutableList.Builder<>();
        final Map<String, ProtocolAdapterWrapper> adapters = protocolAdapterManager.getProtocolAdapters();
        for (final ProtocolAdapterWrapper instance : adapters.values()) {
            builder.add(AdapterStatusModelConversionUtils.getAdapterStatus(instance));
        }
        return Response.ok(new StatusList().items(builder.build())).build();
    }

    @Override
    public @NotNull Response getAdapterDomainTags(final @NotNull String adapterId) {
        return protocolAdapterManager.getTagsForAdapter(adapterId)
                .map(tags -> {
                    if (tags.isEmpty()) {
                        return Response.ok(new DomainTagList().items(List.of())).build();
                    } else {
                        final List<DomainTag> domainTagModels = tags.stream()
                                .map(com.hivemq.persistence.domain.DomainTag::toModel)
                                .collect(Collectors.toList());
                        return Response.ok(new DomainTagList().items(domainTagModels)).build();
                    }
                })
                .orElse(ErrorResponseUtil.errorResponse(new AdapterNotFoundError(String.format("Adapter not found '%s'",
                        adapterId))));
    }

    @Override
    public @NotNull Response addAdapterDomainTags(
            final @NotNull String adapterId, final @NotNull DomainTag domainTag) {
        if (!systemInformation.isConfigWriteable()) {
            return ErrorResponseUtil.errorResponse(new ConfigWritingDisabled());
        }

        log.debug("Adding adapter domain tag {} for adapter {}", domainTag.getName(), adapterId);
        return protocolAdapterConfig
                .getAdapterByAdapterId(adapterId)
                .map(oldInstance -> {

                    if(oldInstance.getTags().stream().anyMatch(tag -> tag.getName().equals(domainTag.getName()))) {
                        return ErrorResponseUtil.errorResponse(new AdapterCannotBeUpdatedError(adapterId));
                    }

                    final var newTagList = new ArrayList<>(oldInstance.getTags());

                    newTagList.add(new TagEntity(
                            domainTag.getName(),
                            domainTag.getDescription(),
                            objectMapper.convertValue(domainTag.getDefinition(), new TypeReference<>(){})));

                    var updated = protocolAdapterConfig.updateAdapter(new ProtocolAdapterEntity(
                            oldInstance.getAdapterId(),
                            oldInstance.getProtocolId(),
                            oldInstance.getConfigVersion(),
                            oldInstance.getConfig(),
                            oldInstance.getNorthboundMappingEntities(),
                            oldInstance.getSouthboundMappingEntities(),
                            newTagList
                    ));
                    if(updated) {
                        return Response.ok().build();
                    } else {
                        log.warn("Adapter '{}' failed updating.", adapterId);
                        return ErrorResponseUtil.errorResponse(new AdapterCannotBeUpdatedError(adapterId));
                    }

                })
                .orElseGet(() -> {
                    log.warn("Tags could not be added for adapter '{}' because the adapter was not found.", adapterId);
                    return ErrorResponseUtil.errorResponse(new AdapterNotFoundError(String.format("Adapter not found '%s'",
                            adapterId)));
                });


    }

    @Override
    public @NotNull Response deleteAdapterDomainTags(
            final @NotNull String adapterId, final @NotNull String tagName) {
        if (!systemInformation.isConfigWriteable()) {
            return ErrorResponseUtil.errorResponse(new ConfigWritingDisabled());
        }
        final String decodedTagName = URLDecoder.decode(tagName, StandardCharsets.UTF_8);

        return protocolAdapterConfig
                .getAdapterByAdapterId(adapterId)
                .map(oldInstance -> {
                    final var newTagList = oldInstance.getTags().stream().filter(tag -> !tag.getName().equals(decodedTagName)).toList();
                    if(newTagList.size() != oldInstance.getTags().size()) {
                        final ProtocolAdapterEntity newConfig = new ProtocolAdapterEntity(
                                oldInstance.getAdapterId(),
                                oldInstance.getProtocolId(),
                                oldInstance.getConfigVersion(),
                                oldInstance.getConfig(),
                                oldInstance.getNorthboundMappingEntities(),
                                oldInstance.getSouthboundMappingEntities(),
                                oldInstance.getTags().stream().filter(tag -> !tag.getName().equals(decodedTagName)).toList());
                        protocolAdapterConfig.updateAdapter(newConfig);
                        return Response.ok().build();
                    } else {
                        return ErrorResponseUtil.errorResponse(new DomainTagNotFoundError(decodedTagName));
                    }
                })
                .orElseGet(() -> {
                    log.info("Adapter '{}' does not exist.", adapterId);
                    return ErrorResponseUtil.errorResponse(new AdapterNotFoundError(adapterId));
                });
    }

    @Override
    public @NotNull Response updateAdapterDomainTag(
            final @NotNull String adapterId, final @NotNull String tagName, final @NotNull DomainTag domainTag) {
        if (!systemInformation.isConfigWriteable()) {
            return ErrorResponseUtil.errorResponse(new ConfigWritingDisabled());
        }
        final String decodedTagName = new String(Base64.getDecoder().decode(tagName.getBytes(StandardCharsets.UTF_8)));

        return protocolAdapterConfig
                .getAdapterByAdapterId(adapterId)
                .map(oldInstance -> {
                    final var updated = new AtomicBoolean(false);
                    final var newTagList = oldInstance.getTags().stream().map(tag -> {
                        if (tag.getName().equals(decodedTagName)) {
                            updated.set(true);
                            return new TagEntity(
                                    domainTag.getName(),
                                    domainTag.getDescription(),
                                    objectMapper.convertValue(domainTag.getDefinition(), new TypeReference<>(){})
                            );
                        } else {
                            return tag;
                        }
                    }).toList();
                    if(updated.get()) {
                        final ProtocolAdapterEntity newConfig = new ProtocolAdapterEntity(oldInstance.getAdapterId(),
                                oldInstance.getProtocolId(),
                                oldInstance.getConfigVersion(),
                                oldInstance.getConfig(),
                                oldInstance.getNorthboundMappingEntities(),
                                oldInstance.getSouthboundMappingEntities(),
                                newTagList);
                        protocolAdapterConfig.updateAdapter(newConfig);
                        return Response.ok().build();
                    } else {
                        return ErrorResponseUtil.errorResponse(new DomainTagNotFoundError(tagName));
                    }
                })
                .orElseGet(() -> {
                    log.warn("Tag could not be updated for adapter '{}' because the adapter was not found.", adapterId);
                    return ErrorResponseUtil.errorResponse(new AdapterNotFound403Error(String.format("Adapter not found '%s'",
                            adapterId)));
                });
    }

    @Override
    public @NotNull Response updateAdapterDomainTags(
            final @NotNull String adapterId, final @NotNull DomainTagList domainTagList) {
        if (!systemInformation.isConfigWriteable()) {
            return ErrorResponseUtil.errorResponse(new ConfigWritingDisabled());
        }

        return protocolAdapterConfig
                .getAdapterByAdapterId(adapterId)
                .map(oldInstance -> {
                    final var newTagList = domainTagList.getItems().stream()
                            .map(tag ->

                                    new TagEntity(
                                            tag.getName(),
                                            tag.getDescription(),
                                            objectMapper.convertValue(tag.getDefinition(), new TypeReference<>(){})
                                    ))
                            .toList();
                    var newConfig = new ProtocolAdapterEntity(oldInstance.getAdapterId(),
                            oldInstance.getProtocolId(),
                            oldInstance.getConfigVersion(),
                            oldInstance.getConfig(),
                            oldInstance.getNorthboundMappingEntities(),
                            oldInstance.getSouthboundMappingEntities(),
                            newTagList);
                    protocolAdapterConfig.updateAdapter(newConfig);
                    return Response.ok().build();
                })
                .orElseGet(() -> {
                    log.warn("Tags could not be updated for adapter '{}' because the adapter was not found.", adapterId);
                    return ErrorResponseUtil.errorResponse(new AdapterNotFoundError(String.format("Adapter not found '%s'",
                            adapterId)));
                });
        //TODO

//        case ALREADY_USED_BY_ANOTHER_ADAPTER:
//        //noinspection DataFlowIssue cant be null here.
//        final @NotNull String tagName = domainTagUpdateResult.getErrorMessage();
//        return ErrorResponseUtil.errorResponse(new AlreadyExistsError("The tag '" +
//                tagName +
//                "' cannot be created since another item already exists with the same id."));
    }

    @Override
    public @NotNull Response getDomainTags() {
        final List<com.hivemq.persistence.domain.DomainTag> domainTags = protocolAdapterManager.getDomainTags();
        if (domainTags.isEmpty()) {
            // empty list is also 200 as discussed.
            return Response.ok(new DomainTagList().items(List.of())).build();
        }
        final List<DomainTag> domainTagModels =
                domainTags.stream().map(com.hivemq.persistence.domain.DomainTag::toModel).collect(Collectors.toList());
        return Response.ok(new DomainTagList().items(domainTagModels)).build();
    }

    @Override
    public @NotNull Response getDomainTag(final @NotNull String tagName) {
        final String decodedTagName = URLDecoder.decode(tagName, StandardCharsets.UTF_8);
        return protocolAdapterManager.getDomainTagByName(decodedTagName)
                .map(tag -> Response.ok(tag.toModel()).build())
                .orElse(ErrorResponseUtil.errorResponse(new DomainTagNotFoundError(decodedTagName)));
    }

    @Override
    public @NotNull Response getTagSchema(final @NotNull String protocolId) {
        return protocolAdapterManager.getAdapterTypeById(protocolId)
                .map(info -> Response.ok(new TagSchema().protocolId(protocolId)
                                .configSchema(customConfigSchemaGenerator.generateJsonSchema(info.tagConfigurationClass())))
                        .build())
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
                protocolAdapterManager.getProtocolAdapterWrapperByAdapterId(adapterId);
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
    public @NotNull Response createCompleteAdapter(
            final @NotNull String adapterType,
            final @NotNull String adapterName,
            final @NotNull AdapterConfig adapterConfig) {
        if (!systemInformation.isConfigWriteable()) {
            return ErrorResponseUtil.errorResponse(new ConfigWritingDisabled());
        }
        final Optional<ProtocolAdapterInformation> protocolAdapterInformation =
                protocolAdapterManager.getAdapterTypeById(adapterType);
        if (protocolAdapterInformation.isEmpty()) {
            return ErrorResponseUtil.errorResponse(new AdapterTypeNotFoundError(String.format("Adapter not found '%s'",
                    adapterType)));
        }
        final var errorMessages = ApiErrorUtils.createErrorContainer();
        final var adapterId = adapterConfig.getConfig().getId();
        if (protocolAdapterConfig.getAdapterByAdapterId(adapterId).isPresent()) {
            ApiErrorUtils.addValidationError(errorMessages, "id", "Adapter ID must be unique in system");
            return ErrorResponseUtil.errorResponse(new AdapterFailedSchemaValidationError(errorMessages.toErrorList()));
        }
        validateAdapterSchema(errorMessages, adapterConfig.getConfig());
        if (ApiErrorUtils.hasRequestErrors(errorMessages)) {
            return ErrorResponseUtil.errorResponse(new AdapterFailedSchemaValidationError(errorMessages.toErrorList()));
        }
        try {
            final var configMap = (LinkedHashMap) adapterConfig.getConfig().getConfig();

            final var domainTags = adapterConfig.getTags()
                    .stream()
                    .map(dtm -> new TagEntity(dtm.getName(), dtm.getDescription(), (Map<String, Object>)dtm.getDefinition()))
                    .collect(Collectors.toList());

            final var northboundMappings = adapterConfig.getNorthboundMappings()
                    .stream()
                    .map(NorthboundMappingEntity::fromApi)
                    .collect(Collectors.toList());

            final var southboundMappings = adapterConfig.getSouthboundMappings()
                    .stream()
                    .map(this::parseAndEnrichWithSchema)
                    .collect(Collectors.toList());

            protocolAdapterConfig.addAdapter(new ProtocolAdapterEntity(
                    adapterId,
                    adapterType,
                    protocolAdapterInformation.get().getCurrentConfigVersion(),
                    configMap,
                    northboundMappings,
                    southboundMappings,
                    domainTags));
        } catch (final IllegalArgumentException e) {
            if (e.getCause() instanceof UnrecognizedPropertyException) {
                ApiErrorUtils.addValidationError(errorMessages,
                        ((UnrecognizedPropertyException) e.getCause()).getPropertyName(),
                        "Unknown field on adapterConfig configuration");
            } else {
                log.error("Error processing incoming request", e);
            }
            return ErrorResponseUtil.errorResponse(new AdapterFailedSchemaValidationError(errorMessages.toErrorList()));
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Added protocol adapterConfig of type {} with ID {}.",
                    adapterType,
                    adapterConfig.getConfig().getId());
        }
        return Response.ok().build();
    }

    @Override
    public @NotNull Response getAdapterNorthboundMappings(final @NotNull String adapterId) {
        return protocolAdapterConfig
                .getAdapterByAdapterId(adapterId)
                .map(adapter -> adapter.getNorthboundMappingEntities()
                        .stream()
                        .map(NorthboundMappingModel::fromEntity)
                        .collect(Collectors.toList()))
                .map(NorthboundMappingListModel::new)
                .map(mappingsList -> Response.ok(mappingsList).build())
                .orElseGet(() -> ErrorResponseUtil.errorResponse(new AdapterNotFoundError(String.format(
                        "Adapter not found '%s'",
                        adapterId))));
    }

    @Override
    public @NotNull Response getNorthboundMappings() {
        final var northboundMappingListModels = protocolAdapterConfig.getAllConfigs()
                .stream()
                .flatMap(adapter -> adapter.getNorthboundMappingEntities().stream().map(NorthboundMappingModel::fromEntity))
                .collect(Collectors.toList());
        return Response.status(200).entity(new NorthboundMappingListModel(northboundMappingListModels)).build();
    }


    @Override
    public @NotNull Response getSouthboundMappings() {
        final List<com.hivemq.edge.api.model.SouthboundMapping> southboundMappingModels =
                protocolAdapterConfig.getAllConfigs()
                        .stream()
                        .flatMap(adapter -> adapter.getSouthboundMappingEntities().stream().map(SouthboundMappingEntity::toAPi))
                        .collect(Collectors.toList());
        return Response.status(200).entity(new SouthboundMappingList().items(southboundMappingModels)).build();
    }


    @Override
    public @NotNull Response updateAdapterNorthboundMappings(
            final @NotNull String adapterId, final @NotNull NorthboundMappingList northboundMappingList) {
        if (!systemInformation.isConfigWriteable()) {
            return ErrorResponseUtil.errorResponse(new ConfigWritingDisabled());
        }
        return protocolAdapterConfig.getAdapterByAdapterId(adapterId)
                .map(adapter -> {
                    final Set<String> requiredTags = new HashSet<>();
                    final List<NorthboundMapping> converted = northboundMappingList.getItems().stream().map(mapping -> {
                        requiredTags.add(mapping.getTagName());
                        return NorthboundMapping.fromModel(mapping);
                    }).collect(Collectors.toList());
                    adapter.getTags().forEach(tag -> requiredTags.remove(tag.getName()));

                    if (requiredTags.isEmpty()) {
                        return protocolAdapterConfig
                                .getAdapterByAdapterId(adapterId)
                                .map(cfg ->
                                        new ProtocolAdapterEntity(
                                                cfg.getAdapterId(),
                                                cfg.getProtocolId(),
                                                cfg.getConfigVersion(),
                                                cfg.getConfig(),
                                                northboundMappingList.getItems().stream().map(NorthboundMappingEntity::fromApi).toList(),
                                                cfg.getSouthboundMappingEntities(),
                                                cfg.getTags()))
                                .map(newCfg -> {
                                            protocolAdapterConfig.updateAdapter(newCfg);
                                            log.info("Successfully updated northbound mappings for adapter '{}'.", adapterId);
                                            return Response.ok(northboundMappingList).build();
                                        })
                                .orElseGet(() -> {
                                    log.error("Something went wrong updating the adapter {}", adapterId);
                                    return ErrorResponseUtil.errorResponse(new InternalServerError(null));
                                });
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
    public @NotNull Response getAdapterSouthboundMappings(final @NotNull String adapterId) {
        return protocolAdapterConfig.getAdapterByAdapterId(adapterId)
                .map(adapter -> adapter.getSouthboundMappingEntities()
                        .stream()
                        .map(SouthboundMappingEntity::toAPi)
                        .toList())
                .map(southboundMappings -> new SouthboundMappingList().items(southboundMappings))
                .map(mappingsList -> Response.ok(mappingsList).build())
                .orElseGet(() -> ErrorResponseUtil.errorResponse(new AdapterNotFoundError(String.format(
                        "Adapter not found '%s'",
                        adapterId))));
    }

    @Override
    public @NotNull Response updateAdapterSouthboundMappings(
            final @NotNull String adapterId, final @NotNull SouthboundMappingList southboundMappingListModel) {
        if (!systemInformation.isConfigWriteable()) {
            return ErrorResponseUtil.errorResponse(new ConfigWritingDisabled());
        }
        return protocolAdapterConfig.getAdapterByAdapterId(adapterId)
                .map(adapter -> {
                    final Set<String> requiredTags = new HashSet<>();
                    final List<SouthboundMappingEntity> converted =
                            southboundMappingListModel.getItems().stream().map(mapping -> {
                                requiredTags.add(mapping.getTagName());
                                return parseAndEnrichWithSchema(mapping);
                            }).toList();
                    adapter.getTags().forEach(tag -> requiredTags.remove(tag.getName()));

                    if (requiredTags.isEmpty()) {
                        return protocolAdapterConfig
                                .getAdapterByAdapterId(adapterId)
                                .map(cfg ->
                                        new ProtocolAdapterEntity(
                                                cfg.getAdapterId(),
                                                cfg.getProtocolId(),
                                                cfg.getConfigVersion(),
                                                cfg.getConfig(),
                                                cfg.getNorthboundMappingEntities(),
                                                converted,
                                                cfg.getTags()))
                                .map(newCfg -> {
                                    protocolAdapterConfig.updateAdapter(newCfg);
                                    log.info("Successfully updated fromMappings for adapter '{}'.", adapterId);
                                    return Response.ok(southboundMappingListModel).build();
                                })
                                .orElseGet(() -> {
                                    log.error("Something went wrong updating the adapter {}", adapterId);
                                    return ErrorResponseUtil.errorResponse(new InternalServerError(null));
                                });
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


    private @NotNull SouthboundMappingEntity parseAndEnrichWithSchema(final @NotNull com.hivemq.edge.api.model.SouthboundMapping model) {
        final TopicFilterPojo topicFilter = topicFilterPersistence.getTopicFilter(model.getTopicFilter());
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
        return SouthboundMappingEntity.fromApi(model, schema);
    }
}
