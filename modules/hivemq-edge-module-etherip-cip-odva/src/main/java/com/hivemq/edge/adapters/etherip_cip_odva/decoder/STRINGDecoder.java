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
import org.jetbrains.annotations.NotNull;

class STRINGDecoder extends SSTRINGDecoder {

    @Override
    protected int getStringLength(@NotNull CipTag cipTag, int elementIndex, @NotNull ByteBuffer buf)
            throws OdvaDecodeException {
        assertAvailableBuffer(cipTag, elementIndex, buf, 2);
        return toUnsigned(buf.getShort());
    }

    int toUnsigned(short length) {
        return length < 0 ? length + (1 << 16) : length;
    }
}
