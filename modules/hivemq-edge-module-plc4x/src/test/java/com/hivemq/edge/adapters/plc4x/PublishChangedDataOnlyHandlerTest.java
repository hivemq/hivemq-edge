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
package com.hivemq.edge.adapters.plc4x;

import com.hivemq.edge.modules.adapters.impl.factories.AdapterFactoriesImpl;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class PublishChangedDataOnlyHandlerTest {

    @Test
    public void test() {
        final var factory = new AdapterFactoriesImpl();
        final var dataPointFactory = factory.dataPointFactory();
        final var toTest = new PublishChangedDataOnlyHandler();
        final var initial = toTest.checkIfValuesHaveChangedSinceLastInvocation("tag1", List.of(dataPointFactory.create("tag1", "value1")));
        final var secondTry = toTest.checkIfValuesHaveChangedSinceLastInvocation("tag1", List.of(dataPointFactory.create("tag1", "value1")));

        assertThat(initial).isTrue();
        assertThat(secondTry).isFalse();
    }
}
