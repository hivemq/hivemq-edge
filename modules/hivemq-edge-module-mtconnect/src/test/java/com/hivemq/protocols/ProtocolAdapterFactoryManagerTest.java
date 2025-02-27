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

import com.hivemq.adapter.sdk.api.factories.ProtocolAdapterFactory;
import com.hivemq.configuration.info.SystemInformationImpl;
import com.hivemq.edge.HiveMQEdgeConstants;
import com.hivemq.edge.impl.events.EventServiceDelegateImpl;
import com.hivemq.edge.impl.events.InMemoryEventImpl;
import com.hivemq.edge.modules.ModuleLoader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class ProtocolAdapterFactoryManagerTest {
    @BeforeEach
    public void setUp() {
        System.setProperty(HiveMQEdgeConstants.DEVELOPMENT_MODE, String.valueOf(true));
    }

    @AfterEach
    public void tearDown() {
        System.setProperty(HiveMQEdgeConstants.DEVELOPMENT_MODE, String.valueOf(false));
    }

    @Test
    public void testProtocolAdapterFactoryManagerFindAllAdapters() {
        final ModuleLoader moduleLoader = new ModuleLoader(new SystemInformationImpl(true));
        moduleLoader.loadModules();
        final Set<ModuleLoader.EdgeModule> modules = moduleLoader.getModules();
        assertThat(modules)
                .isNotEmpty()
                .filteredOn(module -> "hivemq-edge-module-mtconnect".equals(module.getRoot().getName()))
                .hasSize(1);
        final Map<String, ProtocolAdapterFactory<?>> adapterFactoryMap =
                ProtocolAdapterFactoryManager.findAllAdapters(
                        moduleLoader,
                        new EventServiceDelegateImpl(new InMemoryEventImpl()),
                        true);
        assertThat(adapterFactoryMap)
                .isNotEmpty()
                .containsKey("mtconnect");
    }
}
