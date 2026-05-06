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

import com.hivemq.configuration.service.ConfigurationService;
import com.hivemq.edge.integration.api.message.MessagePublisher;
import com.hivemq.extension.sdk.api.packets.general.Qos;
import com.hivemq.extension.sdk.api.services.builder.PublishBuilder;
import com.hivemq.extension.sdk.api.services.publish.PublishService;
import com.hivemq.extensions.services.builder.PublishBuilderImpl;
import jakarta.inject.Inject;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import org.jetbrains.annotations.NotNull;

public final class MessagePublisherImpl implements MessagePublisher {

    private final @NotNull PublishService publishService;
    private final @NotNull ConfigurationService configurationService;

    @Inject
    public MessagePublisherImpl(
            final @NotNull PublishService publishService, final @NotNull ConfigurationService configurationService) {
        this.publishService = publishService;
        this.configurationService = configurationService;
    }

    @Override
    public @NotNull OutgoingMessageBuilder newMessage(final @NotNull String topic, final byte @NotNull [] payload) {
        final PublishBuilder builder = new PublishBuilderImpl(configurationService)
                .qos(Qos.AT_LEAST_ONCE)
                .topic(topic)
                .payload(ByteBuffer.wrap(payload));
        return new BuilderImpl(builder);
    }

    private final class BuilderImpl implements OutgoingMessageBuilder {

        private final @NotNull PublishBuilder publishBuilder;

        private BuilderImpl(final @NotNull PublishBuilder publishBuilder) {
            this.publishBuilder = publishBuilder;
        }

        @Override
        public @NotNull OutgoingMessageBuilder addUserProperty(
                final @NotNull String name, final @NotNull String value) {
            publishBuilder.userProperty(name, value);
            return this;
        }

        @Override
        public @NotNull CompletableFuture<Void> publish() {
            return publishService.publish(publishBuilder.build());
        }
    }
}
