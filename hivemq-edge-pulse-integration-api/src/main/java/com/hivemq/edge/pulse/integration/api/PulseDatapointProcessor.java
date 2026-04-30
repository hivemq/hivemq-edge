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
package com.hivemq.edge.pulse.integration.api;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Processes incoming datapoints. Implemented by the Pulse Agent integration and registered with HiveMQ Edge so that
 * each incoming publish is forwarded to the agent before being delivered.
 *
 * <p>The publish is always passed through unchanged — the implementation only observes the datapoint, it does not
 * modify or drop it.
 */
public interface PulseDatapointProcessor {

    /**
     * @param datapoint the incoming datapoint
     * @param sender    the MQTT client id of the sender, or {@code null} if unknown
     * @param executor  an executor the implementation may use for asynchronous work
     * @return a future that completes when processing is finished
     */
    @NotNull
    CompletableFuture<Void> process(
            @NotNull IncomingDatapoint datapoint, @Nullable String sender, @NotNull ExecutorService executor);
}
