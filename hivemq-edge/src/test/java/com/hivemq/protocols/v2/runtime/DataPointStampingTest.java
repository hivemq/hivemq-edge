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

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.schema.ScalarSchema;
import com.hivemq.adapter.sdk.api.schema.ScalarType;
import com.hivemq.adapter.sdk.api.v2.node.Node;
import com.hivemq.adapter.sdk.api.v2.node.NodeProperty;
import com.hivemq.adapter.sdk.api.v2.node.NodeTagPair;
import com.hivemq.adapter.sdk.api.v2.node.Tag;
import com.hivemq.datapoint.DataPointWithMetadata;
import java.util.EnumSet;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

/**
 * S21 at unit level: the framework stamps the owning tag's name and the adapter id onto an adapter-produced
 * {@link DataPoint}; the payload stays intact and the adapter's instance is never mutated.
 */
class DataPointStampingTest {

    private static final class TestNode extends Node {
        @Override
        public @NotNull String nodeId() {
            return "test-node";
        }

        @Override
        public @NotNull String nodeString() {
            return "{\"identifier\":\"test-node\"}";
        }

        @Override
        public @NotNull EnumSet<NodeProperty> properties() {
            return EnumSet.of(NodeProperty.UNIQUE, NodeProperty.TYPED, NodeProperty.VALID);
        }
    }

    private record AdapterProducedDataPoint(@NotNull Object value, boolean treatAsJson) implements DataPoint {
        @Override
        public @NotNull Object getTagValue() {
            return value;
        }

        @Override
        public boolean treatTagValueAsJson() {
            return treatAsJson;
        }

        @Override
        public @NotNull String getTagName() {
            return "";
        }
    }

    private static @NotNull Tag temperatureTag() {
        final NodeTagPair pair = NodeTagPair.create(
                new TestNode(),
                "temperature",
                new ScalarSchema(ScalarType.DOUBLE, null, null, null, null, false, true, false),
                true,
                false);
        return pair.tag();
    }

    @Test
    void plainDataPoint_isRecreatedWithStampedTagNameAndAdapterId_payloadIntact() {
        final Object payload = new Object();
        final DataPoint produced = new AdapterProducedDataPoint(payload, false);

        final DataPoint stamped = DataPointStamping.stamp(produced, temperatureTag(), "adapter-1");

        assertThat(stamped.getTagName()).isEqualTo("temperature");
        assertThat(stamped.getAdapterId()).isEqualTo("adapter-1");
        assertThat(stamped.getTagValue()).isSameAs(payload);
        assertThat(stamped.treatTagValueAsJson()).isFalse();
    }

    @Test
    void treatTagValueAsJsonFlag_isPreserved() {
        final DataPoint produced = new AdapterProducedDataPoint("{\"unit\":\"celsius\"}", true);

        final DataPoint stamped = DataPointStamping.stamp(produced, temperatureTag(), "adapter-1");

        assertThat(stamped.treatTagValueAsJson()).isTrue();
        assertThat(stamped.getTagValue()).isEqualTo("{\"unit\":\"celsius\"}");
    }

    @Test
    void dataPointWithMetadata_keepsValueTimestampMetadataAndContext() {
        final ObjectNode envelope = JsonNodeFactory.instance.objectNode();
        envelope.put("tagName", "adapter-side-name");
        envelope.put("timestamp", 1234L);
        envelope.put("value", 21.5d);
        envelope.putObject("metadata").put("unit", "celsius");
        envelope.putObject("context").put("source", "poll");
        final DataPointWithMetadata produced = new DataPointWithMetadata(envelope, "");

        final DataPoint stamped = DataPointStamping.stamp(produced, temperatureTag(), "adapter-1");

        assertThat(stamped).isInstanceOf(DataPointWithMetadata.class);
        final DataPointWithMetadata stampedWithMetadata = (DataPointWithMetadata) stamped;
        assertThat(stampedWithMetadata.getTagName()).isEqualTo("temperature");
        assertThat(stampedWithMetadata.getAdapterId()).isEqualTo("adapter-1");
        assertThat(stampedWithMetadata.getTimestamp()).isEqualTo(1234L);
        assertThat(stampedWithMetadata.getTagValue().doubleValue()).isEqualTo(21.5d);
        assertThat(stampedWithMetadata.getMetadata())
                .hasValueSatisfying(
                        metadata -> assertThat(metadata.get("unit").asText()).isEqualTo("celsius"));
        assertThat(stampedWithMetadata.getContext())
                .hasValueSatisfying(
                        context -> assertThat(context.get("source").asText()).isEqualTo("poll"));
    }

    @Test
    void dataPointWithMetadata_originalEnvelopeIsNeverMutated() {
        final ObjectNode envelope = JsonNodeFactory.instance.objectNode();
        envelope.put("tagName", "adapter-side-name");
        envelope.put("timestamp", 1234L);
        envelope.put("value", 21.5d);
        final DataPointWithMetadata produced = new DataPointWithMetadata(envelope, "");

        DataPointStamping.stamp(produced, temperatureTag(), "adapter-1");

        assertThat(produced.getTagName()).isEqualTo("adapter-side-name");
        assertThat(produced.getAdapterId()).isEmpty();
        assertThat(envelope.get("tagName").asText()).isEqualTo("adapter-side-name");
    }
}
