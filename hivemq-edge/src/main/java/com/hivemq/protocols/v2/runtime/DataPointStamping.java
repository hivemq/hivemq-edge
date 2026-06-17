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
package com.hivemq.protocols.v2.runtime;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.v2.node.Node;
import com.hivemq.adapter.sdk.api.v2.node.Tag;
import com.hivemq.datapoint.DataPointWithMetadata;
import com.hivemq.edge.modules.adapters.data.DataPointImpl;
import org.jetbrains.annotations.NotNull;

/**
 * Stamps tag identity onto an adapter-produced {@link DataPoint}.
 * <p>
 * In the Nevsky framework, correlation across the adapter boundary is by {@link Node} reference — a protocol
 * adapter reports values as {@code dataPoint(Node, DataPoint)} and is NOT required to set
 * {@link DataPoint#getTagName()} or {@link DataPoint#getAdapterId()}. Before a value is handed to northbound
 * consumers, the framework re-creates it with the owning tag's name and the adapter's id, preserving the
 * payload:
 * <ul>
 * <li>a {@link DataPointWithMetadata} keeps its value, timestamp, metadata, and context — only the
 * {@code tagName} inside the JSON envelope is rewritten (on a copy; the adapter's instance is never
 * mutated);</li>
 * <li>any other {@link DataPoint} is re-created with the same value reference and the same
 * {@link DataPoint#treatTagValueAsJson()} flag.</li>
 * </ul>
 */
public final class DataPointStamping {

    private DataPointStamping() {}

    /**
     * @param dataPoint the adapter-produced data point; never mutated.
     * @param tag       the owning tag of the node the adapter reported the value for.
     * @param adapterId the id of the adapter instance that produced the value.
     * @return a new data point carrying the tag's name and the adapter id, with the payload intact.
     */
    public static @NotNull DataPoint stamp(
            final @NotNull DataPoint dataPoint, final @NotNull Tag tag, final @NotNull String adapterId) {
        if (dataPoint instanceof final DataPointWithMetadata dataPointWithMetadata) {
            final ObjectNode stampedEnvelope =
                    dataPointWithMetadata.getJsonNode().deepCopy();
            stampedEnvelope.put("tagName", tag.name());
            return new DataPointWithMetadata(stampedEnvelope, adapterId);
        }
        return new DataPointImpl(tag.name(), dataPoint.getTagValue(), adapterId, dataPoint.treatTagValueAsJson());
    }
}
