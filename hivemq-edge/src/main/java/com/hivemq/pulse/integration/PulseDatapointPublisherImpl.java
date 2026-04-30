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
import com.hivemq.edge.pulse.integration.api.PulseDatapointPublisher;
import com.hivemq.extension.sdk.api.packets.general.Qos;
import com.hivemq.extension.sdk.api.services.builder.PublishBuilder;
import com.hivemq.extension.sdk.api.services.publish.PublishService;
import com.hivemq.extensions.services.builder.PublishBuilderImpl;
import jakarta.inject.Inject;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import org.jetbrains.annotations.NotNull;

public final class PulseDatapointPublisherImpl implements PulseDatapointPublisher {

    private final @NotNull PublishService publishService;
    private final @NotNull ConfigurationService configurationService;

    @Inject
    public PulseDatapointPublisherImpl(
            final @NotNull PublishService publishService, final @NotNull ConfigurationService configurationService) {
        this.publishService = publishService;
        this.configurationService = configurationService;
    }

    @Override
    public @NotNull CompletableFuture<Void> publish(final @NotNull OutgoingDatapoint datapoint) {
        final PublishBuilder builder = new PublishBuilderImpl(configurationService)
                .qos(Qos.AT_LEAST_ONCE)
                .topic(datapoint.topic())
                .payload(ByteBuffer.wrap(datapoint.payload()));
        for (final UserProperty property : datapoint.userProperties()) {
            builder.userProperty(property.name(), property.value());
        }
        return publishService.publish(builder.build());
    }
}
