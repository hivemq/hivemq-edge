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

import com.hivemq.adapter.sdk.api.v2.factories.ProtocolAdapterFactory;
import com.hivemq.adapter.sdk.api.v2.messaging.MailboxSender;
import com.hivemq.adapter.sdk.api.v2.messaging.MessageHandler;
import com.hivemq.adapter.sdk.api.v2.node.NodeTagPair;
import com.hivemq.protocols.v2.config.ProtocolAdapterEntity;
import com.hivemq.protocols.v2.manager.ProtocolAdapterHandleRegistry.ProtocolAdapterHandle;
import com.hivemq.protocols.v2.manager.ProtocolAdapterManagerMessage.ActivateAdapter;
import com.hivemq.protocols.v2.manager.ProtocolAdapterManagerMessage.BrowseRequested;
import com.hivemq.protocols.v2.manager.ProtocolAdapterManagerMessage.ConfigurationChanged;
import com.hivemq.protocols.v2.manager.ProtocolAdapterManagerMessage.DeactivateAdapter;
import com.hivemq.protocols.v2.manager.ProtocolAdapterManagerMessage.ProtocolAdapterManagerTick;
import com.hivemq.protocols.v2.manager.ProtocolAdapterManagerMessage.RetryTag;
import com.hivemq.protocols.v2.manager.ProtocolAdapterManagerMessage.ShutdownRequested;
import com.hivemq.protocols.v2.manager.ProtocolAdapterManagerMessage.WrapperError;
import com.hivemq.protocols.v2.manager.ProtocolAdapterManagerMessage.WrapperStarted;
import com.hivemq.protocols.v2.manager.ProtocolAdapterManagerMessage.WrapperStopped;
import com.hivemq.protocols.v2.runtime.Clock;
import com.hivemq.protocols.v2.view.AdapterStatusSnapshot;
import com.hivemq.protocols.v2.wrapper.BrowseRejectedException;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperBrowseRequest;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperCommand;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperMessage;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperState;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Protocol Adapter Manager — the supervisor {@link MessageHandler} of the v2 subsystem. It owns
 * the wrapper/adapter pairs, applies the gentlest correct transition on configuration change, routes the
 * runtime-state commands from REST, and reacts to wrapper health. It reads <b>only</b> the
 * {@code <v2>} section (through the {@link ConfigurationChanged} message the extractor's consumer
 * tells it) and it never writes configuration.
 * <p>
 * Like every v2 component it is an actor: {@link #receive(ProtocolAdapterManagerMessage)} runs on the manager's
 * single dispatch thread, so its maps need no locks. The only state that crosses the boundary outward is the
 * immutable {@link ProtocolAdapterManagerSnapshot} health summary it publishes on each tick, and each wrapper's own
 * snapshot in the {@link ProtocolAdapterHandleRegistry} — read by REST threads without locking (the actor model).
 * <p>
 * The manager is bound to its own mailbox through {@link #bindSelf(MailboxSender)} once, before any message is
 * handled (the two-phase init mirrors the wrapper context's {@code bindMachine}): the manager needs its own
 * send-only handle to give each wrapper a health-notification seam and to schedule its tick (a later wiring task).
 */
public final class ProtocolAdapterManager implements MessageHandler<ProtocolAdapterManagerMessage> {

    private static final @NotNull Logger log = LoggerFactory.getLogger(ProtocolAdapterManager.class);

    /**
     * The wrapper sender of an {@code ERROR} handle for an unknown / un-instantiable adapter: there is no wrapper,
     * so a {@code tell} is dropped. REST commands to such an adapter are therefore harmless no-ops.
     */
    private static final @NotNull MailboxSender<ProtocolAdapterWrapperMessage> NO_OP_WRAPPER_SENDER = message -> {};

    private final @NotNull ProtocolAdapterFactoryRegistry factoryRegistry;
    private final @NotNull ProtocolAdapterHandleRegistry handleRegistry;
    private final @NotNull ProtocolAdapterWrapperFactory wrapperFactory;
    private final @NotNull Clock clock;

    /** adapterId &rarr; the adapter the manager currently owns. Mutated only on the dispatch thread. */
    private final @NotNull Map<String, ProtocolAdapterContainer> containerMap = new LinkedHashMap<>();

    /** adapterId &rarr; an adapter told to stop, awaiting its {@code stopped()} before teardown / recreate. */
    private final @NotNull Map<String, PendingRemoval> pendingRemovalMap = new LinkedHashMap<>();

    private final @NotNull AtomicReference<ProtocolAdapterManagerSnapshot> healthSummary =
            new AtomicReference<>(ProtocolAdapterManagerSnapshot.empty());

    private @Nullable ProtocolAdapterManagerHealthListener healthListener;

    /** Set while a subsystem shutdown is draining; counted down once every managed adapter has wound down. */
    private @Nullable CountDownLatch shutdownLatch;

    /**
     * @param factoryRegistry the protocol-adapter type factories (empty in production until a v2 adapter module is
     *                        bundled).
     * @param handleRegistry  the REST-readable adapter registry the manager populates.
     * @param wrapperFactory the seam that builds and attaches a wrapper/adapter pair from one configuration.
     * @param clock          the clock used to stamp the {@code ERROR} snapshots and the health summary.
     */
    public ProtocolAdapterManager(
            final @NotNull ProtocolAdapterFactoryRegistry factoryRegistry,
            final @NotNull ProtocolAdapterHandleRegistry handleRegistry,
            final @NotNull ProtocolAdapterWrapperFactory wrapperFactory,
            final @NotNull Clock clock) {
        this.factoryRegistry = factoryRegistry;
        this.handleRegistry = handleRegistry;
        this.wrapperFactory = wrapperFactory;
        this.clock = clock;
    }

    /**
     * Close the construction cycle: the manager needs its own send-only handle to wire each wrapper's health
     * notifications back to itself. Called once, before the first message is handled.
     *
     * @param selfSender the manager's own mailbox sender.
     */
    public void bindSelf(final @NotNull MailboxSender<ProtocolAdapterManagerMessage> selfSender) {
        this.healthListener = new ProtocolAdapterManagerHealthListener(selfSender);
    }

    /**
     * @return the latest health summary the manager has published.
     */
    public @NotNull ProtocolAdapterManagerSnapshot healthSummary() {
        return Objects.requireNonNullElse(healthSummary.get(), ProtocolAdapterManagerSnapshot.empty());
    }

    @Override
    public void receive(final @NotNull ProtocolAdapterManagerMessage message) {
        switch (message) {
            case ConfigurationChanged configuration -> reconcile(configuration.adapters());
            case ActivateAdapter activate ->
                forwardCommand(
                        activate.adapterId(),
                        new ProtocolAdapterWrapperCommand.ActivateDirection(activate.direction()));
            case DeactivateAdapter deactivate ->
                forwardCommand(
                        deactivate.adapterId(),
                        new ProtocolAdapterWrapperCommand.DeactivateDirection(deactivate.direction()));
            case RetryTag retry ->
                forwardCommand(retry.adapterId(), new ProtocolAdapterWrapperCommand.RetryTag(retry.tagName()));
            case BrowseRequested browse -> handleBrowse(browse);
            case ShutdownRequested shutdown -> onShutdownRequested(shutdown.done());
            case WrapperStarted started -> onWrapperStarted(started.adapterId());
            case WrapperStopped stopped -> onWrapperStopped(stopped.adapterId());
            case WrapperError error -> onWrapperError(error.adapterId(), error.reason());
            case ProtocolAdapterManagerTick tick -> publishHealthSummary(tick.nowMillis());
        }
    }

    // ── configuration difference → gentlest transition ────────────────────────────────────────────

    private void reconcile(final @NotNull List<ProtocolAdapterEntity> newConfigs) {
        final Map<String, ProtocolAdapterEntity> updatedById = new LinkedHashMap<>();
        for (final ProtocolAdapterEntity entity : newConfigs) {
            updatedById.put(entity.getAdapterId(), entity);
        }

        // Removals first: adapters no longer in the configuration are stopped and discarded.
        final List<String> toRemove = new ArrayList<>();
        for (final String adapterId : containerMap.keySet()) {
            if (!updatedById.containsKey(adapterId)) {
                toRemove.add(adapterId);
            }
        }
        for (final String adapterId : toRemove) {
            stopAndDiscard(adapterId, null);
        }

        // Adds and updates.
        for (final ProtocolAdapterEntity entity : newConfigs) {
            final String adapterId = entity.getAdapterId();
            final ProtocolAdapterContainer existing = containerMap.get(adapterId);
            if (existing != null) {
                applyDifference(existing, entity);
            } else if (pendingRemovalMap.containsKey(adapterId)) {
                // The previous instance is still stopping; recreate with this configuration once it reports stopped.
                final PendingRemoval pending = pendingRemovalMap.get(adapterId);
                pendingRemovalMap.put(adapterId, new PendingRemoval(pending.stopping(), entity));
            } else {
                createAdapter(entity);
            }
        }
    }

    private void applyDifference(
            final @NotNull ProtocolAdapterContainer existing, final @NotNull ProtocolAdapterEntity updated) {
        final String adapterId = updated.getAdapterId();
        final ProtocolAdapterEntity running = existing.appliedEntity();

        if (!existing.isReal()) {
            // An unknown / un-instantiable adapter. If its protocol-id changed it may now resolve (or fail
            // differently), so re-evaluate by discarding and recreating; otherwise just record the new config.
            if (running.getProtocolId().equals(updated.getProtocolId())) {
                existing.appliedEntity(updated);
            } else {
                stopAndDiscard(adapterId, updated);
            }
            return;
        }

        switch (ProtocolAdapterConfigDiffUtils.classify(running, updated)) {
            case NO_CHANGE -> {
                // Nothing changed in the configuration; a REST live goal (if any) is deliberately preserved.
            }
            case ACTIVATION_ONLY -> {
                tellWrapper(existing, activationCommand(updated));
                existing.appliedEntity(updated);
            }
            case TAGS_ONLY -> applyTagsOnly(existing, running, updated);
            case FULL_RECREATE -> stopAndDiscard(adapterId, updated);
        }
    }

    private void applyTagsOnly(
            final @NotNull ProtocolAdapterContainer existing,
            final @NotNull ProtocolAdapterEntity running,
            final @NotNull ProtocolAdapterEntity updated) {
        final Optional<ProtocolAdapterFactory> factory = factoryRegistry.findByProtocolId(updated.getProtocolId());
        if (factory.isEmpty()) {
            // Connection-critical fields are equal for TAGS_ONLY, so the factory that built the running adapter
            // must still be present; fall back to a recreate if that invariant is ever broken.
            stopAndDiscard(updated.getAdapterId(), updated);
            return;
        }
        final List<NodeTagPair> nodes;
        try {
            nodes = wrapperFactory.translateNodes(updated, factory.get());
        } catch (final ProtocolAdapterConfigException exception) {
            log.error(
                    "Cannot apply the tag set of v2 adapter '{}' in place: {}",
                    updated.getAdapterId(),
                    exception.getMessage());
            stopAndDiscard(updated.getAdapterId(), updated);
            return;
        }
        tellWrapper(
                existing,
                new ProtocolAdapterWrapperCommand.UpdateTagSet(
                        nodes,
                        ProtocolAdapterConfigSupport.activationOf(updated),
                        ProtocolAdapterConfigSupport.pollIntervalMillisByTagName(updated),
                        updated.getReadUsedTagNames(),
                        updated.getWriteUsedTagNames()));
        if (ProtocolAdapterConfigDiffUtils.adapterDirectionChanged(running, updated)) {
            // The tag-set update does not carry the adapter direction goal; re-assert the config-declared goal when
            // it changed too. Still never reconnects.
            tellWrapper(existing, activationCommand(updated));
        }
        existing.appliedEntity(updated);
    }

    // ── instance lifecycle ────────────────────────────────────────────────────────────────────────

    private void createAdapter(final @NotNull ProtocolAdapterEntity entity) {
        final String adapterId = entity.getAdapterId();
        final Optional<ProtocolAdapterFactory> factory = factoryRegistry.findByProtocolId(entity.getProtocolId());
        if (factory.isEmpty()) {
            registerErrorAdapter(entity, "no registered adapter type for protocol-id [" + entity.getProtocolId() + "]");
            return;
        }
        final ProtocolAdapterContainer adapter;
        try {
            adapter = wrapperFactory.create(entity, factory.get(), requireHealthListener());
        } catch (final ProtocolAdapterConfigException exception) {
            final String reason = Objects.requireNonNullElse(exception.getMessage(), "invalid adapter configuration");
            log.error("Cannot create v2 adapter '{}': {}", adapterId, reason);
            registerErrorAdapter(entity, reason);
            return;
        }
        containerMap.put(adapterId, adapter);
        handleRegistry.register(adapter.handle());
        // Apply the config-declared activation to bring the freshly-created wrapper to its initial goal. The wrapper
        // starts in STOPPED; this is the command that steps it toward CONNECTED when a direction
        // is activated, and leaves it STOPPED when neither is.
        tellWrapper(adapter, activationCommand(entity));
    }

    private void registerErrorAdapter(final @NotNull ProtocolAdapterEntity entity, final @NotNull String reason) {
        final String adapterId = entity.getAdapterId();
        final AdapterStatusSnapshot errorSnapshot = new AdapterStatusSnapshot(
                adapterId,
                ProtocolAdapterWrapperState.ERROR,
                entity.isNorthboundActivated(),
                entity.isSouthboundActivated(),
                List.of(),
                clock.nowMillis(),
                reason);
        final ProtocolAdapterHandle handle =
                new ProtocolAdapterHandle(adapterId, NO_OP_WRAPPER_SENDER, new AtomicReference<>(errorSnapshot));
        containerMap.put(adapterId, ProtocolAdapterContainer.unknown(handle, entity));
        handleRegistry.register(handle);
        log.warn("v2 adapter '{}' is unavailable: {}", adapterId, reason);
    }

    /**
     * Begin discarding an adapter: stop the wrapper, then close its resources once it reports {@code stopped()}, and
     * optionally recreate it from a new configuration (a full recreate). An already-stopped or unknown adapter is
     * torn down immediately.
     *
     * @param adapterId  the adapter to discard.
     * @param recreateAs the configuration to recreate the adapter from once stopped, or {@code null} for a pure
     *                   removal.
     */
    private void stopAndDiscard(final @NotNull String adapterId, final @Nullable ProtocolAdapterEntity recreateAs) {
        final ProtocolAdapterContainer adapter = containerMap.remove(adapterId);
        if (adapter == null) {
            // Already stopping: fold the (possibly new) recreate target into the pending removal.
            final PendingRemoval pending = pendingRemovalMap.get(adapterId);
            if (pending != null) {
                pendingRemovalMap.put(adapterId, new PendingRemoval(pending.stopping(), recreateAs));
            } else if (recreateAs != null) {
                createAdapter(recreateAs);
            }
            return;
        }
        if (!adapter.isReal() || isStopped(adapter)) {
            // No wrapper to wind down, or it is already at rest — tear down now.
            handleRegistry.unregister(adapterId);
            adapter.close();
            if (recreateAs != null) {
                createAdapter(recreateAs);
            }
            return;
        }
        // Running: ask it to stop and tear it down when it reports stopped. Remove the handle now so REST no longer
        // sees an adapter that is going away.
        tellWrapper(adapter, new ProtocolAdapterWrapperCommand.StopAdapter());
        handleRegistry.unregister(adapterId);
        pendingRemovalMap.put(adapterId, new PendingRemoval(adapter, recreateAs));
    }

    // ── subsystem shutdown ──────────────────────────────────────────────────────────────────────

    /**
     * Wind the whole subsystem down on the dispatch thread: stop and discard every managed adapter (so each
     * protocol adapter gets a clean stop and every container's resources are released), then count the latch down
     * once they have all stopped. Pending recreates are dropped so a stop during shutdown never respawns an adapter.
     *
     * @param done the latch the bootstrap awaits before tearing the runtime down.
     */
    private void onShutdownRequested(final @NotNull CountDownLatch done) {
        shutdownLatch = done;
        pendingRemovalMap.replaceAll((adapterId, pending) -> new PendingRemoval(pending.stopping(), null));
        for (final String adapterId : new ArrayList<>(containerMap.keySet())) {
            stopAndDiscard(adapterId, null);
        }
        completeShutdownIfDone();
    }

    private void completeShutdownIfDone() {
        final CountDownLatch latch = shutdownLatch;
        if (latch != null && pendingRemovalMap.isEmpty()) {
            shutdownLatch = null;
            log.info("The v2 protocol-adapter manager has stopped every adapter");
            latch.countDown();
        }
    }

    // ── wrapper health ──────────────────────────────────────────────────────────────────────

    private void onWrapperStarted(final @NotNull String adapterId) {
        // Health is read from the wrapper's own snapshot; the event is logged for visibility. No action needed.
        log.debug("v2 adapter '{}' started", adapterId);
    }

    private void onWrapperStopped(final @NotNull String adapterId) {
        final PendingRemoval pending = pendingRemovalMap.remove(adapterId);
        if (pending == null) {
            // A normal stop (for example the user deactivated both directions). The adapter stays managed and
            // registered, its snapshot now showing STOPPED.
            return;
        }
        pending.stopping().close();
        if (pending.recreateAs() != null) {
            createAdapter(pending.recreateAs());
        }
        // A shutdown may be waiting for the last adapter to wind down.
        completeShutdownIfDone();
    }

    private void onWrapperError(final @NotNull String adapterId, final @NotNull String reason) {
        // The wrapper's snapshot already shows ERROR. The manager performs NO automatic recreate
        // (manual recovery in this project): the user deactivates and re-activates, or replaces the
        // configuration.
        log.warn("v2 adapter '{}' entered ERROR: {}", adapterId, reason);
    }

    // ── browse bridge ─────────────────────────────────────────────────────────────────────────────

    private void handleBrowse(final @NotNull BrowseRequested browse) {
        final ProtocolAdapterHandle handle = handleRegistry.find(browse.adapterId());
        if (handle == null) {
            // A race: the adapter was removed after the resource's own 404 check. The resource maps this to 404.
            browse.completion()
                    .completeExceptionally(new IllegalArgumentException("no v2 adapter [" + browse.adapterId() + "]"));
            return;
        }
        // The capability check (400) is the resource's — it holds the factory. The manager checks the connection
        // (409) on the snapshot and forwards to the wrapper, which (on its own dispatch thread) rechecks it is
        // CONNECTED with no browse in flight, issues browse(filter), and completes the future from the result or
        // the deadline.
        final AdapterStatusSnapshot snapshot = handle.snapshot().get();
        if (snapshot == null || snapshot.machineState() != ProtocolAdapterWrapperState.CONNECTED) {
            browse.completion()
                    .completeExceptionally(new BrowseRejectedException(
                            BrowseRejectedException.Reason.NOT_CONNECTED,
                            "adapter '" + browse.adapterId() + "' is not connected"));
            return;
        }
        handle.wrapperSender().tell(new ProtocolAdapterWrapperBrowseRequest(browse.filter(), browse.completion()));
    }

    // ── health summary ────────────────────────────────────────────────────────────────────────────

    private void publishHealthSummary(final long nowMillis) {
        int total = 0;
        int connected = 0;
        int error = 0;
        int stopped = 0;
        int transitioning = 0;
        for (final ProtocolAdapterHandle handle : handleRegistry.all()) {
            total++;
            final AdapterStatusSnapshot snapshot = handle.snapshot().get();
            if (snapshot == null) {
                transitioning++;
                continue;
            }
            switch (snapshot.machineState()) {
                case CONNECTED -> connected++;
                case ERROR -> error++;
                case STOPPED -> stopped++;
                default -> transitioning++;
            }
        }
        healthSummary.set(
                new ProtocolAdapterManagerSnapshot(total, connected, error, stopped, transitioning, nowMillis));
    }

    // ── helpers ─────────────────────────────────────────────────────────────────────────────────────────────────

    private void forwardCommand(final @NotNull String adapterId, final @NotNull ProtocolAdapterWrapperCommand command) {
        final ProtocolAdapterHandle handle = handleRegistry.find(adapterId);
        if (handle == null) {
            log.warn(
                    "Dropping {} for unknown v2 adapter '{}'",
                    command.getClass().getSimpleName(),
                    adapterId);
            return;
        }
        handle.wrapperSender().tell(command);
    }

    private static void tellWrapper(
            final @NotNull ProtocolAdapterContainer adapter, final @NotNull ProtocolAdapterWrapperCommand command) {
        adapter.handle().wrapperSender().tell(command);
    }

    private static @NotNull ProtocolAdapterWrapperCommand.ApplyActivation activationCommand(
            final @NotNull ProtocolAdapterEntity entity) {
        return new ProtocolAdapterWrapperCommand.ApplyActivation(
                ProtocolAdapterConfigSupport.goalOf(entity), ProtocolAdapterConfigSupport.activationOf(entity));
    }

    private static boolean isStopped(final @NotNull ProtocolAdapterContainer adapter) {
        final AdapterStatusSnapshot snapshot = adapter.handle().snapshot().get();
        return snapshot != null && snapshot.machineState() == ProtocolAdapterWrapperState.STOPPED;
    }

    private @NotNull ProtocolAdapterManagerHealthListener requireHealthListener() {
        return Objects.requireNonNull(
                healthListener, "bindSelf must be called before the manager handles a configuration");
    }

    /**
     * An adapter told to stop, awaiting its {@code stopped()} acknowledgment before its resources are released and
     * (for a full recreate) a fresh instance is created.
     *
     * @param stopping   the stopping adapter, whose resources are closed once it reports stopped.
     * @param recreateAs the configuration to recreate from after teardown, or {@code null} for a pure removal.
     */
    private record PendingRemoval(
            @NotNull ProtocolAdapterContainer stopping,
            @Nullable ProtocolAdapterEntity recreateAs) {}
}
