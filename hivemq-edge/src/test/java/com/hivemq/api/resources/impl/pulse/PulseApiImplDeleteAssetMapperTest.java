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

import com.hivemq.api.errors.ConfigWritingDisabled;
import com.hivemq.api.errors.pulse.AssetMapperNotFoundError;
import com.hivemq.combining.model.DataCombiner;
import com.hivemq.combining.model.DataCombining;
import com.hivemq.combining.model.DataCombiningDestination;
import com.hivemq.combining.model.DataCombiningSources;
import com.hivemq.combining.model.DataIdentifierReference;
import com.hivemq.combining.model.EntityReference;
import com.hivemq.combining.model.EntityType;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PulseApiImplDeleteAssetMapperTest extends AbstractPulseApiImplTest {
    @Test
    public void whenConfigNotWritable_thenReturnsConfigWritingDisabledError() {
        when(systemInformation.isConfigWriteable()).thenReturn(false);
        try (final Response response = pulseApi.deleteAssetMapper(UUID.randomUUID())) {
            assertThat(response.getStatus()).isEqualTo(403);
            assertThat(response.getEntity()).isInstanceOf(ConfigWritingDisabled.class);
        }
    }

    @Test
    public void whenCombinerNotFound_thenReturnsDataCombinerNotFoundError() {
        when(assetMappingExtractor.getCombinerById(any())).thenReturn(Optional.empty());
        try (final Response response = pulseApi.deleteAssetMapper(UUID.randomUUID())) {
            assertThat(response.getStatus()).isEqualTo(404);
            assertThat(response.getEntity()).isInstanceOf(AssetMapperNotFoundError.class);
        }
    }

    @Test
    public void whenCombinerFound_thenReturnsOK() {
        final UUID id = UUID.randomUUID();
        when(assetMappingExtractor.getCombinerById(any())).thenReturn(Optional.of(new DataCombiner(id,
                "name",
                "description",
                List.of(new EntityReference(EntityType.PULSE_AGENT, UUID.randomUUID().toString())),
                List.of(new DataCombining(UUID.randomUUID(),
                        new DataCombiningSources(new DataIdentifierReference(UUID.randomUUID().toString(),
                                DataIdentifierReference.Type.PULSE_ASSET), List.of(), List.of()),
                        new DataCombiningDestination(UUID.randomUUID().toString(), "topic", "{}"),
                        List.of())))));
        try (final Response response = pulseApi.deleteAssetMapper(id)) {
            assertThat(response.getStatus()).isEqualTo(200);
            final ArgumentCaptor<UUID> dataCombinerArgumentCaptor = ArgumentCaptor.forClass(UUID.class);
            verify(assetMappingExtractor).deleteDataCombiner(dataCombinerArgumentCaptor.capture());
            assertThat(dataCombinerArgumentCaptor.getValue()).isEqualTo(id);
        }
    }
}
