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

import com.hivemq.protocols.v2.fsm.FSMTransitionTable;
import org.jetbrains.annotations.NotNull;

/**
 * The write-aspect transition table — a direct encoding of the write state diagram. The five shared
 * pre-operating rows are built by {@link TagAspectPreOperatingTransitions} (the same builder the read tables use);
 * the role-specific rows are the write cycle: a write arriving while the aspect rests at
 * {@code WAITING_FOR_WRITE_REQUEST} requests it and moves to {@code WAITING_FOR_WRITE_RESULT}; the acknowledgment
 * returns it to the request state (a failure is logged and counted but does <b>not</b> flap to {@code ERROR}).
 * <p>
 * The table is built once and shared by every write aspect; an action acts through the {@link TagAspectWrite}
 * passed as the machine context. Goal and adapter-readiness changes never reach it — they are applied directly by
 * {@link TagAspectWrite}. The mandatory {@code unmatched} slot is lenient: an unexpected event is logged and
 * ignored, never a reset — this is what enforces the single-in-flight-write rule. A write arriving outside the
 * resting state lands here too and is settled rather than ignored ({@link TagAspectWrite#logUnexpectedEvent}):
 * {@link SouthboundWriteOutcome#REJECTED_BUSY} while one is already in flight (a violation of the advertised
 * window of one — the aspect never queues), {@link SouthboundWriteOutcome#ABORTED} while the aspect cannot write
 * at all, so the sender keeps the command queued for redelivery.
 */
public final class TagAspectWriteTransitions {

    private TagAspectWriteTransitions() {}

    private static final @NotNull FSMTransitionTable<TagAspectState, TagAspectEvent, TagAspectWrite> TABLE =
            buildTable();

    /**
     * @return the write-aspect transition table.
     */
    public static @NotNull FSMTransitionTable<TagAspectState, TagAspectEvent, TagAspectWrite> table() {
        return TABLE;
    }

    private static @NotNull FSMTransitionTable<TagAspectState, TagAspectEvent, TagAspectWrite> buildTable() {
        final FSMTransitionTable.Builder<TagAspectState, TagAspectEvent, TagAspectWrite> builder =
                FSMTransitionTable.builder();
        TagAspectPreOperatingTransitions.addPreOperatingRows(
                builder,
                TagAspectWriteState.WAITING_FOR_VERIFICATION,
                TagAspectWriteState.WAITING_FOR_VERIFICATION_RETRY,
                TagAspectWriteState.ERROR_PERMANENT_VERIFICATION_FAILURE);
        return builder
                // A southbound write arrived while resting ready: request it, remember its completion, and wait
                // for the acknowledgment.
                .on(TagAspectWriteState.WAITING_FOR_WRITE_REQUEST, TagAspectEvent.WriteRequested.class)
                .then((current, event, aspect) -> {
                    aspect.beginWrite((TagAspectEvent.WriteRequested) event);
                    return TagAspectWriteState.WAITING_FOR_WRITE_RESULT;
                })
                // The write was acknowledged successfully: settle its completion and return to the resting state.
                .on(TagAspectWriteState.WAITING_FOR_WRITE_RESULT, TagAspectEvent.WriteSucceeded.class)
                .then((current, event, aspect) -> aspect.completeInFlightWrite(true, null))
                // The write failed: log and count, settle its completion, return to the resting state — no flap
                // to ERROR.
                .on(TagAspectWriteState.WAITING_FOR_WRITE_RESULT, TagAspectEvent.WriteFailed.class)
                .then((current, event, aspect) ->
                        aspect.completeInFlightWrite(false, TagAspectPreOperatingTransitions.reasonOf(event)))
                .unmatched((current, event, aspect) -> {
                    aspect.logUnexpectedEvent(event);
                    return current;
                })
                .build();
    }
}
