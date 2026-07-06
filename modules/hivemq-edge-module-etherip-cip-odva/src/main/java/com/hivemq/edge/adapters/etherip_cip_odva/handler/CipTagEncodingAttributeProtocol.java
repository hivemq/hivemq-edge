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

import com.hivemq.edge.adapters.etherip_cip_odva.config.tag.CipTag;
import com.hivemq.edge.adapters.etherip_cip_odva.encoder.CipTagEncoders;
import com.hivemq.edge.adapters.etherip_cip_odva.exception.OdvaException;
import etherip.protocol.BaseEncodingAttributeProtocol;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Builds the bytes of a single CIP attribute and patches the supplied tag values into them.
 * <p>
 * The buffer is first seeded, then the supplied tags are encoded on top at their individual offsets:
 * <ul>
 *     <li><b>COMPLETE_WRITE</b> (no prefill): the buffer is zero-filled up to the span of the supplied tags,
 *         so it carries exactly those tags. This is correct only when they cover the whole attribute (the
 *         caller's guarantee for COMPLETE_WRITE); otherwise the request is shorter than the device's attribute
 *         and is rejected.</li>
 *     <li><b>PARTIAL_WRITE</b> (prefill = the current attribute bytes): the buffer starts as the attribute
 *         read back from the device, so bytes not covered by a supplied tag keep their current value.</li>
 * </ul>
 */
public class CipTagEncodingAttributeProtocol extends BaseEncodingAttributeProtocol<Void> {

    private final CipTagEncoders cipTagEncoders;
    private final List<CipTag> tags;
    private final ByteOrder byteOrder;
    private final CipTagValueProducer<Object> valueProducer;
    private final byte @Nullable [] prefill;

    private int totalRequestSize = 0;

    /**
     * COMPLETE_WRITE: no prefill; the buffer carries only the supplied tags (assumed to span the attribute).
     */
    public CipTagEncodingAttributeProtocol(
            final CipTagEncoders cipTagsEncoders,
            final List<CipTag> tags,
            final ByteOrder byteOrder,
            final CipTagValueProducer<Object> valueProducer) {
        this(cipTagsEncoders, tags, byteOrder, valueProducer, null);
    }

    /**
     * If {@code prefill} is non-null the buffer is seeded with it (PARTIAL_WRITE); otherwise it carries only
     * the supplied tags (COMPLETE_WRITE).
     */
    public CipTagEncodingAttributeProtocol(
            final CipTagEncoders cipTagsEncoders,
            final List<CipTag> tags,
            final ByteOrder byteOrder,
            final CipTagValueProducer<Object> valueProducer,
            final byte @Nullable [] prefill) {

        super();

        this.tags = tags;
        this.byteOrder = byteOrder;
        this.valueProducer = valueProducer;
        this.cipTagEncoders = cipTagsEncoders;
        this.prefill = prefill == null ? null : prefill.clone();
    }

    @Override
    protected void writeToBuffer(final ByteBuffer buf, final StringBuilder log) throws OdvaException {

        final int startPosition = buf.position();

        // Seed the buffer: current attribute bytes for PARTIAL_WRITE (prefill), otherwise zeros (COMPLETE_WRITE).
        // totalRequestSize is set by a previous call to internalGetRequestSize.
        if (prefill != null) {
            buf.put(prefill, 0, Math.min(prefill.length, totalRequestSize));
            for (int i = prefill.length; i < totalRequestSize; i++) {
                buf.put((byte) 0);
            }
        } else {
            clearBuffer(buf, totalRequestSize);
        }

        // Patch the supplied tag values on top of the seeded buffer.
        buf.position(startPosition);
        cipTagEncoders.encode(tags, buf, byteOrder, valueProducer);
        buf.position(startPosition + totalRequestSize);
    }

    private static void clearBuffer(@NotNull ByteBuffer buf, int requestSize) {
        for (int i = 0; i < requestSize; i++) {
            buf.put((byte) 0);
        }
    }

    @Override
    protected int internalGetRequestSize() {
        // For PARTIAL_WRITE the attribute size is what was read (prefill); for COMPLETE_WRITE it is the span the
        // supplied tags require (which the caller guarantees is the whole attribute).
        final int encoderSize = cipTagEncoders.getRequestSize(tags, valueProducer);
        this.totalRequestSize = prefill == null ? encoderSize : Math.max(encoderSize, prefill.length);
        return totalRequestSize;
    }
}
