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
package com.hivemq.mqtt.services;

import com.google.common.util.concurrent.ListenableFuture;
import com.hivemq.api.mqtt.PublishReturnCode;
import com.hivemq.mqtt.handler.publish.PublishingResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.hivemq.mqtt.message.publish.PUBLISH;

import java.util.concurrent.ExecutorService;

public interface InternalPublishService {

    /**
     * Send a message to all clients and shared subscription groups which have an active subscription
     *
     * @param publish         the message to send
     * @param executorService the executor service in which all callbacks are executed
     * @param sender          client identifier of the client which sent the message
     */
    @NotNull ListenableFuture<PublishingResult> publish(
            final @NotNull PUBLISH publish,
            final @NotNull ExecutorService executorService,
            final @Nullable String sender);

}
