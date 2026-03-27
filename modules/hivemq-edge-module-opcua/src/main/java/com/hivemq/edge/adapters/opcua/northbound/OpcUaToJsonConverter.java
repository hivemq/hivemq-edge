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

import com.hivemq.adapter.sdk.api.datapoint.DataPointBuilder;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.UUID;
import org.eclipse.milo.opcua.sdk.core.types.DynamicEnumType;
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
import org.eclipse.milo.opcua.stack.core.types.enumerated.IdType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// see also https://reference.opcfoundation.org/Core/Part6/v105/docs/5.4
public class OpcUaToJsonConverter {

    private static final @NotNull Logger log = LoggerFactory.getLogger(OpcUaToJsonConverter.class);

    private static final @NotNull Base64.Encoder BASE_64 = Base64.getEncoder();

    public static void convertPayload(
            final @NotNull EncodingContext serializationContext,
            final @NotNull DataValue dataValue,
            final @NotNull DataPointBuilder<?> builder) {
        final Object value = dataValue.getValue().getValue();
        if (value == null) {
            builder.valueNull();
        } else {
            addValue(builder, value, serializationContext);
        }
        final var metadataBuilder = builder.startObjectMetadata();
        if (dataValue.getStatusCode().getValue() >= 0) {
            populateStatusCode(metadataBuilder.startObject("statusCode"), dataValue.getStatusCode())
                    .endObject();
        }
        if (dataValue.getSourceTime() != null) {
            metadataBuilder.put(
                    "sourceTimestamp",
                    DateTimeFormatter.ISO_INSTANT.format(
                            dataValue.getSourceTime().getJavaInstant()));
        }
        if (dataValue.getSourcePicoseconds() != null) {
            metadataBuilder.put(
                    "sourcePicoseconds", dataValue.getSourcePicoseconds().intValue());
        }
        if (dataValue.getServerTime() != null) {
            metadataBuilder.put(
                    "serverTimestamp",
                    DateTimeFormatter.ISO_INSTANT.format(
                            dataValue.getServerTime().getJavaInstant()));
        }
        if (dataValue.getServerPicoseconds() != null) {
            metadataBuilder.put(
                    "serverPicoseconds", dataValue.getServerPicoseconds().intValue());
        }
        metadataBuilder.endObject();
    }

    private static void addValue(
            final @NotNull DataPointBuilder<?> builder,
            final @Nullable Object value,
            final @NotNull EncodingContext ctx) {

        // SIMPLE TYPES — use builder.value(...) directly
        if (value == null) {
            builder.valueNull();
        } else if (value instanceof final Boolean b) {
            builder.value(b);
        } else if (value instanceof final Byte b) {
            builder.value((int) b);
        } else if (value instanceof final UByte ubyte) {
            builder.value(ubyte.intValue());
        } else if (value instanceof final Short s) {
            builder.value(s);
        } else if (value instanceof final UShort ushort) {
            builder.value(ushort.intValue());
        } else if (value instanceof final Integer i) {
            builder.value(i);
        } else if (value instanceof final UInteger uint) {
            builder.value(uint.longValue());
        } else if (value instanceof final Long l) {
            builder.value(l);
        } else if (value instanceof final ULong ulong) {
            builder.value(ulong.toBigInteger());
        } else if (value instanceof final Float f) {
            builder.value(f);
        } else if (value instanceof final Double d) {
            builder.value(d);
        } else if (value instanceof final String str) {
            builder.value(str);
        } else if (value instanceof final DateTime dt) {
            builder.value(DateTimeFormatter.ISO_INSTANT.format(dt.getJavaInstant()));
        } else if (value instanceof final UUID uuid) {
            builder.value(uuid.toString());
        } else if (value instanceof final ByteString bs) {
            builder.value(BASE_64.encodeToString(bs.bytesOrEmpty()));
        } else if (value instanceof final XmlElement xe) {
            final String fragment = xe.getFragment();
            if (fragment != null) {
                builder.value(fragment);
            } else {
                builder.valueNull();
            }
        } else if (value instanceof final ExpandedNodeId enid) {
            builder.value(enid.toParseableString());
        } else if (value.getClass().isArray()) {
            final Object[] values = (Object[]) value;
            final var arr = builder.startArrayValue();
            for (final Object elem : values) {
                addValueToArray(arr, elem, ctx);
            }
            arr.endArray();
        }

        // COMPLEX TYPES — use builder.startObjectValue(), populate fields directly
        else if (value instanceof final DataValue dv) {
            final var obj = builder.startObjectValue();
            addValueToObject(obj, "value", dv.getValue(), ctx);
            obj.endObject();
        } else if (value instanceof final NodeId nid) {
            populateNodeId(builder.startObjectValue(), nid).endObject();
        } else if (value instanceof final StatusCode sc) {
            populateStatusCode(builder.startObjectValue(), sc).endObject();
        } else if (value instanceof final QualifiedName qn) {
            final var obj = builder.startObjectValue();
            final String name = qn.getName();
            if (name != null) {
                obj.put("name", name);
            }
            final int nsIdx = qn.getNamespaceIndex().intValue();
            if (nsIdx > 0) {
                obj.put("namespaceIndex", nsIdx);
            }
            obj.endObject();
        } else if (value instanceof final LocalizedText lt) {
            final var obj = builder.startObjectValue();
            final String locale = lt.getLocale();
            if (locale != null) {
                obj.put("locale", locale);
            }
            final String text = lt.getText();
            if (text != null) {
                obj.put("text", text);
            }
            obj.endObject();
        } else if (value instanceof final ExtensionObject eo) {
            try {
                final Object decodedValue = eo.decode(ctx);
                addValue(builder, decodedValue, ctx);
            } catch (final Throwable t) {
                log.debug("Not able to decode body of OPC UA ExtensionObject, using undecoded body value instead", t);
                addValue(builder, eo.getBody(), ctx);
            }
        } else if (value instanceof final Variant variant) {
            final Object variantValue = variant.getValue();
            if (variantValue != null) {
                final var obj = builder.startObjectValue();
                addValueToObject(obj, "value", variantValue, ctx);
                obj.endObject();
            } else {
                builder.valueNull();
            }
        } else if (value instanceof final DiagnosticInfo info) {
            populateDiagnosticInfo(builder.startObjectValue(), info).endObject();
        } else if (value instanceof final DynamicStructType struct) {
            final var obj = builder.startObjectValue();
            struct.getMembers().forEach((k, v) -> addValueToObject(obj, k, v, ctx));
            obj.endObject();
        } else {
            log.warn(
                    "No explicit converter for OPC UA type {} falling back to string representation",
                    value.getClass().getSimpleName());
            builder.value(value.toString());
        }
    }

