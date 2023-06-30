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
package com.hivemq.bridge.mqtt;

import com.google.common.util.concurrent.ListenableFuture;
import com.hivemq.bridge.config.MqttBridge;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.mqtt.handler.publish.PublishReturnCode;
import com.hivemq.mqtt.message.publish.PUBLISH;

import java.util.concurrent.ExecutorService;

public interface BridgeInterceptorHandler {

    @NotNull ListenableFuture<PublishReturnCode> interceptOrDelegateInbound(
            @NotNull PUBLISH publish, @NotNull ExecutorService executorService, @NotNull MqttBridge bridge);

    public @NotNull ListenableFuture<InterceptorResult> interceptOrDelegateOutbound(
            final @NotNull PUBLISH publish,
            final @NotNull ExecutorService executorService,
            final @NotNull MqttBridge bridge);

    class InterceptorResult {
        private final @NotNull InterceptorOutcome outcome;
        private final @Nullable PUBLISH publish;

        public InterceptorResult(@NotNull final InterceptorOutcome outcome, @Nullable final PUBLISH publish) {
            this.outcome = outcome;
            this.publish = publish;
        }

        public @NotNull InterceptorOutcome getOutcome() {
            return outcome;
        }

        public @Nullable PUBLISH getPublish() {
            return publish;
        }
    }

    enum InterceptorOutcome {
        SUCCESS,
        DROP
    }
}
