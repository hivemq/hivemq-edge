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
import com.hivemq.edge.api.model.CombinerList;
import com.hivemq.edge.api.model.DataIdentifierReference;
import com.hivemq.edge.api.model.EntityType;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class PulseApiImplGetAssetMappersTest extends AbstractPulseApiImplTest {
    @Test
    public void whenCombinersNotFound_thenReturnsEmptyList() {
        when(dataCombiningExtractor.getAllCombiners()).thenReturn(List.of());
        try (final Response response = pulseApi.getAssetMappers()) {
            assertThat(response.getStatus()).isEqualTo(200);
        }
    }

    @Test
    public void whenCombinersFound_thenReturnsOK() {
        final List<Combiner> expectedCombiners = Stream.of(EntityType.values())
                .flatMap(entityType -> Stream.of(DataIdentifierReference.TypeEnum.values())
                        .map(typeEnum -> createCombiner(entityType, typeEnum)))
                .toList();
        when(dataCombiningExtractor.getAllCombiners()).thenReturn(expectedCombiners.stream()
                .map(DataCombiner::fromModel)
                .toList());
        try (final Response response = pulseApi.getAssetMappers()) {
            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(response.getEntity()).isInstanceOf(CombinerList.class);
            final CombinerList actualCombiners = (CombinerList) response.getEntity();
            assertThat(actualCombiners.getItems().size()).isEqualTo(expectedCombiners.size());
            assertThat(actualCombiners.getItems()).isEqualTo(expectedCombiners);
        }
    }
}
