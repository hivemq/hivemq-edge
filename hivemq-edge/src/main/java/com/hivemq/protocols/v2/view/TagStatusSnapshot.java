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
 * The immutable per-tag status the wrapper publishes for readers (design §6.6). Part of an
 * {@link AdapterStatusSnapshot}; produced by the actor on its own dispatch thread, read by REST threads. All
 * read-side views ({@link TagStatus}, mapping status — a later task) are pure functions of these fields.
 *
 * @param tagName               the tag name.
 * @param readActivated         the persisted read-aspect activation preference.
 * @param writeActivated        the persisted write-aspect activation preference.
 * @param readUsed              whether a northbound mapping consumes the tag.
 * @param writeUsed             whether a southbound mapping produces to the tag.
 * @param readAspectStateName   the read aspect's current state name.
 * @param writeAspectStateName  the write aspect's current state name.
 * @param failureCount          the cumulative failure count (poll / write / subscription).
 * @param lastFailureReason     the most recent failure reason, or {@code null} if none.
 * @param lastTransitionAtMillis the clock time of the last aspect transition, in milliseconds.
 */
public record TagStatusSnapshot(
        @NotNull String tagName,
        boolean readActivated,
        boolean writeActivated,
        boolean readUsed,
        boolean writeUsed,
        @NotNull String readAspectStateName,
        @NotNull String writeAspectStateName,
        int failureCount,
        @Nullable String lastFailureReason,
        long lastTransitionAtMillis) {}
