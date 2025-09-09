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

package com.hivemq.api.pulse;

import com.hivemq.configuration.entity.pulse.PulseAssetsEntity;
import com.hivemq.configuration.entity.pulse.PulseEntity;
import com.hivemq.configuration.reader.PulseExtractor;
import com.hivemq.configuration.service.ConfigurationService;
import com.hivemq.edge.api.PulseApi;
import com.hivemq.pulse.asset.AssetProviderRegistry;
import com.hivemq.pulse.asset.ExternalAssetProvider;
import com.hivemq.pulse.status.StatusProvider;
import com.hivemq.pulse.status.StatusProviderRegistry;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Set;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public abstract class AbstractPulseApiImplTest {
    @Mock
    protected @NotNull ExternalAssetProvider assetProvider;

    @Mock
    protected @NotNull AssetProviderRegistry assetProviderRegistry;

    @Mock
    protected @NotNull StatusProvider statusProvider;

    @Mock
    protected @NotNull StatusProviderRegistry statusProviderRegistry;

    @Mock
    protected @NotNull ConfigurationService configurationService;

    @Mock
    protected @NotNull PulseExtractor pulseExtractor;

    @Mock
    protected @NotNull PulseEntity pulseEntity;

    @Mock
    protected @NotNull PulseAssetsEntity pulseAssetsEntity;

    protected @NotNull PulseApi pulseApi;

    @BeforeEach
    public void setUp() {
        when(configurationService.pulseExtractor()).thenReturn(pulseExtractor);
        when(pulseExtractor.getPulseEntity()).thenReturn(pulseEntity);
        when(pulseEntity.getLock()).thenReturn(new Object());
        when(pulseEntity.getPulseAssetsEntity()).thenReturn(pulseAssetsEntity);
        when(assetProviderRegistry.getAssetProviders()).thenReturn(Set.of(assetProvider));
        when(statusProviderRegistry.getStatusProviders()).thenReturn(Set.of(statusProvider));
        pulseApi = new PulseApiImpl(configurationService, assetProviderRegistry, statusProviderRegistry);
    }
}
