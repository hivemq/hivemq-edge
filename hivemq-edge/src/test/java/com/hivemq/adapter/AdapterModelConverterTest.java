package com.hivemq.adapter;

import com.hivemq.api.model.adapters.ProtocolAdapterCategory;
import com.hivemq.api.resources.impl.ProtocolAdapterApiUtils;
import com.hivemq.api.resources.impl.ProtocolAdaptersResourceImpl;
import com.hivemq.edge.modules.adapters.ProtocolAdapterConstants;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

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
}
