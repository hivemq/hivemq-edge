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
package com.hivemq.protocols.v2.manager;

import com.codahale.metrics.MetricRegistry;
import com.hivemq.adapter.sdk.api.v2.factories.ProtocolAdapterFactory;
import com.hivemq.adapter.sdk.api.v2.messaging.MailboxSender;
import com.hivemq.adapter.sdk.api.v2.node.NodeTagPair;
import com.hivemq.protocols.v2.config.ProtocolAdapterEntity;
import com.hivemq.protocols.v2.manager.ProtocolAdapterHandleRegistry.ProtocolAdapterHandle;
import com.hivemq.protocols.v2.runtime.ProtocolAdapterMetrics;
import com.hivemq.protocols.v2.view.AdapterStatusSnapshot;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperEventListener;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperMessage;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperState;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A {@link ProtocolAdapterWrapperFactory} double that builds a {@link ProtocolAdapterContainer} whose wrapper sender simply
 * records the commands told to it and whose published snapshot is settable by the test. It lets the manager tests
 * drive supervision, routing, and the gentlest-transition logic without the full actor stack — the real wrapper
 * factory has its own test.
 * <p>
 * A freshly-created adapter starts with a {@code STOPPED} snapshot (as a real wrapper does); a test moves it to
 * {@code CONNECTED} or {@code ERROR} with {@link #setMachineState} to exercise the stop-and-discard paths, and
 * fires the captured {@link #healthListener()} to simulate wrapper health events.
 */
final class RecordingWrapperFactory implements ProtocolAdapterWrapperFactory {

    private final @NotNull List<String> createdAdapterIds = new ArrayList<>();
    private final @NotNull Map<String, Recorded> recordedByAdapterId = new LinkedHashMap<>();
    private final @NotNull List<String> translateNodesAdapterIds = new ArrayList<>();
    private final @NotNull List<String> closedAdapterIds = new ArrayList<>();
    private final @NotNull List<String> validatedAdapterIds = new ArrayList<>();
    private @NotNull Predicate<ProtocolAdapterEntity> schemaInvalid = entity -> false;
    private @Nullable ProtocolAdapterWrapperEventListener healthListener;

    @Override
    public @NotNull ProtocolAdapterContainer create(
            final @NotNull ProtocolAdapterEntity entity,
            final @NotNull ProtocolAdapterFactory factory,
            final @NotNull ProtocolAdapterWrapperEventListener healthListener) {
        this.healthListener = healthListener;
        final String adapterId = entity.getAdapterId();
        createdAdapterIds.add(adapterId);

        final List<ProtocolAdapterWrapperMessage> commands = new ArrayList<>();
        final AtomicReference<AdapterStatusSnapshot> snapshot =
                new AtomicReference<>(snapshotOf(entity, ProtocolAdapterWrapperState.STOPPED));
        recordedByAdapterId.put(adapterId, new Recorded(commands, snapshot, entity));

        final MailboxSender<ProtocolAdapterWrapperMessage> sender = commands::add;
        final ProtocolAdapterHandle handle = new ProtocolAdapterHandle(adapterId, sender, snapshot);
        final ProtocolAdapterMetrics metrics = new ProtocolAdapterMetrics(new MetricRegistry(), adapterId, () -> 0);
        // Record container teardown so a test can assert the manager closes a wrapper's resources on shutdown. The
        // recording double owns no protocol-adapter dispatch thread, so the adapter dispatcher binding is null.
        return new ProtocolAdapterContainer(
                handle, () -> closedAdapterIds.add(adapterId), null, () -> {}, metrics, entity);
    }

    @Override
    public void validateConfiguration(
            final @NotNull ProtocolAdapterEntity entity, final @NotNull ProtocolAdapterFactory factory) {
        validatedAdapterIds.add(entity.getAdapterId());
        if (schemaInvalid.test(entity)) {
            throw new ProtocolAdapterConfigException(
                    "adapter [" + entity.getAdapterId() + "] configuration does not match its type's schema");
        }
    }

    @Override
    public @NotNull List<NodeTagPair> translateNodes(
            final @NotNull ProtocolAdapterEntity entity, final @NotNull ProtocolAdapterFactory factory) {
        translateNodesAdapterIds.add(entity.getAdapterId());
        return List.of();
    }

    // ── test helpers ────────────────────────────────────────────────────────────────────────────────────────────

    @NotNull
    List<String> createdAdapterIds() {
        return createdAdapterIds;
    }

    @NotNull
    List<String> translateNodesAdapterIds() {
        return translateNodesAdapterIds;
    }

    @NotNull
    List<String> closedAdapterIds() {
        return closedAdapterIds;
    }

    @NotNull
    List<String> validatedAdapterIds() {
        return validatedAdapterIds;
    }

    /**
     * Make {@link #validateConfiguration} reject (throw) for any entity the predicate matches, simulating a
     * configuration that fails its type's schema. Set before the reload the test exercises.
     *
     * @param schemaInvalid the predicate selecting the configurations to reject.
     */
    void rejectSchemaWhen(final @NotNull Predicate<ProtocolAdapterEntity> schemaInvalid) {
        this.schemaInvalid = schemaInvalid;
    }

    @NotNull
    ProtocolAdapterWrapperEventListener healthListener() {
        if (healthListener == null) {
            throw new IllegalStateException("no wrapper has been created yet");
        }
        return healthListener;
    }

    @NotNull
    List<ProtocolAdapterWrapperMessage> commands(final @NotNull String adapterId) {
        final Recorded recorded = recordedByAdapterId.get(adapterId);
        return recorded == null ? List.of() : recorded.commands();
    }

    void setMachineState(final @NotNull String adapterId, final @NotNull ProtocolAdapterWrapperState state) {
        final Recorded recorded = recordedByAdapterId.get(adapterId);
        if (recorded != null) {
            recorded.snapshot().set(snapshotOf(recorded.entity(), state));
        }
    }

    private static @NotNull AdapterStatusSnapshot snapshotOf(
            final @NotNull ProtocolAdapterEntity entity, final @NotNull ProtocolAdapterWrapperState state) {
        return new AdapterStatusSnapshot(
                entity.getAdapterId(),
                state,
                entity.isNorthboundActivated(),
                entity.isSouthboundActivated(),
                List.of(),
                0L,
                null);
    }

    private record Recorded(
            @NotNull List<ProtocolAdapterWrapperMessage> commands,
            @NotNull AtomicReference<AdapterStatusSnapshot> snapshot,
            @NotNull ProtocolAdapterEntity entity) {}
}
