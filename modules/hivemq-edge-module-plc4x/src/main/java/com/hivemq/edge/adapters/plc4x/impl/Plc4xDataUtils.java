package com.hivemq.edge.adapters.plc4x.impl;

import com.hivemq.edge.modules.config.impl.AbstractProtocolAdapterConfig;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.util.Bytes;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.plc4x.java.api.messages.PlcReadResponse;
import org.apache.plc4x.java.api.messages.PlcSubscriptionEvent;
import org.apache.plc4x.java.api.types.PlcValueType;
import org.apache.plc4x.java.api.value.PlcValue;

import javax.xml.crypto.Data;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.sql.Struct;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Some data utilies to manage the interaction with the PLC API
 */
public class Plc4xDataUtils {
    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();
    private final static String AMP = "&";
    private final static String EQUALS = "=";

    public static String toHex(final @NotNull byte... bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static List<Pair<String, PlcValue>> readDataFromReadResponse(@NotNull final PlcReadResponse evt){
        List<Pair<String, PlcValue>> output = new ArrayList<>();
        Collection<String> s = evt.getTagNames();
        for (String field : s) {
            output.add(Pair.of(field,evt.getPlcValue(field)));
        }
        return output;
    }

    public static final String createQueryString(final @NotNull Map<String, String> map, boolean includeKeysForNullValues){
        StringBuilder res = new StringBuilder();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (res.length() > 0) {
                res.append(AMP);
            }
            if(entry.getValue() != null || includeKeysForNullValues){
                res.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8))
                        .append(EQUALS);
                if(entry.getKey() != null){
                    res.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
                }
            }
        }
        return res.toString();
    }

    public static byte[] convertNative(PlcValue value){
        final PlcValueType type = value.getPlcValueType();
        try (ByteArrayOutputStream memoryStream = new ByteArrayOutputStream()) {
            try (DataOutputStream outputStream = new DataOutputStream(memoryStream)) {
                switch(type){

                    case BOOL:  //Boolean
                        outputStream.writeBoolean(value.getBoolean());
                        break;

                    case WORD:  //Short
                    case INT:
                    case USINT:
                    case WCHAR:
                        outputStream.writeShort(value.getShort());
                        break;

                    case DWORD: //Integer
                    case UINT:
                    case DINT:
                        outputStream.writeInt(value.getInt());
                        break;

                    case LWORD: //Long
                    case UDINT:
                    case LINT:
                        outputStream.writeLong(value.getLong());
                        break;

                    case ULINT: //BigInteger
                        outputStream.write(value.getBigInteger().toByteArray());
                        break;

                    case REAL: //Float
                        outputStream.writeFloat(value.getFloat());
                        break;

                    case CHAR: //Character
                        outputStream.writeChar(value.getByte());
                        break;

                    case WSTRING: //String
                    case STRING:
                        outputStream.writeChars(value.getString());
                        break;

                    case LTIME://Duration
                    case TIME:
                        outputStream.writeLong(value.getDuration().toMillis());
                        break;

                    case DATE: //LocalDate
                    case LDATE:
                        outputStream.writeChars(value.getDate().toString());
                        break;

                    case TIME_OF_DAY: //LocalTime
                    case LTIME_OF_DAY:
                        outputStream.writeChars(value.getTime().toString());
                        break;

                    case DATE_AND_TIME: //LocalDateTime
                    case LDATE_AND_TIME:
                        outputStream.writeChars(value.getDateTime().toString());
                        break;

                    case BYTE: //Byte
                    case SINT:
                        outputStream.writeByte(value.getByte());
                        break;

                    case RAW_BYTE_ARRAY:
                        outputStream.write(value.getRaw());
                        break;

                    case Struct: //HashMap
                    case List: //ArrayList
                    case NULL:
                    default:
                        break;
                }
            }
            return memoryStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Object convertObject(PlcValue value){
        final PlcValueType type = value.getPlcValueType();
        switch(type){

            case BOOL:  //Boolean
                return value.getBoolean();

            case WORD:  //Short
            case INT:
            case USINT:
            case WCHAR:
                return value.getShort();

            case DWORD: //Integer
            case UINT:
            case DINT:
                return value.getInt();

            case LWORD: //Long
            case UDINT:
            case LINT:
                return value.getLong();

            case ULINT: //BigInteger
                return value.getBigInteger();

            case REAL: //Float
                return value.getFloat();

            case CHAR: //Character
                return value.getByte();

            case WSTRING: //String
            case STRING:
                return value.getString();

            case LTIME://Duration
            case TIME:
                return value.getDuration().toMillis();

            case DATE: //LocalDate
            case LDATE:
                return value.getDate().toString();

            case TIME_OF_DAY: //LocalTime
            case LTIME_OF_DAY:
                return value.getTime().toString();

            case DATE_AND_TIME: //LocalDateTime
            case LDATE_AND_TIME:
                return value.getDateTime().toString();

            case BYTE: //Byte
            case SINT:
                return toHex(value.getByte());

            case RAW_BYTE_ARRAY:
                return toHex(value.getRaw());

            case Struct: //HashMap
            case List: //ArrayList
            case NULL:
            default:
                return null;
        }
    }
}
