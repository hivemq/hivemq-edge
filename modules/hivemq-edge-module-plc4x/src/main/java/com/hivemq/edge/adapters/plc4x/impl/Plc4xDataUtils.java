package com.hivemq.edge.adapters.plc4x.impl;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.plc4x.java.api.messages.PlcReadResponse;
import org.apache.plc4x.java.api.types.PlcValueType;
import org.apache.plc4x.java.api.value.PlcValue;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static java.time.temporal.ChronoField.HOUR_OF_DAY;
import static java.time.temporal.ChronoField.MINUTE_OF_HOUR;
import static java.time.temporal.ChronoField.MONTH_OF_YEAR;
import static java.time.temporal.ChronoField.NANO_OF_SECOND;
import static java.time.temporal.ChronoField.SECOND_OF_MINUTE;
import static java.time.temporal.ChronoField.YEAR;

/**
 * Some data utilies to manage the interaction with the PLC API
 */
public class Plc4xDataUtils {
    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();
    private final static String AMP = "&";
    private final static String EQUALS = "=";
    public static final DateTimeFormatter TIME_FORMATTER =
            new DateTimeFormatterBuilder() //custom formatter to prevent weird abbreviations
                    .appendValue(HOUR_OF_DAY, 2)
                    .appendLiteral(':')
                    .appendValue(MINUTE_OF_HOUR, 2)
                    .appendLiteral(':')
                    .appendValue(SECOND_OF_MINUTE, 2)
                    .optionalStart()
                    .appendFraction(NANO_OF_SECOND, 3, 9, true)
                    .toFormatter();

    public static final DateTimeFormatter DATE_TIME_FORMATTER =
            new DateTimeFormatterBuilder() //custom formatter to prevent weird abbreviations
                    .appendValue(YEAR, 4)
                    .appendLiteral("-")
                    .appendValue(MONTH_OF_YEAR, 2)
                    .appendLiteral("-")
                    .appendValue(DAY_OF_MONTH, 2)
                    .appendLiteral("T")
                    .appendValue(HOUR_OF_DAY, 2)
                    .appendLiteral(':')
                    .appendValue(MINUTE_OF_HOUR, 2)
                    .appendLiteral(':')
                    .appendValue(SECOND_OF_MINUTE, 2)
                    .optionalStart()
                    .appendFraction(NANO_OF_SECOND, 3, 9, true)
                    .toFormatter();

    public static String toHex(final @NotNull byte... bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static List<Pair<String, PlcValue>> readDataFromReadResponse(@NotNull final PlcReadResponse evt) {
        List<Pair<String, PlcValue>> output = new ArrayList<>();
        Collection<String> s = evt.getTagNames();
        for (String field : s) {
            output.add(Pair.of(field, evt.getPlcValue(field)));
        }
        return output;
    }

    public static final String createQueryString(
            final @NotNull Map<String, String> map,
            boolean includeKeysForNullValues) {
        StringBuilder res = new StringBuilder();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (res.length() > 0) {
                res.append(AMP);
            }
            if (entry.getValue() != null || includeKeysForNullValues) {
                res.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8)).append(EQUALS);
                if (entry.getKey() != null) {
                    res.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
                }
            }
        }
        return res.toString();
    }

    public static byte[] convertNative(PlcValue value) {
        final PlcValueType type = value.getPlcValueType();
        try (ByteArrayOutputStream memoryStream = new ByteArrayOutputStream()) {
            try (DataOutputStream outputStream = new DataOutputStream(memoryStream)) {
                switch (type) {

                    case BOOL:  //Boolean 1bit
                        outputStream.writeBoolean(value.getBoolean());
                        break;

                    case INT:   //16bit signed
                    case USINT: //8bit unsigned
                        outputStream.writeShort(value.getShort());
                        break;

                    case WORD:  //16bit unsigned
                    case DWORD: //32bit unsigned
                    case UINT:  //16bit unsigned
                    case DINT:  //32bit signed
                        outputStream.writeInt(value.getInt());
                        break;

                    case LWORD: //64bit unsigned
                    case UDINT: //32bit unsigned
                    case LINT:  //64bit signed
                        outputStream.writeLong(value.getLong());
                        break;

                    case ULINT: //64bit unsigned
                        outputStream.write(value.getBigInteger().toByteArray());
                        break;

                    case REAL: //32bit signed
                        outputStream.writeFloat(value.getFloat());
                        break;

                    case WCHAR: //16bit unicode
                    case CHAR: //8-bit ascii
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

    public static Object convertObject(PlcValue value) {
        final PlcValueType type = value.getPlcValueType();
        switch (type) {

            case BOOL:  //Boolean 1bit
                return value.getBoolean();

            case INT:    //16bit signed
            case USINT:  //8bit unsigned
            case SINT:   //8bit signed
            case BYTE:   //byte is treated as unsigned for most PLCs/protocols
                return value.getShort();

            case WORD:  //16bit unsigned
            case UINT:  //16bit unsigned
            case DINT:  //32bit signed
                return value.getInt();

            case DWORD: //32bit unsigned
            case UDINT: //32bit unsigned
            case LINT:  //64bit signed
                return value.getLong();

            case LWORD: //64bit unsigned
            case ULINT: //64bit unsigned
                return value.getBigInteger();

            case REAL: //32bit signed
                return value.getFloat();

            case LREAL: //Float
                return value.getDouble();

            case WCHAR:  //16bit unicode
            case CHAR:   //8bit ascii
                return value.getString(); //internally already converts numerical into character string

            case WSTRING: //String
            case STRING:
                return value.getString();

            case TIME:  //32bit signed, millisecond of the day (or day prior) (-24d20h31m23s648ms - +24d20h31m23s647ms)
                return value.getDuration().toMillis();

            case LTIME: //64bit signed, nanosecond of the day (or day prior) (--106751d23h47m16s854ms775us808ns - +106751d23h47m16s854ms775us807ns)
                return value.getDuration().toNanos();

            case DATE: //16bit signed, days since 1990-1-1 (1990-01-01 - 2168-12-31)
            case LDATE:
                return value.getDate().toString(); //ISO date

            case TIME_OF_DAY: //32bit signed, millisecond of the day (00:00:00.000 - 23:59:59.999)
            case LTIME_OF_DAY: //64bit signed, nanosecond of the day (00:00:00.000000000 - 23:59:59.999999999)
                return TIME_FORMATTER.format(value.getTime()); //ISO time

            case DATE_AND_TIME: //64bit signed, milliseconds since 1990-1-1 (1990-01-01-0:0:0 - 2089-12-31-23:59:59.999)
            case LDATE_AND_TIME: //64bit signed, nanoseconds since 1970-1-1 (1970-01-01-0:0:0.000000000 - 2262-04-11-23:47:16.854775807)
                return DATE_TIME_FORMATTER.format(value.getDateTime());

            case RAW_BYTE_ARRAY:
                return value.getRaw();

            case Struct: //HashMap
            case List: //ArrayList
            case NULL:
            default:
                return null;
        }
    }
}
