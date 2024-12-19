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
package com.hivemq.edge.adapters.postgresql;


import com.hivemq.adapter.sdk.api.model.ProtocolAdapterInput;
import com.hivemq.edge.adapters.postgresql.config.PostgreSQLAdapterConfig;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class PostgreSQLPollingProtocolAdapterTest {
    private final @NotNull ProtocolAdapterInput<PostgreSQLAdapterConfig> adapterInput = mock();
    private final @NotNull PostgreSQLAdapterConfig config = mock();

    @Test
    void test_poll_queryDatabaseWithFakeData() {
        // TO BE WRITTEN
        // Faking test
        var result = 0;
        assertEquals(0, result);

    }
}