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
package com.hivemq.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.hivemq.api.config.HttpListener;
import com.hivemq.api.model.adapters.ProtocolAdapter;
import com.hivemq.api.model.adapters.ProtocolAdapterCategory;
import com.hivemq.api.model.components.Module;
import com.hivemq.api.resources.impl.ProtocolAdapterApiUtils;
import com.hivemq.configuration.service.ApiConfigurationService;
import com.hivemq.configuration.service.ConfigurationService;
import com.hivemq.configuration.service.impl.ApiConfigurationServiceImpl;
import com.hivemq.edge.HiveMQEdgeConstants;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @author Simon L Johnson
 */
public class AdapterModelConverterTest {

    @Test
    void testCategoriesConvertToTransportModel() {

        ProtocolAdapterCategory apiCategory = ProtocolAdapterApiUtils.convertApiCategory(
                com.hivemq.adapter.sdk.api.ProtocolAdapterCategory.INDUSTRIAL);

        assertEquals(com.hivemq.adapter.sdk.api.ProtocolAdapterCategory.INDUSTRIAL.name(), apiCategory.getName());
        assertNotNull(apiCategory.getDisplayName(), "Category Display Name should not be null");
        assertNotNull(apiCategory.getDescription(), "Category Description should not be null");
        assertNull(apiCategory.getImage(), "Category Image should be null");
    }

    @Test
    void testProtocolAdapterModuleConversionUtils() {

        ConfigurationService configurationService = mock(ConfigurationService.class);
        Module testModule = ModuleModelTests.createTestModule();
        ProtocolAdapter adapter = ProtocolAdapterApiUtils.convertModuleAdapterType(testModule, configurationService);
        assertEquals(testModule.getName(), adapter.getName(), "Adapter name should match module name");
        assertEquals(testModule.getDescription(), adapter.getDescription(), "Adapter description should match");
        assertEquals(testModule.getId(), adapter.getId(), "Adapter id should match");
        assertEquals(testModule.getId(), adapter.getProtocol(), "Adapter protocol should match");
        assertEquals(false, adapter.getInstalled(), "Adapter should not be installed");
        assertEquals(testModule.getAuthor(), adapter.getAuthor(), "Adapter author should match");
        assertEquals(testModule.getVersion(), adapter.getVersion(), "Adapter version should match");
    }

    @Test
    void testProtocolAdapterImageConversionInProductionMode() {
        ConfigurationService configurationService = mock(ConfigurationService.class);
        String inputLogoUrl = "/mylogo.png";
        String resultLogoUrl =
                ProtocolAdapterApiUtils.applyAbsoluteServerAddressInDeveloperMode(inputLogoUrl, configurationService);
        assertEquals(inputLogoUrl, resultLogoUrl, "logos should not change when not in dev mode");
    }

    @Test
    void testProtocolAdapterImageConversionInDeveloperMode() {

        System.setProperty(HiveMQEdgeConstants.DEVELOPMENT_MODE, String.valueOf(true));
        ConfigurationService configurationService = mock(ConfigurationService.class);
        ApiConfigurationService apiConfig = new ApiConfigurationServiceImpl();
        apiConfig.setListeners(List.of(new HttpListener(8080, "localhost")));
        when(configurationService.apiConfiguration()).thenReturn(apiConfig);

        {
            String inputLogoUrl = "/mylogo.png";
            String resultLogoUrl = ProtocolAdapterApiUtils.applyAbsoluteServerAddressInDeveloperMode(
                    inputLogoUrl, configurationService);
            assertEquals(
                    "http://localhost:8080/mylogo.png",
                    resultLogoUrl,
                    "logos should be fully qualified change when in dev mode");
        }

        {
            String inputLogoUrl = "mylogo.png";
            String resultLogoUrl = ProtocolAdapterApiUtils.applyAbsoluteServerAddressInDeveloperMode(
                    inputLogoUrl, configurationService);
            assertEquals(
                    "http://localhost:8080/mylogo.png",
                    resultLogoUrl,
                    "logos should be fully qualified and contain correct uri separation");
        }
    }

    @Test
    void testProtocolAdapterDiscoveryDisabled() {

        ConfigurationService configurationService = mock(ConfigurationService.class);
        Module testModule = ModuleModelTests.createTestModule();
        ProtocolAdapter adapter = ProtocolAdapterApiUtils.convertModuleAdapterType(testModule, configurationService);
        assertFalse(
                adapter.getCapabilities().contains(ProtocolAdapter.Capability.DISCOVER),
                "Module generated adapter should not support discovery");
    }

    @Test
    void testProtocolAdapterReadDisabled() {

        ConfigurationService configurationService = mock(ConfigurationService.class);
        Module testModule = ModuleModelTests.createTestModule();
        ProtocolAdapter adapter = ProtocolAdapterApiUtils.convertModuleAdapterType(testModule, configurationService);
        assertFalse(
                adapter.getCapabilities().contains(ProtocolAdapter.Capability.READ),
                "Module generated adapter should not support read");
    }
}
