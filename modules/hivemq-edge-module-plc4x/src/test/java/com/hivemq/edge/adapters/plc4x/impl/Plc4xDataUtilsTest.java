package com.hivemq.edge.adapters.plc4x.impl;

import org.apache.plc4x.java.spi.values.PlcBOOL;
import org.apache.plc4x.java.spi.values.PlcBYTE;
import org.apache.plc4x.java.spi.values.PlcCHAR;
import org.apache.plc4x.java.spi.values.PlcDATE;
import org.apache.plc4x.java.spi.values.PlcDATE_AND_TIME;
import org.apache.plc4x.java.spi.values.PlcDINT;
import org.apache.plc4x.java.spi.values.PlcDWORD;
import org.apache.plc4x.java.spi.values.PlcINT;
import org.apache.plc4x.java.spi.values.PlcLDATE_AND_TIME;
import org.apache.plc4x.java.spi.values.PlcLINT;
import org.apache.plc4x.java.spi.values.PlcLREAL;
import org.apache.plc4x.java.spi.values.PlcLTIME_OF_DAY;
import org.apache.plc4x.java.spi.values.PlcLWORD;
import org.apache.plc4x.java.spi.values.PlcREAL;
import org.apache.plc4x.java.spi.values.PlcSINT;
import org.apache.plc4x.java.spi.values.PlcTIME;
import org.apache.plc4x.java.spi.values.PlcTIME_OF_DAY;
import org.apache.plc4x.java.spi.values.PlcUDINT;
import org.apache.plc4x.java.spi.values.PlcUINT;
import org.apache.plc4x.java.spi.values.PlcULINT;
import org.apache.plc4x.java.spi.values.PlcUSINT;
import org.apache.plc4x.java.spi.values.PlcWCHAR;
import org.apache.plc4x.java.spi.values.PlcWORD;
import org.apache.plc4x.java.spi.values.PlcWSTRING;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static com.hivemq.edge.adapters.plc4x.impl.Plc4xDataUtils.convertObject;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class Plc4xDataUtilsTest {

    @Test
    public void whenConvertBool_thenReturnBool() {
        assertEquals(Boolean.FALSE, convertObject(new PlcBOOL(false)));
        assertEquals(Boolean.TRUE, convertObject(new PlcBOOL(true)));
    }

    @Test
    public void whenConvertWord_thenReturnUnsignedNumber() {
        assertEquals(0, convertObject(new PlcWORD(0)));
        assertEquals(65535, convertObject(new PlcWORD(65535)));
    }

    @Test
    public void whenConvertUint_thenReturnUnsignedNumber() {
        assertEquals(0, convertObject(new PlcUINT(0)));
        assertEquals(65535, convertObject(new PlcUINT(65535)));
    }

    @Test
    public void whenConvertDWord_thenReturnUnsignedNumber() {
        assertEquals(0L, convertObject(new PlcDWORD(0)));
        assertEquals(4294967295L, convertObject(new PlcDWORD(4294967295L)));
    }

    @Test
    public void whenConvertUsint_thenReturnUsignedNumber() {
        assertEquals((short) 0, convertObject(new PlcUSINT(0)));
        assertEquals((short) 255, convertObject(new PlcUSINT(255)));
    }

    @Test
    public void whenConvertSint_thenReturnSignedNumber() {
        assertEquals((short) 0, convertObject(new PlcSINT(0)));
        assertEquals((short) 127, convertObject(new PlcSINT(127)));
        assertEquals((short) -127, convertObject(new PlcSINT(-127)));
    }

    @Test
    public void whenConvertInt_thenReturnSignedNumber() {
        assertEquals((short) 0, convertObject(new PlcINT(0)));
        assertEquals((short) 32767, convertObject(new PlcINT(32767)));
        assertEquals((short) -32768, convertObject(new PlcINT(-32768)));
    }

    @Test
    public void whenConvertByte_thenReturnSignedNumber() {
        assertEquals((short) 0, convertObject(new PlcBYTE(0b0000_0000)));
        assertEquals((short) 255, convertObject(new PlcBYTE(0b1111_1111)));
    }

    @Test
    public void whenConvertDint_thenReturnSignedNumber() {
        assertEquals(0, convertObject(new PlcDINT(0)));
        assertEquals(2147483647, convertObject(new PlcDINT(2147483647)));
        assertEquals(-2147483647, convertObject(new PlcDINT(-2147483647)));
    }

    @Test
    public void whenConvertDword_thenReturnUnsignedNumber() {
        assertEquals(0L, convertObject(new PlcDWORD(0L)));
        assertEquals(4294967295L, convertObject(new PlcDWORD(4294967295L)));
    }

    @Test
    public void whenConvertUdint_thenReturnUnsignedNumber() {
        assertEquals(0L, convertObject(new PlcUDINT(0L)));
        assertEquals(4294967295L, convertObject(new PlcUDINT(4294967295L)));
    }

    @Test
    public void whenConvertLword_thenReturnUnsignedNumber() {
        assertEquals(BigInteger.ZERO, convertObject(new PlcLWORD(BigInteger.ZERO)));
        assertEquals(new BigInteger("18446744073709551615"),
                convertObject(new PlcLWORD(new BigInteger("18446744073709551615"))));
    }

    @Test
    public void whenConvertULint_thenReturnUnsignedNumber() {
        assertEquals(BigInteger.ZERO, convertObject(new PlcULINT(BigInteger.ZERO)));
        assertEquals(new BigInteger("18446744073709551615"),
                convertObject(new PlcULINT(new BigInteger("18446744073709551615"))));
    }

    @Test
    public void whenConvertLint_thenReturnSignedNumber() {
        assertEquals(0L, convertObject(new PlcLINT(0L)));
        assertEquals(9223372036854775807L, convertObject(new PlcLINT(9223372036854775807L)));
        assertEquals(-9223372036854775808L, convertObject(new PlcLINT(-9223372036854775808L)));
    }

    @Test
    public void whenConvertReal_thenReturnSignedNumber() {
        assertEquals(0f, convertObject(new PlcREAL(0f)));
        assertEquals(Float.MAX_VALUE, convertObject(new PlcREAL(Float.MAX_VALUE)));
        assertEquals(Float.MIN_VALUE, convertObject(new PlcREAL(Float.MIN_VALUE)));
    }

    @Test
    public void whenConvertLreal_thenReturnSignedNumber() {
        assertEquals(0d, convertObject(new PlcLREAL(0d)));
        assertEquals(Double.MAX_VALUE, convertObject(new PlcLREAL(Double.MAX_VALUE)));
        assertEquals(Double.MIN_VALUE, convertObject(new PlcLREAL(Double.MIN_VALUE)));
    }

    @Test
    public void whenConvertWchar_thenReturnUnicodeCharacter() {
        assertEquals("a", convertObject(new PlcWCHAR("a")));
        assertEquals("Z", convertObject(new PlcWCHAR("Z")));
        assertEquals("@", convertObject(new PlcWCHAR("@")));
        assertEquals("/", convertObject(new PlcWCHAR("/")));
        assertEquals("Ɣ", convertObject(new PlcWCHAR("Ɣ")));
        assertEquals("ʝ", convertObject(new PlcWCHAR("ʝ")));
    }

    @Test
    public void whenConvertChar_thenReturnAsciiCharacter() {
        assertEquals("a", convertObject(new PlcCHAR("a")));
        assertEquals("Z", convertObject(new PlcCHAR("Z")));
        assertEquals("@", convertObject(new PlcCHAR("@")));
        assertEquals("/", convertObject(new PlcCHAR("/")));
    }

    @Test
    public void whenConvertWString_thenReturnUnicodeString() {
        assertEquals("abdefghhijklmnopqrstuvwxyzABDEFGHHIJKLMNOPQRSTUVWXYZ0123456789",
                convertObject(new PlcWSTRING("abdefghhijklmnopqrstuvwxyzABDEFGHHIJKLMNOPQRSTUVWXYZ0123456789")));
        assertEquals("@/;:-?=!\\\"$$%\r\n", convertObject(new PlcWSTRING("@/;:-?=!\\\"$$%\r\n")));
        assertEquals("Ɣʝ", convertObject(new PlcWSTRING("Ɣʝ")));
    }

    @Test
    public void whenConvertString_thenReturnAsciiString() {
        assertEquals("abdefghhijklmnopqrstuvwxyzABDEFGHHIJKLMNOPQRSTUVWXYZ0123456789",
                convertObject(new PlcWSTRING("abdefghhijklmnopqrstuvwxyzABDEFGHHIJKLMNOPQRSTUVWXYZ0123456789")));
        assertEquals("@/;:-?=!\\\"$$%\r\n", convertObject(new PlcWSTRING("@/;:-?=!\\\"$$%\r\n")));
    }

    @Test
    public void whenConvertTime_thenReturnSignedMillis() {

        final Duration expected1 = Duration.ofDays(0);
        assertEquals(0L, convertObject(new PlcTIME(expected1)));

        final Duration expectedMax = Duration.ofDays(24).plusHours(20).plusMinutes(31).plusSeconds(23).plusMillis(647);
        assertEquals(expectedMax.toMillis(), convertObject(new PlcTIME(expectedMax)));

        final Duration expectedMin =
                Duration.ofDays(0).minusDays(24).minusHours(20).minusMinutes(31).minusSeconds(23).minusMillis(648);
        assertEquals(expectedMin.toMillis(), convertObject(new PlcTIME(expectedMin)));
    }

    @Test
    public void whenConvertLTime_thenReturnSignedNanos() {
        assertEquals(0L, convertObject(new PlcTIME(0L)));
        assertEquals(9223372036854775807L, convertObject(new PlcTIME(9223372036854775807L)));
        assertEquals(-9223372036854775808L, convertObject(new PlcTIME(-9223372036854775808L)));
    }

    @Test
    public void whenConvertDate_thenReturnIsoDate() {
        assertEquals("2020-04-13", convertObject(new PlcDATE(LocalDate.of(2020, 4, 13))));
        assertEquals("1990-01-01", convertObject(new PlcDATE(LocalDate.of(1990, 1, 1)))); //min
        assertEquals("2168-12-31", convertObject(new PlcDATE(LocalDate.of(2168, 12, 31)))); //max
    }

    @Test
    public void whenConvertTimeOfDay_thenReturnIsoTimeWithMillis() {
        assertEquals("01:23:45.678", convertObject(new PlcTIME_OF_DAY(LocalTime.of(1,23, 45).plusNanos(678000000))));
        assertEquals("00:00:00.000", convertObject(new PlcTIME_OF_DAY(LocalTime.of(0,0,0))));
        assertEquals("23:59:59.999", convertObject(new PlcTIME_OF_DAY(LocalTime.of(23,59,59).plusNanos(999000000))));
    }

    @Test
    public void whenConvertLtimeOfDay_thenReturnIsoTimeWithNanos() {
        assertEquals("01:23:45.678901234", convertObject(new PlcLTIME_OF_DAY(LocalTime.of(1,23, 45).plusNanos(678901234))));
        assertEquals("00:00:00.000", convertObject(new PlcLTIME_OF_DAY(LocalTime.of(0,0,0))));
        assertEquals("23:59:59.999999999", convertObject(new PlcTIME_OF_DAY(LocalTime.of(23,59,59).plusNanos(999999999))));
    }

    @Test
    public void whenConvertDateAndTime_thenReturnIsoTimeWithMillis() {
        assertEquals("2089-12-31T23:59:59.999", convertObject(new PlcDATE_AND_TIME(LocalDateTime.of(2089, 12,31, 23, 59, 59, 999000000))));
        assertEquals("1970-01-01T00:00:00.000", convertObject(new PlcDATE_AND_TIME(LocalDateTime.of(1970, 1,1, 0, 0, 0, 0))));
    }

    @Test
    public void whenConvertLdateAndTime_thenReturnIsoTimeWithNanos() {
        assertEquals("2262-04-11T23:47:16.854775807", convertObject(new PlcLDATE_AND_TIME(LocalDateTime.of(2262, 4,11, 23, 47, 16, 854775807))));
        assertEquals("1970-01-01T00:00:00.000", convertObject(new PlcLDATE_AND_TIME(LocalDateTime.of(1970, 1,1, 0, 0, 0, 0))));
    }

}
