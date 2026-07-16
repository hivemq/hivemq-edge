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
import com.hivemq.protocols.v2.tag.TagWriteReadinessListener;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperMessage;
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
 * <b>write-mapped</b> tag (a tag referenced by at least one southbound mapping), plus the wiring that opens and
 * closes each queue's delivery window from the tag's own readiness. It is the producer-facing front of the write
 * path: MQTT intake (a later task) and tests {@link #offer} commands here; the adapter side stays untouched.
 * <p>
 * As the adapter's {@link TagWriteReadinessListener} it turns the write aspect's writability boundary into window
 * calls: {@link #tagWritable} → {@link SouthboundWriteQueue#resume() resume}, {@link #tagUnwritable} →
 * {@link SouthboundWriteQueue#suspend() suspend}. Queues are created <b>suspended</b> — the first
 * {@code tagWritable} (the tag verified) opens the window — so a command arriving before the tag is ready waits in
 * the backlog instead of bouncing off an aspect that can only abort it.
 * <p>
 * On a tags-only reload ({@link #updateTagSet}) the plane follows the config: channels for tags that are no longer
 * write-mapped are dropped (their pending commands are discarded, observably logged), new write-mapped tags get a
 * fresh suspended channel, and a surviving tag <b>keeps its backlog</b> — pending commands ride out the reload and
 * redeliver when the rebuilt aspect re-verifies. The wrapper rebuilds aspects only after this plane has been
 * updated (the manager updates the plane before telling {@code UpdateTagSet}), so a readiness notification can
 * never race ahead of its channel.
 * <p>
 * Thread-safety: readiness notifications arrive on the wrapper's dispatch thread, {@link #offer} on producer
 * threads, {@link #updateTagSet}/{@link #close} on the manager's thread. The channel map is concurrent and the
 * queues are internally synchronized; no call blocks — a readiness notification at most enqueues to the wrapper
 * mailbox.
 */
public final class SouthboundWritePlane implements TagWriteReadinessListener, AutoCloseable {

    private static final @NotNull Logger log = LoggerFactory.getLogger(SouthboundWritePlane.class);

    /** One write-mapped tag's delivery channel: its durable backlog and the queue pacing it to the aspect. */
    public record TagChannel(
            @NotNull Node node,
            @NotNull SouthboundWriteBacklog backlog,
            @NotNull SouthboundWriteQueue queue) {}

    private final @NotNull String adapterId;
    private final @NotNull MailboxSender<ProtocolAdapterWrapperMessage> wrapperSender;
    private final @NotNull SouthboundWriteBacklogFactory backlogFactory;
    private final @NotNull ConcurrentHashMap<String, TagChannel> channels = new ConcurrentHashMap<>();

    /**
     * A plane over the interim in-memory backlogs.
     *
     * @param adapterId         the owning adapter's id.
     * @param wrapperSender     the send-only handle to the adapter wrapper's mailbox.
     * @param backlogCapacity   the per-tag backlog bound ({@code southbound-write-backlog-capacity}); offers beyond
     *                          it shed the newest.
     * @param nodes             the configured node/tag pairs.
     * @param writeUsedTagNames the tags referenced by a southbound mapping — one channel each.
     */
    public SouthboundWritePlane(
            final @NotNull String adapterId,
            final @NotNull MailboxSender<ProtocolAdapterWrapperMessage> wrapperSender,
            final int backlogCapacity,
            final @NotNull List<NodeTagPair> nodes,
            final @NotNull Set<String> writeUsedTagNames) {
        this(
                adapterId,
                wrapperSender,
                SouthboundWriteBacklogFactory.inMemory(backlogCapacity),
                nodes,
                writeUsedTagNames);
    }

    /**
     * @param adapterId         the owning adapter's id.
     * @param wrapperSender     the send-only handle to the adapter wrapper's mailbox.
     * @param backlogFactory    builds the backlog behind each tag's channel (in-memory today; the durable
     *                          client-queue one once the MQTT intake supplies queue ids).
     * @param nodes             the configured node/tag pairs.
     * @param writeUsedTagNames the tags referenced by a southbound mapping — one channel each.
     */
    public SouthboundWritePlane(
            final @NotNull String adapterId,
            final @NotNull MailboxSender<ProtocolAdapterWrapperMessage> wrapperSender,
            final @NotNull SouthboundWriteBacklogFactory backlogFactory,
            final @NotNull List<NodeTagPair> nodes,
            final @NotNull Set<String> writeUsedTagNames) {
        this.adapterId = adapterId;
        this.wrapperSender = wrapperSender;
        this.backlogFactory = backlogFactory;
        for (final NodeTagPair pair : nodes) {
            if (writeUsedTagNames.contains(pair.tag().name())) {
                channels.put(pair.tag().name(), newSuspendedChannel(pair.tag().name(), pair.node()));
            }
        }
    }

    /**
     * Offer a southbound command for the named tag — the "an MQTT write arrived" trigger. Enqueued on the tag's
     * durable backlog (shed if the backlog is at its bound) and delivered by the tag's queue as its window and the
     * single-in-flight cycle allow.
     *
     * @param tagName the write-mapped tag to command.
     * @param value   the value to write.
     * @return whether the tag has a channel (is write-mapped); a {@code false} means the command went nowhere.
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
            // A durable backlog is fed by the broker (its MQTT queue), never offered to directly.
            log.warn(
                    "Southbound write for tag '{}' on adapter '{}' offered to a broker-fed backlog — discarded "
                            + "(publish to the mapped topic instead)",
                    tagName,
                    adapterId);
            return false;
        }
        offerable.offer(value);
        return true;
    }

    /**
     * Follow a tags-only configuration reload: drop channels whose tag is gone from the write-mapped set (their
     * pending commands are discarded), create suspended channels for new write-mapped tags, and keep a surviving
     * tag's backlog so pending commands ride out the reload. A surviving channel is re-suspended here — the rebuilt
     * aspect re-verifies from scratch, and its {@code tagWritable} reopens the window.
     * <p>
     * Must be called <b>before</b> the wrapper is told the corresponding {@code UpdateTagSet}, so every readiness
     * notification from the rebuilt aspects finds its channel in place.
     *
     * @param nodes             the new node/tag pairs.
     * @param writeUsedTagNames the new write-mapped tag names.
     */
    public void updateTagSet(final @NotNull List<NodeTagPair> nodes, final @NotNull Set<String> writeUsedTagNames) {
        // Drop channels that are no longer write-mapped (or whose tag disappeared entirely).
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
                channels.put(tagName, newSuspendedChannel(tagName, pair.node()));
            } else if (!existing.node().equals(pair.node())) {
                // The tag now addresses a different node: pending commands were aimed at the old one — replace the
                // channel rather than deliver them to a node they never targeted.
                dropChannel(tagName, existing, "the tag's node changed");
                channels.put(tagName, newSuspendedChannel(tagName, pair.node()));
            } else {
                // Surviving tag: keep the backlog (pending commands ride out the reload), close the window until the
                // rebuilt aspect re-verifies and reports writable again.
                existing.queue().suspend();
            }
        }
    }

    @Override
    public void tagWritable(final @NotNull String tagName) {
        final TagChannel channel = channels.get(tagName);
        if (channel != null) {
            channel.queue().resume();
        }
    }

    @Override
    public void tagUnwritable(final @NotNull String tagName) {
        final TagChannel channel = channels.get(tagName);
        if (channel != null) {
            channel.queue().suspend();
        }
    }

    /**
     * @param tagName the tag to look up.
     * @return the tag's delivery channel, or {@code null} when the tag is not write-mapped — for observation and
     *         tests; producers use {@link #offer}.
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

    /**
     * Close every delivery window, close the backlogs, and drop the channels. In-memory backlogs die with the
     * plane; a durable backlog's storage outlives it by construction — only its callbacks and leases are released.
     */
    @Override
    public void close() {
        for (final Map.Entry<String, TagChannel> entry : channels.entrySet()) {
            entry.getValue().queue().suspend();
            closeBacklog(entry.getKey(), entry.getValue());
        }
        channels.clear();
    }

    private @NotNull TagChannel newSuspendedChannel(final @NotNull String tagName, final @NotNull Node node) {
        final SouthboundWriteBacklog backlog = backlogFactory.create(tagName, node);
        final SouthboundWriteQueue queue = new SouthboundWriteQueue(wrapperSender, node, backlog);
        // The window opens on the tag's first tagWritable (verified); until then commands wait in the backlog
        // instead of bouncing off an aspect that can only abort them.
        queue.suspend();
        return new TagChannel(node, backlog, queue);
    }

    private void dropChannel(final @NotNull String tagName, final @NotNull TagChannel channel,
            final @NotNull String reason) {
        channel.queue().suspend();
        if (channel.backlog() instanceof final InMemorySouthboundWriteBacklog inMemory
                && inMemory.pendingSize() > 0) {
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
            log.warn("Failed to close the southbound backlog of tag '{}' on adapter '{}'", tagName, adapterId,
                    exception);
        }
    }
}
