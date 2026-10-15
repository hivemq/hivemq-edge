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

import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

/// That every field of a northbound mapping survives both directions of this converter.
///
/// There are two converters between a stored northbound mapping and its REST shape:
/// `NorthboundMappingEntityConverter`, and this one, which serves the owner-shaped lists that carry the owning
/// adapter's id. **A field present in one and missing from the other is worse than a display bug**, and the two
/// consequences are asymmetric:
///
///   - **Reading** returns a default for a value that is stored and is being applied. The frontend renders what
///     a read returns, so a setting shows as off while it still takes effect.
///   - **Writing** drops it, because the entity is rebuilt from the REST shape that never carried it. So an
///     operator who opens such a mapping and saves it does not merely see a stale value -- they store one,
///     silently changing behaviour they never touched. That is the read-then-save shape of EDG-898.
///
/// This happened once already, with a correlation-token setting that was added to the sibling converter and not
/// to this one. That setting is gone -- a northbound mapping has no method-call configuration at all now, since
/// what a response carries is decided by the southbound mapping that sent the request -- so these tests cover
/// the fields that remain, and the whole-object assertion below is what will catch the next one.
class NorthboundMappingOwnerConverterTest {

    private static @NotNull NorthboundMappingEntity entity() {
        return new NorthboundMappingEntity(
                "boiler-methods",
                "methods/boiler/responses",
                1,
                null,
                true,
                true,
                false,
                List.of(new MqttUserPropertyEntity("plant", "vienna")),
                60000L);
    }

    private static @NotNull NorthboundMappingEntity roundTripped() {
        return NorthboundMappingOwnerConverter.INSTANCE.toInternalEntity(
                NorthboundMappingOwnerConverter.INSTANCE.toRestEntity(entity(), "opcua-1"));
    }

    @Test
    void everyFieldSurvivesTheRoundTrip() {
        final NorthboundMappingEntity back = roundTripped();

        assertThat(back.getTagName()).isEqualTo("boiler-methods");
        assertThat(back.getTopic()).isEqualTo("methods/boiler/responses");
        assertThat(back.getMaxQoS()).isEqualTo(1);
        assertThat(back.isIncludeTagNames()).isTrue();
        assertThat(back.isIncludeTimestamp()).isTrue();
        assertThat(back.getMessageExpiryInterval()).isEqualTo(60000L);
    }

    @Test
    void theUserPropertiesSurviveTheRoundTrip() {
        // Separate from the fields above because it is the only one carrying a nested list, so it goes through a
        // converter of its own rather than a scalar getter -- the shape most likely to be missed.
        assertThat(roundTripped().getUserProperties())
                .as("a mapping's user properties must not be lost by a read-then-save")
                .singleElement()
                .satisfies(property -> {
                    assertThat(property.getName()).isEqualTo("plant");
                    assertThat(property.getValue()).isEqualTo("vienna");
                });
    }

    @Test
    void theRoundTrippedEntityEqualsTheOriginal() {
        // The whole-object form of the assertions above, which is what catches a field added later and carried
        // by neither direction -- the failure mode the per-field tests cannot see.
        assertThat(roundTripped()).isEqualTo(entity());
    }

    @Test
    void theAdapterIdIsCarried() {
        assertThat(NorthboundMappingOwnerConverter.INSTANCE
                        .toRestEntity(entity(), "opcua-1")
                        .getAdapterId())
                .isEqualTo("opcua-1");
    }
}
