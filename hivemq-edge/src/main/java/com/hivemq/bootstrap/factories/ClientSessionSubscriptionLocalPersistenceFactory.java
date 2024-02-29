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

import com.hivemq.configuration.service.PersistenceConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.message.dropping.MessageDroppedService;
import com.hivemq.persistence.PersistenceStartup;
import com.hivemq.persistence.clientqueue.ClientQueueLocalPersistence;
import com.hivemq.persistence.local.ClientSessionSubscriptionLocalPersistence;
import com.hivemq.persistence.payload.PublishPayloadPersistence;
import com.hivemq.util.LocalPersistenceFileUtil;

public interface ClientSessionSubscriptionLocalPersistenceFactory {


    @NotNull ClientSessionSubscriptionLocalPersistence buildClientSessionSubscriptionLocalPersistence(@NotNull LocalPersistenceFileUtil localPersistenceFileUtil,
                                                                                                      @NotNull PublishPayloadPersistence payloadPersistence,
                                                                                                      @NotNull PersistenceStartup persistenceStartup);
}
