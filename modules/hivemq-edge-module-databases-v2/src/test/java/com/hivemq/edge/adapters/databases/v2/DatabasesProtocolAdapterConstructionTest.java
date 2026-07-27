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

import com.hivemq.adapter.sdk.api.v2.node.NodeTagPair;
import com.hivemq.protocols.v2.runtime.ManualDispatcher;
import java.util.List;
import org.junit.jupiter.api.Test;

class DatabasesProtocolAdapterConstructionTest {

    private final ManualDispatcher dispatcher = new ManualDispatcher();
    private final RecordingProtocolAdapterOutput output = new RecordingProtocolAdapterOutput();

    @Test
    void constructsFromAnInputWithTwoQueryTagsAndExposesItsAdapterId() {
        final NodeTagPair first =
                DatabasesAdapterTestFixtures.queryTag("products", "SELECT * FROM products", SplitMode.ALL_IN_ONE);
        final NodeTagPair second =
                DatabasesAdapterTestFixtures.queryTag("orders", "SELECT * FROM orders", SplitMode.ONE_PER_ROW);

        final DatabasesProtocolAdapter adapter = new DatabasesProtocolAdapter(
                DatabasesAdapterTestFixtures.input(
                        "databases-v2-1",
                        dispatcher,
                        new DatabasesAdapterTestFixtures.TestDataPointFactory(),
                        DatabasesAdapterTestFixtures.configuration("POSTGRESQL", 5432),
                        List.of(first, second)),
                output);

        assertThat(adapter.adapterId()).isEqualTo("databases-v2-1");
        assertThat(first.node()).isInstanceOf(DatabaseNode.class);
        assertThat(second.node()).isInstanceOf(DatabaseNode.class);
        // Construction is synchronous and cheap: no pool is opened and nothing is acknowledged yet.
        assertThat(output.events).isEmpty();
    }
}