    private static void addValueToObject(
            final @NotNull DataPointBuilder.ObjectBuilder<?> obj,
            final @NotNull String key,
            final @Nullable Object value,
            final @NotNull EncodingContext ctx) {

        // SIMPLE TYPES
        if (value == null) {
            obj.putNull(key);
        } else if (value instanceof final Boolean b) {
            obj.put(key, b);
        } else if (value instanceof final Byte b) {
            obj.put(key, (int) b);
        } else if (value instanceof final UByte ubyte) {
            obj.put(key, ubyte.intValue());
        } else if (value instanceof final Short s) {
            obj.put(key, s);
        } else if (value instanceof final UShort ushort) {
            obj.put(key, ushort.intValue());
        } else if (value instanceof final Integer i) {
            obj.put(key, i);
        } else if (value instanceof final UInteger uint) {
            obj.put(key, uint.longValue());
        } else if (value instanceof final Long l) {
            obj.put(key, l);
        } else if (value instanceof final ULong ulong) {
            obj.put(key, ulong.toBigInteger());
        } else if (value instanceof final Float f) {
            obj.put(key, f);
        } else if (value instanceof final Double d) {
            obj.put(key, d);
        } else if (value instanceof final String str) {
            obj.put(key, str);
        } else if (value instanceof final DateTime dt) {
            obj.put(key, DateTimeFormatter.ISO_INSTANT.format(dt.getJavaInstant()));
        } else if (value instanceof final UUID uuid) {
            obj.put(key, uuid.toString());
        } else if (value instanceof final ByteString bs) {
            obj.put(key, BASE_64.encodeToString(bs.bytesOrEmpty()));
        } else if (value instanceof final XmlElement xe) {
            final String fragment = xe.getFragment();
            if (fragment != null) {
                obj.put(key, fragment);
            } else {
                obj.putNull(key);
            }
        } else if (value.getClass().isArray()) {
            final Object[] values = (Object[]) value;
            final var arr = obj.startArray(key);
            for (final Object elem : values) {
                addValueToArray(arr, elem, ctx);
            }
            arr.endArray();
        }

        // COMPLEX TYPES
        else if (value instanceof final DataValue dv) {
            addValueToObject(obj, key, dv.getValue(), ctx);
        } else if (value instanceof final NodeId nid) {
            populateNodeId(obj.startObject(key), nid).endObject();
        } else if (value instanceof final ExpandedNodeId enid) {
            obj.put(key, enid.toParseableString());
        } else if (value instanceof final StatusCode sc) {
            populateStatusCode(obj.startObject(key), sc).endObject();
        } else if (value instanceof final QualifiedName qn) {
            final var nested = obj.startObject(key);
            final String name = qn.getName();
            if (name != null) {
                nested.put("name", name);
            }
            final int nsIdx = qn.getNamespaceIndex().intValue();
            if (nsIdx > 0) {
                nested.put("namespaceIndex", nsIdx);
            }
            nested.endObject();
        } else if (value instanceof final LocalizedText lt) {
            final var nested = obj.startObject(key);
            final String locale = lt.getLocale();
            if (locale != null) {
                nested.put("locale", locale);
            }
            final String text = lt.getText();
            if (text != null) {
                nested.put("text", text);
            }
            nested.endObject();
        } else if (value instanceof final ExtensionObject eo) {
            try {
                final Object decodedValue = eo.decode(ctx);
                addValueToObject(obj, key, decodedValue, ctx);
            } catch (final Throwable t) {
                log.debug("Not able to decode body of OPC UA ExtensionObject, using undecoded body value instead", t);
                addValueToObject(obj, key, eo.getBody(), ctx);
            }
        } else if (value instanceof final Variant variant) {
            final Object variantValue = variant.getValue();
            if (variantValue != null) {
                addValueToObject(obj, key, variantValue, ctx);
            } else {
                obj.putNull(key);
            }
        } else if (value instanceof final DiagnosticInfo info) {
            populateDiagnosticInfo(obj.startObject(key), info).endObject();
        } else if (value instanceof final DynamicStructType struct) {
            final var nested = obj.startObject(key);
            struct.getMembers().forEach((k, v) -> addValueToObject(nested, k, v, ctx));
            nested.endObject();
        } else {
            log.warn(
                    "No explicit converter for OPC UA type {} falling back to string representation",
                    value.getClass().getSimpleName());
            obj.put(key, value.toString());
        }
    }

