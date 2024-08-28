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
package com.hivemq.edge.adapters.etherip.model;

import com.hivemq.edge.adapters.etherip.model.dataypes.EtherIpBool;
import com.hivemq.edge.adapters.etherip.model.dataypes.EtherIpDouble;
import com.hivemq.edge.adapters.etherip.model.dataypes.EtherIpInt;
import com.hivemq.edge.adapters.etherip.model.dataypes.EtherIpLong;
import etherip.types.CIPData;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

public class EtherIpValueFactoryTest {

    @Test
    public void test_EtherIpBool_False() throws Exception {
        CIPData data = new CIPData(CIPData.Type.BOOL, 1);
        data.set(0, 0);
        assertThat(EtherIpValueFactory.fromTagAddressAndCipData("wullewu", data))
                .isNotEmpty()
                .get()
                .isInstanceOf(EtherIpBool.class)
                .extracting(EtherIpValue::getTagAdress, EtherIpValue::getValue)
                .containsExactly("wullewu", false);
    }

    @Test
    public void test_EtherIpBool_True() throws Exception {
        CIPData data = new CIPData(CIPData.Type.BOOL, 1);
        data.set(0, 1);
        assertThat(EtherIpValueFactory.fromTagAddressAndCipData("wullewu", data))
                .isNotEmpty()
                .get()
                .isInstanceOf(EtherIpBool.class)
                .extracting(EtherIpValue::getTagAdress, EtherIpValue::getValue)
                .containsExactly("wullewu", true);
    }

    @Test
    public void test_EtherIpDouble() throws Exception {
        CIPData data = new CIPData(CIPData.Type.REAL, 1);
        data.set(0, 1.12);
        assertThat(EtherIpValueFactory.fromTagAddressAndCipData("wullewu", data))
                .isNotEmpty()
                .get()
                .isInstanceOf(EtherIpDouble.class)
                .satisfies(it -> {
                    assertThat(it.getTagAdress()).isEqualTo("wullewu");
                    assertThat((Double)it.getValue()).isEqualTo(1.12, offset(2D));
                });
    }

    @Test
    public void test_EtherIpInt() throws Exception {
        CIPData data = new CIPData(CIPData.Type.INT, 1);
        data.set(0, 17);
        assertThat(EtherIpValueFactory.fromTagAddressAndCipData("wullewu", data))
                .isNotEmpty()
                .get()
                .isInstanceOf(EtherIpInt.class)
                .extracting(EtherIpValue::getTagAdress, EtherIpValue::getValue)
                .containsExactly("wullewu", 17);
    }

    @Test
    public void test_EtherIpLong() throws Exception {
        CIPData data = new CIPData(CIPData.Type.DINT, 1);
        data.set(0, 17L);
        assertThat(EtherIpValueFactory.fromTagAddressAndCipData("wullewu", data))
                .isNotEmpty()
                .get()
                .isInstanceOf(EtherIpLong.class)
                .extracting(EtherIpValue::getTagAdress, EtherIpValue::getValue)
                .containsExactly("wullewu", 17L);
    }

    @Test
    @Disabled("CIPData needs to be constructed using the CIPData(Type type, byte[] data) constructor")
    //FIXME: enabled test
    public void test_EtherIpString() throws Exception {
        CIPData data = new CIPData(CIPData.Type.STRUCT, 1);
        data.setString("test");
        assertThat(EtherIpValueFactory.fromTagAddressAndCipData("wullewu", data))
                .isNotEmpty()
                .get()
                .isInstanceOf(EtherIpBool.class)
                .extracting(EtherIpValue::getTagAdress, EtherIpValue::getValue)
                .containsExactly("wullewu", "test");
    }
}
