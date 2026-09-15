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
package com.hivemq.edge.adapters.etherip_cip_odva.composite;

import com.hivemq.edge.adapters.etherip_cip_odva.config.tag.CipTag;
import com.hivemq.edge.adapters.etherip_cip_odva.tag.TagGroup;
import org.jetbrains.annotations.NotNull;

public class CompositeValuesFactory {
    public static final NoopCompositeValues NOOP_COMPOSITE_VALUES = new NoopCompositeValues();

    private CompositeValuesFactory() {}

    public static @NotNull CompositeValues create(final @NotNull TagGroup tagGroup) {
        final CipTag composite = tagGroup.getComposite();
        if (composite != null && composite.isComposite()) {
            return new DefaultCompositeValues(composite.getName());
        }
        return NOOP_COMPOSITE_VALUES;
    }
}
