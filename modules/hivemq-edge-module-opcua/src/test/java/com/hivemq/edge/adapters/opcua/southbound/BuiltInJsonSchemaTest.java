package com.hivemq.edge.adapters.opcua.southbound;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.milo.opcua.stack.core.OpcUaDataType;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BuiltInJsonSchemaTest {

    private static final @NotNull Set<OpcUaDataType> EXCLUDED_TYPES = Set.of(OpcUaDataType.ExtensionObject,
            OpcUaDataType.DataValue,
            OpcUaDataType.Variant,
            OpcUaDataType.DiagnosticInfo);

    @Test
    public void createJsonSchemaForBuiltInType() throws Exception {
        for (final OpcUaDataType type : OpcUaDataType.values()) {
            final JsonNode node = BuiltinJsonSchema.createJsonSchemaForBuiltInType(type);
            assertTrue(node != null || EXCLUDED_TYPES.contains(type));
            if (node != null) {
                final JsonNode expected =
                        new ObjectMapper().readTree(getClass().getResource("/schemas/" + type.name() + ".json"));
                assertEquals(expected.toPrettyString(), node.toPrettyString());
            }
        }
    }
}
