package com.hivemq.protocols;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.type.LogicalType;
import com.hivemq.extension.sdk.api.annotations.NotNull;

/**
 * @author Simon L Johnson
 */
public class ProtocolAdapterUtils {

    public static @NotNull ObjectMapper createProtocolAdapterMapper(@NotNull final ObjectMapper objectMapper){
        ObjectMapper copyObjectMapper = objectMapper.copy();
        copyObjectMapper.coercionConfigFor(LogicalType.POJO).
                setCoercion(CoercionInputShape.EmptyString, CoercionAction.AsNull);
        return copyObjectMapper;
    }
}
