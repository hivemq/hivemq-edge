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
package com.hivemq.edge.adapters.chaos;

import com.hivemq.adapter.sdk.api.data.DataPoint;
import org.jetbrains.annotations.NotNull;

/**
 * A minimal reused-v1 {@link DataPoint} the chaos scripts carry as poll responses, subscription pushes, and write
 * values. The wrapper stamps the owning tag's name before handing it northbound, so the simplest
 * concrete value suffices — the scripts use this where a real adapter would build one with the v1
 * {@code DataPointFactory}.
 *
 * @param tagName the tag name carried by the value.
 * @param value   the tag value.
 */
public record ChaosDataPoint(
        @NotNull String tagName, @NotNull Object value) implements DataPoint {

    @Override
    public @NotNull Object getTagValue() {
        return value;
    }

    @Override
    public @NotNull String getTagName() {
        return tagName;
    }
}
