package com.hivemq.adapter;

import com.hivemq.api.model.adapters.ProtocolAdapter;
import com.hivemq.api.model.adapters.ProtocolAdapterCategory;
import com.hivemq.api.model.components.Module;
import com.hivemq.api.resources.impl.ProtocolAdapterApiUtils;
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;
import com.hivemq.configuration.service.ConfigurationService;
import com.hivemq.edge.modules.adapters.ProtocolAdapterConstants;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

/**
 * @author Simon L Johnson
 */
public class AdapterModelConverterTest {

    ConfigurationService configurationService = mock(ConfigurationService.class);

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
}
