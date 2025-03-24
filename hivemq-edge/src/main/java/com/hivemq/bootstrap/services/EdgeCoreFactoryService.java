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
package com.hivemq.bootstrap.services;

import com.hivemq.bootstrap.factories.DataCombiningTransformationServiceFactory;
import com.hivemq.bootstrap.factories.WritingServiceFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EdgeCoreFactoryService {

    private @Nullable WritingServiceFactory writingServiceFactory;
    private @Nullable DataCombiningTransformationServiceFactory dataCombiningTransformationServiceFactory;

    public void provideWritingServiceFactory(final @NotNull WritingServiceFactory writingServiceFactory) {
        this.writingServiceFactory = writingServiceFactory;
    }

    public void provideDataCombiningTransformationServiceFactory(
            final @NotNull DataCombiningTransformationServiceFactory dataCombiningTransformationServiceFactory) {
        this.dataCombiningTransformationServiceFactory = dataCombiningTransformationServiceFactory;
    }

    public @Nullable WritingServiceFactory getWritingServiceFactory() {
        return writingServiceFactory;
    }

    public @Nullable DataCombiningTransformationServiceFactory getDataCombiningTransformationServiceFactory() {
        return dataCombiningTransformationServiceFactory;
    }
}
