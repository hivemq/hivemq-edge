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

import org.jetbrains.annotations.NotNull;
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

    /**
     * Sends any messages that were buffered in-memory while the remote broker was disconnected.
     * Unlike {@link #drainQueue()}, this does NOT reset inflight markers in persistence -
     * it only sends messages that are already in the local queue buffer.
     * <p>
     * This should be called on initial connection when messages may have been buffered
     * while waiting for the remote broker to become available.
     */
    void flushBufferedMessages();

    void setAfterForwardCallback(@NotNull MqttForwarder.AfterForwardCallback callback);

    void setResetInflightMarkerCallback(@NotNull MqttForwarder.ResetInflightMarkerCallback callback);

    void setResetAllInflightMarkersCallback(@NotNull MqttForwarder.ResetAllInflightMarkersCallback callback);

    void setOnReconnectCallback(@NotNull MqttForwarder.OnReconnectCallback callback);

    /**
     * Called after the remote client reconnects. This triggers the reconnect callback
     * which should poll from the persistence queue for any messages that need to be retried.
     */
    void onReconnect();

    /**
     * Forces a reconnection by disconnecting the underlying MQTT client.
     * The auto-reconnect logic will then establish a new connection.
     * <p>
     * This is used to recover from timeout situations where inflight markers
     * could not be reset within the expected time. Forcing a reconnection
     * triggers a new {@link #drainQueue()} cycle which will retry the marker reset.
     */
    void forceReconnect();

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

    @FunctionalInterface
    interface ResetAllInflightMarkersCallback {
        void resetAll(@NotNull String sharedSubscription);
    }

    @FunctionalInterface
    interface OnReconnectCallback {
        void onReconnect();
    }
}
