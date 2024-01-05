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
package com.hivemq.bootstrap.factories;

import com.hivemq.configuration.info.SystemInformation;
import com.hivemq.configuration.service.PersistenceConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.logging.EventLog;
import com.hivemq.metrics.MetricsHolder;
import com.hivemq.mqtt.message.dropping.MessageDroppedService;
import com.hivemq.persistence.PersistenceStartup;
import com.hivemq.persistence.payload.PublishPayloadPersistence;
import com.hivemq.util.LocalPersistenceFileUtil;

import java.util.concurrent.ScheduledExecutorService;

public interface PublishPayloadPersistenceFactory {

    @NotNull PublishPayloadPersistence buildPublishPayloadPersistence(@NotNull LocalPersistenceFileUtil localPersistenceFileUtil,
                                                                      @NotNull MetricsHolder metricsHolder,
                                                                      @NotNull MessageDroppedService messageDroppedService,
                                                                      @NotNull ScheduledExecutorService scheduledExecutorService,
                                                                      @NotNull PersistenceStartup persistenceStartup,
                                                                      @NotNull PersistenceConfigurationService persistenceConfigurationService,
                                                                      @NotNull SystemInformation systemInformation);
}
