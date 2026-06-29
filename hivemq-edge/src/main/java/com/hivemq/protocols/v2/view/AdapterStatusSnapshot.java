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

import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperState;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The immutable status snapshot the {@link com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapper} publishes
 * after every message — the only state that crosses the actor boundary outward. It is published by
 * the actor on its own dispatch thread into an {@code AtomicReference}; REST threads read it without locking, and
 * every read-side view is a pure function of it.
 *
 * @param adapterId              the adapter instance id.
 * @param machineState           the adapter machine's current state.
 * @param northboundActivated    whether the read side is activated (the live goal).
 * @param southboundActivated    whether the write side is activated (the live goal).
 * @param tags                   the per-tag status snapshots.
 * @param lastTransitionAtMillis the clock time of the last machine transition, in milliseconds.
 * @param lastErrorReason        the most recent error reason, or {@code null} if none.
 */
public record AdapterStatusSnapshot(
        @NotNull String adapterId,
        @NotNull ProtocolAdapterWrapperState machineState,
        boolean northboundActivated,
        boolean southboundActivated,
        @NotNull List<TagStatusSnapshot> tags,
        long lastTransitionAtMillis,
        @Nullable String lastErrorReason) {}
