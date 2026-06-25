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
package com.hivemq.protocols.v2.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

/**
 * The {@code used} derivation: {@code readUsed} = referenced by a northbound mapping; {@code writeUsed} =
 * referenced by a southbound mapping. A pure scan of this adapter's mappings, feeding S16.
 */
class UsedDerivationTest {

    @Test
    void readUsed_followsNorthboundMappings() {
        final ProtocolAdapterEntity entity = adapter();
        entity.getNorthboundMappings().add(new NorthboundMappingEntity("temperature", "plant/a/temperature"));

        assertThat(entity.getReadUsedTagNames()).containsExactly("temperature");
        assertThat(entity.isReadUsed("temperature")).isTrue();
        assertThat(entity.isReadUsed("pressure")).isFalse();
        assertThat(entity.getWriteUsedTagNames()).isEmpty();
    }

    @Test
    void writeUsed_followsSouthboundMappings() {
        final ProtocolAdapterEntity entity = adapter();
        entity.getSouthboundMappings().add(new SouthboundMappingEntity("plant/a/setpoint", "setpoint"));

        assertThat(entity.getWriteUsedTagNames()).containsExactly("setpoint");
        assertThat(entity.isWriteUsed("setpoint")).isTrue();
        assertThat(entity.isWriteUsed("temperature")).isFalse();
        assertThat(entity.getReadUsedTagNames()).isEmpty();
    }

    @Test
    void aTagConsumedBothWays_isBothReadAndWriteUsed() {
        final ProtocolAdapterEntity entity = adapter();
        entity.getNorthboundMappings().add(new NorthboundMappingEntity("temperature", "plant/a/temperature"));
        entity.getSouthboundMappings().add(new SouthboundMappingEntity("plant/a/temperature", "temperature"));

        assertThat(entity.isReadUsed("temperature")).isTrue();
        assertThat(entity.isWriteUsed("temperature")).isTrue();
    }

    @Test
    void multipleMappingsToOneTag_collapseToOneUsedEntry() {
        final ProtocolAdapterEntity entity = adapter();
        entity.getNorthboundMappings().add(new NorthboundMappingEntity("temperature", "plant/a/temperature"));
        entity.getNorthboundMappings().add(new NorthboundMappingEntity("temperature", "plant/b/temperature"));

        assertThat(entity.getReadUsedTagNames()).containsExactly("temperature");
        assertThat(entity.isReadUsed("temperature")).isTrue();
    }

    @Test
    void removingTheLastConsumingMapping_flipsUsed() {
        final ProtocolAdapterEntity entity = adapter();
        final NorthboundMappingEntity mapping = new NorthboundMappingEntity("temperature", "plant/a/temperature");
        entity.getNorthboundMappings().add(mapping);
        assertThat(entity.isReadUsed("temperature")).isTrue();

        entity.getNorthboundMappings().remove(mapping);
        assertThat(entity.isReadUsed("temperature")).isFalse();
        assertThat(entity.getReadUsedTagNames()).isEmpty();
    }

    private static @NotNull ProtocolAdapterEntity adapter() {
        return new ProtocolAdapterEntity(
                "chaos-1",
                "chaos",
                2,
                true,
                true,
                false,
                Map.of(),
                new RetryPolicyEntity(),
                30_000,
                10_000,
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>());
    }
}
