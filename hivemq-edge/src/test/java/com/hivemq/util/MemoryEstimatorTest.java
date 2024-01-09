/*
 * Copyright 2019-present HiveMQ GmbH
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
