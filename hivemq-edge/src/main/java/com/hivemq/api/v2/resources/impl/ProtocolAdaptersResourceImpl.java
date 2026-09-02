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
package com.hivemq.api.v2.resources.impl;

import static com.hivemq.api.v2.errors.ProtocolAdapterErrorFactory.adapterActivationInvalidError;
import static com.hivemq.api.v2.errors.ProtocolAdapterErrorFactory.adapterNotFoundError;
import static com.hivemq.api.v2.errors.ProtocolAdapterErrorFactory.adapterTypeNotFoundError;
import static com.hivemq.api.v2.errors.ProtocolAdapterErrorFactory.tagNotFoundError;
import static com.hivemq.util.ErrorResponseUtil.errorResponse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.adapter.sdk.api.schema.SchemaJsonRepresentation;
import com.hivemq.adapter.sdk.api.v2.ProtocolAdapterCapability;
import com.hivemq.adapter.sdk.api.v2.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.v2.factories.ProtocolAdapterFactory;
import com.hivemq.adapter.sdk.api.v2.messaging.MailboxSender;
import com.hivemq.adapter.sdk.api.v2.model.BrowseFilter;
import com.hivemq.adapter.sdk.api.v2.model.BrowseNode;
import com.hivemq.adapter.sdk.api.v2.node.Node;
import com.hivemq.api.AbstractApi;
import com.hivemq.api.v2.errors.ProtocolAdapterErrorFactory;
import com.hivemq.edge.api.v2.ProtocolAdaptersApi;
import com.hivemq.edge.api.v2.model.AccessFlags;
import com.hivemq.edge.api.v2.model.AdapterActivation;
import com.hivemq.edge.api.v2.model.AdapterDefinition;
import com.hivemq.edge.api.v2.model.AdapterMachineState;
import com.hivemq.edge.api.v2.model.AdapterStatus;
import com.hivemq.edge.api.v2.model.AdapterType;
import com.hivemq.edge.api.v2.model.BrowseCommand;
import com.hivemq.edge.api.v2.model.BrowseResult;
import com.hivemq.edge.api.v2.model.BrowseResultEntry;
import com.hivemq.edge.api.v2.model.Mapping;
import com.hivemq.edge.api.v2.model.MappingBlockedBy;
import com.hivemq.edge.api.v2.model.NodeTagPair;
import com.hivemq.edge.api.v2.model.SkippedTagRetry;
import com.hivemq.edge.api.v2.model.TagRetryRequest;
import com.hivemq.edge.api.v2.model.TagRetryResult;
import com.hivemq.edge.api.v2.model.TagStatusDetail;
import com.hivemq.edge.api.v2.model.TagSummary;
import com.hivemq.protocols.v2.browse.BrowsedNode;
import com.hivemq.protocols.v2.config.AccessFlagsEntity;
import com.hivemq.protocols.v2.config.NorthboundMappingEntity;
import com.hivemq.protocols.v2.config.ProtocolAdapterEntity;
import com.hivemq.protocols.v2.config.ProtocolAdapterExtractor;
import com.hivemq.protocols.v2.config.SouthboundMappingEntity;
import com.hivemq.protocols.v2.config.TagEntity;
import com.hivemq.protocols.v2.manager.ProtocolAdapterFactoryRegistry;
import com.hivemq.protocols.v2.manager.ProtocolAdapterHandleRegistry;
import com.hivemq.protocols.v2.manager.ProtocolAdapterHandleRegistry.ProtocolAdapterHandle;
import com.hivemq.protocols.v2.manager.ProtocolAdapterManagerMessage;
import com.hivemq.protocols.v2.view.AdapterStatusColor;
import com.hivemq.protocols.v2.view.AdapterStatusSnapshot;
import com.hivemq.protocols.v2.view.MappingStatus;
import com.hivemq.protocols.v2.view.TagStatus;
import com.hivemq.protocols.v2.view.TagStatusSnapshot;
import com.hivemq.protocols.v2.wrapper.BrowseRejectedException;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterDirection;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

