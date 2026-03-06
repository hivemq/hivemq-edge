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
            builder.setNullValue();
            return;
        }
        final var objBuilder = builder.valueStart();
        if (value instanceof final DataValue v) {
            if (v.getStatusCode().getValue() > 0) {
                populateStatusCode(objBuilder.objectStart("statusCode"), v.getStatusCode()).objectEnd();
            }
            if (v.getSourceTime() != null) {
                objBuilder.add(
                        "sourceTimestamp",
                        DateTimeFormatter.ISO_INSTANT.format(v.getSourceTime().getJavaInstant()));
            }
            if (v.getSourcePicoseconds() != null) {
                objBuilder.add("sourcePicoseconds", v.getSourcePicoseconds().intValue());
            }
            if (v.getServerTime() != null) {
                objBuilder.add(
                        "serverTimestamp",
                        DateTimeFormatter.ISO_INSTANT.format(v.getServerTime().getJavaInstant()));
            }
            if (v.getServerPicoseconds() != null) {
                objBuilder.add("serverPicoseconds", v.getServerPicoseconds().intValue());
            }
        }
        addValueToObject(objBuilder, "value", value, serializationContext);
        objBuilder.valueStop();
    }

    private static void addValueToObject(
            final @NotNull DataPointBuilder.ObjectBuilder<?> obj,
            final @NotNull String key,
            final @Nullable Object value,
            final @NotNull EncodingContext ctx) {
        if (value == null) {
            obj.addNull(key);
        } else if (value instanceof final DataValue dv) {
            addValueToObject(obj, key, dv.getValue(), ctx);
        } else if (value instanceof final Boolean b) {
            obj.add(key, b);
        } else if (value instanceof final Byte b) {
            obj.add(key, (int) b);
        } else if (value instanceof final UByte ubyte) {
            obj.add(key, ubyte.intValue());
        } else if (value instanceof final Short s) {
            obj.add(key, s);
        } else if (value instanceof final UShort ushort) {
            obj.add(key, ushort.intValue());
        } else if (value instanceof final Integer i) {
            obj.add(key, i);
        } else if (value instanceof final UInteger uint) {
            obj.add(key, uint.longValue());
        } else if (value instanceof final Long l) {
            obj.add(key, l);
        } else if (value instanceof final ULong ulong) {
            obj.add(key, ulong.toBigInteger());
        } else if (value instanceof final Float f) {
            obj.add(key, f);
        } else if (value instanceof final Double d) {
            obj.add(key, d);
        } else if (value instanceof final String str) {
            obj.add(key, str);
        } else if (value instanceof final DateTime dt) {
            obj.add(key, DateTimeFormatter.ISO_INSTANT.format(dt.getJavaInstant()));
        } else if (value instanceof final UUID uuid) {
            obj.add(key, uuid.toString());
        } else if (value instanceof final ByteString bs) {
            obj.add(key, BASE_64.encodeToString(bs.bytesOrEmpty()));
        } else if (value instanceof final XmlElement xe) {
            final String fragment = xe.getFragment();
            if (fragment != null) {
                obj.add(key, fragment);
            } else {
                obj.addNull(key);
            }
        } else if (value instanceof final NodeId nid) {
            populateNodeId(obj.objectStart(key), nid).objectEnd();
        } else if (value instanceof final ExpandedNodeId enid) {
            obj.add(key, enid.toParseableString());
        } else if (value instanceof final StatusCode sc) {
            populateStatusCode(obj.objectStart(key), sc).objectEnd();
        } else if (value instanceof final QualifiedName qn) {
            final var nested = obj.objectStart(key);
            final String name = qn.getName();
            if (name != null) {
                nested.add("name", name);
            }
            final int nsIdx = qn.getNamespaceIndex().intValue();
            if (nsIdx > 0) {
                nested.add("namespaceIndex", nsIdx);
            }
            nested.objectEnd();
        } else if (value instanceof final LocalizedText lt) {
            final var nested = obj.objectStart(key);
            final String locale = lt.getLocale();
            if (locale != null) {
                nested.add("locale", locale);
            }
            final String text = lt.getText();
            if (text != null) {
                nested.add("text", text);
            }
            nested.objectEnd();
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
                obj.addNull(key);
            }
        } else if (value instanceof final DiagnosticInfo info) {
            populateDiagnosticInfo(obj.objectStart(key), info).objectEnd();
        } else if (value instanceof final DynamicStructType struct) {
            final var nested = obj.objectStart(key);
            struct.getMembers().forEach((k, v) -> addValueToObject(nested, k, v, ctx));
            nested.objectEnd();
        } else if (value.getClass().isArray()) {
            final Object[] values = (Object[]) value;
            final var arr = obj.arrayStart(key);
            for (final Object elem : values) {
                addValueToArray(arr, elem, ctx);
            }
            arr.arrayEnd();
        } else {
            log.warn(
                    "No explicit converter for OPC UA type {} falling back to string representation",
                    value.getClass().getSimpleName());
            obj.add(key, value.toString());
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
            populateNodeId(arr.objectStart(), nid).objectEnd();
        } else if (value instanceof final ExpandedNodeId enid) {
            arr.add(enid.toParseableString());
        } else if (value instanceof final StatusCode sc) {
            populateStatusCode(arr.objectStart(), sc).objectEnd();
        } else if (value instanceof final QualifiedName qn) {
            final var nested = arr.objectStart();
            final String name = qn.getName();
            if (name != null) {
                nested.add("name", name);
            }
            final int nsIdx = qn.getNamespaceIndex().intValue();
            if (nsIdx > 0) {
                nested.add("namespaceIndex", nsIdx);
            }
            nested.objectEnd();
        } else if (value instanceof final LocalizedText lt) {
            final var nested = arr.objectStart();
            final String locale = lt.getLocale();
            if (locale != null) {
                nested.add("locale", locale);
            }
            final String text = lt.getText();
            if (text != null) {
                nested.add("text", text);
            }
            nested.objectEnd();
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
            populateDiagnosticInfo(arr.objectStart(), info).objectEnd();
        } else if (value instanceof final DynamicStructType struct) {
            final var nested = arr.objectStart();
            struct.getMembers().forEach((k, v) -> addValueToObject(nested, k, v, ctx));
            nested.objectEnd();
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
                obj.add("idType", IdType.Numeric.getValue());
                obj.add("id", ((Number) nodeId.getIdentifier()).longValue());
            }
            case String -> {
                obj.add("idType", IdType.String.getValue());
                obj.add("id", nodeId.getIdentifier().toString());
            }
            case Guid -> {
                obj.add("idType", IdType.Guid.getValue());
                obj.add("id", nodeId.getIdentifier().toString());
            }
            case Opaque -> {
                obj.add("idType", IdType.Opaque.getValue());
                obj.add("id", BASE_64.encodeToString(((ByteString) nodeId.getIdentifier()).bytesOrEmpty()));
            }
        }

        final int namespaceIndex = nodeId.getNamespaceIndex().intValue();
        if (namespaceIndex == 1) { // 1 is always encoded as a number
            obj.add("namespaceIndex", namespaceIndex);
        } else {
            obj.add("namespaceIndex", nodeId.toParseableString());
        }
        return obj;
    }

    private static <P> @NotNull DataPointBuilder.ObjectBuilder<P> populateDiagnosticInfo(
            final @NotNull DataPointBuilder.ObjectBuilder<P> obj, final @NotNull DiagnosticInfo info) {
        obj.add("namespaceUri", info.namespaceUri());
        obj.add("symbolicId", info.symbolicId());
        obj.add("locale", info.locale());
        obj.add("localizedText", info.localizedText());
        if (info.additionalInfo() != null) {
            obj.add("additionalInfo", info.additionalInfo());
        }
        if (info.innerStatusCode() != null) {
            populateStatusCode(obj.objectStart("innerStatusCode"), info.innerStatusCode()).objectEnd();
        }
        if (info.innerDiagnosticInfo() != null) {
            populateDiagnosticInfo(obj.objectStart("innerDiagnosticInfo"), info.innerDiagnosticInfo()).objectEnd();
        }
        return obj;
    }

    private static <P> @NotNull DataPointBuilder.ObjectBuilder<P> populateStatusCode(
            final @NotNull DataPointBuilder.ObjectBuilder<P> obj, final @NotNull StatusCode value) {
        final long statusCodeNr = value.getValue();
        obj.add("code", statusCodeNr);
        StatusCodes.lookup(statusCodeNr).ifPresent(code -> obj.add("symbol", code[0]));
        return obj;
    }
}
