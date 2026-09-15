/*
 * Copyright 2023-present HiveMQ GmbH
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
package com.hivemq.edge.adapters.opcua.northbound;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hivemq.adapter.sdk.api.datapoint.DataPointBuilder;
import com.hivemq.datapoint.DataPointWithMetadata;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTag;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTagDefinition;
import org.eclipse.milo.opcua.stack.core.UaSerializationException;
import org.eclipse.milo.opcua.stack.core.encoding.DefaultEncodingContext;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.ExtensionObject;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.junit.jupiter.api.Test;

/**
 * EDG-776: a custom-struct value whose codec is not (yet) registered in the encoding context must
 * fail loudly instead of being silently published as the base64-encoded binary body.
 */
class OpcUaToJsonConverterDecodeFailureTest {

    @Test
    void whenNoCodecRegisteredForCustomStruct_thenDecodeFailurePropagates() {
        // a custom-struct ExtensionObject whose binary encoding id is unknown to the encoding
        // context, as happens when the dynamic codec registry is incomplete after a session
        // (re)activation
        final ExtensionObject extensionObject =
                ExtensionObject.of(ByteString.of(new byte[] {1, 2, 3}), NodeId.parse("ns=2;i=5001"));
        final DataValue dataValue = new DataValue(new Variant(extensionObject), StatusCode.GOOD, null);
        final DataPointBuilder<Void> builder = DataPointWithMetadata.builder(
                new OpcuaTag("test-tag", "", new OpcuaTagDefinition("ns=2;i=1001")), b -> null);

        assertThatThrownBy(
                        () -> OpcUaToJsonConverter.convertPayload(DefaultEncodingContext.INSTANCE, dataValue, builder))
                .isInstanceOf(UaSerializationException.class)
                .hasMessageContaining("no codec registered");
    }
}
