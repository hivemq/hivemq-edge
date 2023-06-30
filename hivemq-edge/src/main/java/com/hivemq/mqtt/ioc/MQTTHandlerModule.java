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
package com.hivemq.mqtt.ioc;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.handler.connack.MqttConnacker;
import com.hivemq.mqtt.handler.connack.MqttConnackerImpl;
import com.hivemq.mqtt.handler.disconnect.MqttServerDisconnector;
import com.hivemq.mqtt.handler.disconnect.MqttServerDisconnectorImpl;
import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;

import javax.inject.Singleton;

import static com.hivemq.configuration.service.InternalConfigurations.MQTT_EVENT_EXECUTOR_THREAD_COUNT;

@Module
public abstract class MQTTHandlerModule {

    @Provides
    @Singleton
    static @NotNull EventExecutorGroup eventExecutorGroup() {
        return new DefaultEventExecutorGroup(MQTT_EVENT_EXECUTOR_THREAD_COUNT.get(),
                new ThreadFactoryBuilder().setNameFormat("hivemq-event-executor-%d").build());
    }

    @Binds
    abstract @NotNull MqttServerDisconnector mqttServerDisconnector(@NotNull MqttServerDisconnectorImpl mqttServerDisconnector);

    @Binds
    abstract @NotNull MqttConnacker mqttConnacker(@NotNull MqttConnackerImpl mqttConnacker);

}
