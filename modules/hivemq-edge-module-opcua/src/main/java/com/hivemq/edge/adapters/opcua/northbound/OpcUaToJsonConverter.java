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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.eclipse.milo.opcua.sdk.core.types.DynamicStructType;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.encoding.EncodingContext;
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
import java.util.Base64;
import java.util.UUID;

//see also https://reference.opcfoundation.org/Core/Part6/v105/docs/5.4
public class OpcUaToJsonConverter {

    private static final @NotNull Logger log = LoggerFactory.getLogger(OpcUaToJsonConverter.class);

    private static final @NotNull Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    public static @NotNull ByteBuffer convertPayload(
            final @NotNull EncodingContext serializationContext,
            final @NotNull DataValue dataValue) {
        final var value = dataValue.getValue().getValue();
        final JsonObject jsonObject = new JsonObject();

        if(value == null) {
            return ByteBuffer.wrap(new byte[0]);
        }

        if  (value instanceof DataValue) {
            addDataValueFields((DataValue) value, jsonObject);
        }
        final var converted = convertValue(value, serializationContext);

        if (converted instanceof final JsonObject jo) {
            jsonObject.add("value", jo);
        } else {
            jsonObject.add("value", convertValue(value, serializationContext));
        }
        return ByteBuffer.wrap(GSON.toJson(jsonObject).getBytes(StandardCharsets.UTF_8));
    }
    
    private static JsonElement convertValue(
            final @NotNull Object value,
            final @NotNull EncodingContext serializationContext) {
        if (value instanceof final DataValue dv) {
            return convertValue(dv.getValue(), serializationContext);
        } else if (value instanceof final Boolean b) {
            return new JsonPrimitive(b);
        } else if (value instanceof final Byte b) {
            return new JsonPrimitive(b);
        } else if (value instanceof final UByte ubyte) {
            return new JsonPrimitive(ubyte.intValue());
        } else if (value instanceof final Short s) {
            return new JsonPrimitive(s);
        } else if (value instanceof final UShort ushort) {
            return new JsonPrimitive(ushort.intValue());
        } else if (value instanceof final Integer i) {
            return new JsonPrimitive(i);
        } else if (value instanceof final UInteger uint) {
            return new JsonPrimitive(uint.longValue());
        } else if (value instanceof final Long l) {
            return new JsonPrimitive(l);
        } else if (value instanceof final ULong ulong) {
            return new JsonPrimitive(ulong.toBigInteger());
        } else if (value instanceof final Float f) {
            return new JsonPrimitive(f);
        } else if (value instanceof final Double d) {
            return new JsonPrimitive(d);
        } else if (value instanceof final String str) {
            return new JsonPrimitive(str);
        } else if (value instanceof final DateTime dt) {
            return new JsonPrimitive((DateTimeFormatter.ISO_INSTANT.format(dt.getJavaInstant())));
        } else if (value instanceof final UUID uuid) {
            return new JsonPrimitive(uuid.toString());
        } else if (value instanceof final ByteString bs) {
            return convertByteString(bs);
        } else if (value instanceof final XmlElement xe) {
            final String fragment = xe.getFragment();
            if (fragment != null) {
                return new JsonPrimitive(fragment);
            }
            return null;
        } else if (value instanceof final NodeId nid) {
            return convertNodeId(nid);
        } else if (value instanceof final ExpandedNodeId enid) {
            return new JsonPrimitive(enid.toParseableString());
        } else if (value instanceof final StatusCode sc) {
            return convertStatusCode(sc);
        } else if (value instanceof final QualifiedName qn) {
            final JsonObject qualifiedName = new JsonObject();
            final String name = qn.getName();
            if (name != null) {
                qualifiedName.add("name", new JsonPrimitive(name));
            }
            final int nsIdx = qn.getNamespaceIndex().intValue();
            if (nsIdx > 0) {
                qualifiedName.add("uri", new JsonPrimitive(nsIdx));
            }
            return qualifiedName;
        } else if (value instanceof final LocalizedText lt) {
            final JsonObject localizedText = new JsonObject();
            final String locale = lt.getLocale();
            if (locale != null) {
                localizedText.add("locale", new JsonPrimitive(locale));
            }
            final String text = lt.getText();
            if (text != null) {
                localizedText.add("text", new JsonPrimitive(text));
            }
            return localizedText;
        } else if (value instanceof final ExtensionObject eo) {
            try {
                final Object decodedValue = eo.decode(serializationContext);
                return convertValue(decodedValue, serializationContext);
            } catch (final Throwable t) {
                log.debug("Not able to decode body of OPC UA ExtensionObject, using undecoded body value instead",
                        t);
                return convertValue(((ExtensionObject) value).getBody(), serializationContext);
            }
        } else if (value instanceof final Variant variant) {
            if(variant.getValue() == null) {
                return null;
            } else {
                return convertValue(variant.getValue(), serializationContext);
            }
        } else if (value instanceof final DiagnosticInfo info) {
            return convertDiagnosticInfo(info);
        } else if (value instanceof final DynamicStructType struct) {
            final JsonObject structRoot = new JsonObject();
            struct.getMembers()
                    .forEach((key, value1) -> structRoot.add(key, convertValue(value1, serializationContext)));
            return structRoot;
        } else if(value.getClass().isArray()) {
            final Object[] values = (Object[])value;
            final JsonArray ret = new JsonArray();
            Arrays.asList(values).forEach(in -> ret.add(convertValue(in, serializationContext)));
            return ret;
        } else {
            log.warn("No explicit converter for OPC UA type {} falling back to best effort json", value.getClass().getSimpleName());
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
        return new JsonPrimitive(Base64.getEncoder().encodeToString(bytes));
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
        diagnosticInfo.add("symbolicId", new JsonPrimitive(value.symbolicId()));
        diagnosticInfo.add("namespaceUri", new JsonPrimitive(value.namespaceUri()));
        diagnosticInfo.add("locale", new JsonPrimitive(value.locale()));
        diagnosticInfo.add("localizedText", new JsonPrimitive(value.localizedText()));
        if (value.additionalInfo() != null) {
            diagnosticInfo.add("additionalInfo", new JsonPrimitive(value.additionalInfo()));
        }
        if (value.innerStatusCode() != null) {
            diagnosticInfo.add("innerStatusCode", convertStatusCode(value.innerStatusCode()));
        }
        if (value.innerDiagnosticInfo() != null) {
            diagnosticInfo.add("innerDiagnosticInfo",
                    convertDiagnosticInfo(value.innerDiagnosticInfo()));
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
            jsonObject.add("sourcePicoSeconds", new JsonPrimitive(dataValue.getSourcePicoseconds().intValue()));
        }
        if (dataValue.getStatusCode().getValue() > 0) {
            jsonObject.add("status", convertStatusCode(dataValue.getStatusCode()));
        }
    }
}
