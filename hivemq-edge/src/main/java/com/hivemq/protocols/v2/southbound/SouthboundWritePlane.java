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
package com.hivemq.protocols.v2.southbound;

import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.v2.messaging.MailboxSender;
import com.hivemq.adapter.sdk.api.v2.node.Node;
import com.hivemq.adapter.sdk.api.v2.node.NodeTagPair;
import com.hivemq.protocols.v2.runtime.ProtocolAdapterMetrics;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperMessage;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperSouthboundMessage;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperSouthboundMessage.SouthboundArrival;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperSouthboundMessage.SouthboundRead;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperSouthboundMessage.SouthboundSize;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperSouthboundMessage.TagWritability;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperSouthboundMessage.WriteSettled;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * One adapter's southbound delivery side: a {@link SouthboundWriteQueue} over a {@link SouthboundWriteBacklog} per
 * <b>write-mapped</b> tag, and the router that turns every southbound mailbox message into a call on the right
 * channel.
 * <p>
 * <b>The delivery side runs entirely on the wrapper's single dispatch thread.</b> Everything it reacts to arrives
 * there as a message — the write aspect's settlements and writability reports, the store's read and depth answers,
 * the broker's arrival hints — so each channel's state is ordinary single-threaded state. There is no readiness
 * listener interface and no completion callback any more: the aspect tells the mailbox, and the wrapper routes.
 * <p>
 * Two entry points do not come from the dispatch thread, and only these two:
 * <ul>
 * <li>{@link #offer} — producer threads, for the in-memory store only. It touches no channel state: the value goes
 *     to the store, which hints the dispatch thread through the mailbox exactly as the broker would.</li>
 * <li>{@link #close} — the manager's thread during teardown. This is the only reason the channel map is concurrent.
 *     By the time the container closes the plane it has already detached the wrapper from its dispatcher, so no
 *     dispatch-thread work is in flight; the concurrent map makes that safe rather than merely likely.</li>
 * </ul>
 * <p>
 * On a tags-only reload {@link #updateTagSet} follows the config: channels whose tag is no longer write-mapped are
 * dropped, new write-mapped tags get a fresh channel with a closed window, and a <b>surviving tag keeps its channel
 * entirely</b> — store, leased head and in-flight delivery — following the tag to whatever node it now addresses and
 * closing its window until the rebuilt aspect re-verifies. It is called from the wrapper's {@code UpdateTagSet}
 * handler <i>before</i> the aspects are rebuilt, so the ordering that used to be a cross-thread contract is now plain
 * statement order.
 */
public final class SouthboundWritePlane implements AutoCloseable {

    private static final @NotNull Logger log = LoggerFactory.getLogger(SouthboundWritePlane.class);

    /** One write-mapped tag's delivery channel: its durable store and the queue pacing it to the aspect. */
    public record TagChannel(
            @NotNull SouthboundWriteBacklog backlog,
            @NotNull SouthboundWriteQueue queue) {}

    private final @NotNull String adapterId;
    private final @NotNull MailboxSender<ProtocolAdapterWrapperMessage> wrapperSender;
    private final @NotNull SouthboundWriteBacklogFactory backlogFactory;
    private final @NotNull ProtocolAdapterMetrics metrics;

    /**
     * Mints the read, depth-check and delivery tokens of <b>every</b> channel on this plane. Plane-wide rather than
     * per-channel on purpose: store answers are routed to a channel by tag name alone, so a channel rebuilt while one
     * of its reads was still outstanding would otherwise mint a token its predecessor had already used, and consume
     * the predecessor's answer as its own. Dispatch-thread only, like everything else here.
     */
    private long nextToken;

    /** Concurrent for exactly one reason: {@link #close} runs on the manager's thread. See the class javadoc. */
    private final @NotNull ConcurrentHashMap<String, TagChannel> channels = new ConcurrentHashMap<>();

    /**
     * A plane over the non-durable in-memory stores.
     *
     * @param adapterId         the owning adapter's id.
     * @param wrapperSender     the send-only handle to the adapter wrapper's mailbox.
     * @param backlogCapacity   the per-tag bound ({@code southbound-write-backlog-capacity}).
     * @param nodes             the configured node/tag pairs.
     * @param writeUsedTagNames the tags referenced by a southbound mapping — one channel each.
     * @param metrics           the per-adapter metrics; each channel counts its dead-letters here.
     */
    public SouthboundWritePlane(
            final @NotNull String adapterId,
            final @NotNull MailboxSender<ProtocolAdapterWrapperMessage> wrapperSender,
            final int backlogCapacity,
            final @NotNull List<NodeTagPair> nodes,
            final @NotNull Set<String> writeUsedTagNames,
            final @NotNull ProtocolAdapterMetrics metrics) {
        this(
                adapterId,
                wrapperSender,
                SouthboundWriteBacklogFactory.inMemory(backlogCapacity),
                nodes,
                writeUsedTagNames,
                metrics);
    }

    /**
     * @param adapterId         the owning adapter's id.
     * @param wrapperSender     the send-only handle to the adapter wrapper's mailbox.
     * @param backlogFactory    builds the store behind each tag's channel.
     * @param nodes             the configured node/tag pairs.
     * @param writeUsedTagNames the tags referenced by a southbound mapping — one channel each.
     * @param metrics           the per-adapter metrics; each channel counts its dead-letters here.
     */
    public SouthboundWritePlane(
            final @NotNull String adapterId,
            final @NotNull MailboxSender<ProtocolAdapterWrapperMessage> wrapperSender,
            final @NotNull SouthboundWriteBacklogFactory backlogFactory,
            final @NotNull List<NodeTagPair> nodes,
            final @NotNull Set<String> writeUsedTagNames,
            final @NotNull ProtocolAdapterMetrics metrics) {
        this.adapterId = adapterId;
        this.wrapperSender = wrapperSender;
        this.backlogFactory = backlogFactory;
        this.metrics = metrics;
        try {
            for (final NodeTagPair pair : nodes) {
                if (writeUsedTagNames.contains(pair.tag().name())) {
                    channels.put(pair.tag().name(), newChannel(pair.tag().name(), pair.node()));
                }
            }
        } catch (final RuntimeException failure) {
            // A failed channel build must not leak the channels already created — their stores hold broker-side
            // callbacks and possibly a lease.
            close();
            throw failure;
        }
    }

    // ── message routing (dispatch thread) ───────────────────────────────────────────────────────────────────────

    /**
     * Route one southbound message to its tag's channel. A message for a tag with no channel is dropped: a
     * de-mapped or replaced tag can still have answers and settlements in flight behind it, and those are stale by
     * definition.
     *
     * @param message the message the wrapper routed here.
     */
    public void onMessage(final @NotNull ProtocolAdapterWrapperSouthboundMessage message) {
        switch (message) {
            case final WriteSettled settled -> {
                final TagChannel channel = channels.get(settled.tagName());
                if (channel != null) {
                    channel.queue().onSettled(settled.deliveryToken(), settled.outcome(), settled.reason());
                }
            }
            case final TagWritability writability -> {
                final TagChannel channel = channels.get(writability.tagName());
                if (channel != null) {
                    if (writability.writable()) {
                        channel.queue().openWindow();
                    } else {
                        channel.queue().closeWindow();
                    }
                }
            }
            case final SouthboundRead read -> {
                final TagChannel channel = channels.get(read.tagName());
                if (channel != null) {
                    channel.queue()
                            .onReadAnswer(
                                    read.readToken(),
                                    read.command(),
                                    read.undeliverableCommandId(),
                                    read.undeliverableReason(),
                                    read.failure());
                }
            }
            case final SouthboundSize size -> {
                final TagChannel channel = channels.get(size.tagName());
                if (channel != null) {
                    channel.queue().onSizeAnswer(size.readToken(), size.size(), size.failure());
                }
            }
            case final SouthboundArrival arrival -> {
                final TagChannel channel = channels.get(arrival.tagName());
                if (channel != null) {
                    channel.queue().onArrival();
                }
            }
        }
    }

    /**
     * The wrapper ticked: give every channel its backstop-poll cadence. A counter bump per channel in the common
     * case, and the reason no scheduler exists anywhere in the southbound path.
     * <p>
     * Each channel is stepped independently. This runs inside the tick, ahead of the batch dispatch: an escaping
     * throwable would skip the remaining channels <b>and</b> that tick's batch dispatch, and the wrapper's contract
     * guard would fault the whole adapter into {@code ERROR} — far too much blast radius for a hiccup on one tag.
     */
    public void onTick() {
        for (final Map.Entry<String, TagChannel> entry : channels.entrySet()) {
            try {
                entry.getValue().queue().onTick();
            } catch (final Exception failure) {
                log.warn(
                        "Southbound tick failed for tag '{}' on adapter '{}' — retrying on the next tick",
                        entry.getKey(),
                        adapterId,
                        failure);
            }
        }
    }

    /**
     * Follow a tags-only configuration reload. Called on the dispatch thread from the wrapper's
     * {@code UpdateTagSet} handler, <b>before</b> the aspects are rebuilt, so a rebuilt aspect's writability report
     * always finds its channel in place.
     *
     * @param nodes             the new node/tag pairs.
     * @param writeUsedTagNames the new write-mapped tag names.
     */
    public void updateTagSet(final @NotNull List<NodeTagPair> nodes, final @NotNull Set<String> writeUsedTagNames) {
        for (final Map.Entry<String, TagChannel> entry : channels.entrySet()) {
            if (!writeUsedTagNames.contains(entry.getKey())) {
                final TagChannel dropped = channels.remove(entry.getKey());
                if (dropped != null) {
                    dropChannel(entry.getKey(), dropped, "the tag is no longer write-mapped");
                }
            }
        }
        for (final NodeTagPair pair : nodes) {
            final String tagName = pair.tag().name();
            if (!writeUsedTagNames.contains(tagName)) {
                continue;
            }
            final TagChannel existing = channels.get(tagName);
            if (existing == null) {
                channels.put(tagName, newChannel(tagName, pair.node()));
            } else {
                // A surviving tag keeps its channel outright — its store, its leased head and any delivery still in
                // flight all ride out the reload — and simply follows the tag to whatever node it now addresses.
                // Rebuilding the channel instead (which is what a node comparison here used to do, on every reload,
                // because Node correlates by identity and is deserialized afresh each time) bought nothing: the
                // durable queue is keyed by the mapping topic, so the successor read back exactly the commands the
                // rebuild claimed to discard — after redelivering the leased head, i.e. writing to the device twice.
                existing.queue().retarget(pair.node());
                // The window closes until the rebuilt aspect re-verifies and reports itself writable again.
                existing.queue().closeWindow();
            }
        }
    }

    // ── producer and teardown entry points (other threads) ──────────────────────────────────────────────────────

    /**
     * Offer a southbound command for the named tag — the "an MQTT write arrived" trigger for a store the broker
     * does not feed. Touches no channel state.
     *
     * @param tagName the write-mapped tag to command.
     * @param value   the value to write.
     * @return whether the tag has a channel fed this way; {@code false} means the command went nowhere.
     */
    public boolean offer(final @NotNull String tagName, final @NotNull DataPoint value) {
        final TagChannel channel = channels.get(tagName);
        if (channel == null) {
            log.debug(
                    "Southbound write for tag '{}' on adapter '{}' has no channel (not write-mapped) — discarded",
                    tagName,
                    adapterId);
            return false;
        }
        if (!(channel.backlog() instanceof final InMemorySouthboundWriteBacklog offerable)) {
            // A durable store is fed by the broker (its MQTT queue), never offered to directly.
            log.warn(
                    "Southbound write for tag '{}' on adapter '{}' offered to a broker-fed store — discarded "
                            + "(publish to the mapped topic instead)",
                    tagName,
                    adapterId);
            return false;
        }
        offerable.offer(value);
        return true;
    }

    /**
     * Close every delivery window, close the stores, and drop the channels. In-memory stores die with the plane; a
     * durable store's storage outlives it by construction — only its callback and its leases are released.
     */
    @Override
    public void close() {
        for (final Map.Entry<String, TagChannel> entry : channels.entrySet()) {
            entry.getValue().queue().closeWindow();
            closeBacklog(entry.getKey(), entry.getValue());
        }
        channels.clear();
    }

    // ── observation ─────────────────────────────────────────────────────────────────────────────────────────────

    /**
     * @param tagName the tag to look up.
     * @return the tag's delivery channel, or {@code null} when the tag is not write-mapped.
     */
    public @Nullable TagChannel channel(final @NotNull String tagName) {
        return channels.get(tagName);
    }

    /**
     * @return the write-mapped tag names currently carrying a channel.
     */
    public @NotNull Set<String> writeMappedTagNames() {
        return Set.copyOf(channels.keySet());
    }

    private @NotNull TagChannel newChannel(final @NotNull String tagName, final @NotNull Node node) {
        final SouthboundWriteBacklog backlog = backlogFactory.create(tagName, node, wrapperSender);
        // Windows are born closed: the first writability report (the tag verified) opens this one; until then
        // commands wait in the store instead of bouncing off an aspect that could only abort them.
        final SouthboundWriteQueue queue =
                new SouthboundWriteQueue(adapterId, tagName, node, backlog, wrapperSender, this::nextToken, metrics);
        return new TagChannel(backlog, queue);
    }

    /** The plane-wide token source handed to every channel. Dispatch thread only. */
    private long nextToken() {
        return ++nextToken;
    }

    private void dropChannel(
            final @NotNull String tagName, final @NotNull TagChannel channel, final @NotNull String reason) {
        channel.queue().closeWindow();
        if (channel.backlog() instanceof final InMemorySouthboundWriteBacklog inMemory && inMemory.pendingSize() > 0) {
            log.warn(
                    "Dropping {} pending southbound command(s) for tag '{}' on adapter '{}': {}",
                    inMemory.pendingSize(),
                    tagName,
                    adapterId,
                    reason);
        }
        closeBacklog(tagName, channel);
    }

    private void closeBacklog(final @NotNull String tagName, final @NotNull TagChannel channel) {
        try {
            channel.backlog().close();
        } catch (final Exception exception) {
            log.warn("Failed to close the southbound store of tag '{}' on adapter '{}'", tagName, adapterId, exception);
        }
    }
}
