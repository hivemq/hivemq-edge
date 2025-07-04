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
package com.hivemq.mqtt.message.dropping;

import com.hivemq.logging.EventLog;
import com.hivemq.metrics.MetricsHolder;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

/**
 * @author Georg Held
 */
@Singleton
public class MessageDroppedServiceProvider implements Provider<MessageDroppedService> {

    private final MetricsHolder metricsHolder;
    private final EventLog eventLog;


    @Inject
    MessageDroppedServiceProvider(final MetricsHolder metricsHolder,
                                  final EventLog eventLog) {
        this.metricsHolder = metricsHolder;
        this.eventLog = eventLog;
    }

    @Override
    public MessageDroppedService get() {

        return new MessageDroppedServiceImpl(metricsHolder, eventLog);
    }
}