/**
 * The v2 protocol-adapters REST resource. It is <b>state-only</b>: it serves read-only views
 * folded purely from the immutable snapshots the adapter wrappers publish and from the externally-generated
 * configuration section, and it issues runtime-state commands (direction activation, tag retry, browse) by
 * {@code tell}ing the manager — it never touches actor state and never writes configuration.
 * <p>
 * Reads come from the {@link ProtocolAdapterHandleRegistry} snapshots and the read-only
 * {@link ProtocolAdapterExtractor}; types and schema projections come from the {@link ProtocolAdapterFactoryRegistry}
 * (empty in production until a real adapter type is ported, D8); commands go to the manager mailbox through a
 * {@link MailboxSender}. The generated v2 contract lives in its own package ({@code com.hivemq.edge.api.v2}); a few
 * generated DTO enums share simple names with the framework view folds and SDK types, so those are referenced fully
 * qualified. The constructor is {@code @Inject}-ready, but the resource is registered beside v1 only in the
 * side-by-side wiring task (T13).
 */
@Singleton
public class ProtocolAdaptersResourceImpl extends AbstractApi implements ProtocolAdaptersApi {

    private static final @NotNull SchemaJsonRepresentation JSCHEMA = SchemaJsonRepresentation.INSTANCE;

    /**
     * The browse request timeout: the JAX-RS request thread is the only thread that blocks, and it
     * gives up after this long with {@code 504}. The wrapper's own browse deadline is the backstop that releases
     * the in-flight slot.
     */
    private static final long BROWSE_REQUEST_TIMEOUT_MILLIS = 60_000L;

    private final @NotNull MailboxSender<ProtocolAdapterManagerMessage> manager;
    private final @NotNull ProtocolAdapterHandleRegistry handleRegistry;
    private final @NotNull ProtocolAdapterFactoryRegistry factoryRegistry;
    private final @NotNull ProtocolAdapterExtractor configExtractor;
    private final @NotNull ObjectMapper objectMapper;
    private final long browseTimeoutMillis;

    /**
     * @param manager         the manager mailbox the runtime-state commands are told to.
     * @param handleRegistry  the REST-readable adapter registry (snapshots and senders).
     * @param factoryRegistry the registered adapter type factories (empty in production, D8).
     * @param configExtractor the read-only {@code <v2>} configuration extractor.
     * @param objectMapper    the JSON mapper used to deserialize browse filter node strings.
     */
    @Inject
    public ProtocolAdaptersResourceImpl(
            final @NotNull MailboxSender<ProtocolAdapterManagerMessage> manager,
            final @NotNull ProtocolAdapterHandleRegistry handleRegistry,
            final @NotNull ProtocolAdapterFactoryRegistry factoryRegistry,
            final @NotNull ProtocolAdapterExtractor configExtractor,
            final @NotNull ObjectMapper objectMapper) {
        this(manager, handleRegistry, factoryRegistry, configExtractor, objectMapper, BROWSE_REQUEST_TIMEOUT_MILLIS);
    }

    ProtocolAdaptersResourceImpl(
            final @NotNull MailboxSender<ProtocolAdapterManagerMessage> manager,
            final @NotNull ProtocolAdapterHandleRegistry handleRegistry,
            final @NotNull ProtocolAdapterFactoryRegistry factoryRegistry,
            final @NotNull ProtocolAdapterExtractor configExtractor,
            final @NotNull ObjectMapper objectMapper,
            final long browseTimeoutMillis) {
        this.manager = manager;
        this.handleRegistry = handleRegistry;
        this.factoryRegistry = factoryRegistry;
        this.configExtractor = configExtractor;
        this.objectMapper = objectMapper;
        this.browseTimeoutMillis = browseTimeoutMillis;
    }

    private static boolean hasPermanentFailure(final @NotNull TagStatusSnapshot tag) {
        return tag.readAspectPermanentFailure() || tag.writeAspectPermanentFailure();
    }

    private static @NotNull String skipReason(final @NotNull TagStatusSnapshot tag) {
        return !tag.readAspectGoalActive() && !tag.writeAspectGoalActive()
                ? "tag is deactivated"
                : "tag is not in an error state";
    }

