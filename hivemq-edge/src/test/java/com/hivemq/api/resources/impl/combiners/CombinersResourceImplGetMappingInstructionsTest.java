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
package com.hivemq.api.resources.impl.combiners;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.hivemq.api.errors.adapters.DataCombinerNotFoundError;
import com.hivemq.api.model.ItemsResponse;
import com.hivemq.edge.api.model.Combiner;
import com.hivemq.edge.api.model.DataIdentifierReference;
import com.hivemq.edge.api.model.EntityType;
import com.hivemq.edge.api.model.Instruction;
import jakarta.ws.rs.core.Response;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

public class CombinersResourceImplGetMappingInstructionsTest extends AbstractCombinersResourceImplTest {

    @Test
    public void whenCombinerDoesNotExist_thenReturnsDataCombinerNotFoundError() {
        final Combiner combiner = createCombiner(EntityType.ADAPTER, DataIdentifierReference.TypeEnum.TOPIC_FILTER);
        when(dataCombiningExtractor.getCombinerById(combiner.getId())).thenReturn(Optional.empty());

        try (final Response response = combinersApi.getMappingInstructions(combiner.getId(), UUID.randomUUID())) {
            assertThat(response.getStatus()).isEqualTo(404);
            assertThat(response.getEntity()).isInstanceOf(DataCombinerNotFoundError.class);
        }
    }

    @Test
    public void whenMappingExists_thenReturnsInstructions() {
        final Combiner combiner = createCombiner(EntityType.ADAPTER, DataIdentifierReference.TypeEnum.TOPIC_FILTER);
        final UUID mappingId = combiner.getMappings().getItems().getFirst().getId();
        when(dataCombiningExtractor.getCombinerById(combiner.getId())).thenReturn(Optional.of(toDataCombiner(combiner)));

        try (final Response response = combinersApi.getMappingInstructions(combiner.getId(), mappingId)) {
            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(response.getEntity()).isInstanceOf(ItemsResponse.class);
            final ItemsResponse<Instruction> list = (ItemsResponse<Instruction>) response.getEntity();
            assertThat(list.getItems()).hasSize(combiner.getMappings().getItems().getFirst().getInstructions().size());
        }
    }

    @Test
    public void whenMappingDoesNotExist_thenReturnsEmptyInstructions() {
        final Combiner combiner = createCombiner(EntityType.ADAPTER, DataIdentifierReference.TypeEnum.TOPIC_FILTER);
        when(dataCombiningExtractor.getCombinerById(combiner.getId())).thenReturn(Optional.of(toDataCombiner(combiner)));

        try (final Response response = combinersApi.getMappingInstructions(combiner.getId(), UUID.randomUUID())) {
            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(response.getEntity()).isInstanceOf(ItemsResponse.class);
            final ItemsResponse<Instruction> list = (ItemsResponse<Instruction>) response.getEntity();
            assertThat(list.getItems()).isEmpty();
        }
    }
}
