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
package com.hivemq.bootstrap.factories;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.codahale.metrics.MetricRegistry;
import com.hivemq.bootstrap.services.EdgeCoreFactoryService;
import com.hivemq.combining.mapping.DataCombiningTransformationService;
import com.hivemq.combining.vanilla.VanillaDataCombiningTransformationService;
import com.hivemq.edge.HiveMQCapabilityService;
import com.hivemq.mqtt.services.PrePublishProcessorService;
import com.hivemq.edge.pulse.integration.api.management.PulseAgentStatus;
import com.hivemq.pulse.status.PulseAgentStatusChangedListener;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DataCombiningTransformationServiceProviderTest {

    private final DataCombiningTransformationServiceFactory dataCombiningTransformationServiceFactory =
            mock(DataCombiningTransformationServiceFactory.class);
    private final PrePublishProcessorService prePublishProcessorService = mock(PrePublishProcessorService.class);
    private final MetricRegistry metricRegistry = mock(MetricRegistry.class);
    private final HiveMQCapabilityService capabilityService = mock(HiveMQCapabilityService.class);
    private final DataCombiningTransformationService dataCombiningTransformationService =
            mock(DataCombiningTransformationService.class);

    private EdgeCoreFactoryService edgeCoreFactoryService;
    private DataCombiningTransformationServiceProvider dataCombiningTransformationServiceProvider;
    private PulseAgentStatusChangedListener statusChangedListener;

    @BeforeEach
    void setUp() {
        edgeCoreFactoryService = new EdgeCoreFactoryService();
        dataCombiningTransformationServiceProvider = new DataCombiningTransformationServiceProvider(
                edgeCoreFactoryService, prePublishProcessorService, metricRegistry);
        statusChangedListener = new PulseAgentStatusChangedListener(capabilityService);

        when(dataCombiningTransformationServiceFactory.build(
                        any(PrePublishProcessorService.class), any(MetricRegistry.class)))
                .thenReturn(dataCombiningTransformationService);
    }

    @Test
    void whenPulseIsActivatedAndDataHubIsActivated_thenDataCombiningTransformationServiceIsVanilla() {
        edgeCoreFactoryService.provideDataCombiningTransformationServiceFactory(
                dataCombiningTransformationServiceFactory);
        statusChangedListener.onStatusChanged(
                status(PulseAgentStatus.Status.ACTIVATED_CONNECTED));
        assertThat(edgeCoreFactoryService.getDataCombiningTransformationServiceFactory())
                .isEqualTo(dataCombiningTransformationServiceFactory);
        assertThat(dataCombiningTransformationServiceProvider.get())
                .isInstanceOf(DataCombiningTransformationService.class);
    }

    @Test
    void whenPulseIsActivatedAndDataHubIsDeactivated_thenDataCombiningTransformationServiceIsVanilla() {
        statusChangedListener.onStatusChanged(
                status(PulseAgentStatus.Status.ACTIVATED_CONNECTED));
        assertThat(edgeCoreFactoryService.getDataCombiningTransformationServiceFactory())
                .isNull();
        assertThat(dataCombiningTransformationServiceProvider.get())
                .isInstanceOf(VanillaDataCombiningTransformationService.class);
    }

    @Test
    void whenPulseIsDeactivatedAndDataHubIsActivated_thenDataCombiningTransformationServiceIsDataHub() {
        edgeCoreFactoryService.provideDataCombiningTransformationServiceFactory(
                dataCombiningTransformationServiceFactory);
        statusChangedListener.onStatusChanged(
                status(PulseAgentStatus.Status.DEACTIVATED));
        assertThat(edgeCoreFactoryService.getDataCombiningTransformationServiceFactory())
                .isEqualTo(dataCombiningTransformationServiceFactory);
        assertThat(dataCombiningTransformationServiceProvider.get()).isEqualTo(dataCombiningTransformationService);
    }

    @Test
    void whenPulseIsDeactivatedAndDataHubIsDeactivated_thenDataCombiningTransformationServiceIsVanilla() {
        statusChangedListener.onStatusChanged(
                status(PulseAgentStatus.Status.DEACTIVATED));
        assertThat(edgeCoreFactoryService.getDataCombiningTransformationServiceFactory())
                .isNull();
        assertThat(dataCombiningTransformationServiceProvider.get())
                .isInstanceOf(VanillaDataCombiningTransformationService.class);
    }

    private static PulseAgentStatus status(final PulseAgentStatus.Status statusValue) {
        return new PulseAgentStatus() {
            @Override
            public PulseAgentStatus.Status status() {
                return statusValue;
            }

            @Override
            public List<String> errorMessages() {
                return List.of();
            }
        };
    }
}
