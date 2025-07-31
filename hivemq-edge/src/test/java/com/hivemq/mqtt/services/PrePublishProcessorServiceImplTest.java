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
package com.hivemq.mqtt.services;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.bootstrap.factories.HandlerResult;
import com.hivemq.bootstrap.factories.InternalPublishServiceHandlingProvider;
import com.hivemq.bootstrap.factories.PrePublishProcessorHandlingProvider;
import com.hivemq.mqtt.message.publish.PUBLISH;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PrePublishProcessorServiceImplTest {


    private final @NotNull InternalPublishService internalPublishService = mock();
    private final @NotNull PrePublishProcessorHandlingProvider processorHandlingProvider = mock();
    private final @NotNull InternalPublishServiceHandlingProvider internalPublishServiceHandlingProvider = mock();

    private final @NotNull PrePublishProcessorServiceImpl prePublishProcessorService =
            new PrePublishProcessorServiceImpl(internalPublishService,
                    processorHandlingProvider,
                    internalPublishServiceHandlingProvider);

    @BeforeEach
    void setUp() {

    }

    @Test
    void test_publish_noPreprocessors_directlyPublishOnInternalPublishService() {
        final PUBLISH publish = mock(PUBLISH.class);
        final ListeningExecutorService executorService = MoreExecutors.newDirectExecutorService();

        prePublishProcessorService.publish(publish, executorService, "me");

        verify(internalPublishService, times(1)).publish(same(publish), same(executorService), eq("me"));
    }

    @Test
    void test_publish_onePreprocessor_applyPreprocessorAndRedirectToInternalPublishService() {
        final PUBLISH publish = mock(PUBLISH.class);
        final PUBLISH modifiedPublish = mock(PUBLISH.class);

        final ListeningExecutorService executorService = MoreExecutors.newDirectExecutorService();
        when(processorHandlingProvider.get()).thenReturn(List.of((originalPublish, sender, executorService1) -> {
            final SettableFuture<HandlerResult> settableFuture = SettableFuture.create();
            settableFuture.set(new HandlerResult(false, modifiedPublish, null));
            return settableFuture;
        }));

        prePublishProcessorService.publish(publish, executorService, "me");

        verify(internalPublishService, times(1)).publish(same(modifiedPublish), same(executorService), eq("me"));
    }

    @Test
    void test_publish_twoPreprocessor_applyPreprocessorAndRedirectToInternalPublishService() {
        final PUBLISH publish = mock(PUBLISH.class);
        final PUBLISH modifiedPublish = mock(PUBLISH.class);
        final PUBLISH modifiedPublish2 = mock(PUBLISH.class);
        final AtomicBoolean correctModifiedPublish = new AtomicBoolean(false);

        final ListeningExecutorService executorService = MoreExecutors.newDirectExecutorService();
        when(processorHandlingProvider.get()).thenReturn(List.of((originalPublish, sender, executorService1) -> {
            final SettableFuture<HandlerResult> settableFuture = SettableFuture.create();
            settableFuture.set(new HandlerResult(false, modifiedPublish, null));
            return settableFuture;
        }, (originalPublish, sender, executorService2) -> {
            final SettableFuture<HandlerResult> settableFuture = SettableFuture.create();
            if (originalPublish == modifiedPublish) {
                correctModifiedPublish.set(true);
            }
            settableFuture.set(new HandlerResult(false, modifiedPublish2, null));
            return settableFuture;
        }));

        prePublishProcessorService.publish(publish, executorService, "me");

        verify(internalPublishService, times(1)).publish(same(modifiedPublish2), same(executorService), eq("me"));
        assertTrue(correctModifiedPublish.get());
    }

    @Test
    void test_publish_oneProcessorAndProhibitOfPublishing_noInternalPublish() {
        final PUBLISH publish = mock(PUBLISH.class);
        final PUBLISH modifiedPublish = mock(PUBLISH.class);

        final ListeningExecutorService executorService = MoreExecutors.newDirectExecutorService();
        when(processorHandlingProvider.get()).thenReturn(List.of((originalPublish, sender, executorService1) -> {
            final SettableFuture<HandlerResult> settableFuture = SettableFuture.create();
            settableFuture.set(new HandlerResult(true, modifiedPublish, null));
            return settableFuture;
        }));

        prePublishProcessorService.publish(publish, executorService, "me");

        verify(internalPublishService, times(0)).publish(same(modifiedPublish), same(executorService), eq("me"));
    }


    @Test
    void test_publish_whenFirstProhibitsPublishing_noInternalPublish() {
        final PUBLISH publish = mock(PUBLISH.class);
        final PUBLISH modifiedPublish = mock(PUBLISH.class);
        final PUBLISH modifiedPublish2 = mock(PUBLISH.class);
        final AtomicBoolean secondCalled = new AtomicBoolean(false);

        final ListeningExecutorService executorService = MoreExecutors.newDirectExecutorService();
        when(processorHandlingProvider.get()).thenReturn(List.of((originalPublish, sender, executorService1) -> {
            final SettableFuture<HandlerResult> settableFuture = SettableFuture.create();
            settableFuture.set(new HandlerResult(true, modifiedPublish, null));
            return settableFuture;
        }, (originalPublish, sender, executorService2) -> {
            final SettableFuture<HandlerResult> settableFuture = SettableFuture.create();
            secondCalled.set(true);

            settableFuture.set(new HandlerResult(false, modifiedPublish2, null));
            return settableFuture;
        }));

        prePublishProcessorService.publish(publish, executorService, "me");

        verify(internalPublishService, times(0)).publish(same(modifiedPublish2), same(executorService), eq("me"));
        assertFalse(secondCalled.get());
    }
}
