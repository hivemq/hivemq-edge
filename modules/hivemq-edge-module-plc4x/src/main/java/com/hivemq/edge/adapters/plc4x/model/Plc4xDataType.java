package com.hivemq.edge.adapters.plc4x.model;

import com.hivemq.edge.modules.adapters.annotations.ModuleConfigField;

import java.math.BigInteger;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * @author Simon L Johnson
 */
public class Plc4xDataType {

    //-- A sub-set of the supported core types (TODO fix the object types)
    public enum DATA_TYPE {

        NULL((short) 0x00, null),
        BOOL((short) 0x01, Boolean.class),
        BYTE((short) 0x02, Byte.class),
        WORD((short) 0x03, Short.class),
        DWORD((short) 0x04, Integer.class),
        LWORD((short) 0x05, Long.class),
        USINT((short) 0x11, Short.class),
        UINT((short) 0x12, Integer.class),
        UDINT((short) 0x13, Long.class),
        ULINT((short) 0x14, BigInteger.class),
        SINT((short) 0x21, Byte.class),
        INT((short) 0x22, Short.class),
        DINT((short) 0x23, Integer.class),
        LINT((short) 0x24, Long.class),
        REAL((short) 0x31, Float.class),
        LREAL((short) 0x32, Double.class),
        CHAR((short) 0x41, Character.class),
        WCHAR((short) 0x42, Short.class),
        STRING((short) 0x43, String.class),
        WSTRING((short) 0x44, String.class),
        TIME((short) 0x51, Duration.class),
        LTIME((short) 0x52, Duration.class),
        DATE((short) 0x53, LocalDate.class),
        LDATE((short) 0x54, LocalDate.class),
        TIME_OF_DAY((short) 0x55, LocalTime.class),
        LTIME_OF_DAY((short) 0x56, LocalTime.class),
        DATE_AND_TIME((short) 0x57, LocalDateTime.class),
        LDATE_AND_TIME((short) 0x58, LocalDateTime.class),
        RAW_BYTE_ARRAY((short) 0x71, Byte.class);


        DATA_TYPE(short code, Class<?> javaType){
            this.code = code;
            this.javaType = javaType;
        }

        private short code;
        private Class<?> javaType;

        public short getCode() {
            return code;
        }

        public Class<?> getJavaType() {
            return javaType;
        }
    }

}
