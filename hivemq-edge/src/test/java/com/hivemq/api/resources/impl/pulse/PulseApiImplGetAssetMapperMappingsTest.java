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

import com.hivemq.combining.model.DataCombiner;
import com.hivemq.edge.api.model.Combiner;
import com.hivemq.edge.api.model.DataCombiningList;
import com.hivemq.edge.api.model.DataIdentifierReference;
import com.hivemq.edge.api.model.EntityType;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class PulseApiImplGetAssetMapperMappingsTest extends AbstractPulseApiImplTest {
    @Test
    public void whenCombinerNotFound_thenReturnsDataCombinerNotFoundError() {
        when(dataCombiningExtractor.getCombinerById(any())).thenReturn(Optional.empty());
        try (final Response response = pulseApi.getAssetMapperMappings(UUID.randomUUID())) {
            assertThat(response.getStatus()).isEqualTo(404);
        }
    }

    @Test
    public void whenCombinerExists_thenReturnsOK() {
        final Combiner combiner =
                createCombiner(EntityType.PULSE_AGENT, DataIdentifierReference.TypeEnum.PULSE_ASSET);
        when(dataCombiningExtractor.getCombinerById(any())).thenReturn(Optional.of(DataCombiner.fromModel(
                combiner)));
        try (final Response response = pulseApi.getAssetMapperMappings(combiner.getId())) {
            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(response.getEntity()).isInstanceOf(DataCombiningList.class);
            final DataCombiningList dataCombiningList = (DataCombiningList) response.getEntity();
            assertThat(dataCombiningList).isEqualTo(combiner.getMappings());
        }
    }
}
