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
package com.hivemq.edge.adapters.etherip_cip_odva.handler;

import etherip.protocol.BaseDecodingAttributeProtocol;
import java.nio.ByteBuffer;
import org.jetbrains.annotations.Nullable;

/**
 * Reads a CIP attribute as its raw bytes, without decoding into typed tag values. Used by read-modify-write
 * to obtain the current attribute contents so that the bytes of tags not supplied by the write are preserved.
 */
public class RawAttributeReadProtocol extends BaseDecodingAttributeProtocol<byte[]> {

    private byte @Nullable [] bytes;

    @Override
    protected byte[] readFromBuffer(final ByteBuffer buf, final int available, final StringBuilder log) {
        final byte[] read = new byte[available];
        buf.get(read, 0, available);
        this.bytes = read;
        return read;
    }

    public byte @Nullable [] getBytes() {
        return bytes;
    }
}
