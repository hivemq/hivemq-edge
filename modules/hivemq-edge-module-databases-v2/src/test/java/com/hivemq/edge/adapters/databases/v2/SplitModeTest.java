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
package com.hivemq.edge.adapters.databases.v2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

class SplitModeTest {

    @Test
    void everyModeExposesItsPascalCaseJsonValue() {
        assertThat(SplitMode.ALL_IN_ONE.jsonValue()).isEqualTo("AllInOne");
        assertThat(SplitMode.ONE_PER_ROW.jsonValue()).isEqualTo("OnePerRow");
        assertThat(SplitMode.ONE_PER_BATCH.jsonValue()).isEqualTo("OnePerBatch");
    }

    @Test
    void eachJsonValueResolvesBackToItsMode() {
        for (final SplitMode mode : SplitMode.values()) {
            assertThat(SplitMode.fromJson(mode.jsonValue())).isEqualTo(mode);
        }
    }

    @Test
    void anAbsentValueResolvesToAllInOne() {
        assertThat(SplitMode.fromJson(null)).isEqualTo(SplitMode.ALL_IN_ONE);
    }

    @Test
    void anUnrecognizedValueIsRejected() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> SplitMode.fromJson("Sideways"))
                .withMessageContaining("Sideways")
                .withMessageContaining("AllInOne")
                .withMessageContaining("OnePerRow")
                .withMessageContaining("OnePerBatch");
    }
}
