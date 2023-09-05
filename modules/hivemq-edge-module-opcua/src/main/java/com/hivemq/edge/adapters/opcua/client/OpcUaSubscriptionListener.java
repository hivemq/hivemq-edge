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
package com.hivemq.edge.adapters.opcua.client;

import com.hivemq.edge.modules.adapters.metrics.ProtocolAdapterMetricsHelper;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaSubscription;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaSubscriptionManager;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;

import java.util.function.Consumer;

public class OpcUaSubscriptionListener implements UaSubscriptionManager.SubscriptionListener {

    private final @NotNull ProtocolAdapterMetricsHelper protocolAdapterMetricsHelper;
    private final @NotNull String adapterId;
    private final @NotNull Consumer<UaSubscription> recreateSubscriptionsCallback;

    public OpcUaSubscriptionListener(
            final @NotNull ProtocolAdapterMetricsHelper protocolAdapterMetricsHelper,
            final @NotNull String adapterId,
            final @NotNull Consumer<UaSubscription> recreateSubscriptionsCallback) {
        this.protocolAdapterMetricsHelper = protocolAdapterMetricsHelper;
        this.adapterId = adapterId;
        this.recreateSubscriptionsCallback = recreateSubscriptionsCallback;
    }

    @Override
    public void onKeepAlive(final @NotNull UaSubscription subscription, final @NotNull DateTime publishTime) {
        UaSubscriptionManager.SubscriptionListener.super.onKeepAlive(subscription, publishTime);
        protocolAdapterMetricsHelper.increment("subscription.keepalive.count");
    }

    @Override
    public void onSubscriptionTransferFailed(
            final @NotNull UaSubscription subscription, final @NotNull StatusCode statusCode) {
        protocolAdapterMetricsHelper.increment("subscription.transfer.failed.count");
        recreateSubscriptionsCallback.accept(subscription);
    }
}
