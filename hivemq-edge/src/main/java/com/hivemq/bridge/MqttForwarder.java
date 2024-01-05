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
package com.hivemq.bridge;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.publish.PUBLISH;

import java.util.List;
import java.util.concurrent.ExecutorService;

public interface MqttForwarder {

    int getInflightCount();

    void onMessage(@NotNull PUBLISH publish, @NotNull String queueId);

    void start();

    void stop();

    void drainQueue();

    void setAfterForwardCallback(@NotNull MqttForwarder.AfterForwardCallback callback);

    void setResetInflightMarkerCallback(@NotNull MqttForwarder.ResetInflightMarkerCallback callback);


    void setExecutorService(@NotNull ExecutorService executorService);

    @NotNull String getId();

    @NotNull List<String> getTopics();

    @FunctionalInterface
    interface AfterForwardCallback {
        void afterMessage(@NotNull QoS qos, @NotNull String uniqueId, @NotNull String queueId, boolean cancelled);
    }

    @FunctionalInterface
    interface ResetInflightMarkerCallback {
        void afterMessage(@NotNull String sharedSubscription, @NotNull String uniqueId);
    }
}
