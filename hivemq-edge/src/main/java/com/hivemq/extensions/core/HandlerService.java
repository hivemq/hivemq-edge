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
package com.hivemq.extensions.core;

import com.hivemq.bootstrap.factories.AdapterHandlingFactory;
import com.hivemq.bootstrap.factories.HandlerFactory;
import com.hivemq.bootstrap.factories.InternalPublishServiceHandlingFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HandlerService {

    private @Nullable HandlerFactory handlerFactory;
    private @Nullable AdapterHandlingFactory adapterHandlingFactory;
    private @Nullable InternalPublishServiceHandlingFactory internalPublishServiceHandlingFactory;


    public void supplyHandlerFactory(final @NotNull HandlerFactory handlerFactory) {
        this.handlerFactory = handlerFactory;
    }

    public void supplyAdapterHandlingFactory(final @NotNull AdapterHandlingFactory adapterHandlingFactory) {
        this.adapterHandlingFactory = adapterHandlingFactory;
    }

    public void supplyInternalPublishServiceHandlingFactory(final @NotNull InternalPublishServiceHandlingFactory internalPublishServiceHandlingFactory) {
        this.internalPublishServiceHandlingFactory = internalPublishServiceHandlingFactory;
    }


    public @Nullable HandlerFactory getHandlerFactory() {
        return handlerFactory;
    }

    public @Nullable AdapterHandlingFactory getAdapterHandlerFactory() {
        return adapterHandlingFactory;
    }

    public @Nullable InternalPublishServiceHandlingFactory get() {
        return internalPublishServiceHandlingFactory;
    }

    public @Nullable InternalPublishServiceHandlingFactory getInternalPublishServiceHandlingFactory() {
        return internalPublishServiceHandlingFactory;
    }
}
