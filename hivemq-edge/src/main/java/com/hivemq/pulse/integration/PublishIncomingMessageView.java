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
package com.hivemq.pulse.integration;

import com.hivemq.edge.pulse.integration.api.IncomingMessage;
import com.hivemq.mqtt.message.mqtt5.MqttUserProperty;
import com.hivemq.mqtt.message.publish.PUBLISH;
import java.util.List;
import org.jetbrains.annotations.NotNull;

final class PublishIncomingMessageView implements IncomingMessage {

    private static final byte @NotNull [] EMPTY_PAYLOAD = new byte[0];

    private final @NotNull PUBLISH delegate;

    PublishIncomingMessageView(final @NotNull PUBLISH delegate) {
        this.delegate = delegate;
    }

    @Override
    public @NotNull String topic() {
        return delegate.getTopic();
    }

    @Override
    public byte @NotNull [] payload() {
        final byte[] payload = delegate.getPayload();
        return payload != null ? payload : EMPTY_PAYLOAD;
    }

    @Override
    public @NotNull String uniqueId() {
        return delegate.getUniqueId();
    }

    @Override
    public long timestamp() {
        return delegate.getTimestamp();
    }

    @Override
    public @NotNull List<UserProperty> userProperties() {
        return delegate.getUserProperties().asList().stream()
                .map(p -> (UserProperty) new MqttUserPropertyView(p))
                .toList();
    }

    private record MqttUserPropertyView(@NotNull MqttUserProperty delegate) implements UserProperty {

        @Override
        public @NotNull String name() {
            return delegate.getName();
        }

        @Override
        public @NotNull String value() {
            return delegate.getValue();
        }
    }
}
