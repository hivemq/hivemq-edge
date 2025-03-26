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
package com.hivemq.combining.runtime;

import com.hivemq.combining.mapping.DataCombiningTransformationService;
import com.hivemq.combining.model.DataCombining;
import com.hivemq.combining.model.DataCombiningDestination;
import com.hivemq.configuration.HivemqId;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.publish.PUBLISHFactory;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import javax.inject.Singleton;

import static com.hivemq.mqtt.message.publish.PUBLISH.MESSAGE_EXPIRY_INTERVAL_NOT_SET;

@Singleton
public class DataCombiningPublishService {

    private final @NotNull HivemqId hiveMQId;
    private final @NotNull DataCombiningTransformationService dataCombiningTransformationService;

    @Inject
    public DataCombiningPublishService(
            final @NotNull HivemqId hiveMQId,
            final @NotNull DataCombiningTransformationService dataCombiningTransformationService) {
        this.hiveMQId = hiveMQId;
        this.dataCombiningTransformationService = dataCombiningTransformationService;
    }

    public void publish(
            final @NotNull DataCombiningDestination dataCombiningDestination,
            final byte @NotNull [] payload,
            final @NotNull DataCombining dataCombining) {
        dataCombiningTransformationService.applyMappings(new PUBLISHFactory.Mqtt5Builder().withHivemqId(hiveMQId.get())
                .withQoS(QoS.AT_LEAST_ONCE)
                .withOnwardQos(QoS.AT_LEAST_ONCE)
                .withRetain(false) // this message is not retained
                .withTopic(dataCombiningDestination.topic())
                .withPayload(payload)
                .withMessageExpiryInterval(MESSAGE_EXPIRY_INTERVAL_NOT_SET)
                .withResponseTopic(null)
                .withCorrelationData(null)
                .withPayload(payload)
                .withContentType(null)
                .withPayloadFormatIndicator(null)
                .withUserProperties(Mqtt5UserProperties.of())
                .build(), dataCombining);
    }
}
