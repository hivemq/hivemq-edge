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
package com.hivemq.configuration.entity.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import com.hivemq.edge.api.model.NorthboundMappingOwner;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

/// That the correlation-token flag survives both directions of this converter.
///
/// There are two converters between a stored northbound mapping and its REST shape.
/// `NorthboundMappingEntityConverter` carried the flag from the day method calls landed; this one, which serves
/// the owner-shaped lists, was not updated. The consequences were asymmetric and the second is the worse:
///
///   - **Reading** returned `false` for a flag that was accepted and was being applied. The frontend renders
///     what a read returns, so the switch showed off while the token was still written.
///   - **Writing** dropped it, because the constructor this called did not take it. So an operator who opened
///     such a mapping and saved it did not merely see the wrong value -- they stored `false` and silently
///     stopped the token. That is the read-then-save shape of EDG-898.
class NorthboundMappingOwnerConverterTest {

    private static @NotNull NorthboundMappingEntity entity(final boolean writeCorrelationData) {
        return new NorthboundMappingEntity(
                "boiler-methods",
                "methods/boiler/responses",
                1,
                null,
                true,
                true,
                false,
                List.of(),
                60000L,
                writeCorrelationData);
    }

    @Test
    void theFlagReadsBackAsSet() {
        final NorthboundMappingOwner rest =
                NorthboundMappingOwnerConverter.INSTANCE.toRestEntity(entity(true), "opcua-1");

        assertThat(rest.getWriteCorrelationData())
                .as("a flag that is being applied must not read back as false")
                .isTrue();
    }

    @Test
    void theFlagSurvivesTheWayIn() {
        // The half that stores rather than displays. Without it a read-then-save writes false back.
        final NorthboundMappingOwner rest =
                NorthboundMappingOwnerConverter.INSTANCE.toRestEntity(entity(true), "opcua-1");

        final NorthboundMappingEntity back = NorthboundMappingOwnerConverter.INSTANCE.toInternalEntity(rest);

        assertThat(back.isWriteCorrelationData()).isTrue();
    }

    @Test
    void anUnsetFlagStaysUnset() {
        // The counterpart: every northbound mapping that exists today has this off, and carrying the field must
        // not turn it on.
        final NorthboundMappingOwner rest =
                NorthboundMappingOwnerConverter.INSTANCE.toRestEntity(entity(false), "opcua-1");

        assertThat(rest.getWriteCorrelationData()).isFalse();
        assertThat(NorthboundMappingOwnerConverter.INSTANCE
                        .toInternalEntity(rest)
                        .isWriteCorrelationData())
                .isFalse();
    }

    @Test
    void theOtherFieldsStillRoundTrip() {
        // Guards the change against disturbing what already worked, since this converter is used for every
        // northbound mapping and not only for method tags.
        final NorthboundMappingEntity back = NorthboundMappingOwnerConverter.INSTANCE.toInternalEntity(
                NorthboundMappingOwnerConverter.INSTANCE.toRestEntity(entity(true), "opcua-1"));

        assertThat(back.getTagName()).isEqualTo("boiler-methods");
        assertThat(back.getTopic()).isEqualTo("methods/boiler/responses");
        assertThat(back.getMaxQoS()).isEqualTo(1);
        assertThat(back.isIncludeTagNames()).isTrue();
        assertThat(back.isIncludeTimestamp()).isTrue();
        assertThat(back.getMessageExpiryInterval()).isEqualTo(60000L);
    }

    @Test
    void theAdapterIdIsCarried() {
        assertThat(NorthboundMappingOwnerConverter.INSTANCE
                        .toRestEntity(entity(true), "opcua-1")
                        .getAdapterId())
                .isEqualTo("opcua-1");
    }
}
