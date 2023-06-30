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
package com.hivemq.edge.modules.api.adapters;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.handler.publish.PublishReturnCode;

import java.util.concurrent.CompletableFuture;

public interface ProtocolAdapterPublishBuilder {

    @NotNull ProtocolAdapterPublishBuilder withTopic(@NotNull String mqttTopic);

    @NotNull ProtocolAdapterPublishBuilder withPayload(@NotNull byte[] payload);

    @NotNull ProtocolAdapterPublishBuilder withQoS(@NotNull int qos);

    @NotNull ProtocolAdapterPublishBuilder withMessageExpiryInterval(long messageExpiryInterval);

    @NotNull ProtocolAdapterPublishBuilder withUserProperty(@NotNull String name, @NotNull String value);

    @NotNull ProtocolAdapterPublishBuilder withRetain(boolean retained);

    @NotNull ProtocolAdapterPublishBuilder withContextInformation(@NotNull String key, @NotNull String value);

    @NotNull CompletableFuture<PublishReturnCode> send();

}
