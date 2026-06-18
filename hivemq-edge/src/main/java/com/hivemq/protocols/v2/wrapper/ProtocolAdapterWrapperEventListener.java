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

import org.jetbrains.annotations.NotNull;

/**
 * How the wrapper notifies its supervisor of health transitions (design §6.1, §6.4, §8.3) — the seam to the
 * manager (a later task). The wrapper tells its supervisor when it has started, stopped, or entered {@code ERROR};
 * the manager records the health and, in this project, performs no automatic recreate (manual recovery, design
 * §8.3).
 * <p>
 * Defined as a small listener so this task carries no dependency on the manager package. A later task supplies an
 * implementation that tells the corresponding manager message; tests supply a recording implementation.
 * <p>
 * Every method runs on the wrapper's dispatch thread.
 */
public interface ProtocolAdapterWrapperEventListener {

    /**
     * A listener that ignores every notification — the default when no supervisor is attached.
     */
    @NotNull
    ProtocolAdapterWrapperEventListener NONE = new ProtocolAdapterWrapperEventListener() {
        @Override
        public void wrapperStarted(final @NotNull String adapterId) {}

        @Override
        public void wrapperStopped(final @NotNull String adapterId) {}

        @Override
        public void wrapperError(final @NotNull String adapterId, final @NotNull String reason) {}
    };

    /**
     * The adapter reached {@code CONNECTED}.
     *
     * @param adapterId the adapter instance id.
     */
    void wrapperStarted(@NotNull String adapterId);

    /**
     * The adapter reached {@code STOPPED}.
     *
     * @param adapterId the adapter instance id.
     */
    void wrapperStopped(@NotNull String adapterId);

    /**
     * The adapter entered {@code ERROR}.
     *
     * @param adapterId the adapter instance id.
     * @param reason    a human-readable description of why.
     */
    void wrapperError(@NotNull String adapterId, @NotNull String reason);
}
