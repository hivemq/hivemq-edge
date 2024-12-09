/*
 * Copyright 2023-present HiveMQ GmbH
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
package com.hivemq.edge.adapters.file.payload;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.adapter.sdk.api.config.PollingContext;
import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.data.JsonPayloadCreator;
import com.hivemq.adapter.sdk.api.data.ProtocolAdapterDataSample;
import com.hivemq.edge.adapters.file.FilePollingProtocolAdapter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class FileJsonPayloadCreator implements JsonPayloadCreator {

    private static final @NotNull Logger LOG = LoggerFactory.getLogger(FilePollingProtocolAdapter.class);
    public static final FileJsonPayloadCreator INSTANCE = new FileJsonPayloadCreator();

    @Override
    public @NotNull List<byte[]> convertToJson(
            final @NotNull ProtocolAdapterDataSample sample, final @NotNull ObjectMapper objectMapper) {
        final List<byte[]> payloads = new ArrayList<>();
        for (final DataPoint dataPoint : sample.getDataPoints()) {
            try {

                final PollingContext pollingContext = sample.getPollingContext();

                final FileDataPoint fileDataPoint = (FileDataPoint) dataPoint;
                final FilePayload value = new FilePayload(pollingContext.getUserProperties(),
                        dataPoint.getTagValue(),
                        fileDataPoint.getTag().getDefinition().getContentType(),
                        pollingContext.getIncludeTagNames() ? dataPoint.getTagName() : null,
                        pollingContext.getIncludeTimestamp() ? System.currentTimeMillis() : null);


                payloads.add(objectMapper.writeValueAsBytes(value));
            } catch (final JsonProcessingException e) {
                LOG.warn("Unable to create payload for data data point '{}'. Skipping this data point.", dataPoint);
            }
        }
        return payloads;
    }
}
