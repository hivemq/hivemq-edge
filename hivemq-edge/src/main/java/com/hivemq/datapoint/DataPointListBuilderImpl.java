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
package com.hivemq.datapoint;

import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.datapoint.DataPointBuilder;
import com.hivemq.adapter.sdk.api.datapoint.DataPointListBuilder;
import com.hivemq.adapter.sdk.api.tag.Tag;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import org.jetbrains.annotations.NotNull;

public final class DataPointListBuilderImpl implements DataPointListBuilder {
    private final @NotNull String adapterId;
    private final @NotNull Consumer<DataPointBuilder<?>> enricher;
    private final @NotNull Consumer<List<DataPoint>> receiver;
    private final @NotNull List<DataPointWithMetadata.DataPointBuilderImpl<DataPointListBuilder>> builders =
            new ArrayList<>();

    public DataPointListBuilderImpl(
            @NotNull final String adapterId,
            @NotNull final Consumer<DataPointBuilder<?>> enricher,
            @NotNull final Consumer<List<DataPoint>> receiver) {
        this.enricher = enricher;
        this.receiver = receiver;
        this.adapterId = adapterId;
    }

    @Override
    public @NotNull DataPointBuilder<DataPointListBuilder> addDataPoint(final @NotNull Tag tag) {
        final Function<DataPointBuilder<DataPointListBuilder>, DataPointListBuilder> completer = dp -> this;
        final var builderImpl = (DataPointWithMetadata.DataPointBuilderImpl<DataPointListBuilder>)
                DataPointWithMetadata.builder(adapterId, tag, completer);
        builders.add(builderImpl);
        return builderImpl;
    }

    @Override
    public void publish() {
        final var converted = builders.stream()
                .<DataPoint>map(builder -> {
                    enricher.accept(builder);
                    return builder.build(adapterId);
                })
                .toList();
        receiver.accept(converted);
    }
}
