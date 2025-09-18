/*
 *  Copyright 2019-present HiveMQ GmbH
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.hivemq.configuration.entity.pulse;

import com.hivemq.pulse.asset.Asset;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class PulseAssetEntityTest {
    @Test
    public void whenAllPropertiesAreIdentical_thenEqualsReturnsTrue() {
        final PulseAssetEntity asset1 = PulseAssetEntity.builder()
                .id(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"))
                .name("Test Asset")
                .description("This is a test asset")
                .topic("test/topic")
                .schema("{ \"type\": \"object\" }")
                .mapping(PulseAssetMappingEntity.builder().build())
                .build();
        final Asset asset2 = new Asset("123e4567-e89b-12d3-a456-426614174000",
                "test/topic",
                "Test Asset",
                "This is a test asset",
                "{ \"type\": \"object\" }");
        assertThat(asset1.equals(asset2)).isTrue();
    }

    @Test
    public void whenOnlySchemaPropertyOrderIsDifferent_thenEqualsReturnsTrue() {
        final PulseAssetEntity asset1 = PulseAssetEntity.builder()
                .id(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"))
                .name("Test Asset")
                .description("This is a test asset")
                .topic("test/topic")
                .schema("{\"a\":1,\"b\":2}")
                .mapping(PulseAssetMappingEntity.builder().build())
                .build();
        final Asset asset2 = new Asset("123e4567-e89b-12d3-a456-426614174000",
                "test/topic",
                "Test Asset",
                "This is a test asset",
                "{\"b\":2,\"a\":1}");
        assertThat(asset1.equals(asset2)).isTrue();
    }

    @Test
    public void whenIdIsDifferent_thenEqualsReturnsFalse() {
        final PulseAssetEntity asset1 = PulseAssetEntity.builder()
                .id(UUID.fromString("123e4567-e89b-12d3-a456-426614174001"))
                .name("Test Asset")
                .description("This is a test asset")
                .topic("test/topic")
                .schema("{\"a\":1,\"b\":2}")
                .mapping(PulseAssetMappingEntity.builder().build())
                .build();
        final Asset asset2 = new Asset("123e4567-e89b-12d3-a456-426614174002",
                "test/topic",
                "Test Asset",
                "This is a test asset",
                "{\"a\":1,\"b\":2}");
        assertThat(asset1.equals(asset2)).isFalse();
    }

    @Test
    public void whenNameIsDifferent_thenEqualsReturnsFalse() {
        final PulseAssetEntity asset1 = PulseAssetEntity.builder()
                .id(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"))
                .name("Test Asset 1")
                .description("This is a test asset")
                .topic("test/topic")
                .schema("{\"a\":1,\"b\":2}")
                .mapping(PulseAssetMappingEntity.builder().build())
                .build();
        final Asset asset2 = new Asset("123e4567-e89b-12d3-a456-426614174000",
                "test/topic",
                "Test Asset 2",
                "This is a test asset",
                "{\"a\":1,\"b\":2}");
        assertThat(asset1.equals(asset2)).isFalse();
    }

    @Test
    public void whenDescriptionIsDifferent_thenEqualsReturnsFalse() {
        final PulseAssetEntity asset1 = PulseAssetEntity.builder()
                .id(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"))
                .name("Test Asset")
                .description("This is a test asset")
                .topic("test/topic")
                .schema("{\"a\":1,\"b\":2}")
                .mapping(PulseAssetMappingEntity.builder().build())
                .build();
        final Asset asset2 = new Asset("123e4567-e89b-12d3-a456-426614174000",
                "test/topic",
                "Test Asset",
                "This is another test asset",
                "{\"a\":1,\"b\":2}");
        assertThat(asset1.equals(asset2)).isFalse();
    }

    @Test
    public void whenTopicIsDifferent_thenEqualsReturnsFalse() {
        final PulseAssetEntity asset1 = PulseAssetEntity.builder()
                .id(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"))
                .name("Test Asset")
                .description("This is a test asset")
                .topic("test/topic/1")
                .schema("{\"a\":1,\"b\":2}")
                .mapping(PulseAssetMappingEntity.builder().build())
                .build();
        final Asset asset2 = new Asset("123e4567-e89b-12d3-a456-426614174000",
                "test/topic/2",
                "Test Asset",
                "This is a test asset",
                "{\"a\":1,\"b\":2}");
        assertThat(asset1.equals(asset2)).isFalse();
    }
}
