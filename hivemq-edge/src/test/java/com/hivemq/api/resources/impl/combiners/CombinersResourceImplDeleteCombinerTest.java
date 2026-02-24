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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hivemq.api.errors.ConfigWritingDisabled;
import com.hivemq.api.errors.adapters.DataCombinerNotFoundError;
import com.hivemq.edge.api.model.Combiner;
import com.hivemq.edge.api.model.DataIdentifierReference;
import com.hivemq.edge.api.model.EntityType;
import jakarta.ws.rs.core.Response;
import java.util.Optional;
import org.junit.jupiter.api.Test;

public class CombinersResourceImplDeleteCombinerTest extends AbstractCombinersResourceImplTest {

    @Test
    public void whenConfigNotWritable_thenReturnsConfigWritingDisabledError() {
        final Combiner combiner = createCombiner(EntityType.ADAPTER, DataIdentifierReference.TypeEnum.TOPIC_FILTER);
        when(systemInformation.isConfigWriteable()).thenReturn(false);

        try (final Response response = combinersApi.deleteCombiner(combiner.getId())) {
            assertThat(response.getStatus()).isEqualTo(403);
            assertThat(response.getEntity()).isInstanceOf(ConfigWritingDisabled.class);
        }
    }

    @Test
    public void whenCombinerDoesNotExist_thenReturnsDataCombinerNotFoundError() {
        final Combiner combiner = createCombiner(EntityType.ADAPTER, DataIdentifierReference.TypeEnum.TOPIC_FILTER);
        when(dataCombiningExtractor.getCombinerById(any())).thenReturn(Optional.empty());

        try (final Response response = combinersApi.deleteCombiner(combiner.getId())) {
            assertThat(response.getStatus()).isEqualTo(404);
            assertThat(response.getEntity()).isInstanceOf(DataCombinerNotFoundError.class);
        }
    }

    @Test
    public void whenCombinerExists_thenReturnsOK() {
        final Combiner combiner = createCombiner(EntityType.ADAPTER, DataIdentifierReference.TypeEnum.TOPIC_FILTER);
        when(dataCombiningExtractor.getCombinerById(any())).thenReturn(Optional.of(toDataCombiner(combiner)));

        try (final Response response = combinersApi.deleteCombiner(combiner.getId())) {
            assertThat(response.getStatus()).isEqualTo(200);
            verify(dataCombiningExtractor).deleteDataCombiner(combiner.getId());
        }
    }
}
