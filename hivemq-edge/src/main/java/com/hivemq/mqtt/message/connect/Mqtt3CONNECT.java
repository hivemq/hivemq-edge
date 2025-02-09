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
package com.hivemq.mqtt.message.connect;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.hivemq.mqtt.message.Message;
import com.hivemq.mqtt.message.ProtocolVersion;

/**
 * @author Florian Limpöck
 */
public interface Mqtt3CONNECT extends Message {

    @NotNull ProtocolVersion getProtocolVersion();

    @NotNull String getClientIdentifier();

    int getKeepAlive();

    @Nullable String getUsername();

    byte @Nullable [] getPassword();

    @Nullable String getPasswordAsUTF8String();

    @Nullable MqttWillPublish getWillPublish();
}
