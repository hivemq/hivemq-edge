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
package com.hivemq.edge.modules.adapters.data;

import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.datapoint.DataPointBuilder;
import com.hivemq.adapter.sdk.api.datapoint.DataPointListBuilder;
import com.hivemq.adapter.sdk.api.streaming.ProtocolAdapterTagStreamingService;
import com.hivemq.datapoint.DataPointListBuilderImpl;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Consumer;

public class ProtocolAdapterTagStreamingServiceImpl implements ProtocolAdapterTagStreamingService {

    private final @NotNull String adapterId;
    private final @NotNull TagManager tagManager;
    private final @NotNull Consumer<DataPointBuilder<?>> enricher;

    public ProtocolAdapterTagStreamingServiceImpl(
            final @NotNull String adapterId,
            final @NotNull TagManager tagManager,
            final @NotNull Consumer<DataPointBuilder<?>> enricher) {
        this.adapterId = adapterId;
        this.tagManager = tagManager;
        this.enricher = enricher;
    }

    @Override
    public @NotNull DataPointListBuilder dataPointsPublisher() {
        return new DataPointListBuilderImpl(
                adapterId,
                enricher,
                tagManager::feed);
    }

    @Override
    @Deprecated(since = "This API will be removed by 2026.10. Please migrate to the feed method without tagName.")
    public void feed(final @NotNull String tag, final @NotNull List<DataPoint> dataPoints) {
        tagManager.feed(adapterId, dataPoints);
    }
}
