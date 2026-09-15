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
package com.hivemq.edge.adapters.etherip_cip_odva.decoder;

import com.hivemq.edge.adapters.etherip_cip_odva.config.tag.CipTag;
import com.hivemq.edge.adapters.etherip_cip_odva.exception.OdvaDecodeException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class SSTRINGDecoder extends BaseCipTagDecoder<String> {

    @Override
    protected String internalDecode(
            final @NotNull CipTag cipTag,
            final int elementIndex,
            final @NotNull ByteBuffer buf,
            final @Nullable StringBuilder log)
            throws OdvaDecodeException {

        int stringLength = getStringLength(cipTag, elementIndex, buf);

        assertAvailableBuffer(cipTag, elementIndex, buf, stringLength);
        byte[] stringBytes = new byte[stringLength];
        buf.get(stringBytes, 0, stringLength);

        return new String(stringBytes, StandardCharsets.UTF_8);
    }

    protected int getStringLength(@NotNull CipTag cipTag, final int elementIndex, @NotNull ByteBuffer buf)
            throws OdvaDecodeException {
        assertAvailableBuffer(cipTag, elementIndex, buf, 1);

        byte stringLengthAsByte = buf.get();
        return stringLengthAsByte < 0 ? stringLengthAsByte + 256 : stringLengthAsByte;
    }
}
