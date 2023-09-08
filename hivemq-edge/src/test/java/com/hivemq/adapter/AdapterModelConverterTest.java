package com.hivemq.adapter;

import com.hivemq.api.config.HttpListener;
import com.hivemq.api.model.adapters.ProtocolAdapter;
import com.hivemq.api.model.adapters.ProtocolAdapterCategory;
import com.hivemq.api.model.components.Module;
import com.hivemq.api.resources.impl.ProtocolAdapterApiUtils;
import com.hivemq.configuration.service.ApiConfigurationService;
import com.hivemq.configuration.service.ConfigurationService;
import com.hivemq.configuration.service.impl.ApiConfigurationServiceImpl;
import com.hivemq.edge.HiveMQEdgeConstants;
import com.hivemq.edge.modules.adapters.ProtocolAdapterConstants;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Simon L Johnson
 */
public class AdapterModelConverterTest {

    @Test
    void testCategoriesConvertToTransportModel() {

        ProtocolAdapterCategory apiCategory =
                ProtocolAdapterApiUtils.convertApiCategory(ProtocolAdapterConstants.CATEGORY.INDUSTRIAL);

        assertEquals(ProtocolAdapterConstants.CATEGORY.INDUSTRIAL.name(), apiCategory.getName());
        assertNotNull(apiCategory.getDisplayName(), "Category Display Name should not be null");
        assertNotNull(apiCategory.getDescription(), "Category Description should not be null");
        assertNull(apiCategory.getImage(), "Category Image should be null");
    }

    @Test
    void testProtocolAdapterModuleConversionUtils() {

        ConfigurationService configurationService = mock(ConfigurationService.class);
        Module testModule = ModuleModelTests.createTestModule();
        ProtocolAdapter adapter = ProtocolAdapterApiUtils.convertModuleAdapterType(testModule,configurationService);
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
        String resultLogoUrl = ProtocolAdapterApiUtils.applyAbsoluteServerAddressInDeveloperMode(inputLogoUrl, configurationService);
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
            String resultLogoUrl = ProtocolAdapterApiUtils.applyAbsoluteServerAddressInDeveloperMode(inputLogoUrl, configurationService);
            assertEquals("http://localhost:8080/mylogo.png", resultLogoUrl, "logos should be fully qualified change when in dev mode");
        }

        {
            String inputLogoUrl = "mylogo.png";
            String resultLogoUrl = ProtocolAdapterApiUtils.applyAbsoluteServerAddressInDeveloperMode(inputLogoUrl, configurationService);
            assertEquals("http://localhost:8080/mylogo.png", resultLogoUrl, "logos should be fully qualified and contain correct uri separation");
        }
    }

    @Test
    void testProtocolAdapterDiscoveryEnabled() {

        ConfigurationService configurationService = mock(ConfigurationService.class);
        Module testModule = ModuleModelTests.createTestModule();
        ProtocolAdapter adapter = ProtocolAdapterApiUtils.convertModuleAdapterType(testModule,configurationService);
        assertEquals(false, adapter.getSupportsDiscovery(), "Adapter should not support discovery");
    }
}
