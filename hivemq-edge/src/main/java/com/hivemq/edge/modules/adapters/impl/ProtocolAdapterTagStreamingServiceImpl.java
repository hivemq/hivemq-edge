package com.hivemq.edge.modules.adapters.impl;

import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.datapoint.DataPointBuilder;
import com.hivemq.adapter.sdk.api.datapoint.DataPointListBuilder;
import com.hivemq.adapter.sdk.api.streaming.ProtocolAdapterTagStreamingService;
import com.hivemq.datapoint.DataPointListBuilderImpl;
import com.hivemq.edge.modules.adapters.data.TagManager;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Consumer;

public class ProtocolAdapterTagStreamingServiceImpl implements ProtocolAdapterTagStreamingService {

    private final @NotNull TagManager tagManager;
    private final @NotNull Consumer<DataPointBuilder<?>> enricher;

    public ProtocolAdapterTagStreamingServiceImpl(
            @NotNull final TagManager tagManager,
            @NotNull final Consumer<DataPointBuilder<?>> enricher) {
        this.tagManager = tagManager;
        this.enricher = enricher;
    }

    @Override
    public @NotNull DataPointListBuilder dataPointsPublisher() {
        return new DataPointListBuilderImpl(
                enricher, tagManager::feed);
    }

    @Override
    public void feed(@NotNull final String tag, @NotNull final List<DataPoint> dataPoints) {
        tagManager.feed(tag, dataPoints);
    }
}
