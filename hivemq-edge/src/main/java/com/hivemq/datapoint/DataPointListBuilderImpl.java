package com.hivemq.datapoint;

import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.datapoint.DataPointBuilder;
import com.hivemq.adapter.sdk.api.datapoint.DataPointListBuilder;
import com.hivemq.adapter.sdk.api.tag.Tag;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public final class DataPointListBuilderImpl implements DataPointListBuilder {
    private final @NotNull Consumer<DataPointBuilder<?>> enricher;
    private final @NotNull Consumer<List<DataPoint>> receiver;
    private final @NotNull List<DataPointWithMetadata.DataPointBuilderImpl<DataPointListBuilder>> builders = new ArrayList<>();

    public DataPointListBuilderImpl(
            @NotNull final Consumer<DataPointBuilder<?>> enricher,
            @NotNull final Consumer<List<DataPoint>> receiver) {
        this.enricher = enricher;
        this.receiver = receiver;
    }

    @Override
    public @NotNull DataPointBuilder<DataPointListBuilder> addDataPoint(final @NotNull Tag tag) {
        final Function<DataPointBuilder<DataPointListBuilder>, DataPointListBuilder> completer = dp -> this;
        final var builderImpl = (DataPointWithMetadata.DataPointBuilderImpl<DataPointListBuilder>)
                DataPointWithMetadata.builder(tag, System.currentTimeMillis(), completer);
        builders.add(builderImpl);
        return builderImpl;
    }

    @Override
    public void publish() {
        final var converted = builders.stream()
                .<DataPoint>map(builder -> {
                    enricher.accept(builder);
                    return builder.build();
                })
                .toList();
        receiver.accept(converted);
    }
}
