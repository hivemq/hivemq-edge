package com.hivemq.edge.adapters.file.payload;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.data.JsonPayloadCreator;
import com.hivemq.adapter.sdk.api.data.ProtocolAdapterDataSample;
import com.hivemq.edge.adapters.file.FilePollingProtocolAdapter;
import com.hivemq.edge.adapters.file.config.FilePollingContext;
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
            @NotNull final ProtocolAdapterDataSample sample, final @NotNull ObjectMapper objectMapper) {
        List<byte[]> payloads = new ArrayList<>();
        for (DataPoint dataPoint : sample.getDataPoints()) {
            try {
                final FilePayload value = new FilePayload(System.currentTimeMillis(),
                        sample.getPollingContext().getUserProperties(),
                        dataPoint.getTagValue(),
                        dataPoint.getTagName(),
                        ((FilePollingContext) sample.getPollingContext()).getContentType());

                payloads.add(objectMapper.writeValueAsBytes(value));
            } catch (JsonProcessingException e) {
                LOG.warn("Unable to create payload for data data point '{}'. Skipping this data point.", dataPoint);
            }
        }
        return payloads;
    }
}
