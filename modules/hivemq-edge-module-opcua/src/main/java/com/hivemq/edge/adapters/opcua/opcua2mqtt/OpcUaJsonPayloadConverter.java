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
package com.hivemq.edge.adapters.opcua.opcua2mqtt;

import com.google.common.io.BaseEncoding;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTag;
import org.eclipse.milo.opcua.binaryschema.Struct;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.serialization.SerializationContext;
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
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UByte;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.ULong;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UShort;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.UUID;

//see also https://reference.opcfoundation.org/Core/Part6/v105/docs/5.4
public class OpcUaJsonPayloadConverter {

    private static final Logger log = LoggerFactory.getLogger(OpcUaJsonPayloadConverter.class);

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    private static @NotNull OpcuaTag tag;

    public static @NotNull ByteBuffer convertPayload(
            final @NotNull SerializationContext serializationContext,
            final @NotNull DataValue dataValue) {
        final Object value = dataValue.getValue().getValue();
        final JsonObject jsonObject = new JsonObject();

        if  (value instanceof DataValue) {
            addDataValueFields((DataValue) value, jsonObject);
        }
        jsonObject.add("value", convertValue(value, serializationContext));

        return ByteBuffer.wrap(GSON.toJson(jsonObject).getBytes(StandardCharsets.UTF_8));
    }
    
    private static JsonElement convertValue(
            final @NotNull Object value,
            final @NotNull SerializationContext serializationContext) {
        if (value instanceof DataValue) {
            return convertValue(((DataValue) value).getValue(), serializationContext);
        } else if (value instanceof Boolean) {
            return new JsonPrimitive((Boolean) value);
        } else if (value instanceof Byte) {
            return new JsonPrimitive((Byte) value);
        } else if (value instanceof UByte) {
            return new JsonPrimitive(((UByte) value).intValue());
        } else if (value instanceof Short) {
            return new JsonPrimitive((Short) value);
        } else if (value instanceof UShort) {
            return new JsonPrimitive(((UShort) value).intValue());
        } else if (value instanceof Integer) {
            return new JsonPrimitive(((Integer) value));
        } else if (value instanceof UInteger) {
            return new JsonPrimitive((((UInteger) value).longValue()));
        } else if (value instanceof Long) {
            return new JsonPrimitive(((Long) value));
        } else if (value instanceof ULong) {
            return new JsonPrimitive((((ULong) value).toBigInteger()));
        } else if (value instanceof Float) {
            return new JsonPrimitive(((Float) value));
        } else if (value instanceof Double) {
            return new JsonPrimitive(((Double) value));
        } else if (value instanceof String) {
            return new JsonPrimitive(((String) value));
        } else if (value instanceof DateTime) {
            return new JsonPrimitive((DateTimeFormatter.ISO_INSTANT.format(((DateTime) value).getJavaInstant())));
        } else if (value instanceof UUID) {
            return new JsonPrimitive(value.toString());
        } else if (value instanceof ByteString) {
            return convertByteString((ByteString) value);
        } else if (value instanceof XmlElement) {
            final String fragment = ((XmlElement) value).getFragment();
            if (fragment != null) {
                return new JsonPrimitive(fragment);
            }
            return null;
        } else if (value instanceof NodeId) {
            return convertNodeId((NodeId) value);
        } else if (value instanceof ExpandedNodeId) {
            return new JsonPrimitive(((ExpandedNodeId) value).toParseableString());
        } else if (value instanceof StatusCode) {
            return convertStatusCode((StatusCode) value);
        } else if (value instanceof QualifiedName) {
            final JsonObject qualifiedName = new JsonObject();
            final String name = ((QualifiedName) value).getName();
            if (name != null) {
                qualifiedName.add("name", new JsonPrimitive(name));
            }
            final int nsIdx = ((QualifiedName) value).getNamespaceIndex().intValue();
            if (nsIdx > 0) {
                qualifiedName.add("uri", new JsonPrimitive(nsIdx));
            }
            return qualifiedName;
        } else if (value instanceof LocalizedText) {
            final JsonObject localizedText = new JsonObject();
            final String locale = ((LocalizedText) value).getLocale();
            if (locale != null) {
                localizedText.add("locale", new JsonPrimitive(locale));
            }
            final String text = ((LocalizedText) value).getText();
            if (text != null) {
                localizedText.add("text", new JsonPrimitive(text));
            }
            return localizedText;
        } else if (value instanceof ExtensionObject) {
            try {
                final Object decodedValue = ((ExtensionObject) value).decode(serializationContext);
                return convertValue(decodedValue, serializationContext);
            } catch (final Throwable t) {
                log.debug("Not able to decode body of OPC UA ExtensionObject, using undecoded body value instead",
                        t);
                return convertValue(((ExtensionObject) value).getBody(), serializationContext);
            }
        } else if (value instanceof Variant) {
            return convertValue(((Variant) value).getValue(), serializationContext);
        } else if (value instanceof DiagnosticInfo) {
            return convertDiagnosticInfo((DiagnosticInfo) value);
        } else if (value instanceof Struct) {
            final Struct struct = (Struct) value;
            final JsonObject structRoot = new JsonObject();
            struct.getMembers()
                    .values()
                    .forEach(member -> structRoot.add(member.getName(), convertValue(member.getValue(), serializationContext)));
            return structRoot;
        } else if(value.getClass().isArray()) {
            final Object[] values = (Object[])value;
            final JsonArray ret = new JsonArray();
            Arrays.asList(values).forEach(in -> ret.add(convertValue(in, serializationContext)));
            return ret;
        } else {
            log.warn("No explicit converter for OPC UA type " +
                    value.getClass().getSimpleName() +
                    " falling back to best effort json");
            return GSON.toJsonTree(value);
        }
    }

