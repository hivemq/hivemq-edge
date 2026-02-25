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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.base.Preconditions;
import com.hivemq.adapter.sdk.api.config.ProtocolSpecificAdapterConfig;
import java.util.Iterator;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;

/**
 * @author Simon L Johnson
 */
public class ProtocolAdapterConfigModelTest {

    private static final ObjectMapper mapper = new ObjectMapper();

    @BeforeAll
    static void beforeStart() {
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.setSerializationInclusion(JsonInclude.Include.ALWAYS);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    private static JsonNode findFirstChild(final @NotNull JsonNode parent, final @NotNull String nodeName) {
        Preconditions.checkNotNull(parent);
        JsonNode child = parent.get(nodeName);
        if (child != null) {
            return child;
        } else {
            final Iterator<JsonNode> nodes = parent.iterator();
            while (nodes.hasNext()) {
                if ((child = findFirstChild(nodes.next(), nodeName)) != null) {
                    return child;
                }
            }
        }
        return null;
    }

    static class SpecificAdapterConfiguration implements ProtocolSpecificAdapterConfig {}
}
