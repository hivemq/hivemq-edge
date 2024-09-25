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
package com.hivemq.edge.adapters.opcua.mqtt2opcua;

import org.eclipse.milo.opcua.binaryschema.GenericEnumCodec;
import org.eclipse.milo.opcua.binaryschema.GenericStructCodec;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.BuiltinDataType;
import org.eclipse.milo.opcua.stack.core.serialization.codecs.DataTypeCodec;
import org.jetbrains.annotations.NotNull;
import org.opcfoundation.opcua.binaryschema.FieldType;

public final class BuiltInDataTypeConverter {

    private BuiltInDataTypeConverter() {
    }

    public static @NotNull BuiltinDataType convertFieldTypeToBuiltInDataType(
            final @NotNull FieldType fieldType,
            final @NotNull OpcUaClient client) {
        final String namespaceURI = fieldType.getTypeName().getNamespaceURI();
        final boolean isStandard = namespaceURI.startsWith("http://opcfoundation.org/");
        if (isStandard) {
            final String localPart = fieldType.getTypeName().getLocalPart();
            return BuiltinDataType.valueOf(localPart);
        } else {
            final DataTypeCodec dataTypeCodec =
                    client.getDynamicDataTypeManager().getCodec(namespaceURI, fieldType.getTypeName().getLocalPart());
            if (dataTypeCodec instanceof GenericEnumCodec) {
                // enum are encoded as integers
                return BuiltinDataType.Int32;
            } else if (dataTypeCodec instanceof GenericStructCodec) {
                return BuiltinDataType.ExtensionObject;
            }
        }
        throw new IllegalArgumentException();
    }
}
