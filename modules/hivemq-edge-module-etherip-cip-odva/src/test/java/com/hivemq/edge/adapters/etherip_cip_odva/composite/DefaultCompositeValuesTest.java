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

import java.util.Map;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class DefaultCompositeValuesTest {

    @Test
    void shouldCorrectlyStoreValues() {
        String compositeTagName = "composite-tag-name";
        DefaultCompositeValues compositeValues = new DefaultCompositeValues(compositeTagName);

        Assertions.assertThat(compositeValues.isEmpty()).isTrue();

        Map<String, Object> tags = Map.of("tag-a", "abc", "tag-b", 123);
        tags.forEach(compositeValues::add);

        Assertions.assertThat(compositeValues.isEmpty()).isFalse();
        Assertions.assertThat(compositeValues.getValues()).containsAllEntriesOf(tags);
        Assertions.assertThat(compositeValues.compositeTagName).isEqualTo(compositeTagName);
    }
}
