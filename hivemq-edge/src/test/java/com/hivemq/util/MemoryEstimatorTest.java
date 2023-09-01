package com.hivemq.util;

import com.google.common.primitives.ImmutableIntArray;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MemoryEstimatorTest {

    @Test
    public void test_string_size_when_null_argument() {
        assertEquals(0, MemoryEstimator.stringSize(null));
    }

    @Test
    public void test_string_size_when_non_null_argument() {
        assertEquals(48, MemoryEstimator.stringSize("hello"));
    }

    @Test
    public void test_byte_array_size_when_null_argument() {
        assertEquals(0, MemoryEstimator.byteArraySize(null));
    }

    @Test
    public void test_byte_array_size_when_non_null_argument() {
        assertEquals(16, MemoryEstimator.byteArraySize(new byte[] {0x4f, 0x08, 0x2b, 0x30}));
    }

    @Test
    public void test_immutable_int_array_size_when_null_argument() {
        assertEquals(0, MemoryEstimator.immutableIntArraySize(null));
    }

    @Test
    public void test_immutable_int_array_size_when_non_null_argument() {
        assertEquals(36, MemoryEstimator.immutableIntArraySize(ImmutableIntArray.of(10, 20, 30, 40)));
    }
}
