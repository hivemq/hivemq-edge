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
package com.hivemq.edge.adapters.etherip_cip_odva.handler;

import com.hivemq.edge.adapters.etherip_cip_odva.exception.OdvaException;
import etherip.types.LogicalAddressPath;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class LogicalAdressPathFactoryTest {

    @Test
    void shouldCreate() throws OdvaException {
        // test 1
        LogicalAddressPath logicalAddressPath = LogicalAddressPathFactory.create("@1/2/3");
        Assertions.assertThat(logicalAddressPath.getClass_code()).isEqualTo(1);
        Assertions.assertThat(logicalAddressPath.getInstance()).isEqualTo(2);
        Assertions.assertThat(logicalAddressPath.getAttr()).isEqualTo(3);

        // test 2
        logicalAddressPath = LogicalAddressPathFactory.create("@11111/22222/33333");
        Assertions.assertThat(logicalAddressPath.getClass_code()).isEqualTo(11111);
        Assertions.assertThat(logicalAddressPath.getInstance()).isEqualTo(22222);
        Assertions.assertThat(logicalAddressPath.getAttr()).isEqualTo(33333);
    }

    @Test
    void shouldFailCreate() {
        Assertions.assertThatThrownBy(() -> LogicalAddressPathFactory.create(null))
                .isInstanceOf(OdvaException.class);
        Assertions.assertThatThrownBy(() -> LogicalAddressPathFactory.create(""))
                .isInstanceOf(OdvaException.class);
        Assertions.assertThatThrownBy(() -> LogicalAddressPathFactory.create("@1/2/"))
                .isInstanceOf(OdvaException.class);
        Assertions.assertThatThrownBy(() -> LogicalAddressPathFactory.create("@1/b/3"))
                .isInstanceOf(OdvaException.class);
    }
}
