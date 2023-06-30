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
package com.hivemq.edge.modules.adapters.impl;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.hivemq.configuration.HivemqId;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterPublishBuilder;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterPublishService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.handler.publish.PublishReturnCode;
import net.javacrumbs.futureconverter.java8guava.FutureConverter;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ProtocolAdapterPublishServiceImpl implements ProtocolAdapterPublishService {

    private final @NotNull HivemqId hiveMqId;
    private final @NotNull ProtocolAdapterInterceptorHandler interceptorHandler;

    @Inject
    public ProtocolAdapterPublishServiceImpl(
            final @NotNull HivemqId hiveMqId, final @NotNull ProtocolAdapterInterceptorHandler interceptorHandler) {
        this.hiveMqId = hiveMqId;
        this.interceptorHandler = interceptorHandler;
    }

    @Override
    public @NotNull ProtocolAdapterPublishBuilder publish() {
        return new ProtocolAdapterPublishBuilderImpl(hiveMqId.get(), (publish, protocolAdapter, dynamicContext) -> {

            final ListenableFuture<PublishReturnCode> publishFuture = interceptorHandler.interceptOrDelegateInbound(
                    publish,
                    MoreExecutors.newDirectExecutorService(),
                    protocolAdapter,
                    dynamicContext);

            return FutureConverter.toCompletableFuture(publishFuture);
        });
    }
}


