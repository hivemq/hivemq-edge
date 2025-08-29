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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.google.common.collect.ImmutableList;
import com.hivemq.adapter.sdk.api.ProtocolAdapterCapability;
import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.discovery.ProtocolAdapterDiscoveryInput;
import com.hivemq.adapter.sdk.api.writing.WritingProtocolAdapter;
import com.hivemq.api.AbstractApi;
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
import com.hivemq.persistence.topicfilter.TopicFilterPersistence;
import com.hivemq.persistence.topicfilter.TopicFilterPojo;
import com.hivemq.protocols.InternalProtocolAdapterWritingService;
import com.hivemq.protocols.ProtocolAdapterManager;
import com.hivemq.protocols.ProtocolAdapterSchemaManager;
import com.hivemq.protocols.ProtocolAdapterWrapper;
import com.hivemq.protocols.tag.TagSchemaCreationInputImpl;
import com.hivemq.protocols.tag.TagSchemaCreationOutputImpl;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.core.Response;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.hivemq.api.resources.impl.ProtocolAdapterApiUtils.convertModuleAdapterType;
import static com.hivemq.api.utils.ApiErrorUtils.addValidationError;
import static com.hivemq.api.utils.ApiErrorUtils.hasRequestErrors;
import static com.hivemq.api.utils.ApiErrorUtils.validateRequiredEntity;
import static com.hivemq.api.utils.ApiErrorUtils.validateRequiredField;
import static com.hivemq.api.utils.ApiErrorUtils.validateRequiredFieldRegex;
import static com.hivemq.util.ErrorResponseUtil.errorResponse;

@Singleton
public class ProtocolAdaptersResourceImpl extends AbstractApi implements ProtocolAdaptersApi {

    private static final @NotNull TypeReference<@NotNull Map<String, Object>> AS_MAP_TYPE_REF = new TypeReference<>() {
        // no-op
    };
    private static final @NotNull Logger log = LoggerFactory.getLogger(ProtocolAdaptersResourceImpl.class);

