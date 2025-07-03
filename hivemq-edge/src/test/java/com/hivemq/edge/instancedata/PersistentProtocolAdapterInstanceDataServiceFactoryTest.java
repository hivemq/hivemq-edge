/*
 * Copyright 2019-present HiveMQ GmbH
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
package com.hivemq.edge.instancedata;

import com.hivemq.configuration.info.SystemInformation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PersistentProtocolAdapterInstanceDataServiceFactoryTest {

    private static final Logger log =
            LoggerFactory.getLogger(PersistentProtocolAdapterInstanceDataServiceFactoryTest.class);

    @TempDir
    private Path dataDir;

    @Test
    public void test_putValue() throws Exception {
        final var systemInformation = mock(SystemInformation.class);
        when(systemInformation.getDataFolder()).thenReturn(dataDir.toFile());
        final var factory = new PersistentProtocolAdapterInstanceDataServiceFactory(systemInformation);

        final var firstWrittenValue = factory
                .getOrCreate("testProtocol", "testAdapter")
                .thenApply(instanceData -> {
                    instanceData.putValue("testKey", "testValue");
                    return instanceData;
                })
                .thenCompose(instanceData -> instanceData.getValue("testKey"));

        assertThat(firstWrittenValue.get())
                .isPresent()
                .get()
                .isEqualTo("testValue");

        final var secondWrittenValue = factory
                .getOrCreate("testProtocol", "testAdapter")
                .thenApply(instanceData -> {
                    instanceData.putValue("testKey", "testValue2");
                    return instanceData;
                })
                .thenCompose(instanceData -> instanceData.getValue("testKey"));

        assertThat(secondWrittenValue.get())
                .isPresent()
                .get()
                .isEqualTo("testValue2");

        final var clearedValue = factory
                .getOrCreate("testProtocol", "testAdapter")
                .thenApply(instanceData -> {
                    instanceData.putValue("testKey", null);
                    return instanceData;
                })
                .thenCompose(instanceData -> instanceData.getValue("testKey"));

        assertThat(clearedValue.get())
                .isEmpty();
    }

    @Test
    public void test_destroy() throws Exception {
        final var systemInformation = mock(SystemInformation.class);
        when(systemInformation.getDataFolder()).thenReturn(dataDir.toFile());
        final var factory = new PersistentProtocolAdapterInstanceDataServiceFactory(systemInformation);

        final var firstWrittenValue = factory
                .getOrCreate("testProtocol2", "testAdapter2")
                .thenApply(instanceData -> {
                    instanceData.putValue("testKey", "testValue");
                    return instanceData;
                })
                .thenCompose(instanceData -> instanceData.getValue("testKey"));

        assertThat(firstWrittenValue.get())
                .isPresent()
                .get()
                .isEqualTo("testValue");

        factory.destroy("testProtocol2", "testAdapter2");

        final var emptyValue = factory
                .getOrCreate("testProtocol2", "testAdapter2")
                .thenCompose(instanceData -> instanceData.getValue("testKey"));

        assertThat(emptyValue.get())
                .isEmpty();
    }

}