    private static void addValueToArray(
            final @NotNull DataPointBuilder.ArrayBuilder<?> arr,
            final @Nullable Object value,
            final @NotNull EncodingContext ctx) {
        if (value == null) {
            arr.addNull();
        } else if (value instanceof final DataValue dv) {
            addValueToArray(arr, dv.getValue(), ctx);
        } else if (value instanceof final Boolean b) {
            arr.add(b);
        } else if (value instanceof final Byte b) {
            arr.add((int) b);
        } else if (value instanceof final UByte ubyte) {
            arr.add(ubyte.intValue());
        } else if (value instanceof final Short s) {
            arr.add(s);
        } else if (value instanceof final UShort ushort) {
            arr.add(ushort.intValue());
        } else if (value instanceof final Integer i) {
            arr.add(i);
        } else if (value instanceof final UInteger uint) {
            arr.add(uint.longValue());
        } else if (value instanceof final Long l) {
            arr.add(l);
        } else if (value instanceof final ULong ulong) {
            arr.add(ulong.toBigInteger());
        } else if (value instanceof final Float f) {
            arr.add(f);
        } else if (value instanceof final Double d) {
            arr.add(d);
        } else if (value instanceof final String str) {
            arr.add(str);
        } else if (value instanceof final DateTime dt) {
            arr.add(DateTimeFormatter.ISO_INSTANT.format(dt.getJavaInstant()));
        } else if (value instanceof final UUID uuid) {
            arr.add(uuid.toString());
        } else if (value instanceof final ByteString bs) {
            arr.add(BASE_64.encodeToString(bs.bytesOrEmpty()));
        } else if (value instanceof final XmlElement xe) {
            final String fragment = xe.getFragment();
            if (fragment != null) {
                arr.add(fragment);
            } else {
                arr.addNull();
            }
        } else if (value instanceof final NodeId nid) {
            populateNodeId(arr.startObject(), nid).endObject();
        } else if (value instanceof final ExpandedNodeId enid) {
            arr.add(enid.toParseableString());
        } else if (value instanceof final StatusCode sc) {
            populateStatusCode(arr.startObject(), sc).endObject();
        } else if (value instanceof final QualifiedName qn) {
            final var nested = arr.startObject();
            final String name = qn.getName();
            if (name != null) {
                nested.put("name", name);
            }
            final int nsIdx = qn.getNamespaceIndex().intValue();
            if (nsIdx > 0) {
                nested.put("namespaceIndex", nsIdx);
            }
            nested.endObject();
        } else if (value instanceof final LocalizedText lt) {
            final var nested = arr.startObject();
            final String locale = lt.getLocale();
            if (locale != null) {
                nested.put("locale", locale);
            }
            final String text = lt.getText();
            if (text != null) {
                nested.put("text", text);
            }
            nested.endObject();
        } else if (value instanceof final ExtensionObject eo) {
            try {
                final Object decodedValue = eo.decode(ctx);
                addValueToArray(arr, decodedValue, ctx);
            } catch (final Throwable t) {
                log.debug("Not able to decode body of OPC UA ExtensionObject, using undecoded body value instead", t);
                addValueToArray(arr, eo.getBody(), ctx);
            }
        } else if (value instanceof final Variant variant) {
            final Object variantValue = variant.getValue();
            if (variantValue != null) {
                addValueToArray(arr, variantValue, ctx);
            } else {
                arr.addNull();
            }
        } else if (value instanceof final DiagnosticInfo info) {
            populateDiagnosticInfo(arr.startObject(), info).endObject();
        } else if (value instanceof final DynamicEnumType enumValue) {
            final JsonObject enumRoot = new JsonObject();
            final String name = enumValue.getName();
            if (name != null) {
                enumRoot.add("name", new JsonPrimitive(name));
            }
            enumRoot.add("value", new JsonPrimitive(enumValue.getValue()));
        } else if (value instanceof final DynamicStructType struct) {
            final var nested = arr.startObject();
            struct.getMembers().forEach((k, v) -> addValueToObject(nested, k, v, ctx));
            nested.endObject();
        } else if (value.getClass().isArray()) {
            final Object[] values = (Object[]) value;
            for (final Object elem : values) {
                addValueToArray(arr, elem, ctx);
            }
        } else {
            log.warn(
                    "No explicit converter for OPC UA type {} falling back to string representation",
                    value.getClass().getSimpleName());
            arr.add(value.toString());
        }
    }

