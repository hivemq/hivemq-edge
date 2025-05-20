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

import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.OpcUaDataType;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.jetbrains.annotations.NotNull;
import org.opcfoundation.opcua.binaryschema.FieldType;

public final class BuiltInDataTypeConverter {

    private BuiltInDataTypeConverter() {
    }

    public static @NotNull OpcUaDataType convertFieldTypeToBuiltInDataType_old(
            final @NotNull FieldType fieldType,
            final @NotNull OpcUaClient client) {
        final String namespaceURI = fieldType.getTypeName().getNamespaceURI();
        final boolean isStandard = namespaceURI.startsWith("http://opcfoundation.org/");
        if (isStandard) {
            final String localPart = fieldType.getTypeName().getLocalPart();
            return OpcUaDataType.valueOf(localPart);
        } else {
            try {
                final var dataTypeCodec = client
                        .getDynamicDataTypeManager()
                        .getTypeDictionary(namespaceURI)
                        .getType(fieldType.getTypeName().getLocalPart())
                        .getCodec();
                //TODO fix this
                throw new RuntimeException("Enum handling not implemented yer");
//            if (dataTypeCodec instanceof GenericEnumCodec) {
//                // enum are encoded as integers
//                return OpcUaDataType.Int32;
//            } else if (dataTypeCodec instanceof StructCodec) {
//                return OpcUaDataType.ExtensionObject;
//            }
            } catch (UaException e) {
                throw new RuntimeException(e);
            }
        }
//        throw new IllegalArgumentException();
    }

}
