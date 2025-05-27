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
package com.hivemq.bootstrap.ioc;

import com.hivemq.adapter.sdk.api.services.ProtocolAdapterMetricsService;
import com.hivemq.bootstrap.factories.DataCombiningTransformationServiceProvider;
import com.hivemq.bootstrap.factories.WritingServiceProvider;
import com.hivemq.combining.mapping.DataCombiningTransformationService;
import com.hivemq.protocols.InternalProtocolAdapterWritingService;
import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import org.jetbrains.annotations.NotNull;

import javax.inject.Singleton;

@Module
public abstract class CombiningModule {

    @Provides
    @Singleton
    static @NotNull DataCombiningTransformationService dataCombiningTransformationService(final DataCombiningTransformationServiceProvider provider) {
        return provider.get();
    }
}