    private final @NotNull HiveMQEdgeRemoteService remoteService;
    private final @NotNull ConfigurationService configurationService;
    private final @NotNull ProtocolAdapterManager protocolAdapterManager;
    private final @NotNull InternalProtocolAdapterWritingService protocolAdapterWritingService;
    private final @NotNull TopicFilterPersistence topicFilterPersistence;
    private final @NotNull ObjectMapper objectMapper;
    private final @NotNull VersionProvider versionProvider;
    private final @NotNull CustomConfigSchemaGenerator customConfigSchemaGenerator;
    private final @NotNull SystemInformation systemInformation;
    private final @NotNull ProtocolAdapterExtractor configExtractor;

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
            final @NotNull ProtocolAdapterExtractor configExtractor) {
        this.remoteService = remoteService;
        this.configurationService = configurationService;
        this.protocolAdapterManager = protocolAdapterManager;
        this.protocolAdapterWritingService = protocolAdapterWritingService;
        // TODO
//        this.objectMapper = ProtocolAdapterUtils.createProtocolAdapterMapper(objectMapper);
        this.objectMapper = objectMapper;
        this.versionProvider = versionProvider;
        this.topicFilterPersistence = topicFilterPersistence;
        this.systemInformation = systemInformation;
        this.configExtractor = configExtractor;
        this.customConfigSchemaGenerator = new CustomConfigSchemaGenerator();
    }

    private static @NotNull Supplier<? extends Response> adapterNotFoundError(final @NotNull String adapterId) {
        return () -> {
            log.info("Adapter '{}' does not exist.", adapterId);
            return errorResponse(new AdapterNotFoundError("Adapter not found: " + adapterId));
        };
    }

    private static @NotNull Supplier<? extends Response> adapterNotUpdatedError(final @NotNull String adapterId) {
        return () -> {
            log.error("Something went wrong updating the adapter {}", adapterId);
            return errorResponse(new InternalServerError(null));
        };
    }

    private static @NotNull Response adapterCannotBeUpdatedError(final @NotNull String adapterId) {
        log.warn("Adapter '{}' failed updating.", adapterId);
        return errorResponse(new AdapterCannotBeUpdatedError(adapterId));
    }

    @Override
    public @NotNull Response getAdapterTypes(final @Nullable String xOriginalURI) {
        //-- Obtain the adapters installed by the runtime (these will be marked as installed = true).
        final Set<ProtocolAdapter> installed =
                protocolAdapterManager.getAllAvailableAdapterTypes().values().stream().map(adapter -> {
                    try {
                        return ProtocolAdapterApiUtils.convertInstalledAdapterType(objectMapper,
                                protocolAdapterManager,
                                adapter,
                                configurationService,
                                versionProvider,
                                xOriginalURI);
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
                .map(m -> convertModuleAdapterType(m, configurationService))
                .filter(Predicate.not(installed::contains))
                .forEach(installed::add);

        return Response.ok(new ProtocolAdaptersList(new ArrayList<>(installed))).build();
    }

    @Override
    public @NotNull Response getAdapters() {
        return Response.ok(getAdaptersInternal(null)).build();
    }

    @Override
    public @NotNull Response getAdaptersForType(final @NotNull String adapterType) {
        return protocolAdapterManager.getAdapterTypeById(adapterType)
                .map(info -> Response.ok(getAdaptersInternal(adapterType)).build())
                .orElseGet(() -> errorResponse(new AdapterTypeNotFoundError("Adapter of type not found: " +
                        adapterType)));
    }

    @Override
    public @NotNull Response getAdapter(final @NotNull String adapterId) {
        return protocolAdapterManager.getProtocolAdapterWrapperByAdapterId(adapterId)
                .map(wrapper -> Response.ok(toAdapter(wrapper)).build())
                .orElseGet(adapterNotFoundError(adapterId));
    }

    @Override
    public @NotNull Response discoverDataPoints(
            final @NotNull String adapterId,
            final @Nullable String rootNode,
            final @Nullable Integer depth) {

        final Optional<ProtocolAdapterWrapper> maybeWrapper =
                protocolAdapterManager.getProtocolAdapterWrapperByAdapterId(adapterId);
        if (maybeWrapper.isEmpty()) {
            return adapterNotFoundError(adapterId).get();
        }

        final ProtocolAdapterWrapper wrapper = maybeWrapper.get();
        if (!wrapper.getAdapterInformation().getCapabilities().contains(ProtocolAdapterCapability.DISCOVER)) {
            return errorResponse(new AdapterFailedValidationError("Adapter does not support discovery"));
        }
        final ProtocolAdapterDiscoveryOutputImpl output = new ProtocolAdapterDiscoveryOutputImpl();

        final Thread currentThread = Thread.currentThread();
        final ClassLoader ctxClassLoader = currentThread.getContextClassLoader();
        try {
            currentThread.setContextClassLoader(wrapper.getAdapterFactory().getClass().getClassLoader());

            wrapper.discoverValues(new ProtocolAdapterDiscoveryInput() {
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

            return Response.ok(new ValuesTree(output.getNodeTree().getRootNode().getChildren())).build();
        } catch (final @NotNull ExecutionException e) {
            final Throwable cause = e.getCause();
            log.warn("Exception occurred during discovery for adapter '{}'", adapterId, cause);
            return errorResponse(new InternalServerError("Exception during discovery."));
        } catch (final @NotNull InterruptedException e) {
            currentThread.interrupt();
            log.warn("Thread was interrupted during discovery for adapter '{}'", adapterId);
            return errorResponse(new InternalServerError("Exception during discovery."));
        } catch (final @NotNull Throwable e) {
            log.warn("Exception was thrown during discovery for adapter '{}'.", adapterId);
            return errorResponse(new InternalServerError("Exception during discovery."));
        } finally {
            currentThread.setContextClassLoader(ctxClassLoader);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public @NotNull Response addAdapter(final @NotNull String adapterType, final @NotNull Adapter adapter) {
        if (!systemInformation.isConfigWriteable()) {
            return errorResponse(new ConfigWritingDisabled());
        }

        final Optional<ProtocolAdapterInformation> type = protocolAdapterManager.getAdapterTypeById(adapterType);
        if (type.isEmpty()) {
            return errorResponse(new AdapterTypeNotFoundError(String.format("Adapter not found '%s'", adapterType)));
        }

        final ApiErrorMessages errorMessages = ApiErrorUtils.createErrorContainer();
        if (configExtractor.getAdapterByAdapterId(adapter.getId()).isPresent()) {
            addValidationError(errorMessages, "id", "Adapter ID must be unique in system");
            return errorResponse(new AdapterFailedSchemaValidationError(errorMessages.toErrorList()));
        }
        validateAdapterSchema(errorMessages, adapter);
        if (hasRequestErrors(errorMessages)) {
            return errorResponse(new AdapterFailedSchemaValidationError(errorMessages.toErrorList()));
        }

        try {
            if (configExtractor.addAdapter(new ProtocolAdapterEntity(adapter.getId(),
                    adapterType,
                    type.get().getCurrentConfigVersion(),
                    (LinkedHashMap<String, Object>) adapter.getConfig(),
                    List.of(),
                    List.of(),
                    List.of()))) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Added protocol adapter of type {} with ID {}.", adapterType, adapter.getId());
                }
                return Response.ok().build();
            }

            log.error("Something went wrong adding adapter {} of type {}", adapter.getId(), adapterType);
            return errorResponse(new InternalServerError(null));
        } catch (final @NotNull IllegalArgumentException e) {
            if (e.getCause() instanceof final UnrecognizedPropertyException pe) {
                addValidationError(errorMessages, pe.getPropertyName(), "Unknown field on adapter configuration");
            }
            return errorResponse(new AdapterFailedSchemaValidationError(errorMessages.toErrorList()));
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public @NotNull Response updateAdapter(final @NotNull String adapterId, final @NotNull Adapter adapter) {
        return systemInformation.isConfigWriteable() ?
                configExtractor.getAdapterByAdapterId(adapterId).map(oldInstance -> {
                    final ProtocolAdapterEntity newConfig = new ProtocolAdapterEntity(oldInstance.getAdapterId(),
                            oldInstance.getProtocolId(),
                            oldInstance.getConfigVersion(),
                            (Map<String, Object>) adapter.getConfig(),
                            oldInstance.getNorthboundMappings(),
                            oldInstance.getSouthboundMappings(),
                            oldInstance.getTags());
                    if (!configExtractor.updateAdapter(newConfig)) {
                        return adapterCannotBeUpdatedError(adapterId);
                    }
                    return Response.ok().build();
                }).orElseGet(adapterNotFoundError(adapterId)) :
                errorResponse(new ConfigWritingDisabled());
    }

    @Override
    public @NotNull Response deleteAdapter(final @NotNull String adapterId) {
        if (!systemInformation.isConfigWriteable()) {
            return errorResponse(new ConfigWritingDisabled());
        }
        if (configExtractor.getAdapterByAdapterId(adapterId).isEmpty()) {
            return adapterNotFoundError(adapterId).get();
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Deleting adapter \"{}\".", adapterId);
        }
        configExtractor.deleteAdapter(adapterId);
        return Response.ok().build();
    }

    @Override
    public @NotNull Response transitionAdapterStatus(
            final @NotNull String adapterId,
            final @NotNull StatusTransitionCommand command) {
        final ApiErrorMessages errorMessages = ApiErrorUtils.createErrorContainer();
        validateRequiredField(errorMessages, "id", adapterId, false);
        validateRequiredFieldRegex(errorMessages, "id", adapterId, HiveMQEdgeConstants.ID_REGEX);
        validateRequiredEntity(errorMessages, "command", command);
        if (hasRequestErrors(errorMessages)) {
            return errorResponse(new AdapterFailedSchemaValidationError(errorMessages.toErrorList()));
        }
        if (configExtractor.getAdapterByAdapterId(adapterId).isEmpty()) {
            return adapterNotFoundError(adapterId).get();
        }

        switch (command.getCommand()) {
            case START -> protocolAdapterManager.startAsync(adapterId).whenComplete((result, throwable) -> {
                if (throwable != null) {
                    log.error("Failed to start adapter '{}'.", adapterId, throwable);
                } else {
                    log.trace("Adapter '{}' was started successfully.", adapterId);
                }
            });
            case STOP -> protocolAdapterManager.stopAsync(adapterId, false).whenComplete((result, throwable) -> {
                if (throwable != null) {
                    log.error("Failed to stop adapter '{}'.", adapterId, throwable);
                } else {
                    log.trace("Adapter '{}' was stopped successfully.", adapterId);
                }
            });
            case RESTART -> protocolAdapterManager.stopAsync(adapterId, false)
                    .thenRun(() -> protocolAdapterManager.startAsync(adapterId))
                    .whenComplete((result, throwable) -> {
                        if (throwable != null) {
                            log.error("Failed to restart adapter '{}'.", adapterId, throwable);
                        } else {
                            log.trace("Adapter '{}' was restarted successfully.", adapterId);
                        }
                    });
        }

        return Response.ok(new StatusTransitionResult().status(StatusTransitionResult.StatusEnum.PENDING)
                .type(ApiConstants.ADAPTER_TYPE)
                .identifier(adapterId)
                .status(StatusTransitionResult.StatusEnum.PENDING)
                .callbackTimeoutMillis(ApiConstants.DEFAULT_TRANSITION_WAIT_TIMEOUT)).build();
    }

    @Override
    public @NotNull Response getAdapterStatus(final @NotNull String adapterId) {
        final ApiErrorMessages errorMessages = ApiErrorUtils.createErrorContainer();
        validateRequiredField(errorMessages, "id", adapterId, false);
        validateRequiredFieldRegex(errorMessages, "id", adapterId, HiveMQEdgeConstants.ID_REGEX);
        if (hasRequestErrors(errorMessages)) {
            return errorResponse(new AdapterFailedSchemaValidationError(errorMessages.toErrorList()));
        }
        if (configExtractor.getAdapterByAdapterId(adapterId).isEmpty()) {
            return adapterNotFoundError(adapterId).get();
        }
        return Response.ok(getAdapterStatusInternal(adapterId)).build();
    }

    @Override
    public @NotNull Response getAdaptersStatus() {
        final ImmutableList.Builder<@NotNull Status> builder = new ImmutableList.Builder<>();
        for (final ProtocolAdapterWrapper instance : protocolAdapterManager.getProtocolAdapters().values()) {
            builder.add(AdapterStatusModelConversionUtils.getAdapterStatus(instance));
        }
        return Response.ok(new StatusList().items(builder.build())).build();
    }

    @Override
    public @NotNull Response getAdapterDomainTags(final @NotNull String adapterId) {
        return protocolAdapterManager.getTagsForAdapter(adapterId)
                .map(tags -> Response.ok(new DomainTagList().items(tags.stream()
                        .map(com.hivemq.persistence.domain.DomainTag::toModel)
                        .toList())).build())
                .orElse(adapterNotFoundError(adapterId).get());
    }

    @Override
    public @NotNull Response addAdapterDomainTags(final @NotNull String adapterId, final @NotNull DomainTag domainTag) {
        if (log.isDebugEnabled()) {
            log.debug("Adding adapter domain tag {} for adapter {}", domainTag.getName(), adapterId);
        }
        return systemInformation.isConfigWriteable() ?
                configExtractor.getAdapterByAdapterId(adapterId).map(oldInstance -> {
                    if (oldInstance.getTags().stream().anyMatch(tag -> tag.getName().equals(domainTag.getName()))) {
                        return errorResponse(new AdapterCannotBeUpdatedError(adapterId));
                    }

                    final List<TagEntity> newTagList = new ArrayList<>(oldInstance.getTags());
                    newTagList.add(toTagEntity(domainTag));
                    if (!configExtractor.updateAdapter(new ProtocolAdapterEntity(oldInstance.getAdapterId(),
                            oldInstance.getProtocolId(),
                            oldInstance.getConfigVersion(),
                            oldInstance.getConfig(),
                            oldInstance.getNorthboundMappings(),
                            oldInstance.getSouthboundMappings(),
                            newTagList))) {
                        return adapterCannotBeUpdatedError(adapterId);
                    }

                    return Response.ok().build();
                }).orElseGet(() -> {
                    log.warn("Tags could not be added for adapter '{}' because the adapter was not found.", adapterId);
                    return adapterNotFoundError(adapterId).get();
                }) :
                errorResponse(new ConfigWritingDisabled());
    }

    @Override
    public @NotNull Response deleteAdapterDomainTags(final @NotNull String adapterId, final @NotNull String tagName) {
        final String decodedTagName = URLDecoder.decode(tagName, StandardCharsets.UTF_8);
        return systemInformation.isConfigWriteable() ?
                configExtractor.getAdapterByAdapterId(adapterId).map(oldInstance -> {
                    final var newTagList = oldInstance.getTags()
                            .stream()
                            .filter(tag -> !tag.getName().equals(decodedTagName))
                            .toList();
                    if (newTagList.size() != oldInstance.getTags().size()) {
                        final ProtocolAdapterEntity newConfig = new ProtocolAdapterEntity(oldInstance.getAdapterId(),
                                oldInstance.getProtocolId(),
                                oldInstance.getConfigVersion(),
                                oldInstance.getConfig(),
                                oldInstance.getNorthboundMappings(),
                                oldInstance.getSouthboundMappings(),
                                oldInstance.getTags()
                                        .stream()
                                        .filter(tag -> !tag.getName().equals(decodedTagName))
                                        .toList());
                        if (!configExtractor.updateAdapter(newConfig)) {
                            return adapterCannotBeUpdatedError(adapterId);
                        }
                        return Response.ok().build();
                    } else {
                        return errorResponse(new DomainTagNotFoundError(decodedTagName));
                    }
                }).orElseGet(adapterNotFoundError(adapterId)) :
                errorResponse(new ConfigWritingDisabled());
    }

    @Override
    public @NotNull Response updateAdapterDomainTag(
            final @NotNull String adapterId,
            final @NotNull String tagName,
            final @NotNull DomainTag domainTag) {
        final String decodedTagName = URLDecoder.decode(tagName, StandardCharsets.UTF_8);
        return systemInformation.isConfigWriteable() ?
                configExtractor.getAdapterByAdapterId(adapterId).map(oldInstance -> {
                    final AtomicBoolean updated = new AtomicBoolean(false);
                    final var newTagList = oldInstance.getTags().stream().map(tag -> {
                        if (tag.getName().equals(decodedTagName)) {
                            updated.set(true);
                            return toTagEntity(domainTag);
                        }
                        return tag;
                    }).toList();
                    if (updated.get()) {
                        final ProtocolAdapterEntity newConfig = new ProtocolAdapterEntity(oldInstance.getAdapterId(),
                                oldInstance.getProtocolId(),
                                oldInstance.getConfigVersion(),
                                oldInstance.getConfig(),
                                oldInstance.getNorthboundMappings(),
                                oldInstance.getSouthboundMappings(),
                                newTagList);
                        if (!configExtractor.updateAdapter(newConfig)) {
                            return adapterCannotBeUpdatedError(adapterId);
                        }
                        return Response.ok().build();
                    }

                    return errorResponse(new DomainTagNotFoundError(tagName));
                }).orElseGet(() -> {
                    log.warn("Tag could not be updated for adapter '{}' because the adapter was not found.", adapterId);
                    return errorResponse(new AdapterNotFound403Error(String.format("Adapter not found '%s'",
                            adapterId)));
                }) :
                errorResponse(new ConfigWritingDisabled());
    }

    @Override
    public @NotNull Response updateAdapterDomainTags(
            final @NotNull String adapterId,
            final @NotNull DomainTagList domainTagList) {
        return systemInformation.isConfigWriteable() ?
                configExtractor.getAdapterByAdapterId(adapterId).map(oldInstance -> {
                    if (!configExtractor.updateAdapter(new ProtocolAdapterEntity(oldInstance.getAdapterId(),
                            oldInstance.getProtocolId(),
                            oldInstance.getConfigVersion(),
                            oldInstance.getConfig(),
                            oldInstance.getNorthboundMappings(),
                            oldInstance.getSouthboundMappings(),
                            domainTagList.getItems().stream().map(this::toTagEntity).toList()))) {
                        return adapterCannotBeUpdatedError(adapterId);
                    }
                    return Response.ok().build();
                }).orElseGet(() -> {
                    log.warn("Tags could not be updated for adapter '{}' because the adapter was not found.",
                            adapterId);
                    return adapterNotFoundError(adapterId).get();
                }) :
                errorResponse(new ConfigWritingDisabled());

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
        // empty list is also 200 as discussed.
        return Response.ok(new DomainTagList().items(domainTags.stream()
                .map(com.hivemq.persistence.domain.DomainTag::toModel)
                .toList())).build();
    }

    @Override
    public @NotNull Response getDomainTag(final @NotNull String tagName) {
        final String decodedTagName = URLDecoder.decode(tagName, StandardCharsets.UTF_8);
        return protocolAdapterManager.getDomainTagByName(decodedTagName)
                .map(tag -> Response.ok(tag.toModel()).build())
                .orElse(errorResponse(new DomainTagNotFoundError(decodedTagName)));
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
                    return errorResponse(new AdapterTypeNotFoundError("Adapter of type not found: " + protocolId));
                });
    }

    @Override
    public @NotNull Response getWritingSchema(final @NotNull String adapterId, final @NotNull String tagName) {
        final String decodedTagName = URLDecoder.decode(tagName, StandardCharsets.UTF_8);

        final Optional<ProtocolAdapterWrapper> maybeWrapper =
                protocolAdapterManager.getProtocolAdapterWrapperByAdapterId(adapterId);
        if (maybeWrapper.isEmpty()) {
            log.warn("The Json Schema for an adapter '{}' was requested, but the adapter does not exist.", adapterId);
            return adapterNotFoundError(adapterId).get();
        }

        final com.hivemq.adapter.sdk.api.ProtocolAdapter adapter = maybeWrapper.get().getAdapter();
        if (!(adapter instanceof WritingProtocolAdapter)) {
            log.warn("The Json Schema for an adapter '{}' was requested, which does not support writing to PLCs.",
                    adapterId);
            return errorResponse(new AdapterTypeReadOnlyError("The adapter with id '" +
                    adapterId +
                    "' exists, but it does not support writing to PLCs."));
        }

        final TagSchemaCreationOutputImpl tagSchemaCreationOutput = new TagSchemaCreationOutputImpl();
        adapter.createTagSchema(new TagSchemaCreationInputImpl(decodedTagName), tagSchemaCreationOutput);

        try {
            return Response.ok(tagSchemaCreationOutput.getFuture().get()).build(); // JSON schema root node
        } catch (final @NotNull InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Creation of json schema for writing to PLCs were interrupted.");
            log.debug("Original exception: ", e);
            return errorResponse(new InternalServerError(null));
        } catch (final @NotNull ExecutionException e) {
            return switch (tagSchemaCreationOutput.getStatus()) {
                case NOT_SUPPORTED -> errorResponse(new AdapterOperationNotSupportedError("Operation not supported:" +
                        e.getCause().getMessage()));
                case ADAPTER_NOT_STARTED ->
                        errorResponse(new AdapterOperationNotSupportedError("Adapter not started: " +
                                e.getCause().getMessage()));
                case TAG_NOT_FOUND -> errorResponse(new DomainTagNotFoundError(tagName));
                default -> {
                    log.warn("Exception was raised during creation of json schema for writing to PLCs.");
                    if (log.isDebugEnabled()) {
                        log.debug("Original exception: ", e);
                    }
                    yield errorResponse(new InternalServerError(null));
                }
            };
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public @NotNull Response createCompleteAdapter(
            final @NotNull String adapterType,
            final @NotNull String adapterName,
            final @NotNull AdapterConfig adapterConfig) {
        if (!systemInformation.isConfigWriteable()) {
            return errorResponse(new ConfigWritingDisabled());
        }

        final Optional<ProtocolAdapterInformation> maybeInfo = protocolAdapterManager.getAdapterTypeById(adapterType);
        if (maybeInfo.isEmpty()) {
            return errorResponse(new AdapterTypeNotFoundError(String.format("Adapter not found '%s'", adapterType)));
        }

        final ApiErrorMessages errorMessages = ApiErrorUtils.createErrorContainer();
        final String adapterId = adapterConfig.getConfig().getId();
        if (configExtractor.getAdapterByAdapterId(adapterId).isPresent()) {
            addValidationError(errorMessages, "id", "Adapter ID must be unique in system");
            return errorResponse(new AdapterFailedSchemaValidationError(errorMessages.toErrorList()));
        }
        validateAdapterSchema(errorMessages, adapterConfig.getConfig());
        if (hasRequestErrors(errorMessages)) {
            return errorResponse(new AdapterFailedSchemaValidationError(errorMessages.toErrorList()));
        }

        try {
            configExtractor.addAdapter(new ProtocolAdapterEntity(adapterId,
                    adapterType,
                    maybeInfo.get().getCurrentConfigVersion(),
                    (Map<String, Object>) adapterConfig.getConfig().getConfig(),
                    adapterConfig.getNorthboundMappings().stream().map(NorthboundMappingEntity::fromApi).toList(),
                    adapterConfig.getSouthboundMappings().stream().map(this::toSouthboundMappingEntity).toList(),
                    adapterConfig.getTags().stream().map(this::toTagEntity).toList()));
        } catch (final IllegalArgumentException e) {
            if (e.getCause() instanceof UnrecognizedPropertyException) {
                addValidationError(errorMessages,
                        ((UnrecognizedPropertyException) e.getCause()).getPropertyName(),
                        "Unknown field on adapterConfig configuration");
            } else {
                log.error("Error processing incoming request", e);
            }
            return errorResponse(new AdapterFailedSchemaValidationError(errorMessages.toErrorList()));
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Added protocol adapterConfig of type {} with ID {}.",
                    adapterType,
                    adapterConfig.getConfig().getId());
        }
        return Response.ok().build();
    }

    @Override
    public @NotNull Response getNorthboundMappings() {
        return Response.status(200)
                .entity(new NorthboundMappingListModel(configExtractor.getAllConfigs()
                        .stream()
                        .flatMap(adapter -> adapter.getNorthboundMappings()
                                .stream()
                                .map(NorthboundMappingModel::fromEntity))
                        .toList()))
                .build();
    }

    @Override
    public @NotNull Response getSouthboundMappings() {
        return Response.status(200)
                .entity(new SouthboundMappingList().items(configExtractor.getAllConfigs()
                        .stream()
                        .flatMap(adapter -> adapter.getSouthboundMappings()
                                .stream()
                                .map(SouthboundMappingEntity::toAPi))
                        .toList()))
                .build();
    }

    @Override
    public @NotNull Response getAdapterNorthboundMappings(final @NotNull String adapterId) {
        return configExtractor.getAdapterByAdapterId(adapterId)
                .map(adapter -> Response.ok(new NorthboundMappingListModel(adapter.getNorthboundMappings()
                        .stream()
                        .map(NorthboundMappingModel::fromEntity)
                        .toList())).build())
                .orElseGet(adapterNotFoundError(adapterId));
    }

    @Override
    public @NotNull Response updateAdapterNorthboundMappings(
            final @NotNull String adapterId,
            final @NotNull NorthboundMappingList northboundMappings) {
        return systemInformation.isConfigWriteable() ?
                configExtractor.getAdapterByAdapterId(adapterId)
                        .map(updateAdapterNorthboundMappingsResponse(adapterId, northboundMappings))
                        .orElseGet(adapterNotFoundError(adapterId)) :
                errorResponse(new ConfigWritingDisabled());
    }

    @Override
    public @NotNull Response getAdapterSouthboundMappings(final @NotNull String adapterId) {
        return configExtractor.getAdapterByAdapterId(adapterId)
                .map(adapter -> adapter.getSouthboundMappings().stream().map(SouthboundMappingEntity::toAPi).toList())
                .map(SouthboundMappingList::new)
                .map(mappingsList -> Response.ok(mappingsList).build())
                .orElseGet(adapterNotFoundError(adapterId));
    }

    @Override
    public @NotNull Response updateAdapterSouthboundMappings(
            final @NotNull String adapterId,
            final @NotNull SouthboundMappingList southboundMappings) {
        return systemInformation.isConfigWriteable() ?
                configExtractor.getAdapterByAdapterId(adapterId)
                        .map(updateAdapterSouthboundMappingsResponse(adapterId, southboundMappings))
                        .orElseGet(adapterNotFoundError(adapterId)) :
                errorResponse(new ConfigWritingDisabled());
    }

    private @NotNull Function<ProtocolAdapterEntity, Response> updateAdapterNorthboundMappingsResponse(
            final @NotNull String adapterId,
            final @NotNull NorthboundMappingList northboundMappings) {
        return adapter -> {

            final Set<String> missingTags = new HashSet<>();
            final List<NorthboundMappingEntity> converted = northboundMappings.getItems().stream().map(mapping -> {
                missingTags.add(mapping.getTagName());
                return NorthboundMappingEntity.fromApi(mapping);
            }).toList();
            adapter.getTags().forEach(tag -> missingTags.remove(tag.getName()));
            if (!missingTags.isEmpty()) {
                log.error("The following tags were missing for updating the northbound mappings for adapter {}: {}",
                        adapterId,
                        missingTags);
                return errorResponse(new BadRequestError("Tags were missing for updating the northbound mappings" +
                        missingTags));
            }

            return configExtractor.getAdapterByAdapterId(adapterId)
                    .map(cfg -> new ProtocolAdapterEntity(cfg.getAdapterId(),
                            cfg.getProtocolId(),
                            cfg.getConfigVersion(),
                            cfg.getConfig(),
                            converted,
                            cfg.getSouthboundMappings(),
                            cfg.getTags()))
                    .map(newCfg -> {
                        if (!configExtractor.updateAdapter(newCfg)) {
                            return adapterCannotBeUpdatedError(adapterId);
                        }
                        log.info("Successfully updated northbound mappings for adapter '{}'.", adapterId);
                        return Response.ok(northboundMappings).build();
                    })
                    .orElseGet(adapterNotUpdatedError(adapterId));
        };
    }

    private @NotNull Function<ProtocolAdapterEntity, Response> updateAdapterSouthboundMappingsResponse(
            final @NotNull String adapterId,
            final @NotNull SouthboundMappingList southboundMappings) {
        return adapter -> {

            final Set<String> missingTags = new HashSet<>();
            final List<SouthboundMappingEntity> converted = southboundMappings.getItems().stream().map(mapping -> {
                missingTags.add(mapping.getTagName());
                return toSouthboundMappingEntity(mapping);
            }).toList();
            adapter.getTags().forEach(tag -> missingTags.remove(tag.getName()));
            if (!missingTags.isEmpty()) {
                log.error("The following tags were missing for updating the southbound mappings for adapter {}: {}",
                        adapterId,
                        missingTags);
                return errorResponse(new BadRequestError("Tags were missing for updating the southbound mappings" +
                        missingTags));
            }

            return configExtractor.getAdapterByAdapterId(adapterId)
                    .map(cfg -> new ProtocolAdapterEntity(cfg.getAdapterId(),
                            cfg.getProtocolId(),
                            cfg.getConfigVersion(),
                            cfg.getConfig(),
                            cfg.getNorthboundMappings(),
                            converted,
                            cfg.getTags()))
                    .map(newCfg -> {
                        if (!configExtractor.updateAdapter(newCfg)) {
                            return adapterCannotBeUpdatedError(adapterId);
                        }
                        log.info("Successfully updated fromMappings for adapter '{}'.", adapterId);
                        return Response.ok(southboundMappings).build();
                    })
                    .orElseGet(adapterNotUpdatedError(adapterId));
        };
    }

    private @NotNull SouthboundMappingEntity toSouthboundMappingEntity(final @NotNull com.hivemq.edge.api.model.SouthboundMapping model) {
        final TopicFilterPojo topicFilter = topicFilterPersistence.getTopicFilter(model.getTopicFilter());
        if (topicFilter == null) {
            throw new IllegalStateException("Cannot create Southbound mapping with 'null' topic filter");
        }
        if (topicFilter.getSchema() == null) {
            throw new IllegalStateException("Cannot create Southbound mapping with topic filter '" +
                    topicFilter +
                    "', because it has no schema attached");
        }
        return SouthboundMappingEntity.fromApi(model,
                new String(Base64.getDecoder().decode(topicFilter.getSchema().getData())));
    }

    private @NotNull TagEntity toTagEntity(final @NotNull DomainTag dmt) {
        return new TagEntity(dmt.getName(),
                dmt.getDescription(),
                objectMapper.convertValue(dmt.getDefinition(), AS_MAP_TYPE_REF));
    }

    private void validateAdapterSchema(
            final @NotNull ApiErrorMessages apiErrorMessages,
            final @NotNull Adapter adapter) {
        final ProtocolAdapterInformation info =
                protocolAdapterManager.getAllAvailableAdapterTypes().get(adapter.getType());
        if (info == null) {
            addValidationError(apiErrorMessages, "config", "Unable to find adapter type by supplied adapterTypeId");
            return;
        }
        if (adapter.getConfig() == null) {
            addValidationError(apiErrorMessages, "config", "Config must be supplied on the adapter");
            return;
        }
        final var validator = new ProtocolAdapterSchemaManager(objectMapper,
                protocolAdapterWritingService.writingEnabled() ?
                        info.configurationClassNorthAndSouthbound() :
                        info.configurationClassNorthbound());
        validator.validateObject(adapter.getConfig())
                .forEach(e -> addValidationError(apiErrorMessages, e.getFieldName(), e.getMessage()));
    }

    private @NotNull AdaptersList getAdaptersInternal(final @Nullable String adapterType) {
        Stream<ProtocolAdapterWrapper> stream = protocolAdapterManager.getProtocolAdapters().values().stream();
        if (adapterType != null) {
            stream = stream.filter(wrapper -> wrapper.getAdapterInformation().getProtocolId().equals(adapterType));
        }
        return new AdaptersList(stream.map(this::toAdapter).toList());
    }

    private @NotNull Adapter toAdapter(final @NotNull ProtocolAdapterWrapper value) {
        final Thread currentThread = Thread.currentThread();
        final ClassLoader ctxClassLoader = currentThread.getContextClassLoader();
        final Map<String, Object> config;
        try {
            currentThread.setContextClassLoader(value.getAdapterFactory().getClass().getClassLoader());
            config = value.getAdapterFactory().unconvertConfigObject(objectMapper, value.getConfigObject());
            config.put("id", value.getId());
        } finally {
            currentThread.setContextClassLoader(ctxClassLoader);
        }
        final String adapterId = value.getId();
        return new Adapter(adapterId).type(value.getAdapterInformation().getProtocolId())
                .config(objectMapper.valueToTree(config))
                .status(getAdapterStatusInternal(adapterId));
    }

    private @NotNull Status getAdapterStatusInternal(final @NotNull String adapterId) {
        return protocolAdapterManager.getProtocolAdapterWrapperByAdapterId(adapterId)
                .map(AdapterStatusModelConversionUtils::getAdapterStatus)
                .orElseGet(() -> new Status().id(adapterId)
                        .type(ApiConstants.ADAPTER_TYPE)
                        .connection(Status.ConnectionEnum.UNKNOWN)
                        .runtime(Status.RuntimeEnum.STOPPED));
    }
}
