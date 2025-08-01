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

import com.hivemq.bootstrap.factories.HandlerFactory;
import com.hivemq.bootstrap.factories.PrePublishProcessorHandlingFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

public class HandlerService {

    private @Nullable HandlerFactory handlerFactory;
    private final @NotNull SortedMap<Integer, PrePublishProcessorHandlingFactory> prePublishProcessorHandlingFactories =
            new TreeMap<>();

    public void supplyHandlerFactory(final @NotNull HandlerFactory handlerFactory) {
        this.handlerFactory = handlerFactory;
    }


    /**
     * @param prio                               lower prio is executed first, same prio will throw exception
     * @param prePublishProcessorHandlingFactory the HandlerFactory will only get called once when the first message
     *                                           is processed
     */
    public void supplyPrePublishProcessorHandlingFactory(
            final int prio,
            final @NotNull PrePublishProcessorHandlingFactory prePublishProcessorHandlingFactory) {
        final PrePublishProcessorHandlingFactory prev =
                prePublishProcessorHandlingFactories.putIfAbsent(prio, prePublishProcessorHandlingFactory);
        if (prev != null) {
            throw new IllegalStateException("PrePublishProcessorHandlingFactory with same priority " +
                    prio +
                    "already set. existing:  " +
                    prev.getClass().getCanonicalName() +
                    ", new: " +
                    prePublishProcessorHandlingFactory.getClass().getCanonicalName());
        }
    }


    public @Nullable HandlerFactory getHandlerFactory() {
        return handlerFactory;
    }

    public @NotNull List<PrePublishProcessorHandlingFactory> getPrePublishProcessorHandlingFactories() {
        return List.copyOf(prePublishProcessorHandlingFactories.values());
    }
}
