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
package com.hivemq.protocols.v2.wrapper;

import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * A {@link ProtocolAdapterWrapperEventListener} that records each notification, so a test can assert that the supervisor was
 * told about a start, stop, or error.
 */
final class RecordingProtocolAdapterWrapperEventListener implements ProtocolAdapterWrapperEventListener {

    final @NotNull List<String> started = new ArrayList<>();
    final @NotNull List<String> stopped = new ArrayList<>();
    final @NotNull List<String> errorReasons = new ArrayList<>();

    @Override
    public void wrapperStarted(final @NotNull String adapterId) {
        started.add(adapterId);
    }

    @Override
    public void wrapperStopped(final @NotNull String adapterId) {
        stopped.add(adapterId);
    }

    @Override
    public void wrapperError(final @NotNull String adapterId, final @NotNull String reason) {
        errorReasons.add(reason);
    }
}
