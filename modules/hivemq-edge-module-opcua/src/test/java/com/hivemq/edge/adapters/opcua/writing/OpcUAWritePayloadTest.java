package com.hivemq.edge.adapters.opcua.writing;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class OpcUAWritePayloadTest {


    @Test
    void test_whenDataGetsConvertedToJsonAndThenConvertedBack_thenObjectsShouldBeEqual() throws IOException {
        OpcUAWritePayload payload = new OpcUAWritePayload("ns=2;i=999", 1337, OpcUaValueType.INTEGER);

        ObjectMapper objectMapper = new ObjectMapper();
        final byte[] bytes = objectMapper.writeValueAsBytes(payload);
        System.err.println(new String(bytes));

        final OpcUAWritePayload afterConversion = objectMapper.readValue(bytes, OpcUAWritePayload.class);
        assertEquals(payload, afterConversion);

        final Object opcUAObject =
                JsonToOpcUAConverter.convertToOpcUAValue(afterConversion.getValue(), afterConversion.getType());
        assertInstanceOf(Integer.class, opcUAObject);
        assertEquals(1337, opcUAObject);

    }
}
