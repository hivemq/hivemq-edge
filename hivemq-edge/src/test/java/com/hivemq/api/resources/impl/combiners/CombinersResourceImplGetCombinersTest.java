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

import com.hivemq.edge.api.model.Combiner;
import com.hivemq.edge.api.model.CombinerList;
import com.hivemq.edge.api.model.DataIdentifierReference;
import com.hivemq.edge.api.model.EntityType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.junit.jupiter.api.Test;

public class CombinersResourceImplGetCombinersTest extends AbstractCombinersResourceImplTest {

    @Test
    public void whenNoCombinersExist_thenReturnsEmptyList() {
        when(dataCombiningExtractor.getAllCombiners()).thenReturn(List.of());

        try (final Response response = combinersApi.getCombiners()) {
            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(response.getEntity()).isInstanceOf(CombinerList.class);
            final CombinerList combinerList = (CombinerList) response.getEntity();
            assertThat(combinerList.getItems()).isEmpty();
        }
    }

    @Test
    public void whenCombinersExist_thenReturnsCombinerList() {
        final Combiner combiner = createCombiner(EntityType.ADAPTER, DataIdentifierReference.TypeEnum.TOPIC_FILTER);
        when(dataCombiningExtractor.getAllCombiners()).thenReturn(List.of(toDataCombiner(combiner)));

        try (final Response response = combinersApi.getCombiners()) {
            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(response.getEntity()).isInstanceOf(CombinerList.class);
            final CombinerList combinerList = (CombinerList) response.getEntity();
            assertThat(combinerList.getItems()).hasSize(1);
            assertThat(combinerList.getItems().getFirst().getId()).isEqualTo(combiner.getId());
        }
    }
}
