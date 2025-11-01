/*
 *  Copyright 2019-present HiveMQ GmbH
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.hivemq.protocols.fsm;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.TimeUnit;

public record ProtocolAdapterTransitionRequest(boolean requestWriteLock, long timeout, TimeUnit timeUnit) {
    public static class Builder {
        private boolean requestWriteLock;
        private long timeout;
        private @Nullable TimeUnit timeUnit;

        public @NotNull Builder requestWriteLock(final boolean requestWriteLock) {
            this.requestWriteLock = requestWriteLock;
            return this;
        }

        public @NotNull Builder timeout(final long timeout) {
            this.timeout = timeout;
            return this;
        }

        public @NotNull Builder timeUnit(final @NotNull TimeUnit timeUnit) {
            this.timeUnit = timeUnit;
            return this;
        }

        public @NotNull ProtocolAdapterTransitionRequest build() {
            return new ProtocolAdapterTransitionRequest(requestWriteLock, timeout, timeUnit);
        }
    }
}
