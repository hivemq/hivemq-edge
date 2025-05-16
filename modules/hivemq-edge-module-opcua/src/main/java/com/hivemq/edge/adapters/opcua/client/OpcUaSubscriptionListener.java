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

import com.hivemq.adapter.sdk.api.services.ProtocolAdapterMetricsService;
import org.eclipse.milo.opcua.sdk.client.subscriptions.OpcUaSubscription;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class OpcUaSubscriptionListener implements OpcUaSubscription.SubscriptionListener {

    private final @NotNull ProtocolAdapterMetricsService protocolAdapterMetricsService;
    private final @NotNull Consumer<OpcUaSubscription> recreateSubscriptionsCallback;

    public OpcUaSubscriptionListener(
            final @NotNull ProtocolAdapterMetricsService protocolAdapterMetricsService,
            final @NotNull Consumer<OpcUaSubscription> recreateSubscriptionsCallback) {
        this.protocolAdapterMetricsService = protocolAdapterMetricsService;
        this.recreateSubscriptionsCallback = recreateSubscriptionsCallback;
    }

    @Override
    public void onKeepAliveReceived(final OpcUaSubscription subscription) {
        protocolAdapterMetricsService.increment("subscription.keepalive.count");
        OpcUaSubscription.SubscriptionListener.super.onKeepAliveReceived(subscription);
    }

    @Override
    public void onTransferFailed(final OpcUaSubscription subscription, final StatusCode status) {
        protocolAdapterMetricsService.increment("subscription.transfer.failed.count");
        recreateSubscriptionsCallback.accept(subscription);
    }
}
