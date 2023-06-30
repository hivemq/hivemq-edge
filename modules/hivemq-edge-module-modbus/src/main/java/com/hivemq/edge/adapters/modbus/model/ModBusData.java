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

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.message.QoS;

import java.util.Arrays;

/**
 * @author Simon L Johnson
 */
public class ModBusData {

    public enum TYPE {
        COILS,
        INPUT_REGISTERS,
        HOLDING_REGISTERS,
    }

    private Object[][]  data;
    private final long systemTime;
    private final TYPE type;
    private String topic;
    private QoS qos;

    public ModBusData(final @NotNull TYPE type, final String topic, final QoS qos) {
        this.type = type;
        this.systemTime = System.currentTimeMillis();
        this.topic = topic;
        this.qos = qos;
    }

    public long getSystemTime() {
        return systemTime;
    }

    public TYPE getType() {
        return type;
    }

    public Object[][] getData() {
        return data;
    }

    public String getTopic() {
        return topic;
    }

    public QoS getQos() {
        return qos;
    }

    public void setData(int startIdx, Object[] data) {
        Object[][] arr = new Object[data.length][2];
        for(int i = 0; i < data.length; i++){
            arr[i][0] = startIdx + i;
            arr[i][1] = data[i];
        }
        this.data = arr;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ModBusData{");
        sb.append("systemTime=").append(systemTime);
        sb.append(", type=").append(type);
        sb.append(", data=").append(Arrays.deepToString(data));
        sb.append('}');
        return sb.toString();
    }
}
