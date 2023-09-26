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
package com.hivemq.edge.adapters.modbus.model;

import com.hivemq.edge.modules.adapters.data.ProtocolAdapterDataSample;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.message.QoS;

import java.util.Arrays;

/**
 * @author Simon L Johnson
 */
public class ModBusData extends ProtocolAdapterDataSample {

    public enum TYPE {
        COILS,
        INPUT_REGISTERS,
        HOLDING_REGISTERS,
    }

    private final TYPE type;

    public ModBusData(final Object data, final String topic, final int qos, final TYPE type) {
        super(data, topic, qos);
        this.type = type;
    }

    public TYPE getType() {
        return type;
    }

    public void setData(int startIdx, Object[] data) {
        Object[][] arr = new Object[data.length][2];
        for(int i = 0; i < data.length; i++){
            arr[i][0] = startIdx + i;
            arr[i][1] = data[i];
        }
        this.data = arr;
    }
}
