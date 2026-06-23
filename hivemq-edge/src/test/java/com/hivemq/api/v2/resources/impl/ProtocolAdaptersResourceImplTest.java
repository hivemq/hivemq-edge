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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hivemq.adapter.sdk.api.ProtocolAdapterCategory;
import com.hivemq.adapter.sdk.api.discovery.NodeType;
import com.hivemq.adapter.sdk.api.schema.ScalarSchema;
import com.hivemq.adapter.sdk.api.schema.ScalarType;
import com.hivemq.adapter.sdk.api.schema.Schema;
import com.hivemq.adapter.sdk.api.v2.ProtocolAdapterCapability;
import com.hivemq.adapter.sdk.api.v2.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.v2.factories.ProtocolAdapterFactory;
import com.hivemq.adapter.sdk.api.v2.messaging.MailboxSender;
import com.hivemq.adapter.sdk.api.v2.model.BrowseResultEntry;
import com.hivemq.adapter.sdk.api.v2.node.AccessTriState;
import com.hivemq.adapter.sdk.api.v2.node.Node;
import com.hivemq.adapter.sdk.api.v2.node.NodeProperty;
import com.hivemq.edge.api.v2.ProtocolAdaptersApi;
import com.hivemq.edge.api.v2.model.AdapterActivation;
import com.hivemq.edge.api.v2.model.AdapterActivationInvalidError;
import com.hivemq.edge.api.v2.model.AdapterDirection;
import com.hivemq.edge.api.v2.model.AdapterNotConnectedError;
import com.hivemq.edge.api.v2.model.AdapterNotFoundError;
import com.hivemq.edge.api.v2.model.AdapterStatus;
import com.hivemq.edge.api.v2.model.AdapterStatusColor;
import com.hivemq.edge.api.v2.model.AdapterType;
import com.hivemq.edge.api.v2.model.BrowseCommand;
import com.hivemq.edge.api.v2.model.BrowseResult;
import com.hivemq.edge.api.v2.model.BrowseTimeoutError;
import com.hivemq.edge.api.v2.model.Mapping;
import com.hivemq.edge.api.v2.model.MappingStatus;
import com.hivemq.edge.api.v2.model.NodeTagPair;
import com.hivemq.edge.api.v2.model.TagNotFoundError;
import com.hivemq.edge.api.v2.model.TagRetryRequest;
import com.hivemq.edge.api.v2.model.TagRetryResult;
import com.hivemq.edge.api.v2.model.TagStatus;
import com.hivemq.edge.api.v2.model.TagStatusDetail;
import com.hivemq.protocols.v2.config.AccessFlagsEntity;
import com.hivemq.protocols.v2.config.NorthboundMappingEntity;
import com.hivemq.protocols.v2.config.ProtocolAdapterEntity;
import com.hivemq.protocols.v2.config.ProtocolAdapterExtractor;
import com.hivemq.protocols.v2.config.RetryPolicyEntity;
import com.hivemq.protocols.v2.config.SouthboundMappingEntity;
import com.hivemq.protocols.v2.config.TagEntity;
import com.hivemq.protocols.v2.manager.ProtocolAdapterFactoryRegistry;
import com.hivemq.protocols.v2.manager.ProtocolAdapterHandleRegistry;
import com.hivemq.protocols.v2.manager.ProtocolAdapterHandleRegistry.ProtocolAdapterHandle;
import com.hivemq.protocols.v2.manager.ProtocolAdapterManagerMessage;
import com.hivemq.protocols.v2.manager.ProtocolAdapterManagerMessage.ActivateAdapter;
import com.hivemq.protocols.v2.manager.ProtocolAdapterManagerMessage.BrowseRequested;
import com.hivemq.protocols.v2.manager.ProtocolAdapterManagerMessage.DeactivateAdapter;
import com.hivemq.protocols.v2.manager.ProtocolAdapterManagerMessage.RetryTag;
import com.hivemq.protocols.v2.view.AdapterStatusSnapshot;
import com.hivemq.protocols.v2.view.TagStatusSnapshot;
import com.hivemq.protocols.v2.wrapper.BrowseRejectedException;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterDirection;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperMessage;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperState;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.core.Response;
import java.lang.reflect.Method;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