    @NotNull
    private static JsonElement convertStatusCode(final @NotNull StatusCode value) {
        final JsonObject statusCode = new JsonObject();
        final long statusCodeNr = value.getValue();
        statusCode.add("code", new JsonPrimitive(statusCodeNr));
        StatusCodes
                .lookup(statusCodeNr)
                .ifPresent(code -> statusCode.add("symbol", new JsonPrimitive(code[0])));
        return statusCode;
    }

    @NotNull
    private static JsonPrimitive convertByteString(final @NotNull ByteString value) {
        final byte[] bytes = value.bytesOrEmpty();
        return new JsonPrimitive(BaseEncoding.base64().encode(bytes));
    }

    @NotNull
    private static JsonObject convertNodeId(
            final @NotNull NodeId nodeId) {
        final JsonObject nodeIdObj = new JsonObject();

        switch (nodeId.getType()) {
            case Numeric:
                nodeIdObj.add("id", new JsonPrimitive((Number) nodeId.getIdentifier()));
                break;
            case String:
                nodeIdObj.add("idType", new JsonPrimitive(1));
                nodeIdObj.add("id", new JsonPrimitive((String) nodeId.getIdentifier()));
                break;
            case Guid:
                nodeIdObj.add("idType", new JsonPrimitive(2));
                nodeIdObj.add("id", new JsonPrimitive(nodeId.getIdentifier().toString())); //UUID.toString()
                break;
            case Opaque: //ByteString
                nodeIdObj.add("idType", new JsonPrimitive(3));
                nodeIdObj.add("id", convertByteString((ByteString) nodeId.getIdentifier())); //UUID.toString()
                break;
        }

        final int namespaceIndex = nodeId.getNamespaceIndex().intValue();
        if (namespaceIndex == 1) { // 1 is always encoded as a number
            nodeIdObj.add("namespace", new JsonPrimitive(namespaceIndex));
        } else {
            nodeIdObj.add("namespace", new JsonPrimitive(nodeId.toParseableString()));
        }
        return nodeIdObj;
    }

    private static @NotNull JsonObject convertDiagnosticInfo(final DiagnosticInfo value) {
        final JsonObject diagnosticInfo = new JsonObject();
        diagnosticInfo.add("symbolicId", new JsonPrimitive(value.getSymbolicId()));
        diagnosticInfo.add("namespaceUri", new JsonPrimitive(value.getNamespaceUri()));
        diagnosticInfo.add("locale", new JsonPrimitive(value.getLocale()));
        diagnosticInfo.add("localizedText", new JsonPrimitive(value.getLocalizedText()));
        if (value.getAdditionalInfo() != null) {
            diagnosticInfo.add("additionalInfo", new JsonPrimitive(value.getAdditionalInfo()));
        }
        if (value.getInnerStatusCode() != null) {
            diagnosticInfo.add("innerStatusCode", convertStatusCode(value.getInnerStatusCode()));
        }
        if (value.getInnerDiagnosticInfo() != null) {
            diagnosticInfo.add("innerDiagnosticInfo",
                    convertDiagnosticInfo(value.getInnerDiagnosticInfo()));
        }
        return diagnosticInfo;
    }

    private static void addDataValueFields(
            final @NotNull DataValue dataValue, final @NotNull JsonObject jsonObject) {
        if (dataValue.getServerTime() != null) {
            final Instant javaInstant = dataValue.getServerTime().getJavaInstant();
            jsonObject.add("serverTimestamp", new JsonPrimitive(DateTimeFormatter.ISO_INSTANT.format(javaInstant)));
        }
        if (dataValue.getSourceTime() != null) {
            final Instant javaInstant = dataValue.getSourceTime().getJavaInstant();
            jsonObject.add("sourceTimestamp", new JsonPrimitive(DateTimeFormatter.ISO_INSTANT.format(javaInstant)));
        }
        if (dataValue.getServerPicoseconds() != null) {
            jsonObject.add("serverPicoSeconds", new JsonPrimitive(dataValue.getServerPicoseconds().intValue()));
        }
        if (dataValue.getSourcePicoseconds() != null) {
            jsonObject.add("sourcePicoSeconds", new JsonPrimitive(dataValue.getServerPicoseconds().intValue()));
        }
        if (dataValue.getStatusCode() != null && dataValue.getStatusCode().getValue() > 0) {
            jsonObject.add("status", convertStatusCode(dataValue.getStatusCode()));
        }
    }
}
