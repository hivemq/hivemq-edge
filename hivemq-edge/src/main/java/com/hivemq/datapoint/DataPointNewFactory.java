package com.hivemq.datapoint;

import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.datapoint.DataPointBuilder;
import com.hivemq.adapter.sdk.api.datapoint.DataPointListBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Consumer;

public class DataPointNewFactory {

    private final @NotNull Consumer<DataPointBuilder<?>> enricher;
    private final @NotNull Consumer<List<DataPoint>> receiver;

    public DataPointNewFactory(
            @NotNull final Consumer<DataPointBuilder<?>> enricher,
            @NotNull final Consumer<List<DataPoint>> receiver) {
        this.enricher = enricher;
        this.receiver = receiver;
    }

    public @NotNull DataPointListBuilder dataPointListBuilder() {
        return new DataPointListBuilderImpl(enricher, receiver);
    }

    public static void main(final @NotNull String[] args) {
        final var factory = new DataPointNewFactory(r -> {}, t -> {});

        // Multiple data points
        factory
                .dataPointListBuilder()
                .dataPoint("sensor/temperature")
                    .valueStart()
                        .add("temperature", 23.5)
                        .add("unit", "celsius")
                    .valueStop()
                .finish()
                .dataPoint("sensor/humidity")
                    .valueStart()
                        .add("humidity", 65.2)
                        .add("unit", "percent")
                    .valueStop()
                .finish()
                .dataPoint("sensor/pressure")
                    .valueStart()
                        .add("pressure", 1013.25)
                        .add("unit", "hPa")
                    .valueStop()
                .finish()
                .send();
    }
}
