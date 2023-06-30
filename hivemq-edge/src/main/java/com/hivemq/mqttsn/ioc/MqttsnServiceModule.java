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
package com.hivemq.mqttsn.ioc;

import com.hivemq.bootstrap.netty.BroadcastChannelCreatedListener;
import com.hivemq.mqttsn.IMqttsnTopicRegistry;
import com.hivemq.mqttsn.MqttsnTopicRegistry;
import com.hivemq.mqttsn.services.GatewayBroadcastServiceImpl;
import com.hivemq.mqttsn.services.IGatewayBroadcastService;
import com.hivemq.mqttsn.services.UdpChannelCreatedListener;
import dagger.Binds;
import dagger.Module;

/**
 * Services related to the functioning of the MQTTSN aspects of the system
 *
 * @author Simon L Johnson
 */
@Module
public interface MqttsnServiceModule {

    @Binds
    IMqttsnTopicRegistry mqttsnTopicRegistry(MqttsnTopicRegistry mqttsnTopicRegistry);

    @Binds
    IGatewayBroadcastService gatewayBroadcastService(GatewayBroadcastServiceImpl gatewayBroadcastService);

    @Binds
    BroadcastChannelCreatedListener broadcastChannelCreatedListener(UdpChannelCreatedListener udpChannelCreatedListener);

}
