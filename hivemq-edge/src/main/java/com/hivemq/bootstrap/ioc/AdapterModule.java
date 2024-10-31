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

import com.hivemq.adapter.sdk.api.services.ProtocolAdapterTagService;
import com.hivemq.bootstrap.factories.WritingServiceProvider;
import com.hivemq.edge.modules.adapters.ProtocolAdapterTagServiceImpl;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterWritingService;
import dagger.Binds;
import dagger.Module;
import dagger.Provides;

import javax.inject.Singleton;

@Module
public abstract class AdapterModule {

    @Provides
    @Singleton
    static @NotNull ProtocolAdapterWritingService adapterWritingService(final WritingServiceProvider writingServiceProvider) {
        return writingServiceProvider.get();
    }

    @Binds
    abstract @NotNull ProtocolAdapterTagService ProtocolAdapterTagService(@NotNull ProtocolAdapterTagServiceImpl protocolAdapterTagService);




}
