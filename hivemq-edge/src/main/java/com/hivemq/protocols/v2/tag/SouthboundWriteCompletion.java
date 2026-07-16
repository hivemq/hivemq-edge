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
package com.hivemq.protocols.v2.tag;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The one-shot callback a southbound write carries so its submitter learns when the write reaches a terminal
 * {@link SouthboundWriteOutcome}. This is the seam the southbound write path hangs on: the write aspect stays
 * strictly single-in-flight and never buffers, and the queue in front of it uses this signal to hold the next
 * write until the current one settles — so the backlog waits in the durable queue, not in the adapter, and a
 * command is deleted from that queue only on a terminal outcome.
 * <p>
 * <b>Invoked on the adapter's single dispatch thread</b>, exactly once per write. Implementations must not block
 * that thread: hand the outcome to the submitter (release a gate, complete a future) and return. This is
 * <em>not</em> an acknowledgement to the external MQTT client — it is an internal queue↔adapter completion
 * signal; a correlation-id reply to the sender is a separate, deferred concern.
 */
@FunctionalInterface
public interface SouthboundWriteCompletion {

    /**
     * Report the write's terminal outcome. Called once, on the dispatch thread.
     *
     * @param outcome the terminal outcome.
     * @param reason  what made it terminal — the device's own failure reason for a {@code FAILED} write, why the
     *                write was abandoned for an {@code ABORTED} one; {@code null} on success. Travels into the
     *                published verdict, so the device's words are what the operator reads.
     */
    void settle(@NotNull SouthboundWriteOutcome outcome, @Nullable String reason);

    /** A completion that discards the outcome — for fire-and-forget callers that do not back-pressure. */
    @NotNull
    SouthboundWriteCompletion IGNORED = (outcome, reason) -> {};
}
