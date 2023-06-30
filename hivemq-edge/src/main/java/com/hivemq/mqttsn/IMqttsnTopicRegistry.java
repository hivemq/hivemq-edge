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
package com.hivemq.mqttsn;

import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.util.Optional;

/**
 * @author Simon L Johnson
 */
public interface IMqttsnTopicRegistry {

    String readTopicName(@NotNull String clientId, int topicIdType, byte[] topicData, boolean readNormalAsFullTopic) throws MqttsnProtocolException ;

    Optional<MqttsnTopicAlias> readTopicAlias(@NotNull String clientId, @NotNull String topicName) throws MqttsnProtocolException ;

    int register(@NotNull String clientId, @NotNull String topicName) throws MqttsnProtocolException ;

    void clearSessionAliases(@NotNull String clientId);
}
