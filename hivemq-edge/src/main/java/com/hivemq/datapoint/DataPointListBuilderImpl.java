package com.hivemq.datapoint;

import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.datapoint.DataPointBuilder;
import com.hivemq.adapter.sdk.api.datapoint.DataPointListBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class DataPointListBuilderImpl implements DataPointListBuilder {
    private final @NotNull Consumer<DataPointBuilder<?>> enricher;
    private final @NotNull Consumer<List<DataPoint>> receiver;
    private final @NotNull List<DataPointBuilder<DataPointListBuilder>> points = new ArrayList<>();

    public DataPointListBuilderImpl(
            @NotNull final Consumer<DataPointBuilder<?>> enricher,
            @NotNull final Consumer<List<DataPoint>> receiver) {
        this.enricher = enricher;
        this.receiver = receiver;
    }

    @Override
    public @NotNull DataPointBuilder<DataPointListBuilder> dataPoint(final @NotNull String tagName) {
        return DataPointWithMetadata.builder(tagName, System.currentTimeMillis(), dp -> {
            points.add(dp);
            return this;
        });
    }

    @Override
    public void send() {
        final var converted = points.stream()
                .map(point -> {
                    enricher.accept(point);
                    return ((DataPointWithMetadata.DataPointBuilderImpl<?>) point).build();
                })
                .toList();
        receiver.accept(converted);
    }
}
