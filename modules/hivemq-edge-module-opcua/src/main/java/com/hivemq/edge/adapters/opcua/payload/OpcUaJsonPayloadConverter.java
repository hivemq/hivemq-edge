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

import com.google.common.io.BaseEncoding;
import com.google.gson.*;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import org.eclipse.milo.opcua.binaryschema.Struct;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.types.builtin.*;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UByte;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.ULong;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UShort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;

//see also https://reference.opcfoundation.org/Core/Part6/v105/docs/5.4
public class OpcUaJsonPayloadConverter {

    private static final Logger log = LoggerFactory.getLogger(OpcUaJsonPayloadConverter.class);

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    public static @NotNull ByteBuffer convertPayload(
            final @NotNull OpcUaClient opcUaClient, @NotNull final DataValue dataValue) {
        final Object value = dataValue.getValue().getValue();
        final boolean reversibleMode = false;
        final JsonObject jsonObject = new JsonObject();
        if (reversibleMode) {
            addDataValueFields(dataValue, jsonObject, reversibleMode);
        }
        convertValue(value, jsonObject, reversibleMode, "value", opcUaClient);
        return ByteBuffer.wrap(GSON.toJson(jsonObject).getBytes(StandardCharsets.UTF_8));
    }

    private static void convertValue(
            final @NotNull Object value,
            final @NotNull JsonObject holder,
            final boolean reversibleMode,
            final @NotNull String fieldName,
            final @NotNull OpcUaClient opcUaClient) {
        if (value instanceof DataValue) {
            addDataValueFields((DataValue) value, holder, reversibleMode);
            convertValue(((DataValue) value).getValue(), holder, reversibleMode, fieldName, opcUaClient);
        } else if (value instanceof Boolean) {
            holder.add(fieldName, new JsonPrimitive((Boolean) value));
        } else if (value instanceof Byte) {
            holder.add(fieldName, new JsonPrimitive((Byte) value));
        } else if (value instanceof UByte) {
            holder.add(fieldName, new JsonPrimitive(((UByte) value).intValue()));
        } else if (value instanceof Short) {
            holder.add(fieldName, new JsonPrimitive((Short) value));
        } else if (value instanceof UShort) {
            holder.add(fieldName, new JsonPrimitive(((UShort) value).intValue()));
        } else if (value instanceof Integer) {
            holder.add(fieldName, new JsonPrimitive(((Integer) value)));
        } else if (value instanceof UInteger) {
            holder.add(fieldName, new JsonPrimitive((((UInteger) value).longValue())));
        } else if (value instanceof Long) {
            holder.add(fieldName, new JsonPrimitive(((Long) value)));
        } else if (value instanceof ULong) {
            holder.add(fieldName, new JsonPrimitive((((ULong) value).toBigInteger())));
        } else if (value instanceof Float) {
            holder.add(fieldName, new JsonPrimitive(((Float) value)));
        } else if (value instanceof Double) {
            holder.add(fieldName, new JsonPrimitive(((Double) value)));
        } else if (value instanceof String) {
            holder.add(fieldName, new JsonPrimitive(((String) value)));
        } else if (value instanceof DateTime) {
            holder.add(fieldName,
                    new JsonPrimitive((DateTimeFormatter.ISO_INSTANT.format(((DateTime) value).getJavaInstant()))));
        } else if (value instanceof UUID) {
            holder.add(fieldName, new JsonPrimitive(value.toString()));
        } else if (value instanceof ByteString) {
            final JsonPrimitive base64value = convertByteString((ByteString) value);
            holder.add(fieldName, base64value);
        } else if (value instanceof XmlElement) {
            final String fragment = ((XmlElement) value).getFragment();
            if (fragment != null) {
                holder.add(fieldName, new JsonPrimitive(fragment));
            }
        } else if (value instanceof NodeId) {
            final JsonObject nodeIdObj = convertNodeId((NodeId) value, reversibleMode, fieldName);
            holder.add(fieldName, nodeIdObj);
        } else if (value instanceof ExpandedNodeId) {
            holder.add(fieldName, new JsonPrimitive(((ExpandedNodeId) value).toParseableString()));
        } else if (value instanceof StatusCode) {
            holder.add(fieldName, convertStatusCode((StatusCode) value, reversibleMode));
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
            holder.add(fieldName, qualifiedName);
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
            holder.add(fieldName, localizedText);
        } else if (value instanceof ExtensionObject) {
            if (!reversibleMode) {
                try {
                    final Object decodedValue =
                            ((ExtensionObject) value).decode(opcUaClient.getDynamicSerializationContext());
                    convertValue(decodedValue, holder, reversibleMode, fieldName, opcUaClient);
                } catch (Throwable t) {
                    log.debug("Not able to decode body of OPC-UA ExtensionObject, using undecoded body value instead",
                            t);
                    convertValue(((ExtensionObject) value).getBody(), holder, reversibleMode, fieldName, opcUaClient);
                }

            } else {
                final JsonObject extensionObject = new JsonObject();
                extensionObject.add("typeId",
                        new JsonPrimitive(((ExtensionObject) value).getEncodingId().toParseableString()));
                final ExtensionObject.BodyType bodyType = ((ExtensionObject) value).getBodyType();
                if (bodyType != null) {
                    switch (bodyType) {
                        case ByteString:
                            extensionObject.add("encoding", new JsonPrimitive(1));
                        case XmlElement:
                            extensionObject.add("encoding", new JsonPrimitive(2));
                    }
                }

                final Object decodedValue =
                        ((ExtensionObject) value).decode(opcUaClient.getDynamicSerializationContext());
                convertValue(decodedValue, extensionObject, reversibleMode, "body", opcUaClient);
                holder.add(fieldName, extensionObject);
            }
        } else if (value instanceof Variant) {
            if (!reversibleMode) {
                convertValue(((Variant) value).getValue(), holder, reversibleMode, fieldName, opcUaClient);
            } else {
                final JsonObject variant = new JsonObject();
                final Optional<ExpandedNodeId> dataType = ((Variant) value).getDataType();
                if (dataType.isPresent()) {
                    final Number typeId = dataType.get().getNamespaceIndex();
                    variant.add("type", new JsonPrimitive(typeId));
                }
                convertValue(((Variant) value).getValue(), variant, reversibleMode, "body", opcUaClient);
                holder.add(fieldName, variant);
            }
        } else if (value instanceof DiagnosticInfo) {
            final JsonObject diagnosticInfo = convertDiagnosticInfo((DiagnosticInfo) value, reversibleMode);
            holder.add(fieldName, diagnosticInfo);
        } else if (value instanceof Struct) {
            final Struct struct = (Struct) value;
            final JsonObject structRoot = new JsonObject();
            for (Struct.Member member : struct.getMembers().values()) {
                convertValue(member.getValue(), structRoot, reversibleMode, member.getName(), opcUaClient);
            }
            holder.add(fieldName, structRoot);
        } else {
            //fallback, best effort
            if (log.isTraceEnabled()) {
                log.trace("No explicit converter for OPC-UA type " +
                        value.getClass().getSimpleName() +
                        " falling back to best effort json");
            }
            holder.add(fieldName, GSON.toJsonTree(value));
        }
    }

