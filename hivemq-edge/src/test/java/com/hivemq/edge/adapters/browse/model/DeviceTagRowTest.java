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
package com.hivemq.edge.adapters.browse.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.hivemq.edge.adapters.browse.BrowsedNode;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DeviceTagRowTest {

    @Test
    void builder_allFields() {
        final DeviceTagRow row = DeviceTagRow.builder()
                .nodePath("/Objects/Data/Int32")
                .namespaceUri("urn:test")
                .namespaceIndex(2)
                .nodeId("ns=2;i=100")
                .dataType("Int32")
                .accessLevel("READ_WRITE")
                .nodeDescription("A test node")
                .tagName("my-tag")
                .tagNameDefault("adapter-int32")
                .tagDescription("Tag description")
                .northboundTopic("adapter/data/int32")
                .northboundTopicDefault("adapter/data/int32")
                .southboundTopic("adapter/write/data/int32")
                .southboundTopicDefault("adapter/write/data/int32")
                .southboundFieldMapping(List.of(new FieldMappingInstruction("value", "value")))
                .maxQos(1)
                .messageExpiryInterval(3600L)
                .includeTimestamp(true)
                .includeTagNames(false)
                .includeMetadata(true)
                .mqttUserProperties(Map.of("key1", "val1"))
                .build();

        assertThat(row.getNodePath()).isEqualTo("/Objects/Data/Int32");
        assertThat(row.getNamespaceUri()).isEqualTo("urn:test");
        assertThat(row.getNamespaceIndex()).isEqualTo(2);
        assertThat(row.getNodeId()).isEqualTo("ns=2;i=100");
        assertThat(row.getDataType()).isEqualTo("Int32");
        assertThat(row.getAccessLevel()).isEqualTo("READ_WRITE");
        assertThat(row.getNodeDescription()).isEqualTo("A test node");
        assertThat(row.getTagName()).isEqualTo("my-tag");
        assertThat(row.getTagNameDefault()).isEqualTo("adapter-int32");
        assertThat(row.getTagDescription()).isEqualTo("Tag description");
        assertThat(row.getNorthboundTopic()).isEqualTo("adapter/data/int32");
        assertThat(row.getNorthboundTopicDefault()).isEqualTo("adapter/data/int32");
        assertThat(row.getSouthboundTopic()).isEqualTo("adapter/write/data/int32");
        assertThat(row.getSouthboundTopicDefault()).isEqualTo("adapter/write/data/int32");
        assertThat(row.getSouthboundFieldMapping()).hasSize(1);
        assertThat(row.getMaxQos()).isEqualTo(1);
        assertThat(row.getMessageExpiryInterval()).isEqualTo(3600L);
        assertThat(row.getIncludeTimestamp()).isTrue();
        assertThat(row.getIncludeTagNames()).isFalse();
        assertThat(row.getIncludeMetadata()).isTrue();
        assertThat(row.getMqttUserProperties()).containsEntry("key1", "val1");
    }

    @Test
    void builder_defaults_areNull() {
        final DeviceTagRow row = DeviceTagRow.builder().build();

        assertThat(row.getNodePath()).isNull();
        assertThat(row.getNamespaceUri()).isNull();
        assertThat(row.getNamespaceIndex()).isEqualTo(0);
        assertThat(row.getNodeId()).isNull();
        assertThat(row.getDataType()).isNull();
        assertThat(row.getAccessLevel()).isNull();
        assertThat(row.getNodeDescription()).isNull();
        assertThat(row.getTagName()).isNull();
        assertThat(row.getTagNameDefault()).isNull();
        assertThat(row.getTagDescription()).isNull();
        assertThat(row.getNorthboundTopic()).isNull();
        assertThat(row.getNorthboundTopicDefault()).isNull();
        assertThat(row.getSouthboundTopic()).isNull();
        assertThat(row.getSouthboundTopicDefault()).isNull();
        assertThat(row.getSouthboundFieldMapping()).isNull();
        assertThat(row.getMaxQos()).isNull();
        assertThat(row.getMessageExpiryInterval()).isNull();
        assertThat(row.getIncludeTimestamp()).isNull();
        assertThat(row.getIncludeTagNames()).isNull();
        assertThat(row.getIncludeMetadata()).isNull();
        assertThat(row.getMqttUserProperties()).isNull();
    }

    @Test
    void fromBrowsedNode_copiesInformationalFields() {
        final BrowsedNode node = new BrowsedNode(
                "/Objects/Data/Bool",
                "urn:test:ns",
                2,
                "ns=2;i=50",
                "Boolean",
                "READ",
                "A boolean node",
                "adapter-bool",
                "Bool node desc",
                "adapter/data/bool",
                "adapter/write/data/bool");

        final DeviceTagRow row = DeviceTagRow.fromBrowsedNode(node);

        assertThat(row.getNodePath()).isEqualTo("/Objects/Data/Bool");
        assertThat(row.getNamespaceUri()).isEqualTo("urn:test:ns");
        assertThat(row.getNamespaceIndex()).isEqualTo(2);
        assertThat(row.getNodeId()).isEqualTo("ns=2;i=50");
        assertThat(row.getDataType()).isEqualTo("Boolean");
        assertThat(row.getAccessLevel()).isEqualTo("READ");
        assertThat(row.getNodeDescription()).isEqualTo("A boolean node");
        assertThat(row.getTagNameDefault()).isEqualTo("adapter-bool");
        assertThat(row.getTagDescription()).isEqualTo("Bool node desc");
        assertThat(row.getNorthboundTopicDefault()).isEqualTo("adapter/data/bool");
        assertThat(row.getSouthboundTopicDefault()).isEqualTo("adapter/write/data/bool");
    }

    @Test
    void fromBrowsedNode_editableFieldsAreNull() {
        final BrowsedNode node = new BrowsedNode(
                "/path",
                "urn:ns",
                0,
                "ns=0;i=1",
                "Int32",
                "READ",
                null,
                "default-tag",
                null,
                "default/topic",
                "default/write/topic");

        final DeviceTagRow row = DeviceTagRow.fromBrowsedNode(node);

        assertThat(row.getTagName()).isNull();
        assertThat(row.getNorthboundTopic()).isNull();
        assertThat(row.getSouthboundTopic()).isNull();
        assertThat(row.getSouthboundFieldMapping()).isNull();
        assertThat(row.getMaxQos()).isNull();
        assertThat(row.getMessageExpiryInterval()).isNull();
        assertThat(row.getIncludeTimestamp()).isNull();
        assertThat(row.getIncludeTagNames()).isNull();
        assertThat(row.getIncludeMetadata()).isNull();
        assertThat(row.getMqttUserProperties()).isNull();
    }

    @Test
    void hasTag_trueWhenTagNamePresent() {
        final DeviceTagRow row = DeviceTagRow.builder().tagName("my-tag").build();
        assertThat(row.hasTag()).isTrue();
    }

    @Test
    void hasTag_falseWhenTagNameNull() {
        final DeviceTagRow row = DeviceTagRow.builder().build();
        assertThat(row.hasTag()).isFalse();
    }

    @Test
    void hasTag_falseWhenTagNameEmpty() {
        final DeviceTagRow row = DeviceTagRow.builder().tagName("").build();
        assertThat(row.hasTag()).isFalse();
    }

    @Test
    void hasNorthboundMapping_trueWhenTagAndTopicPresent() {
        final DeviceTagRow row = DeviceTagRow.builder()
                .tagName("my-tag")
                .northboundTopic("topic/a")
                .build();
        assertThat(row.hasNorthboundMapping()).isTrue();
    }

    @Test
    void hasNorthboundMapping_falseWhenNoTag() {
        final DeviceTagRow row =
                DeviceTagRow.builder().northboundTopic("topic/a").build();
        assertThat(row.hasNorthboundMapping()).isFalse();
    }

    @Test
    void hasNorthboundMapping_falseWhenNoTopic() {
        final DeviceTagRow row = DeviceTagRow.builder().tagName("my-tag").build();
        assertThat(row.hasNorthboundMapping()).isFalse();
    }

    @Test
    void hasSouthboundMapping_trueWhenTagAndTopicPresent() {
        final DeviceTagRow row = DeviceTagRow.builder()
                .tagName("my-tag")
                .southboundTopic("topic/write")
                .build();
        assertThat(row.hasSouthboundMapping()).isTrue();
    }

    @Test
    void hasSouthboundMapping_falseWhenNoTag() {
        final DeviceTagRow row =
                DeviceTagRow.builder().southboundTopic("topic/write").build();
        assertThat(row.hasSouthboundMapping()).isFalse();
    }

    @Test
    void equals_sameFields() {
        final DeviceTagRow row1 =
                DeviceTagRow.builder().nodeId("ns=2;i=1").tagName("t1").build();
        final DeviceTagRow row2 =
                DeviceTagRow.builder().nodeId("ns=2;i=1").tagName("t1").build();
        assertThat(row1).isEqualTo(row2);
        assertThat(row1.hashCode()).isEqualTo(row2.hashCode());
    }

    @Test
    void equals_differentFields() {
        final DeviceTagRow row1 =
                DeviceTagRow.builder().nodeId("ns=2;i=1").tagName("t1").build();
        final DeviceTagRow row2 =
                DeviceTagRow.builder().nodeId("ns=2;i=2").tagName("t2").build();
        assertThat(row1).isNotEqualTo(row2);
    }

    @Test
    void toString_containsKeyFields() {
        final DeviceTagRow row = DeviceTagRow.builder()
                .nodePath("/path")
                .nodeId("ns=2;i=1")
                .tagName("my-tag")
                .tagNameDefault("default")
                .build();
        final String str = row.toString();
        assertThat(str).contains("nodePath='/path'");
        assertThat(str).contains("nodeId='ns=2;i=1'");
        assertThat(str).contains("tagName='my-tag'");
    }
}
