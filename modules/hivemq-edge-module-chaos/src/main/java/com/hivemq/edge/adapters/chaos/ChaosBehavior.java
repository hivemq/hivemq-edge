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
package com.hivemq.edge.adapters.chaos;

import org.jetbrains.annotations.NotNull;

/**
 * How the {@link ChaosProtocolAdapter} answers a lifecycle command ({@code start} / {@code connect} /
 * {@code disconnect} / {@code stop}):
 * <ul>
 * <li>{@link Succeed} — acknowledge successfully (e.g. {@code output.started()});</li>
 * <li>{@link FailAdapter} — report {@code error(ADAPTER, reason)};</li>
 * <li>{@link FailConnection} — report {@code error(CONNECTION, reason)};</li>
 * <li>{@link Drop} / {@link NoResponse} — stay silent, so the wrapper parks in the waiting state until its
 *     watchdog fires;</li>
 * <li>{@link Delay} — apply the inner behavior {@code ticks} harness ticks later, exercising the slow-but-normal
 *     and watchdog-tripping paths.</li>
 * </ul>
 * Behaviors are immutable; the simulator holds no timers — the harness drives every deferral by advancing the
 * shared {@code FakeClock}.
 */
public sealed interface ChaosBehavior {

    /**
     * @return a behavior that acknowledges the command successfully.
     */
    static @NotNull ChaosBehavior succeed() {
        return new Succeed();
    }

    /**
     * @param reason the failure reason.
     * @return a behavior that reports an {@code error(ADAPTER, reason)}.
     */
    static @NotNull ChaosBehavior failAdapter(final @NotNull String reason) {
        return new FailAdapter(reason);
    }

    /**
     * @param reason the failure reason.
     * @return a behavior that reports an {@code error(CONNECTION, reason)}.
     */
    static @NotNull ChaosBehavior failConnection(final @NotNull String reason) {
        return new FailConnection(reason);
    }

    /**
     * @return a behavior that stays silent — the command is dropped and never acknowledged.
     */
    static @NotNull ChaosBehavior drop() {
        return new Drop();
    }

    /**
     * @return a behavior that records the command but never acknowledges it.
     */
    static @NotNull ChaosBehavior noResponse() {
        return new NoResponse();
    }

    /**
     * @param ticks the number of harness ticks to wait before applying the inner behavior.
     * @param then  the behavior to apply once the delay elapses.
     * @return a behavior that defers {@code then} by {@code ticks} harness ticks.
     */
    static @NotNull ChaosBehavior delay(final int ticks, final @NotNull ChaosBehavior then) {
        return new Delay(ticks, then);
    }

    /**
     * Acknowledge the command successfully.
     */
    record Succeed() implements ChaosBehavior {}

    /**
     * Report an adapter-scoped failure.
     *
     * @param reason the failure reason.
     */
    record FailAdapter(@NotNull String reason) implements ChaosBehavior {}

    /**
     * Report a connection-scoped failure.
     *
     * @param reason the failure reason.
     */
    record FailConnection(@NotNull String reason) implements ChaosBehavior {}

    /**
     * Drop the command silently — no acknowledgment, so the wrapper's watchdog eventually fires.
     */
    record Drop() implements ChaosBehavior {}

    /**
     * Record the command but never acknowledge it — like {@link Drop}, it parks the wrapper.
     */
    record NoResponse() implements ChaosBehavior {}

    /**
     * Defer the inner behavior by a number of harness ticks.
     *
     * @param ticks the number of harness ticks to wait.
     * @param then  the behavior applied once the delay elapses.
     */
    record Delay(int ticks, @NotNull ChaosBehavior then) implements ChaosBehavior {}
}
