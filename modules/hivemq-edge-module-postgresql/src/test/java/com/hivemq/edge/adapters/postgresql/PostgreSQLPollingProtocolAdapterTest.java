/*
 * Copyright 2024-present HiveMQ GmbH
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
import com.hivemq.adapter.sdk.api.polling.PollingInput;
import com.hivemq.edge.adapters.postgresql.config.PostgreSQLAdapterConfig;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PostgreSQLPollingProtocolAdapterTest {

    @TempDir
    @NotNull
    File temporaryDir;

    private final @NotNull ProtocolAdapterInput<PostgreSQLAdapterConfig> adapterInput = mock();
    private final @NotNull PostgreSQLAdapterConfig config = mock();

    @Test
    void test_poll_whenFileIsPresent_thenFileContentsAreSetInOutput() throws IOException {
//        final File fileWithData = new File(temporaryDir, "data.txt");
//        Files.write(fileWithData.toPath(), "Hello World".getBytes(StandardCharsets.UTF_8));
//        when(adapterInput.getConfig()).thenReturn(config);
//        PollingInput pollingInput = mock();
//        when(pollingInput.getPollingContext()).thenReturn(mock());
//        TestPollingOutput pollingOutput = new TestPollingOutput();
//
//        PostgreSQLPollingProtocolAdapter adapter = new PostgreSQLPollingProtocolAdapter(new PostgreSQLProtocolAdapterInformation(), adapterInput);
//
//        adapter.poll(pollingInput, pollingOutput);
//
//        assertEquals(42, pollingOutput.getDataPoints().get("dataPoint1"));
//        assertEquals(1337, pollingOutput.getDataPoints().get("dataPoint2"));

    }
}