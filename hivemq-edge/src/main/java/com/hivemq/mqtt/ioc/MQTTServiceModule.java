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
package com.hivemq.mqtt.ioc;

import com.hivemq.limitation.TopicAliasLimiter;
import com.hivemq.limitation.TopicAliasLimiterImpl;
import com.hivemq.mqtt.services.InternalPublishService;
import com.hivemq.mqtt.services.InternalPublishServiceImpl;
import com.hivemq.mqtt.services.PrePublishProcessorService;
import com.hivemq.mqtt.services.PrePublishProcessorServiceImpl;
import com.hivemq.mqtt.services.PublishDistributor;
import com.hivemq.mqtt.services.PublishDistributorImpl;
import com.hivemq.mqtt.services.PublishPollService;
import com.hivemq.mqtt.services.PublishPollServiceImpl;
import dagger.Binds;
import dagger.Module;
import org.jetbrains.annotations.NotNull;

@Module
public interface MQTTServiceModule {

    @Binds
    @NotNull PrePublishProcessorService prePublishProcessorService(@NotNull PrePublishProcessorServiceImpl prePublishProcessorService);

    @Binds
    @NotNull InternalPublishService internalPublishService(@NotNull InternalPublishServiceImpl internalPublishService);

    @Binds
    @NotNull PublishDistributor publishDistributor(@NotNull PublishDistributorImpl publishDistributor);

    @Binds
    @NotNull TopicAliasLimiter topicAliasLimiter(@NotNull TopicAliasLimiterImpl topicAliasLimiter);

    @Binds
    @NotNull PublishPollService publishPollService(@NotNull PublishPollServiceImpl publishPollService);

}
