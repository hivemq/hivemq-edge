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
package com.hivemq.mqtt.message;

/**
 * @author Dominik Obermaier
 */
public enum ProtocolVersion {

    /**
     * The protocol version which indicates a MQTT 3.1 connection
     */
    MQTTv3_1,
    /**
     * The protocol version which indicates a MQTT 3.1.1 connection
     */
    MQTTv3_1_1,
    /**
     * The protocol version which indicates a MQTT 5 connection
     */
    MQTTv5,
    /**
     * The protocol version which indicates a MQTT-SN 1.2 connection
     */
    MQTTSNv1_2,
    /**
     * The protocol version which indicates a MQTT-SN 2.0 connection
     */
    MQTTSNv2_0,

}
