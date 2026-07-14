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

/**
 * The one-shot callback a southbound write carries so its submitter learns when the write reaches a terminal
 * {@link SouthboundWriteOutcome}. This is the seam that makes option D possible: the write aspect stays strictly
 * single-in-flight and never queues, and the producer uses this signal to hold the next write until the current
 * one settles — so the backlog waits in the producer's durable queue, not in the adapter.
 * <p>
 * <b>Invoked on the adapter's single dispatch thread</b>, exactly once per write. Implementations must not block
 * that thread: hand the outcome to the producer (release a gate, complete a future) and return. This is
 * <em>not</em> an acknowledgement to the external MQTT client — it is an internal producer↔adapter completion
 * signal; a correlation-id reply to the sender is a separate, deferred concern.
 */
@FunctionalInterface
public interface SouthboundWriteCompletion {

    /**
     * Report the write's terminal outcome. Called once, on the dispatch thread.
     *
     * @param outcome the terminal outcome.
     */
    void settle(@NotNull SouthboundWriteOutcome outcome);

    /** A completion that discards the outcome — for fire-and-forget callers that do not back-pressure. */
    @NotNull
    SouthboundWriteCompletion IGNORED = outcome -> {};
}
