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

import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.v2.model.ErrorScope;
import com.hivemq.adapter.sdk.api.v2.node.Node;
import org.jetbrains.annotations.NotNull;

/**
 * An event the {@link ChaosProtocolAdapter} reports spontaneously at a chosen harness tick, via
 * {@link ChaosScript.Builder#injectAtTick(long, ChaosEvent)} (design §10.2). It models the device — or a
 * misbehaving adapter — acting outside any command-response exchange: a spontaneous connection {@link Disconnect}
 * or {@link ErrorReport}, an unsolicited {@link DataPointPush} or {@link NodeErrorPush}, or an out-of-band lifecycle
 * callback ({@link Started}, {@link Stopped}, {@link Connected}) that the wrapper does not expect in its current
 * state — the trigger for a defensive reset and the {@code ERROR} absorption rule (design §6.4).
 */
public sealed interface ChaosEvent {

    /**
     * @return an event that reports {@code output.started()} — unexpected outside the start handshake.
     */
    static @NotNull ChaosEvent started() {
        return new Started();
    }

    /**
     * @return an event that reports {@code output.stopped()}.
     */
    static @NotNull ChaosEvent stopped() {
        return new Stopped();
    }

    /**
     * @return an event that reports {@code output.connected()} — unexpected outside the connect handshake.
     */
    static @NotNull ChaosEvent connected() {
        return new Connected();
    }

    /**
     * @return an event that reports a spontaneous connection loss ({@code output.disconnected()}).
     */
    static @NotNull ChaosEvent disconnect() {
        return new Disconnect();
    }

    /**
     * @param scope  the error scope.
     * @param reason the failure reason.
     * @return an event that reports {@code output.error(scope, reason)}.
     */
    static @NotNull ChaosEvent error(final @NotNull ErrorScope scope, final @NotNull String reason) {
        return new ErrorReport(scope, reason);
    }

    /**
     * @param node  the node the value belongs to.
     * @param value the value to push.
     * @return an event that pushes a value for the node ({@code output.dataPoint(node, value)}).
     */
    static @NotNull ChaosEvent dataPoint(final @NotNull Node node, final @NotNull DataPoint value) {
        return new DataPointPush(node, value);
    }

    /**
     * @param node        the node the failure belongs to.
     * @param reason      the failure reason.
     * @param spontaneous whether the loss is spontaneous (selects the read aspect's recovery path, design §7.4).
     * @return an event that reports a per-node error ({@code output.nodeError(node, reason, spontaneous)}).
     */
    static @NotNull ChaosEvent nodeError(
            final @NotNull Node node, final @NotNull String reason, final boolean spontaneous) {
        return new NodeErrorPush(node, reason, spontaneous);
    }

    /**
     * A spurious {@code started()}.
     */
    record Started() implements ChaosEvent {}

    /**
     * A spurious {@code stopped()}.
     */
    record Stopped() implements ChaosEvent {}

    /**
     * A spurious {@code connected()}.
     */
    record Connected() implements ChaosEvent {}

    /**
     * A spontaneous connection loss.
     */
    record Disconnect() implements ChaosEvent {}

    /**
     * A spontaneously reported error.
     *
     * @param scope  the error scope.
     * @param reason the failure reason.
     */
    record ErrorReport(@NotNull ErrorScope scope, @NotNull String reason) implements ChaosEvent {}

    /**
     * An unsolicited value push.
     *
     * @param node  the node the value belongs to.
     * @param value the pushed value.
     */
    record DataPointPush(@NotNull Node node, @NotNull DataPoint value) implements ChaosEvent {}

    /**
     * A per-node error reported spontaneously.
     *
     * @param node        the node the failure belongs to.
     * @param reason      the failure reason.
     * @param spontaneous whether the loss is spontaneous (design §7.4).
     */
    record NodeErrorPush(@NotNull Node node, @NotNull String reason, boolean spontaneous) implements ChaosEvent {}
}
