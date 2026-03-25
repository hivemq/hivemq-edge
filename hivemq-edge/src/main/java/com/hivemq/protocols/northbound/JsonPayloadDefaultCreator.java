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
package com.hivemq.protocols.northbound;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.adapter.sdk.api.config.PollingContext;
import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.data.JsonPayloadCreator;
import com.hivemq.adapter.sdk.api.exceptions.ProtocolAdapterException;
import com.hivemq.edge.modules.adapters.data.AbstractProtocolAdapterJsonPayload;
import com.hivemq.edge.modules.adapters.data.ProtocolAdapterPublisherJsonPayload;
import com.hivemq.edge.modules.adapters.data.TagSample;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Singleton
/*
  @deprecated no longer needed. To be removed in 2026.10
 */
@ApiStatus.ScheduledForRemoval(inVersion = "2026.10")
@Deprecated
public class JsonPayloadDefaultCreator implements JsonPayloadCreator {

    @Inject
    JsonPayloadDefaultCreator() {}

    @Override
    public @NotNull List<byte[]> convertToJson(
            final @NotNull List<DataPoint> dataPoints,
            final @NotNull PollingContext pollingContext,
            final @NotNull ObjectMapper objectMapper) {
        final List<AbstractProtocolAdapterJsonPayload> payloads =
                convertAdapterSampleToPublishes(dataPoints, pollingContext);
        final List<byte[]> jsonPayloadsAsBytes = new ArrayList<>();
        payloads.forEach(payload -> {
            try {
                jsonPayloadsAsBytes.add(convertToJson(payload, objectMapper));
            } catch (final ProtocolAdapterException e) {
                throw new RuntimeException(e);
            }
        });
        return jsonPayloadsAsBytes;
    }

    public byte @NotNull [] convertToJson(
            final @NotNull AbstractProtocolAdapterJsonPayload data, final @NotNull ObjectMapper objectMapper)
            throws ProtocolAdapterException {
        try {
            return objectMapper.writeValueAsBytes(data);
        } catch (final JsonProcessingException e) {
            throw new ProtocolAdapterException("Error Wrapping Adapter Data", e);
        }
    }

    public @NotNull List<AbstractProtocolAdapterJsonPayload> convertAdapterSampleToPublishes(
            final @NotNull List<DataPoint> dataPoints, final PollingContext pollingContext) {
        final List<AbstractProtocolAdapterJsonPayload> list = new ArrayList<>();
        // -- Only include the timestamp if the settings say so
        final Long timestamp = pollingContext.getIncludeTimestamp() ? System.currentTimeMillis() : null;
        // -- Put all derived samples into individual publish messages
        dataPoints.stream()
                .map(dp -> createPublishPayload(timestamp, dp, pollingContext.getIncludeTagNames()))
                .map(pp -> decoratePayloadMessage(pp, pollingContext))
                .forEach(list::add);
        return list;
    }

    protected @NotNull ProtocolAdapterPublisherJsonPayload createPublishPayload(
            final @Nullable Long timestamp, @NotNull final DataPoint dataPoint, final boolean includeTagName) {
        return new ProtocolAdapterPublisherJsonPayload(timestamp, createTagSample(dataPoint, includeTagName));
    }

    protected static TagSample createTagSample(final @NotNull DataPoint dataPoint, final boolean includeTagName) {
        return new TagSample(includeTagName ? dataPoint.getTagName() : null, dataPoint.getTagValue());
    }

    protected @NotNull AbstractProtocolAdapterJsonPayload decoratePayloadMessage(
            final @NotNull AbstractProtocolAdapterJsonPayload payload, final @NotNull PollingContext pollingContext) {
        if (!pollingContext.getUserProperties().isEmpty()) {
            payload.setMqttUserProperties(pollingContext.getUserProperties());
        }
        return payload;
    }
}