    private static <P> @NotNull DataPointBuilder.ObjectBuilder<P> populateNodeId(
            final @NotNull DataPointBuilder.ObjectBuilder<P> obj, final @NotNull NodeId nodeId) {
        switch (nodeId.getType()) {
            case Numeric -> {
                obj.put("idType", IdType.Numeric.getValue());
                obj.put("id", ((Number) nodeId.getIdentifier()).longValue());
            }
            case String -> {
                obj.put("idType", IdType.String.getValue());
                obj.put("id", nodeId.getIdentifier().toString());
            }
            case Guid -> {
                obj.put("idType", IdType.Guid.getValue());
                obj.put("id", nodeId.getIdentifier().toString());
            }
            case Opaque -> {
                obj.put("idType", IdType.Opaque.getValue());
                obj.put("id", BASE_64.encodeToString(((ByteString) nodeId.getIdentifier()).bytesOrEmpty()));
            }
        }

        final int namespaceIndex = nodeId.getNamespaceIndex().intValue();
        if (namespaceIndex == 1) { // 1 is always encoded as a number
            obj.put("namespaceIndex", namespaceIndex);
        } else {
            obj.put("namespaceIndex", nodeId.toParseableString());
        }
        return obj;
    }

    private static <P> @NotNull DataPointBuilder.ObjectBuilder<P> populateDiagnosticInfo(
            final @NotNull DataPointBuilder.ObjectBuilder<P> obj, final @NotNull DiagnosticInfo info) {
        obj.put("namespaceUri", info.namespaceUri());
        obj.put("symbolicId", info.symbolicId());
        obj.put("locale", info.locale());
        obj.put("localizedText", info.localizedText());
        if (info.additionalInfo() != null) {
            obj.put("additionalInfo", info.additionalInfo());
        }
        if (info.innerStatusCode() != null) {
            populateStatusCode(obj.startObject("innerStatusCode"), info.innerStatusCode())
                    .endObject();
        }
        if (info.innerDiagnosticInfo() != null) {
            populateDiagnosticInfo(obj.startObject("innerDiagnosticInfo"), info.innerDiagnosticInfo())
                    .endObject();
        }
        return obj;
    }

    private static <P> @NotNull DataPointBuilder.ObjectBuilder<P> populateStatusCode(
            final @NotNull DataPointBuilder.ObjectBuilder<P> obj, final @NotNull StatusCode value) {
        final long statusCodeNr = value.getValue();
        obj.put("code", statusCodeNr);
        StatusCodes.lookup(statusCodeNr).ifPresent(code -> obj.put("symbol", code[0]));
        return obj;
    }
}