    @NotNull
    private static JsonElement convertStatusCode(final @NotNull StatusCode value, final boolean reversibleMode) {
        if (reversibleMode) {
            return new JsonPrimitive(value.getValue());
        }

        final JsonObject statusCode = new JsonObject();
        final long statusCodeNr = value.getValue();
        statusCode.add("code", new JsonPrimitive(statusCodeNr));
        final Optional<String[]> statusNamingOptional = StatusCodes.lookup(statusCodeNr);
        if (statusNamingOptional.isPresent()) {
            statusCode.add("symbol", new JsonPrimitive(statusNamingOptional.get()[0]));
        }
        return statusCode;
    }

    @NotNull
    private static JsonPrimitive convertByteString(final @NotNull ByteString value) {
        final byte[] bytes = value.bytesOrEmpty();
        return new JsonPrimitive(BaseEncoding.base64().encode(bytes));
    }

    @NotNull
    private static JsonObject convertNodeId(
            final @NotNull NodeId nodeId, boolean reversibleMode, @NotNull String fieldName) {
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
        if (reversibleMode) {
            if (namespaceIndex != 0) {
                nodeIdObj.add("namespace", new JsonPrimitive(namespaceIndex));
            }
        } else {
            if (namespaceIndex == 1) { // 1 is always encoded as a number
                nodeIdObj.add("namespace", new JsonPrimitive(namespaceIndex));
            } else {
                nodeIdObj.add("namespace", new JsonPrimitive(nodeId.toParseableString()));
            }
        }
        return nodeIdObj;
    }

    @NotNull
    private static JsonObject convertExpandedNodeId(
            final @NotNull ExpandedNodeId nodeId, boolean reversibleMode, @NotNull String fieldName) {
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
        if (reversibleMode) {
            if (namespaceIndex != 0) {
                nodeIdObj.add("namespace", new JsonPrimitive(namespaceIndex));
            }
        } else {
            if (namespaceIndex == 1) { // 1 is always encoded as a number
                nodeIdObj.add("namespace", new JsonPrimitive(namespaceIndex));
            } else {
                nodeIdObj.add("namespace", new JsonPrimitive(nodeId.toParseableString()));
            }
        }

        nodeIdObj.add("serverUri", new JsonPrimitive(nodeId.getServerIndex().longValue()));
        return nodeIdObj;
    }

    private static @NotNull JsonObject convertDiagnosticInfo(DiagnosticInfo value, final boolean reversibleMode) {
        final JsonObject diagnosticInfo = new JsonObject();
        diagnosticInfo.add("symbolicId", new JsonPrimitive(value.getSymbolicId()));
        diagnosticInfo.add("namespaceUri", new JsonPrimitive(value.getNamespaceUri()));
        diagnosticInfo.add("locale", new JsonPrimitive(value.getLocale()));
        diagnosticInfo.add("localizedText", new JsonPrimitive(value.getLocalizedText()));
        if (value.getAdditionalInfo() != null) {
            diagnosticInfo.add("additionalInfo", new JsonPrimitive(value.getAdditionalInfo()));
        }
        if (value.getInnerStatusCode() != null) {
            diagnosticInfo.add("innerStatusCode", convertStatusCode(value.getInnerStatusCode(), reversibleMode));
        }
        if (value.getInnerDiagnosticInfo() != null) {
            diagnosticInfo.add("innerDiagnosticInfo",
                    convertDiagnosticInfo(value.getInnerDiagnosticInfo(), reversibleMode));
        }
        return diagnosticInfo;
    }

    private static void addDataValueFields(
            final @NotNull DataValue dataValue, final @NotNull JsonObject jsonObject, final boolean reversibleMode) {
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
            jsonObject.add("status", convertStatusCode(dataValue.getStatusCode(), reversibleMode));
        }
    }
}