/**
 * The Nevsky (v2) protocol-adapters resource (design §11): every endpoint reads from the snapshot/config and tells
 * the manager correctly, the status DTOs are pure folds of the snapshot, the schema endpoints serve the reused
 * Schema projection (S22), tag retry reports accepted/skipped (S29 REST half), mappings derive their status, the
 * browse bridge maps the rejection/timeout reasons to HTTP status codes (S31 REST half), and the v2 surface has no
 * configuration-writing endpoint.
 */
class ProtocolAdaptersResourceImplTest {

    private static final MailboxSender<ProtocolAdapterWrapperMessage> NO_OP_WRAPPER_SENDER = message -> {};

    private final RecordingManager manager = new RecordingManager();
    private final ProtocolAdapterHandleRegistry registry = new ProtocolAdapterHandleRegistry();
    private final ProtocolAdapterExtractor configExtractor = mock(ProtocolAdapterExtractor.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    private @NotNull ProtocolAdaptersResourceImpl resource(final @NotNull ProtocolAdapterFactoryRegistry factories) {
        return new ProtocolAdaptersResourceImpl(manager, registry, factories, configExtractor, objectMapper, 50L);
    }

    private @NotNull ProtocolAdaptersResourceImpl resource() {
        return resource(new ProtocolAdapterFactoryRegistry(Set.of()));
    }

    // ── types ───────────────────────────────────────────────────────────────────────────────────────────────────

    @Test
    void getTypes_emptyRegistry_returnsEmptyList() {
        final Response response = resource().getAdapterTypes();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat((List<?>) response.getEntity()).isEmpty();
    }

    @Test
    void getTypes_projectsTheFactory() {
        final ProtocolAdapterFactoryRegistry factories = new ProtocolAdapterFactoryRegistry(Set.of(
                mockFactory("chaos", EnumSet.of(ProtocolAdapterCapability.BROWSE, ProtocolAdapterCapability.WRITE))));

        final Response response = resource(factories).getAdapterTypes();

        assertThat(response.getStatus()).isEqualTo(200);
        final List<?> types = (List<?>) response.getEntity();
        assertThat(types).hasSize(1);
        final AdapterType type = (AdapterType) types.get(0);
        assertThat(type.getId()).isEqualTo("chaos");
        assertThat(type.getCapabilities())
                .contains(AdapterType.CapabilitiesEnum.BROWSE, AdapterType.CapabilitiesEnum.WRITE);
        assertThat(type.getNodeSchema()).isNotNull();
        assertThat(type.getAdapterSchema()).isNotNull();
    }

    // ── status views ────────────────────────────────────────────────────────────────────────────────────────────

    @Test
    void listAdapters_foldsEachSnapshotToItsColor() {
        register("a", snapshot("a", ProtocolAdapterWrapperState.CONNECTED, List.of(), true, false, null));
        register("b", snapshot("b", ProtocolAdapterWrapperState.ERROR, List.of(), false, false, "boom"));

        final Response response = resource().listAdapters();

        assertThat(response.getStatus()).isEqualTo(200);
        final List<?> list = (List<?>) response.getEntity();
        assertThat(list).hasSize(2);
        assertThat(colorOf(list, "a")).isEqualTo(AdapterStatusColor.GREEN_CONNECTED);
        assertThat(colorOf(list, "b")).isEqualTo(AdapterStatusColor.RED_ERROR);
    }

    @Test
    void getStatus_found_andNotFound() {
        register("a", snapshot("a", ProtocolAdapterWrapperState.CONNECTED, List.of(), true, false, null));

        final Response found = resource().getAdapterStatus("a");
        assertThat(found.getStatus()).isEqualTo(200);
        assertThat(((AdapterStatus) found.getEntity()).getColor()).isEqualTo(AdapterStatusColor.GREEN_CONNECTED);

        assertThat(resource().getAdapterStatus("missing").getStatus()).isEqualTo(404);
    }

    @Test
    void getDefinition_found_andNotFound() {
        when(configExtractor.getAdapterByAdapterId("a")).thenReturn(Optional.of(entity("a", "chaos")));

        final Response found = resource().getAdapter("a");
        assertThat(found.getStatus()).isEqualTo(200);

        assertThat(resource().getAdapter("missing").getStatus()).isEqualTo(404);
    }

    // ── tags ────────────────────────────────────────────────────────────────────────────────────────────────────

    @Test
    void getTags_combinesConfigAndSnapshot() {
        when(configExtractor.getAdapterByAdapterId("a"))
                .thenReturn(Optional.of(entity("a", "chaos", List.of(tagEntity("temperature")), List.of(), List.of())));
        register(
                "a",
                snapshot(
                        "a",
                        ProtocolAdapterWrapperState.CONNECTED,
                        List.of(tagSnapshot("temperature", true, true, false, false, false, false)),
                        true,
                        false,
                        null));

        final Response response = resource().getAdapterTags("a");

        assertThat(response.getStatus()).isEqualTo(200);
        final List<?> tags = (List<?>) response.getEntity();
        assertThat(tags).hasSize(1);
        final NodeTagPair tag = (NodeTagPair) tags.get(0);
        assertThat(tag.getTagName()).isEqualTo("temperature");
        assertThat(tag.getNodeString()).isNotBlank();
        assertThat(tag.getStatus()).isEqualTo(TagStatus.NORTHBOUND_ONLY);

        assertThat(resource().getAdapterTags("missing").getStatus()).isEqualTo(404);
    }

    @Test
    void getTagStatus_found_andNotFoundPaths() {
        register(
                "a",
                snapshot(
                        "a",
                        ProtocolAdapterWrapperState.CONNECTED,
                        List.of(tagSnapshot("temperature", true, true, false, false, false, false)),
                        true,
                        false,
                        null));

        final Response found = resource().getTagStatus("a", "temperature");
        assertThat(found.getStatus()).isEqualTo(200);
        assertThat(((TagStatusDetail) found.getEntity()).getTagName()).isEqualTo("temperature");

        assertThat(resource().getTagStatus("missing", "temperature").getStatus())
                .isEqualTo(404);
        assertThat(resource().getTagStatus("a", "ghost").getStatus()).isEqualTo(404);
    }

    // ── activation (live-goal command routing) ──────────────────────────────────────────────────────────────────

    @Test
    void setActivation_routesActivateAndDeactivate() {
        register("a", snapshot("a", ProtocolAdapterWrapperState.CONNECTED, List.of(), true, false, null));

        assertThat(resource()
                        .setAdapterActivation(
                                "a",
                                new AdapterActivation()
                                        .direction(AdapterDirection.NORTHBOUND)
                                        .activated(true))
                        .getStatus())
                .isEqualTo(200);
        assertThat(manager.messages).anySatisfy(message -> assertThat(message)
                .isEqualTo(new ActivateAdapter("a", ProtocolAdapterDirection.NORTHBOUND)));

        resource()
                .setAdapterActivation(
                        "a",
                        new AdapterActivation()
                                .direction(AdapterDirection.SOUTHBOUND)
                                .activated(false));
        assertThat(manager.messages).anySatisfy(message -> assertThat(message)
                .isEqualTo(new DeactivateAdapter("a", ProtocolAdapterDirection.SOUTHBOUND)));
    }

    @Test
    void setActivation_unknownAdapter_is404_andMissingFieldsIs400() {
        assertThat(resource()
                        .setAdapterActivation(
                                "missing",
                                new AdapterActivation()
                                        .direction(AdapterDirection.BOTH)
                                        .activated(true))
                        .getStatus())
                .isEqualTo(404);

        register("a", snapshot("a", ProtocolAdapterWrapperState.CONNECTED, List.of(), true, false, null));
        assertThat(resource().setAdapterActivation("a", new AdapterActivation()).getStatus())
                .isEqualTo(400);
    }

    // ── tag retry (accepted / skipped) ──────────────────────────────────────────────────────────────────────────

    @Test
    void retryTag_permanentlyFailed_isAcceptedAndForwarded() {
        register(
                "a",
                snapshot(
                        "a",
                        ProtocolAdapterWrapperState.CONNECTED,
                        List.of(tagSnapshot("bad", true, false, true, false, false, false)),
                        true,
                        false,
                        null));

        final Response retry = resource().retryTag("a", "bad");
        assertThat(retry.getStatus()).isEqualTo(200);
        final TagRetryResult result = (TagRetryResult) retry.getEntity();
        assertThat(result.getAccepted()).containsExactly("bad");
        assertThat(result.getSkipped()).isEmpty();
        assertThat(manager.messages).contains(new RetryTag("a", "bad"));
    }

    @Test
    void retryTag_notInError_isSkippedWithoutForwarding() {
        register(
                "a",
                snapshot(
                        "a",
                        ProtocolAdapterWrapperState.CONNECTED,
                        List.of(tagSnapshot("good", true, true, false, false, false, false)),
                        true,
                        false,
                        null));

        final Response retry = resource().retryTag("a", "good");
        assertThat(retry.getStatus()).isEqualTo(200);
        final TagRetryResult result = (TagRetryResult) retry.getEntity();
        assertThat(result.getAccepted()).isEmpty();
        assertThat(result.getSkipped()).hasSize(1);
        assertThat(result.getSkipped().get(0).getReason()).isEqualTo("tag is not in an error state");
        assertThat(manager.messages).noneMatch(message -> message instanceof RetryTag);
    }

    @Test
    void retryTag_unknownAdapterOrTag_is404() {
        register("a", snapshot("a", ProtocolAdapterWrapperState.CONNECTED, List.of(), true, false, null));
        assertThat(resource().retryTag("missing", "bad").getStatus()).isEqualTo(404);
        assertThat(resource().retryTag("a", "ghost").getStatus()).isEqualTo(404);
    }

    @Test
    void bulkRetry_acceptsFailedTagsAndSkipsTheRest() {
        register(
                "a",
                snapshot(
                        "a",
                        ProtocolAdapterWrapperState.CONNECTED,
                        List.of(
                                tagSnapshot("bad", true, false, true, false, false, false),
                                tagSnapshot("good", true, true, false, false, false, false)),
                        true,
                        false,
                        null));

        final Response response =
                resource().retryTags("a", new TagRetryRequest().tagNames(List.of("bad", "good", "ghost")));

        assertThat(response.getStatus()).isEqualTo(200);
        final TagRetryResult result = (TagRetryResult) response.getEntity();
        assertThat(result.getAccepted()).containsExactly("bad");
        assertThat(result.getSkipped()).hasSize(2);
        assertThat(result.getSkipped())
                .anySatisfy(skipped -> assertThat(skipped.getTagName()).isEqualTo("ghost"));
    }

    @Test
    void bulkRetry_withoutNames_retriesEveryPermanentlyFailedTag() {
        register(
                "a",
                snapshot(
                        "a",
                        ProtocolAdapterWrapperState.CONNECTED,
                        List.of(
                                tagSnapshot("bad", true, false, true, false, false, false),
                                tagSnapshot("good", true, true, false, false, false, false)),
                        true,
                        false,
                        null));

        final Response response = resource().retryTags("a", new TagRetryRequest());

        final TagRetryResult result = (TagRetryResult) response.getEntity();
        assertThat(result.getAccepted()).containsExactly("bad");
    }

    // ── mappings (derived status) ───────────────────────────────────────────────────────────────────────────────

    @Test
    void northboundMapping_active_andSouthboundMapping_deactivatedByTag() {
        when(configExtractor.getAdapterByAdapterId("a"))
                .thenReturn(Optional.of(entity(
                        "a",
                        "chaos",
                        List.of(tagEntity("temperature")),
                        List.of(new NorthboundMappingEntity("temperature", "plant/temperature")),
                        List.of(new SouthboundMappingEntity("plant/setpoint", "temperature")))));
        register(
                "a",
                snapshot(
                        "a",
                        ProtocolAdapterWrapperState.CONNECTED,
                        List.of(tagSnapshot("temperature", true, true, false, false, false, false)),
                        true,
                        false,
                        null));

        final List<?> northbound =
                (List<?>) resource().getNorthboundMappings("a").getEntity();
        assertThat(northbound).hasSize(1);
        assertThat(((Mapping) northbound.get(0)).getStatus()).isEqualTo(MappingStatus.ACTIVE);
        assertThat(((Mapping) northbound.get(0)).getBlockedBy()).isNull();

        final List<?> southbound =
                (List<?>) resource().getSouthboundMappings("a").getEntity();
        assertThat(southbound).hasSize(1);
        assertThat(((Mapping) southbound.get(0)).getStatus()).isEqualTo(MappingStatus.DEACTIVATED_BY_TAG);
        assertThat(((Mapping) southbound.get(0)).getBlockedBy()).isNotNull();

        assertThat(resource().getNorthboundMappings("missing").getStatus()).isEqualTo(404);
    }

    // ── schema projections (S22) ────────────────────────────────────────────────────────────────────────────────

    @Test
    void nodeAndAdapterSchema_serveTheReusedSchemaProjection() {
        final ProtocolAdapterFactoryRegistry factories = new ProtocolAdapterFactoryRegistry(
                Set.of(mockFactory("chaos", EnumSet.noneOf(ProtocolAdapterCapability.class))));
        when(configExtractor.getAdapterByAdapterId("a")).thenReturn(Optional.of(entity("a", "chaos")));

        final Response nodeSchema = resource(factories).getNodeSchema("a");
        assertThat(nodeSchema.getStatus()).isEqualTo(200);
        assertThat(((ObjectNode) nodeSchema.getEntity()).get("type").asText()).isEqualTo("integer");

        final Response adapterSchema = resource(factories).getAdapterSchema("a");
        assertThat(adapterSchema.getStatus()).isEqualTo(200);
        assertThat(((ObjectNode) adapterSchema.getEntity()).get("type").asText())
                .isEqualTo("integer");

        assertThat(resource(factories).getNodeSchema("missing").getStatus()).isEqualTo(404);
    }

    // ── browse bridge (S31 REST half) ───────────────────────────────────────────────────────────────────────────

    @Test
    void browse_withoutCapability_is400() {
        final ProtocolAdapterFactoryRegistry factories = new ProtocolAdapterFactoryRegistry(
                Set.of(mockFactory("chaos", EnumSet.noneOf(ProtocolAdapterCapability.class))));
        when(configExtractor.getAdapterByAdapterId("a")).thenReturn(Optional.of(entity("a", "chaos")));
        register("a", snapshot("a", ProtocolAdapterWrapperState.CONNECTED, List.of(), true, false, null));

        assertThat(resource(factories).browseAdapter("a", new BrowseCommand()).getStatus())
                .isEqualTo(400);
    }

    @Test
    void browse_notConnected_is409() {
        final ProtocolAdapterFactoryRegistry factories = browseCapableFactories();
        when(configExtractor.getAdapterByAdapterId("a")).thenReturn(Optional.of(entity("a", "chaos")));
        register("a", snapshot("a", ProtocolAdapterWrapperState.STOPPED, List.of(), false, false, null));
        manager.browseHandler = request -> request.completion()
                .completeExceptionally(
                        new BrowseRejectedException(BrowseRejectedException.Reason.NOT_CONNECTED, "not connected"));

        assertThat(resource(factories).browseAdapter("a", new BrowseCommand()).getStatus())
                .isEqualTo(409);
    }

    @Test
    void browse_success_is200WithEntries() {
        final ProtocolAdapterFactoryRegistry factories = browseCapableFactories();
        when(configExtractor.getAdapterByAdapterId("a")).thenReturn(Optional.of(entity("a", "chaos")));
        register("a", snapshot("a", ProtocolAdapterWrapperState.CONNECTED, List.of(), true, false, null));
        manager.browseHandler = request ->
                request.completion().complete(List.of(new BrowseResultEntry(new TestNode(), NodeType.VALUE, true)));

        final Response response = resource(factories).browseAdapter("a", new BrowseCommand());
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(((BrowseResult) response.getEntity()).getEntries()).hasSize(1);
    }

    @Test
    void browse_thatNeverCompletes_is504() {
        final ProtocolAdapterFactoryRegistry factories = browseCapableFactories();
        when(configExtractor.getAdapterByAdapterId("a")).thenReturn(Optional.of(entity("a", "chaos")));
        register("a", snapshot("a", ProtocolAdapterWrapperState.CONNECTED, List.of(), true, false, null));
        // no browseHandler — the future never completes, so the request times out

        assertThat(resource(factories).browseAdapter("a", new BrowseCommand()).getStatus())
                .isEqualTo(504);
    }

    @Test
    void browse_unknownAdapter_is404() {
        assertThat(resource().browseAdapter("missing", new BrowseCommand()).getStatus())
                .isEqualTo(404);
    }

    // ── error bodies are typed v2 problem details (design §11, DataHub-style) ────────────────────────────────────

    @Test
    void adapterNotFound_bodyIsTypedProblemDetail() {
        final Response response = resource().getAdapterStatus("missing");

        assertThat(response.getStatus()).isEqualTo(404);
        assertThat(response.getMediaType().toString()).startsWith("application/problem+json");
        final AdapterNotFoundError error = (AdapterNotFoundError) response.getEntity();
        assertThat(error.getType()).hasToString("https://hivemq.com/edge/api/v2/model/AdapterNotFoundError");
        assertThat(error.getStatus()).isEqualTo(404);
        assertThat(error.getTitle()).isEqualTo("Adapter Not Found");
        assertThat(error.getAdapterId()).isEqualTo("missing");
        assertThat(error.getDetail()).contains("missing");
    }

    @Test
    void tagNotFound_bodyCarriesAdapterAndTag() {
        register("a", snapshot("a", ProtocolAdapterWrapperState.CONNECTED, List.of(), true, false, null));

        final TagNotFoundError error =
                (TagNotFoundError) resource().getTagStatus("a", "ghost").getEntity();
        assertThat(error.getType()).hasToString("https://hivemq.com/edge/api/v2/model/TagNotFoundError");
        assertThat(error.getAdapterId()).isEqualTo("a");
        assertThat(error.getTagName()).isEqualTo("ghost");
    }

    @Test
    void activationInvalid_bodyIsTyped() {
        register("a", snapshot("a", ProtocolAdapterWrapperState.CONNECTED, List.of(), true, false, null));

        final AdapterActivationInvalidError error = (AdapterActivationInvalidError)
                resource().setAdapterActivation("a", new AdapterActivation()).getEntity();
        assertThat(error.getType()).hasToString("https://hivemq.com/edge/api/v2/model/AdapterActivationInvalidError");
        assertThat(error.getStatus()).isEqualTo(400);
    }

    @Test
    void browseRejection_mapsToTypedProblemDetail() {
        final ProtocolAdapterFactoryRegistry factories = browseCapableFactories();
        when(configExtractor.getAdapterByAdapterId("a")).thenReturn(Optional.of(entity("a", "chaos")));
        register("a", snapshot("a", ProtocolAdapterWrapperState.STOPPED, List.of(), false, false, null));
        manager.browseHandler = request -> request.completion()
                .completeExceptionally(
                        new BrowseRejectedException(BrowseRejectedException.Reason.NOT_CONNECTED, "down"));

        final Response response = resource(factories).browseAdapter("a", new BrowseCommand());
        assertThat(response.getStatus()).isEqualTo(409);
        final AdapterNotConnectedError error = (AdapterNotConnectedError) response.getEntity();
        assertThat(error.getType()).hasToString("https://hivemq.com/edge/api/v2/model/AdapterNotConnectedError");
        assertThat(error.getAdapterId()).isEqualTo("a");
    }

    @Test
    void browseTimeout_bodyIsTyped() {
        final ProtocolAdapterFactoryRegistry factories = browseCapableFactories();
        when(configExtractor.getAdapterByAdapterId("a")).thenReturn(Optional.of(entity("a", "chaos")));
        register("a", snapshot("a", ProtocolAdapterWrapperState.CONNECTED, List.of(), true, false, null));
        // no browseHandler — the future never completes, so the request times out

        final Response response = resource(factories).browseAdapter("a", new BrowseCommand());
        assertThat(response.getStatus()).isEqualTo(504);
        assertThat(((BrowseTimeoutError) response.getEntity()).getType())
                .hasToString("https://hivemq.com/edge/api/v2/model/BrowseTimeoutError");
    }

    // ── the v2 surface writes no configuration ──────────────────────────────────────────────────────────────────

    @Test
    void theV2SurfaceHasNoConfigurationWritingEndpoint() {
        final Set<String> allowedPostMethods = Set.of("setAdapterActivation", "retryTag", "retryTags", "browseAdapter");
        for (final Method method : ProtocolAdaptersApi.class.getMethods()) {
            assertThat(method.isAnnotationPresent(PUT.class))
                    .as("%s must not be a PUT", method.getName())
                    .isFalse();
            assertThat(method.isAnnotationPresent(DELETE.class))
                    .as("%s must not be a DELETE", method.getName())
                    .isFalse();
            if (method.isAnnotationPresent(POST.class)) {
                assertThat(method.getName())
                        .as("%s is a POST and must be a state-only command", method.getName())
                        .isIn(allowedPostMethods);
            }
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────────────────────────────────────────────

    private void register(final @NotNull String adapterId, final @NotNull AdapterStatusSnapshot snapshot) {
        registry.register(new ProtocolAdapterHandle(
                adapterId, NO_OP_WRAPPER_SENDER, new java.util.concurrent.atomic.AtomicReference<>(snapshot)));
    }

    private static @NotNull AdapterStatusColor colorOf(final @NotNull List<?> statuses, final @NotNull String id) {
        return statuses.stream()
                .map(AdapterStatus.class::cast)
                .filter(status -> id.equals(status.getId()))
                .findFirst()
                .orElseThrow()
                .getColor();
    }

    private static @NotNull AdapterStatusSnapshot snapshot(
            final @NotNull String adapterId,
            final @NotNull ProtocolAdapterWrapperState state,
            final @NotNull List<TagStatusSnapshot> tags,
            final boolean northbound,
            final boolean southbound,
            final @Nullable String error) {
        return new AdapterStatusSnapshot(adapterId, state, northbound, southbound, tags, 1000L, error);
    }

    private static @NotNull TagStatusSnapshot tagSnapshot(
            final @NotNull String tagName,
            final boolean readActive,
            final boolean readOperating,
            final boolean readPermanent,
            final boolean writeActive,
            final boolean writeOperating,
            final boolean writePermanent) {
        return new TagStatusSnapshot(
                tagName,
                true,
                true,
                true,
                false,
                "readState",
                "writeState",
                readActive,
                writeActive,
                readOperating,
                writeOperating,
                readPermanent,
                writePermanent,
                0,
                null,
                1000L);
    }

    private static @NotNull ProtocolAdapterEntity entity(
            final @NotNull String adapterId, final @NotNull String protocolId) {
        return entity(adapterId, protocolId, List.of(), List.of(), List.of());
    }

    private static @NotNull ProtocolAdapterEntity entity(
            final @NotNull String adapterId,
            final @NotNull String protocolId,
            final @NotNull List<TagEntity> tags,
            final @NotNull List<NorthboundMappingEntity> northbound,
            final @NotNull List<SouthboundMappingEntity> southbound) {
        return new ProtocolAdapterEntity(
                adapterId,
                protocolId,
                2,
                true,
                false,
                false,
                Map.of("host", "localhost"),
                new RetryPolicyEntity(),
                30_000,
                5_000,
                tags,
                northbound,
                southbound);
    }

    private static @NotNull TagEntity tagEntity(final @NotNull String name) {
        return new TagEntity(
                name,
                "{\"identifier\":\"" + name + "\"}",
                true,
                true,
                true,
                false,
                1_000,
                new AccessFlagsEntity(
                        AccessTriState.YES, AccessTriState.YES, AccessTriState.YES, AccessTriState.WILL_NOT_USE));
    }

    private @NotNull ProtocolAdapterFactoryRegistry browseCapableFactories() {
        return new ProtocolAdapterFactoryRegistry(
                Set.of(mockFactory("chaos", EnumSet.of(ProtocolAdapterCapability.BROWSE))));
    }

    private static @NotNull ProtocolAdapterFactory mockFactory(
            final @NotNull String protocolId, final @NotNull EnumSet<ProtocolAdapterCapability> capabilities) {
        final ProtocolAdapterFactory factory = mock(ProtocolAdapterFactory.class);
        final ProtocolAdapterInformation information = mock(ProtocolAdapterInformation.class);
        when(information.protocolId()).thenReturn(protocolId);
        when(information.displayName()).thenReturn("Chaos");
        when(information.description()).thenReturn("A scriptable test adapter type");
        when(information.version()).thenReturn("1.0.0");
        when(information.logoUrl()).thenReturn("logo.svg");
        when(information.author()).thenReturn("HiveMQ");
        when(information.category()).thenReturn(ProtocolAdapterCategory.SIMULATION);
        when(information.tags()).thenReturn(List.of());
        when(information.capabilities()).thenReturn(capabilities);
        doReturn(TestNode.class).when(information).nodeClass();
        when(information.currentConfigVersion()).thenReturn(2);
        when(factory.information()).thenReturn(information);
        final Schema schema = new ScalarSchema(ScalarType.LONG, null, null, null, null, false, true, false);
        when(factory.nodeDefinitionSchema()).thenReturn(schema);
        when(factory.adapterConfigSchema()).thenReturn(schema);
        return factory;
    }

    /**
     * A recording {@link MailboxSender} double for the manager mailbox: it captures every command and, for a
     * {@link BrowseRequested}, applies an optional handler so a test can complete or fail the browse future.
     */
    private static final class RecordingManager implements MailboxSender<ProtocolAdapterManagerMessage> {

        private final List<ProtocolAdapterManagerMessage> messages = new java.util.ArrayList<>();
        private @Nullable Consumer<BrowseRequested> browseHandler;

        @Override
        public void tell(final @NotNull ProtocolAdapterManagerMessage message) {
            messages.add(message);
            if (message instanceof final BrowseRequested browse && browseHandler != null) {
                browseHandler.accept(browse);
            }
        }
    }

    /**
     * A Jackson-friendly {@link Node} for the browse filter and browse results.
     */
    public static final class TestNode extends Node {

        public @NotNull String identifier = "";

        @Override
        public @NotNull String nodeId() {
            return identifier;
        }

        @Override
        public @NotNull String nodeString() {
            return "{\"identifier\":\"" + identifier + "\"}";
        }

        @Override
        public @NotNull EnumSet<NodeProperty> properties() {
            return EnumSet.of(NodeProperty.UNIQUE);
        }
    }
}
