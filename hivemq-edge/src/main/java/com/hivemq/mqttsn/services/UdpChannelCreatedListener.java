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
package com.hivemq.mqttsn.services;

import com.hivemq.bootstrap.netty.BroadcastChannelCreatedListener;
import org.jetbrains.annotations.NotNull;
import io.netty.channel.Channel;

import jakarta.inject.Inject;

/**
 * @author Simon L Johnson
 */
public class UdpChannelCreatedListener implements BroadcastChannelCreatedListener {

    private @NotNull IGatewayBroadcastService gatewayBroadcastService;

    @Inject
    public UdpChannelCreatedListener(final @NotNull IGatewayBroadcastService gatewayBroadcastService) {
        this.gatewayBroadcastService = gatewayBroadcastService;
    }

    @Override
    public void notifyChannelCreated(final Channel channel) {
        if(gatewayBroadcastService.needsStarting()){
            gatewayBroadcastService.startBroadcast(channel);
        }
    }
}
