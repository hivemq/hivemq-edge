package com.hivemq.edge.adapters.plc4x.impl;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.message.publish.PUBLISH;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.plc4x.java.api.messages.PlcReadResponse;
import org.apache.plc4x.java.api.messages.PlcSubscriptionEvent;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * //TODO this needs work!
 */
public class Plc4xDataUtils {
    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();
    private final static String AMP = "&";
    private final static String EQUALS = "=";
    private final static String QUESTION = "?";

    public static String toHex(final @NotNull byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }


    public static List<Pair<String, byte[]>> readDataFromSubscriptionEvent(@NotNull final PlcSubscriptionEvent evt){
        List<Pair<String, byte[]>> output = new ArrayList<>();
        Collection<String> s = evt.getFieldNames();
        for (String field : s) {

            byte[] arr = null;
            if(evt.isValidString(field)){
                arr = evt.getString(field).getBytes(StandardCharsets.UTF_8);
            } else if (evt.isValidDouble(field)) {
                arr = ByteBuffer.allocate(8).putDouble(evt.getDouble(field)).array();
            } else if (evt.isValidInteger(field)) {
                arr = ByteBuffer.allocate(4).putInt(evt.getInteger(field)).array();
            } else if (evt.isValidLong(field)) {
                arr = ByteBuffer.allocate(8).putLong(evt.getLong(field)).array();
            } else if (evt.isValidShort(field)) {
                arr = ByteBuffer.allocate(2).putShort(evt.getShort(field)).array();
            } else if (evt.isValidByte(field) || evt.isValidBoolean(field)) {
                arr = ByteBuffer.allocate(1).put(evt.getByte(field)).array();
            }

            output.add(Pair.of(field,arr));
        }
        return output;
    }

    public static List<Pair<String, byte[]>> readDataFromReadResponse(@NotNull final PlcReadResponse evt){
        List<Pair<String, byte[]>> output = new ArrayList<>();
        Collection<String> s = evt.getFieldNames();
        for (String field : s) {
            byte[] arr = null;
            if (evt.isValidDouble(field)) {
                arr = ByteBuffer.allocate(8).putDouble(evt.getDouble(field)).array();
            } else if (evt.isValidInteger(field)) {
                arr = ByteBuffer.allocate(4).putInt(evt.getInteger(field)).array();
            } else if (evt.isValidLong(field)) {
                arr = ByteBuffer.allocate(8).putLong(evt.getLong(field)).array();
            } else if (evt.isValidShort(field)) {
                arr = ByteBuffer.allocate(2).putShort(evt.getShort(field)).array();
            } else if (evt.isValidByte(field) || evt.isValidBoolean(field)) {
                arr = ByteBuffer.allocate(1).put(evt.getByte(field)).array();
            } else if(evt.isValidString(field)){
                arr = evt.getString(field).getBytes(StandardCharsets.UTF_8);
            }
            output.add(Pair.of(field,arr));
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
}
