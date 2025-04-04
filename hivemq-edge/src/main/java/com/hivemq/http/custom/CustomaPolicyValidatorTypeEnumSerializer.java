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
package com.hivemq.http.custom;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.hivemq.edge.api.model.DataPolicyValidator;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class CustomaPolicyValidatorTypeEnumSerializer extends StdDeserializer<DataPolicyValidator.TypeEnum> {

    public CustomaPolicyValidatorTypeEnumSerializer() {
        this(null);
    }

    public CustomaPolicyValidatorTypeEnumSerializer(final @NotNull Class<?> vc) {
        super(vc);
    }

    @Override
    public DataPolicyValidator.@NotNull TypeEnum deserialize(
            final @NotNull JsonParser jp, final @NotNull DeserializationContext ctxt)
            throws IOException {
        final JsonNode node = jp.getCodec().readTree(jp);
        return DataPolicyValidator.TypeEnum.fromString(node.asText().toUpperCase());
    }


}
