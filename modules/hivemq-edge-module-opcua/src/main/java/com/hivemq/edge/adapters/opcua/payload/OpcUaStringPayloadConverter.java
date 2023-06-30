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
package com.hivemq.edge.adapters.opcua.payload;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.DiagnosticInfo;
import org.eclipse.milo.opcua.stack.core.types.builtin.ExpandedNodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.ExtensionObject;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.builtin.XmlElement;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class OpcUaStringPayloadConverter {

    public static @NotNull ByteBuffer convertPayload(@NotNull final DataValue dataValue) {
        final String payload = convertPayloadToString(dataValue.getValue().getValue());
        return StandardCharsets.UTF_8.encode(payload != null ? payload : "");
    }

    public static @Nullable String convertPayloadToString(@NotNull final Object value) {
        if (value instanceof DataValue) {
            return convertPayloadToString(((DataValue) value).getValue().getValue());
        } else if (value instanceof Number) {
            return value.toString();
        } else if (value instanceof String) {
            return (String) value;
        } else if (value instanceof DateTime) {
            return DateTimeFormatter.ISO_INSTANT.format(((DateTime) value).getJavaInstant());
        } else if (value instanceof UUID) {
            return value.toString();
        } else if (value instanceof ByteString) {
            return new String(((ByteString) value).bytes());
        } else if (value instanceof XmlElement) {
            return ((XmlElement) value).getFragment();
        } else if (value instanceof NodeId) {
            return ((NodeId) value).toParseableString();
        } else if (value instanceof ExpandedNodeId) {
            return ((ExpandedNodeId) value).toParseableString();
        } else if (value instanceof StatusCode) {
            return Long.toString(((StatusCode) value).getValue());
        } else if (value instanceof QualifiedName) {
            return ((QualifiedName) value).toParseableString();
        } else if (value instanceof LocalizedText) {
            return ((LocalizedText) value).getText();
        } else if (value instanceof ExtensionObject) {
            return convertPayloadToString(((ExtensionObject) value).getBody());
        } else if (value instanceof Variant) {
            return convertPayloadToString(((Variant) value).getValue());
        } else if (value instanceof DiagnosticInfo) {
            final DiagnosticInfo diagnosticInfo = (DiagnosticInfo) value;
            return (diagnosticInfo.getSymbolicId() == -1 ?
                    diagnosticInfo.getLocalizedText() :
                    diagnosticInfo.getSymbolicId()) + ": " + diagnosticInfo.getAdditionalInfo();
        } else {
            //fallback, best effort
            return value.toString();
        }
    }
}
