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
import org.jetbrains.annotations.NotNull;

/**
 * Publishes messages produced by the Pulse Agent integration back to the HiveMQ Edge broker.
 *
 * <p>Provided by HiveMQ Edge to the Pulse Agent integration; never implemented by integration code.
 */
public interface PulseMessagePublisher {

    /**
     * Starts building a message for the given topic and payload. Add user properties (if any) and call
     * {@link OutgoingMessageBuilder#publish()} to send it.
     */
    @NotNull
    OutgoingMessageBuilder newMessage(@NotNull String topic, byte @NotNull [] payload);

    interface OutgoingMessageBuilder {

        @NotNull
        OutgoingMessageBuilder addUserProperty(@NotNull String name, @NotNull String value);

        @NotNull
        CompletableFuture<Void> publish();
    }
}
