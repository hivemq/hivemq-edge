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
package com.hivemq.edge.adapters.etherip_cip_odva.encoder;

import com.hivemq.edge.adapters.etherip_cip_odva.config.tag.CipTag;
import com.hivemq.edge.adapters.etherip_cip_odva.handler.CipTagValueProducer;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public abstract class BaseStaticCipTagEncoder<T> implements CipTagEncoder<T> {

    @Override
    public int getRequestSizeSingle(final @NotNull CipTag cipTag, final @NotNull CipTagValueProducer<T> valueProducer) {
        return getStaticRequestSizeSingle();
    }

    protected abstract int getStaticRequestSizeSingle();

    @Override
    public int getRequestSizeMultiple(
            final @NotNull CipTag cipTag, final @NotNull CipTagValueProducer<List<T>> valueProducer) {
        return cipTag.getDefinition().getNumberOfElements() * getStaticRequestSizeSingle();
    }
}
