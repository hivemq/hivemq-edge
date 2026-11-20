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
package com.hivemq.protocols;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.codahale.metrics.MetricRegistry;
import com.hivemq.adapter.sdk.api.ProtocolAdapter;
import com.hivemq.adapter.sdk.api.config.ProtocolSpecificAdapterConfig;
import com.hivemq.adapter.sdk.api.events.EventService;
import com.hivemq.adapter.sdk.api.factories.ProtocolAdapterFactory;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterInput;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterPublishService;
import com.hivemq.configuration.reader.ProtocolAdapterExtractor;
import com.hivemq.edge.HiveMQEdgeRemoteService;
import com.hivemq.edge.VersionProvider;
import com.hivemq.edge.modules.adapters.data.TagManager;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterPollingService;
import com.hivemq.protocols.northbound.NorthboundConsumerFactory;
import com.hivemq.security.tls.TLSMaterialProvider;
import com.hivemq.security.tls.TLSMaterialSource;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

/// The manager hands every adapter the node's TLS material through the input it builds for it.
class ProtocolAdapterManagerTlsMaterialTest {

    /// The input an adapter is created with is a TLS material source carrying the node's provider.
    @Test
    void givenTheNodesProvider_whenAnAdapterIsCreated_thenItsInputCarriesThatProvider() {
        final TLSMaterialProvider nodeProvider = new TLSMaterialProvider();
        final AtomicReference<ProtocolAdapterInput<?>> handed = new AtomicReference<>();
        final ProtocolAdapterFactoryManager factoryManager = mock(ProtocolAdapterFactoryManager.class);
        doReturn(Optional.of(capturingFactory(handed))).when(factoryManager).get("test");
        final ProtocolAdapterManager manager = managerWith(factoryManager, nodeProvider);

        manager.createProtocolAdapter(
                new ProtocolAdapterConfig(
                        "adapter-1",
                        "test",
                        1,
                        mock(ProtocolSpecificAdapterConfig.class),
                        List.of(),
                        List.of(),
                        List.of()),
                "1");

        assertThat(handed.get())
                .isInstanceOfSatisfying(TLSMaterialSource.class, source -> assertThat(source.tlsMaterialProvider())
                        .isSameAs(nodeProvider));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static @NotNull ProtocolAdapterFactory<?> capturingFactory(
            final @NotNull AtomicReference<ProtocolAdapterInput<?>> handed) {
        final ProtocolAdapterFactory factory = mock(ProtocolAdapterFactory.class);
        when(factory.createAdapter(any(), any())).thenAnswer(invocation -> {
            handed.set(invocation.getArgument(1));
            return mock(ProtocolAdapter.class);
        });
        return factory;
    }

    private static @NotNull ProtocolAdapterManager managerWith(
            final @NotNull ProtocolAdapterFactoryManager factoryManager,
            final @NotNull TLSMaterialProvider tlsMaterialProvider) {
        return new ProtocolAdapterManager(
                mock(MetricRegistry.class),
                mock(HiveMQEdgeRemoteService.class),
                mock(EventService.class),
                mock(ProtocolAdapterConfigConverter.class),
                mock(VersionProvider.class),
                mock(ProtocolAdapterPollingService.class),
                mock(ProtocolAdapterMetrics.class),
                mock(InternalProtocolAdapterWritingService.class),
                factoryManager,
                mock(NorthboundConsumerFactory.class),
                mock(TagManager.class),
                mock(ProtocolAdapterExtractor.class),
                mock(ProtocolAdapterPublishService.class),
                tlsMaterialProvider);
    }
}
