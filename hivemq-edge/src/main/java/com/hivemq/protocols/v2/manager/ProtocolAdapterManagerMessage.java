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

import com.hivemq.adapter.sdk.api.v2.messaging.MailboxMessage;
import com.hivemq.adapter.sdk.api.v2.messaging.MailboxMessagePriority;
import com.hivemq.adapter.sdk.api.v2.model.BrowseFilter;
import com.hivemq.adapter.sdk.api.v2.model.BrowseResultEntry;
import com.hivemq.protocols.v2.config.ProtocolAdapterEntity;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterDirection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.jetbrains.annotations.NotNull;

/**
 * The single mailbox message type of the {@link ProtocolAdapterManager} supervisor actor. Everything
 * the manager consumes arrives as one of these, in priority-band order:
 * <ul>
 * <li>{@code CONTROL} — externally-driven intent: a freshly-loaded configuration, REST direction activation, REST
 * tag retry, and a REST browse request;</li>
 * <li>{@code EVENT} — wrapper health notifications a managed wrapper tells back ({@code started} / {@code stopped} /
 * {@code error});</li>
 * <li>{@code TICK} — periodic housekeeping (the health summary).</li>
 * </ul>
 * Sealed because all permitted subtypes live in this package; the generic {@link MailboxMessage} marker is the
 * non-sealed bridge they extend.
 */
public sealed interface ProtocolAdapterManagerMessage extends MailboxMessage {

    /**
     * A freshly-loaded {@code <v2>} section from the extractor — the same
     * path at startup and on every reload. The manager diffs it against the running set and applies the gentlest
     * correct transition per adapter.
     *
     * @param adapters the complete new set of adapter configurations.
     */
    record ConfigurationChanged(@NotNull List<ProtocolAdapterEntity> adapters)
            implements ProtocolAdapterManagerMessage {
        @Override
        public @NotNull MailboxMessagePriority priority() {
            return MailboxMessagePriority.CONTROL;
        }
    }

    /**
     * Activate a direction of an adapter (REST origin) — a <b>live-goal command, never persisted</b> (D7). Forwarded to the running wrapper as
     * {@link com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperCommand.ActivateDirection}.
     *
     * @param adapterId the adapter instance id.
     * @param direction the direction to activate.
     */
    record ActivateAdapter(
            @NotNull String adapterId, @NotNull ProtocolAdapterDirection direction)
            implements ProtocolAdapterManagerMessage {
        @Override
        public @NotNull MailboxMessagePriority priority() {
            return MailboxMessagePriority.CONTROL;
        }
    }

    /**
     * Deactivate a direction of an adapter (REST origin) — a <b>live-goal command, never persisted</b> (D7). Forwarded to the running wrapper as
     * {@link com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperCommand.DeactivateDirection}.
     *
     * @param adapterId the adapter instance id.
     * @param direction the direction to deactivate.
     */
    record DeactivateAdapter(
            @NotNull String adapterId, @NotNull ProtocolAdapterDirection direction)
            implements ProtocolAdapterManagerMessage {
        @Override
        public @NotNull MailboxMessagePriority priority() {
            return MailboxMessagePriority.CONTROL;
        }
    }

    /**
     * Retry a permanently-failed tag of an adapter (REST origin) — a runtime-only command, forwarded to
     * the running wrapper as {@link com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperCommand.RetryTag}; never
     * touches configuration.
     *
     * @param adapterId the adapter instance id.
     * @param tagName   the tag to retry.
     */
    record RetryTag(@NotNull String adapterId, @NotNull String tagName) implements ProtocolAdapterManagerMessage {
        @Override
        public @NotNull MailboxMessagePriority priority() {
            return MailboxMessagePriority.CONTROL;
        }
    }

    /**
     * Browse an adapter's device address space (REST origin). Carries the completion future the REST
     * thread blocks on; the manager forwards the request toward the wrapper and the result is relayed back by
     * completing the future. The full browse bridge (capability and connection checks, the wrapper&rarr;adapter
     * forward, the timeout) is completed in the OpenAPI/resource task; here the message is part of the sealed
     * hierarchy and the manager fails the future for a missing or not-yet-bridged adapter.
     *
     * @param adapterId  the adapter instance id.
     * @param filter     the browse filter selecting where to browse.
     * @param completion the future the REST thread awaits; completed with the results or completed exceptionally.
     */
    record BrowseRequested(
            @NotNull String adapterId,
            @NotNull BrowseFilter filter,
            @NotNull CompletableFuture<List<BrowseResultEntry>> completion)
            implements ProtocolAdapterManagerMessage {
        @Override
        public @NotNull MailboxMessagePriority priority() {
            return MailboxMessagePriority.CONTROL;
        }
    }

    /**
     * A managed wrapper reached {@code CONNECTED}. The {@code EVENT} band (the default).
     *
     * @param adapterId the adapter instance id.
     */
    record WrapperStarted(@NotNull String adapterId) implements ProtocolAdapterManagerMessage {}

    /**
     * A managed wrapper reached {@code STOPPED} — the signal that a stop-and-discard or full
     * recreate may now tear the wrapper down. The {@code EVENT} band.
     *
     * @param adapterId the adapter instance id.
     */
    record WrapperStopped(@NotNull String adapterId) implements ProtocolAdapterManagerMessage {}

    /**
     * A managed wrapper entered {@code ERROR}. The manager records it and performs <b>no</b>
     * automatic recreate (manual recovery in this project). The {@code EVENT} band.
     *
     * @param adapterId the adapter instance id.
     * @param reason    a human-readable description of why.
     */
    record WrapperError(@NotNull String adapterId, @NotNull String reason) implements ProtocolAdapterManagerMessage {}

    /**
     * Periodic housekeeping: the manager folds the registry's snapshots into a health summary it
     * publishes for readers. The {@code TICK} band.
     *
     * @param nowMillis the tick's logical time, in milliseconds.
     */
    record ProtocolAdapterManagerTick(long nowMillis) implements ProtocolAdapterManagerMessage {
        @Override
        public @NotNull MailboxMessagePriority priority() {
            return MailboxMessagePriority.TICK;
        }
    }
}
