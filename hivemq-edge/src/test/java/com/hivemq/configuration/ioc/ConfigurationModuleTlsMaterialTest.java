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
package com.hivemq.configuration.ioc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.hivemq.configuration.service.ConfigurationService;
import com.hivemq.security.tls.TLSMaterialProvider;
import org.junit.jupiter.api.Test;

/// The provider injected into the node is the one its configuration reader fills.
class ConfigurationModuleTlsMaterialTest {

    /// The bound provider is the very instance the configuration service holds, not a copy.
    @Test
    void givenAConfigurationService_whenTheProviderIsBound_thenItIsTheServicesOwn() {
        final TLSMaterialProvider filled = new TLSMaterialProvider();
        final ConfigurationService configurationService = mock(ConfigurationService.class);
        when(configurationService.tlsMaterialProvider()).thenReturn(filled);

        assertThat(ConfigurationModule.tlsMaterialProvider(configurationService))
                .isSameAs(filled);
    }
}
