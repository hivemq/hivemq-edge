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

package com.hivemq.api.resources.impl.pulse;

import com.hivemq.api.errors.pulse.AssetMapperNotFoundError;
import com.hivemq.combining.model.DataCombiner;
import com.hivemq.edge.api.model.Combiner;
import com.hivemq.edge.api.model.DataIdentifierReference;
import com.hivemq.edge.api.model.EntityType;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class PulseApiImplGetAssetMapperInstructionsTest extends AbstractPulseApiImplTest {
    @Test
    public void whenCombinerAndMappingIdExist_thenReturnsOK() {
        final Combiner combiner = createCombiner(EntityType.PULSE_AGENT, DataIdentifierReference.TypeEnum.PULSE_ASSET);
        when(assetMappingExtractor.getCombinerById(any())).thenReturn(Optional.of(DataCombiner.fromModel(combiner)));
        try (final Response response = pulseApi.getAssetMapperInstructions(combiner.getId(),
                combiner.getMappings().getItems().getFirst().getId())) {
            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(response.getEntity()).isInstanceOf(List.class);
            assertThat((List) response.getEntity()).isEqualTo(combiner.getMappings()
                    .getItems()
                    .stream()
                    .flatMap(mappings -> mappings.getInstructions().stream())
                    .toList());
        }
    }

    @Test
    public void whenCombinerExistsButMappingIdDoesNotExist_thenReturnsOK() {
        final Combiner combiner = createCombiner(EntityType.PULSE_AGENT, DataIdentifierReference.TypeEnum.PULSE_ASSET);
        when(assetMappingExtractor.getCombinerById(any())).thenReturn(Optional.of(DataCombiner.fromModel(combiner)));
        try (final Response response = pulseApi.getAssetMapperInstructions(combiner.getId(), UUID.randomUUID())) {
            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(response.getEntity()).isInstanceOf(List.class);
            assertThat((List) response.getEntity()).isEmpty();
        }
    }

    @Test
    public void whenCombinerNotFound_thenReturnsAssetMapperNotFoundError() {
        when(assetMappingExtractor.getCombinerById(any())).thenReturn(Optional.empty());
        try (final Response response = pulseApi.getAssetMapperInstructions(UUID.randomUUID(), UUID.randomUUID())) {
            assertThat(response.getStatus()).isEqualTo(404);
            assertThat(response.getEntity()).isInstanceOf(AssetMapperNotFoundError.class);
        }
    }
}
