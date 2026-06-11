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
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ListOfDecoder<T> extends BaseCipTagDecoder<List<T>> {
    private final CipTagDecoder<T> singleValueDecoder;

    public ListOfDecoder(final @NotNull CipTagDecoder<T> singleValueDecoder) {
        this.singleValueDecoder = singleValueDecoder;
    }

    @Override
    protected List<T> internalDecode(
            final @NotNull CipTag cipTag,
            final int elementIndex,
            final @NotNull ByteBuffer buf,
            final @Nullable StringBuilder log)
            throws OdvaDecodeException {

        final List<T> list = new ArrayList<>();

        for (int index = 0; index < cipTag.getDefinition().getNumberOfElements(); index++) {
            singleValueDecoder.decode(cipTag, index, buf, log, (tag, value) -> list.add(value));
        }

        return list;
    }
}
