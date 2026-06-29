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
package com.hivemq.protocols.v2.view;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The immutable per-tag status the wrapper publishes for readers. Part of an
 * {@link AdapterStatusSnapshot}; produced by the actor on its own dispatch thread, read by REST threads. All
 * read-side views ({@link TagStatus} fold, mapping status — a later task) are pure functions of these fields; the
 * per-aspect booleans carry exactly what the five-value {@link TagStatus} fold needs, so the fold
 * stays a pure function of one snapshot.
 *
 * @param tagName                     the tag name.
 * @param readActivated               the persisted read-aspect activation preference.
 * @param writeActivated              the persisted write-aspect activation preference.
 * @param readUsed                    whether a northbound mapping consumes the tag.
 * @param writeUsed                   whether a southbound mapping produces to the tag.
 * @param readAspectStateName         the read aspect's current state name.
 * @param writeAspectStateName        the write aspect's current state name.
 * @param readAspectGoalActive        whether the read aspect's three-condition goal is active.
 * @param writeAspectGoalActive       whether the write aspect's three-condition goal is active.
 * @param readAspectOperating         whether the read aspect is operating (polling, subscribed, or mid round-trip).
 * @param writeAspectOperating        whether the write aspect is operating (ready for or performing a write).
 * @param readAspectPermanentFailure  whether the read aspect is permanently failed.
 * @param writeAspectPermanentFailure whether the write aspect is permanently failed.
 * @param failureCount                the cumulative failure count across both aspects.
 * @param lastFailureReason           the most recent failure reason, or {@code null} if none.
 * @param lastTransitionAtMillis      the clock time of the last aspect transition, in milliseconds.
 */
public record TagStatusSnapshot(
        @NotNull String tagName,
        boolean readActivated,
        boolean writeActivated,
        boolean readUsed,
        boolean writeUsed,
        @NotNull String readAspectStateName,
        @NotNull String writeAspectStateName,
        boolean readAspectGoalActive,
        boolean writeAspectGoalActive,
        boolean readAspectOperating,
        boolean writeAspectOperating,
        boolean readAspectPermanentFailure,
        boolean writeAspectPermanentFailure,
        int failureCount,
        @Nullable String lastFailureReason,
        long lastTransitionAtMillis) {}
