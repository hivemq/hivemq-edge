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
        copyObjectMapper.coercionConfigFor(LogicalType.Collection).
                setCoercion(CoercionInputShape.EmptyString, CoercionAction.AsNull);
        return copyObjectMapper;
    }
}
