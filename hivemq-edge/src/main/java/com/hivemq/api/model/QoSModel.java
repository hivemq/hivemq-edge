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
package com.hivemq.api.model;

import com.hivemq.extension.sdk.api.packets.general.Qos;
import com.hivemq.mqtt.message.QoS;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jetbrains.annotations.NotNull;

@Schema(name = "QoS", enumAsRef=true)
public enum QoSModel {

    /**
     * At most once delivery. The message will be delivered once or never (best effort delivery)
     */
    AT_MOST_ONCE(0),
    /**
     * At least once delivery. The message will be delivered once or multiple times
     */
    AT_LEAST_ONCE(1),
    /**
     * At exactly once delivery. The message will be delivered once and only once
     */
    EXACTLY_ONCE(2);

    private final int qosNumber;
    private final @NotNull Qos qos;

    QoSModel(final int qosNumber) {
        this.qosNumber = qosNumber;
        qos = Qos.valueOf(name());
    }

    public @NotNull Qos toQos() {
        return qos;
    }

    public int getQosNumber() {
        return qosNumber;
    }

    public static @NotNull QoSModel fromNumber(final int qos){
        switch(qos){
            case 0:
                return QoSModel.AT_MOST_ONCE;
            case 1:
                return QoSModel.AT_LEAST_ONCE;
            case 2:
                return QoSModel.EXACTLY_ONCE;
            default:
                throw new IllegalArgumentException("Can not parse '" + qos +"' to QoSModel.");
        }
    }
}
