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
package com.hivemq.edge.tempdata.inmem;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryProtocolAdapterInstanceDataServiceFactoryTest {

    private static final Logger log =
            LoggerFactory.getLogger(InMemoryProtocolAdapterInstanceDataServiceFactoryTest.class);

    @Test
    public void test_getOrCreate() throws Exception {
        final var factory = new InMemoryProtocolAdapterInstanceDataServiceFactory();

        final var trialOne = factory.getOrCreate("testProtocol", "testAdapter");
        final var trialTwo = factory.getOrCreate("testProtocol", "testAdapter");

        assertThat(trialOne.get()).isEqualTo(trialTwo.get());
    }

}