    private static @Nullable TagStatusSnapshot findTag(
            final @Nullable AdapterStatusSnapshot snapshot, final @NotNull String tagName) {
        if (snapshot != null) {
            for (final TagStatusSnapshot tag : snapshot.tags()) {
                if (tag.tagName().equals(tagName)) {
                    return tag;
                }
            }
        }
        return null;
    }

    private static @Nullable OffsetDateTime toOffsetDateTime(final long millis) {
        return millis <= 0 ? null : OffsetDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneOffset.UTC);
    }

    private static @NotNull Response adapterNotFound(final @NotNull String adapterId) {
        return errorResponse(adapterNotFoundError(adapterId));
    }

    private static @NotNull Response adapterTypeUnavailable(final @NotNull String adapterId) {
        return errorResponse(adapterTypeNotFoundError(adapterId));
    }

    private static @NotNull Response tagNotFound(final @NotNull String adapterId, final @NotNull String tagName) {
        return errorResponse(tagNotFoundError(adapterId, tagName));
    }

    private static com.hivemq.edge.api.v2.model.@NonNull TagStatus getTagStatus(@NonNull TagStatusSnapshot snapshot) {
        return com.hivemq.edge.api.v2.model.TagStatus.fromValue(
                TagStatus.of(snapshot).name());
    }

    @Override
    public @NotNull Response getAdapterTypes() {
        return Response.ok(factoryRegistry.all().stream().map(this::toTypeDto).toList())
                .build();
    }

    @Override
    public @NotNull Response listAdapters() {
        return Response.ok(allStatuses()).build();
    }

    @Override
    public @NotNull Response getAdapterStatuses() {
        return Response.ok(allStatuses()).build();
    }

    @Override
    public @NotNull Response getAdapterStatus(final @NotNull String adapterId) {
        final AdapterStatusSnapshot snapshot = snapshotOf(adapterId);
        return snapshot == null
                ? adapterNotFound(adapterId)
                : Response.ok(toStatusDto(snapshot)).build();
    }

    @Override
    public @NotNull Response getAdapter(final @NotNull String adapterId) {
        final Optional<ProtocolAdapterEntity> config = configExtractor.getAdapterByAdapterId(adapterId);
        return config.isEmpty()
                ? adapterNotFound(adapterId)
                : Response.ok(toDefinition(config.get())).build();
    }

    @Override
    public @NotNull Response getAdapterTags(final @NotNull String adapterId) {
        final Optional<ProtocolAdapterEntity> config = configExtractor.getAdapterByAdapterId(adapterId);
        if (config.isEmpty()) {
            return adapterNotFound(adapterId);
        }
        final AdapterStatusSnapshot snapshot = snapshotOf(adapterId);
        final Object nodeSchema = nodeSchemaProjection(config.get());
        return Response.ok(config.get().getTags().stream()
                        .map(tag -> toNodeTagPair(tag, findTag(snapshot, tag.getName()), nodeSchema))
                        .toList())
                .build();
    }

    @Override
    public @NotNull Response getTagStatus(final @NotNull String adapterId, final @NotNull String tagName) {
        final AdapterStatusSnapshot snapshot = snapshotOf(adapterId);
        if (snapshot == null) {
            return adapterNotFound(adapterId);
        }
        final TagStatusSnapshot tag = findTag(snapshot, tagName);
        return tag == null
                ? tagNotFound(adapterId, tagName)
                : Response.ok(toTagStatusDetail(tag)).build();
    }

    @Override
    public @NotNull Response getNorthboundMappings(final @NotNull String adapterId) {
        final Optional<ProtocolAdapterEntity> config = configExtractor.getAdapterByAdapterId(adapterId);
        if (config.isEmpty()) {
            return adapterNotFound(adapterId);
        }
        final AdapterStatusSnapshot snapshot = snapshotOf(adapterId);
        final List<Mapping> mappings = config.get().getNorthboundMappings().stream()
                .map((final NorthboundMappingEntity mapping) ->
                        toMapping(adapterId, snapshot, mapping.getTagName(), mapping.getTopic(), false))
                .toList();
        return Response.ok(mappings).build();
    }

    @Override
    public @NotNull Response getSouthboundMappings(final @NotNull String adapterId) {
        final Optional<ProtocolAdapterEntity> config = configExtractor.getAdapterByAdapterId(adapterId);
        if (config.isEmpty()) {
            return adapterNotFound(adapterId);
        }
        final AdapterStatusSnapshot snapshot = snapshotOf(adapterId);
        final List<Mapping> mappings = config.get().getSouthboundMappings().stream()
                .map((final SouthboundMappingEntity mapping) ->
                        toMapping(adapterId, snapshot, mapping.getTagName(), mapping.getTopic(), true))
                .toList();
        return Response.ok(mappings).build();
    }

    @Override
    public @NotNull Response getNodeSchema(final @NotNull String adapterId) {
        final Optional<ProtocolAdapterFactory> factory = factoryFor(adapterId);
        return factory.isEmpty()
                ? adapterTypeUnavailable(adapterId)
                : Response.ok(JSCHEMA.toJsonSchema(factory.get().nodeDefinitionSchema()))
                        .build();
    }

    @Override
    public @NotNull Response getAdapterSchema(final @NotNull String adapterId) {
        final Optional<ProtocolAdapterFactory> factory = factoryFor(adapterId);
        return factory.isEmpty()
                ? adapterTypeUnavailable(adapterId)
                : Response.ok(JSCHEMA.toJsonSchema(factory.get().adapterConfigSchema()))
                        .build();
    }

    @Override
    public @NotNull Response setAdapterActivation(
            final @NotNull String adapterId, final @NotNull AdapterActivation body) {
        if (body.getDirection() == null || body.getActivated() == null) {
            return errorResponse(adapterActivationInvalidError());
        }
        if (handleRegistry.find(adapterId) == null) {
            return adapterNotFound(adapterId);
        }
        final ProtocolAdapterDirection direction =
                ProtocolAdapterDirection.valueOf(body.getDirection().name());
        if (Boolean.TRUE.equals(body.getActivated())) {
            manager.tell(new ProtocolAdapterManagerMessage.ActivateAdapter(adapterId, direction));
        } else {
            manager.tell(new ProtocolAdapterManagerMessage.DeactivateAdapter(adapterId, direction));
        }
        return Response.ok().build();
    }

    @Override
    public @NotNull Response retryTag(final @NotNull String adapterId, final @NotNull String tagName) {
        final AdapterStatusSnapshot snapshot = snapshotOf(adapterId);
        if (snapshot == null) {
            return adapterNotFound(adapterId);
        }
        return findTag(snapshot, tagName) == null
                ? tagNotFound(adapterId, tagName)
                : Response.ok(retryResult(adapterId, snapshot, List.of(tagName)))
                        .build();
    }

    @Override
    public @NotNull Response retryTags(final @NotNull String adapterId, final @NotNull TagRetryRequest body) {
        final AdapterStatusSnapshot snapshot = snapshotOf(adapterId);
        return snapshot == null
                ? adapterNotFound(adapterId)
                : Response.ok(retryResult(adapterId, snapshot, body.getTagNames()))
                        .build();
    }

    @Override
    public @NotNull Response browseAdapter(final @NotNull String adapterId, final @Nullable BrowseCommand body) {
        if (handleRegistry.find(adapterId) == null) {
            return adapterNotFound(adapterId);
        }
        final Optional<ProtocolAdapterFactory> factory = factoryFor(adapterId);
        if (factory.isEmpty()
                || !factory.get().information().capabilities().contains(ProtocolAdapterCapability.BROWSE)) {
            return errorResponse(ProtocolAdapterErrorFactory.browseNotSupportedError(adapterId));
        }
        final String nodeString = (body == null
                        || body.getNodeString() == null
                        || body.getNodeString().isBlank())
                ? "{}"
                : body.getNodeString();
        final Node filterNode;
        try {
            filterNode = objectMapper.readValue(
                    nodeString, factory.get().information().nodeClass());
        } catch (final JsonProcessingException exception) {
            return errorResponse(
                    ProtocolAdapterErrorFactory.browseFilterInvalidError(adapterId, exception.getOriginalMessage()));
        }

        final CompletableFuture<List<BrowsedNode>> completion = new CompletableFuture<>();
        manager.tell(
                new ProtocolAdapterManagerMessage.BrowseRequested(adapterId, new BrowseFilter(filterNode), completion));
        try {
            final List<BrowsedNode> entries = completion.get(browseTimeoutMillis, TimeUnit.MILLISECONDS);
            return Response.ok(toBrowseResult(entries)).build();
        } catch (final TimeoutException exception) {
            return errorResponse(ProtocolAdapterErrorFactory.browseTimeoutError(adapterId));
        } catch (final InterruptedException exception) {
            Thread.currentThread().interrupt();
            return errorResponse(ProtocolAdapterErrorFactory.browseInterruptedError(adapterId));
        } catch (final ExecutionException exception) {
            return browseFailure(adapterId, exception.getCause());
        }
    }

    private @NotNull Response browseFailure(final @NotNull String adapterId, final @Nullable Throwable cause) {
        if (cause instanceof final BrowseRejectedException rejected) {
            return switch (rejected.reason()) {
                case NOT_CONNECTED -> errorResponse(ProtocolAdapterErrorFactory.adapterNotConnectedError(adapterId));
                case ALREADY_IN_FLIGHT -> errorResponse(ProtocolAdapterErrorFactory.browseInProgressError(adapterId));
                case UNSUPPORTED -> errorResponse(ProtocolAdapterErrorFactory.browseNotSupportedError(adapterId));
                case TIMED_OUT -> errorResponse(ProtocolAdapterErrorFactory.browseTimeoutError(adapterId));
                case FAILED -> errorResponse(ProtocolAdapterErrorFactory.browseFailedError(adapterId));
            };
        }
        if (cause instanceof IllegalArgumentException) {
            return adapterNotFound(adapterId);
        }
        return errorResponse(ProtocolAdapterErrorFactory.browseFailedError(adapterId));
    }

    private @NotNull List<AdapterStatus> allStatuses() {
        final List<AdapterStatus> statuses = new ArrayList<>();
        for (final ProtocolAdapterHandle handle : handleRegistry.all()) {
            final AdapterStatusSnapshot snapshot = handle.snapshot().get();
            if (snapshot != null) {
                statuses.add(toStatusDto(snapshot));
            }
        }
        return statuses;
    }

    private @NotNull AdapterStatus toStatusDto(final @NotNull AdapterStatusSnapshot snapshot) {
        return new AdapterStatus()
                .id(snapshot.adapterId())
                .color(com.hivemq.edge.api.v2.model.AdapterStatusColor.fromValue(
                        AdapterStatusColor.of(snapshot.machineState()).name()))
                .stateMachineState(
                        AdapterMachineState.fromValue(snapshot.machineState().name()))
                .northboundActivated(snapshot.northboundActivated())
                .southboundActivated(snapshot.southboundActivated())
                .tags(toTagSummary(snapshot.tags()))
                .message(snapshot.lastErrorReason())
                .lastTransitionAt(toOffsetDateTime(snapshot.lastTransitionAtMillis()));
    }

    private @NotNull TagSummary toTagSummary(final @NotNull List<TagStatusSnapshot> tags) {
        int northboundAndSouthbound = 0;
        int northboundOnly = 0;
        int southboundOnly = 0;
        int deactivated = 0;
        int error = 0;
        for (final TagStatusSnapshot tag : tags) {
            switch (TagStatus.of(tag)) {
                case NORTHBOUND_AND_SOUTHBOUND -> northboundAndSouthbound++;
                case NORTHBOUND_ONLY -> northboundOnly++;
                case SOUTHBOUND_ONLY -> southboundOnly++;
                case DEACTIVATED -> deactivated++;
                case ERROR -> error++;
            }
        }
        return new TagSummary()
                .total(tags.size())
                .northboundAndSouthbound(northboundAndSouthbound)
                .northboundOnly(northboundOnly)
                .southboundOnly(southboundOnly)
                .deactivated(deactivated)
                .error(error);
    }

    private @NotNull NodeTagPair toNodeTagPair(
            final @NotNull TagEntity tag,
            final @Nullable TagStatusSnapshot snapshot,
            final @Nullable Object nodeSchema) {
        return new NodeTagPair()
                .tagName(tag.getName())
                .nodeString(tag.getNodeString())
                .readActivated(tag.isReadActivated())
                .writeActivated(tag.isWriteActivated())
                .pollable(tag.isPollable())
                .subscribable(tag.isSubscribable())
                .access(toAccessFlags(tag.getAccess()))
                .schema(nodeSchema)
                .status(snapshot == null ? null : getTagStatus(snapshot));
    }

    // ── tag retry result derivation ──────────────────────────────────────────────────────────────────────────────

    private @NotNull AccessFlags toAccessFlags(final @NotNull AccessFlagsEntity access) {
        return new AccessFlags()
                .readable(com.hivemq.edge.api.v2.model.AccessTriState.fromValue(
                        access.getReadable().name()))
                .writable(com.hivemq.edge.api.v2.model.AccessTriState.fromValue(
                        access.getWritable().name()))
                .pollable(com.hivemq.edge.api.v2.model.AccessTriState.fromValue(
                        access.getPollable().name()))
                .subscribable(com.hivemq.edge.api.v2.model.AccessTriState.fromValue(
                        access.getSubscribable().name()));
    }

    private @NotNull AccessFlags toAccessFlags(final @NotNull com.hivemq.adapter.sdk.api.v2.node.AccessFlags access) {
        return new AccessFlags()
                .readable(com.hivemq.edge.api.v2.model.AccessTriState.fromValue(
                        access.readable().name()))
                .writable(com.hivemq.edge.api.v2.model.AccessTriState.fromValue(
                        access.writable().name()))
                .pollable(com.hivemq.edge.api.v2.model.AccessTriState.fromValue(
                        access.pollable().name()))
                .subscribable(com.hivemq.edge.api.v2.model.AccessTriState.fromValue(
                        access.subscribable().name()));
    }

    private @NotNull TagStatusDetail toTagStatusDetail(final @NotNull TagStatusSnapshot tag) {
        return new TagStatusDetail()
                .tagName(tag.tagName())
                .status(getTagStatus(tag))
                .readAspectState(tag.readAspectStateName())
                .writeAspectState(tag.writeAspectStateName())
                .readActivated(tag.readActivated())
                .writeActivated(tag.writeActivated())
                .readUsed(tag.readUsed())
                .writeUsed(tag.writeUsed())
                .failureCount(tag.failureCount())
                .lastFailureReason(tag.lastFailureReason())
                .lastTransitionAt(toOffsetDateTime(tag.lastTransitionAtMillis()));
    }

    private @NotNull Mapping toMapping(
            final @NotNull String adapterId,
            final @Nullable AdapterStatusSnapshot snapshot,
            final @NotNull String tagName,
            final @NotNull String topic,
            final boolean writeSide) {
        final TagStatusSnapshot tag = findTag(snapshot, tagName);
        final MappingStatus status;
        if (snapshot == null || tag == null) {
            // The adapter is not running (no snapshot) or the mapping references a tag absent from the running set.
            status = snapshot == null ? MappingStatus.BLOCKED_BY_ADAPTER : MappingStatus.BLOCKED_BY_TAG_ERROR;
        } else {
            status = MappingStatus.of(snapshot.machineState(), tag, writeSide);
        }
        final Mapping dto = new Mapping()
                .tagName(tagName)
                .topic(topic)
                .status(com.hivemq.edge.api.v2.model.MappingStatus.fromValue(status.name()));
        if (status != MappingStatus.ACTIVE) {
            dto.blockedBy(new MappingBlockedBy()
                    .adapterId(adapterId)
                    .direction(com.hivemq.edge.api.v2.model.AdapterDirection.fromValue(
                            writeSide ? "SOUTHBOUND" : "NORTHBOUND"))
                    .tagName(tagName));
        }
        return dto;
    }

    private @NotNull AdapterType toTypeDto(final @NotNull ProtocolAdapterFactory factory) {
        final ProtocolAdapterInformation information = factory.information();
        return new AdapterType()
                .id(information.protocolId())
                .protocolId(information.protocolId())
                .displayName(information.displayName())
                .description(information.description())
                .version(information.version())
                .logoUrl(information.logoUrl())
                .author(information.author())
                .category(information.category().name())
                .searchTags(information.tags().stream().map(Enum::name).toList())
                .capabilities(information.capabilities().stream()
                        .map(capability -> AdapterType.CapabilitiesEnum.fromValue(capability.name()))
                        .toList())
                .configVersion(information.currentConfigVersion())
                .nodeSchema(JSCHEMA.toJsonSchema(factory.nodeDefinitionSchema()))
                .adapterSchema(JSCHEMA.toJsonSchema(factory.adapterConfigSchema()));
    }

    private @NotNull AdapterDefinition toDefinition(final @NotNull ProtocolAdapterEntity entity) {
        return new AdapterDefinition()
                .id(entity.getAdapterId())
                .protocolId(entity.getProtocolId())
                .configVersion(entity.getConfigVersion())
                .northboundActivated(entity.isNorthboundActivated())
                .southboundActivated(entity.isSouthboundActivated())
                .skipVerification(entity.isSkipVerification())
                .watchdogTimeoutMillis(entity.getWatchdogTimeoutMillis())
                .adapterConfiguration(entity.getAdapterConfiguration());
    }

    private @NotNull BrowseResult toBrowseResult(final @NotNull List<BrowsedNode> nodes) {
        return new BrowseResult()
                .entries(nodes.stream()
                        .map(node -> {
                            final BrowseNode entry = node.entry();
                            return new BrowseResultEntry()
                                    .nodeId(entry.node().nodeId())
                                    .nodeString(entry.node().nodeString())
                                    .type(BrowseResultEntry.TypeEnum.fromValue(
                                            entry.type().name()))
                                    .selectable(entry.selectable())
                                    .path(node.path())
                                    .tagName(node.tagName())
                                    .dataType(node.attributes().dataType())
                                    .access(toAccessFlags(node.attributes().access()))
                                    .description(node.attributes().description());
                        })
                        .toList());
    }

    private @NotNull TagRetryResult retryResult(
            final @NotNull String adapterId,
            final @NotNull AdapterStatusSnapshot snapshot,
            final @Nullable List<String> requestedNames) {
        final List<String> accepted = new ArrayList<>();
        final List<SkippedTagRetry> skipped = new ArrayList<>();
        if (requestedNames == null || requestedNames.isEmpty()) {
            for (final TagStatusSnapshot tag : snapshot.tags()) {
                if (hasPermanentFailure(tag)) {
                    accepted.add(tag.tagName());
                    manager.tell(new ProtocolAdapterManagerMessage.RetryTag(adapterId, tag.tagName()));
                }
            }
        } else {
            for (final String name : requestedNames) {
                final TagStatusSnapshot tag = findTag(snapshot, name);
                if (tag == null) {
                    skipped.add(new SkippedTagRetry().tagName(name).reason("tag not found"));
                } else if (hasPermanentFailure(tag)) {
                    accepted.add(name);
                    manager.tell(new ProtocolAdapterManagerMessage.RetryTag(adapterId, name));
                } else {
                    skipped.add(new SkippedTagRetry().tagName(name).reason(skipReason(tag)));
                }
            }
        }
        return new TagRetryResult().accepted(accepted).skipped(skipped);
    }

    private @Nullable AdapterStatusSnapshot snapshotOf(final @NotNull String adapterId) {
        final ProtocolAdapterHandle handle = handleRegistry.find(adapterId);
        return handle != null ? handle.snapshot().get() : null;
    }

    private @NotNull Optional<ProtocolAdapterFactory> factoryFor(final @NotNull String adapterId) {
        return configExtractor
                .getAdapterByAdapterId(adapterId)
                .flatMap(entity -> factoryRegistry.findByProtocolId(entity.getProtocolId()));
    }

    private @Nullable Object nodeSchemaProjection(final @NotNull ProtocolAdapterEntity entity) {
        return factoryRegistry
                .findByProtocolId(entity.getProtocolId())
                .map(factory -> (Object) JSCHEMA.toJsonSchema(factory.nodeDefinitionSchema()))
                .orElse(null);
    }
}
