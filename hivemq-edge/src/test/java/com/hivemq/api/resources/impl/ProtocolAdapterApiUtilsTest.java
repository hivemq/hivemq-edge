package com.hivemq.api.resources.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

class ProtocolAdapterApiUtilsTest {

    @Test
    void getUiSchemaForAdapter_whenNoneIsPresent_thenBackoffTpDefault() {
        ProtocolAdapterInformation protocolAdapterInformation = mock();
        final JsonNode uiSchemaForAdapter =
                ProtocolAdapterApiUtils.getUiSchemaForAdapter(new ObjectMapper(), protocolAdapterInformation);
        assertNotNull(uiSchemaForAdapter);
    }
}
