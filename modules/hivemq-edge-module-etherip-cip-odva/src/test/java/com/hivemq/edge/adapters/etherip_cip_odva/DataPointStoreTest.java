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
package com.hivemq.edge.adapters.etherip_cip_odva;

import com.hivemq.edge.adapters.etherip_cip_odva.config.CipDataType;
import com.hivemq.edge.adapters.etherip_cip_odva.config.tag.CipTag;
import com.hivemq.edge.adapters.etherip_cip_odva.config.tag.CipTagDefinition;
import java.util.concurrent.atomic.AtomicLong;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class DataPointStoreTest {

    @Test
    void shouldPutAndGetValue() {
        DataPointStore dataPointStore = new DataPointStore();

        CipTag a = new CipTag("a", "", new CipTagDefinition("address", 1, CipDataType.INT, 0d, 1000, 0, null));

        CipTag b = new CipTag("b", "", new CipTagDefinition("address", 1, CipDataType.INT, 0d, 0, 0, null));

        // when
        dataPointStore.put(a, 1, 1000L);
        dataPointStore.put(b, 2, 1000L);

        // then
        Assertions.assertThat(dataPointStore.get(a)).isEqualTo(1);
        Assertions.assertThat(dataPointStore.get(b)).isEqualTo(2);
    }

    @Test
    void testIsValueOlderThan() {

        CipTag cipTagWithMinUpdate =
                new CipTag("withMinUpdate", "", new CipTagDefinition("address", 1, CipDataType.INT, 0d, 1000, 0, null));

        CipTag cipTagWithMinUpdate0 =
                new CipTag("withMinUpdate=0", "", new CipTagDefinition("address", 1, CipDataType.INT, 0d, 0, 0, null));

        CipTag cipTagWithMinUpdateNull = new CipTag(
                "withMinUpdate=null", "", new CipTagDefinition("address", 1, CipDataType.INT, 0d, null, 0, null));

        AtomicLong clock = new AtomicLong(0);

        // when
        DataPointStore dataPointStore = new DataPointStore();
        dataPointStore.put(cipTagWithMinUpdate, 1, clock.get());
        dataPointStore.put(cipTagWithMinUpdate0, 2, clock.get());
        dataPointStore.put(cipTagWithMinUpdateNull, 3, clock.get());

        // then
        Assertions.assertThat(dataPointStore.isValueOlderThan(cipTagWithMinUpdate, 0L))
                .isFalse();
        Assertions.assertThat(dataPointStore.isValueOlderThan(cipTagWithMinUpdate, 999L))
                .isFalse();
        Assertions.assertThat(dataPointStore.isValueOlderThan(cipTagWithMinUpdate, 1000L))
                .isTrue();

        Assertions.assertThat(dataPointStore.isValueOlderThan(cipTagWithMinUpdate0, 0L))
                .isFalse();
        Assertions.assertThat(dataPointStore.isValueOlderThan(cipTagWithMinUpdate0, 999L))
                .isFalse();
        Assertions.assertThat(dataPointStore.isValueOlderThan(cipTagWithMinUpdate0, 1000L))
                .isFalse();

        Assertions.assertThat(dataPointStore.isValueOlderThan(cipTagWithMinUpdateNull, 0L))
                .isFalse();
        Assertions.assertThat(dataPointStore.isValueOlderThan(cipTagWithMinUpdateNull, 999L))
                .isFalse();
        Assertions.assertThat(dataPointStore.isValueOlderThan(cipTagWithMinUpdateNull, 1000L))
                .isFalse();
    }
}
