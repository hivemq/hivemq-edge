/*
 * Copyright 2023-present HiveMQ GmbH
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
package com.hivemq.edge.adapters.opcua;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import org.jetbrains.annotations.NotNull;

/**
 * The ownership answers a test can give an {@link OpcUaClientConnection} in place of an adapter's.
 * <p>
 * In production the answer is {@code opcUaClientConnection.get() == connection}, so a connection may report a
 * status only while it is the one the adapter holds. A test that builds a connection directly has no adapter
 * to ask, and has to say which side of that question it is on.
 * <p>
 * Named rather than written inline as {@code connection -> true}, because the two cases are the finding: a
 * connection is entitled to describe the adapter, or it is a superseded one that is not. Spelling the choice
 * out at each call site is what makes it visible that most tests are deliberately in the first case and are
 * about something else entirely.
 */
final class ConnectionOwnership {

    private ConnectionOwnership() {}

    /**
     * The connection is the adapter's current one, as it is for every ordinary start.
     * <p>
     * What tests unconcerned with supersession want: their connection is the only one, so every status it
     * reports is its to report.
     */
    static @NotNull Predicate<OpcUaClientConnection> alwaysCurrent() {
        return connection -> true;
    }

    /**
     * The connection has been superseded, as it is once the adapter has moved on to another attempt.
     * <p>
     * The state a teardown of an old connection runs in after {@code destroy()} has released the slot.
     */
    static @NotNull Predicate<OpcUaClientConnection> neverCurrent() {
        return connection -> false;
    }

    /**
     * Ownership a test can revoke part way through, which is what the adapter does when it replaces a
     * connection while that connection is still running.
     */
    static @NotNull Predicate<OpcUaClientConnection> currentUntilRevoked(final @NotNull AtomicBoolean owned) {
        return connection -> owned.get();
    }
}
