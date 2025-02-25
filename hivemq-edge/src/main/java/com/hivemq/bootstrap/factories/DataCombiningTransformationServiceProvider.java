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

import com.hivemq.adapter.sdk.api.services.ProtocolAdapterMetricsService;
import com.hivemq.adapter.sdk.api.writing.WritingProtocolAdapter;
import com.hivemq.bootstrap.services.EdgeCoreFactoryService;
import com.hivemq.combining.mapping.DataCombiningTransformationService;
import com.hivemq.mqtt.services.InternalPublishService;
import com.hivemq.protocols.InternalProtocolAdapterWritingService;
import com.hivemq.protocols.InternalWritingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Singleton
public class DataCombiningTransformationServiceProvider {

    private final @NotNull EdgeCoreFactoryService edgeCoreFactoryService;
    private final @NotNull InternalPublishService internalPublishService;

    @Inject
    public DataCombiningTransformationServiceProvider(
            final @NotNull EdgeCoreFactoryService edgeCoreFactoryService,
            final @NotNull InternalPublishService internalPublishService) {
        this.edgeCoreFactoryService = edgeCoreFactoryService;

        this.internalPublishService = internalPublishService;
    }

    public @NotNull DataCombiningTransformationService get() {
        final @Nullable DataCombiningTransformationServiceFactory serviceFactory =
                edgeCoreFactoryService.getDataCombiningTransformationServiceFactory();
        if (serviceFactory == null) {
            return new DataCombiningTransformationServiceNoop();
        }
        return serviceFactory.build(internalPublishService);
    }
}
