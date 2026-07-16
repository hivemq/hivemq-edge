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

/**
 * The terminal fate of one southbound write, reported back to whoever submitted it — the signal the queue in
 * front of the write aspect disposes each delivered command by. The distinction that matters is
 * <b>terminal vs retryable</b>: {@link #SUCCEEDED} and {@link #FAILED} end a command's journey (commit and
 * dead-letter respectively — it is deleted from the durable backlog), while {@link #ABORTED} means the command was
 * never handled and stays queued for redelivery. Every accepted or rejected write settles exactly once, so a
 * sender that holds the next write until the current one settles can never be left waiting.
 */
public enum SouthboundWriteOutcome {

    /** Terminal: the device acknowledged the write successfully — commit (delete) it. */
    SUCCEEDED,

    /**
     * Terminal: the device acknowledged the write as failed (rejected value, protected register, …) —
     * dead-letter it; redelivering a value the device rejects loops forever.
     */
    FAILED,

    /**
     * The write was not attempted because one was already in flight — a violation of the advertised in-flight
     * window of one, which the single-in-flight invariant refused. A sender that paces deliveries to the window
     * never triggers this; a non-zero count is an alarm, not a load condition.
     */
    REJECTED_BUSY,

    /**
     * Retryable: the write was abandoned before a result — it was in flight when the tag deactivated or the
     * connection was lost, or it arrived while the aspect could not write at all. The command was not handled;
     * keep it queued and redeliver when the adapter is ready again.
     */
    ABORTED
}
