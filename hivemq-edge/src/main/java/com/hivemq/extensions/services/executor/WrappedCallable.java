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
package com.hivemq.extensions.services.executor;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

/**
 * @author Florian Limpöck
 * @since 4.0.0
 */
public class WrappedCallable<V> implements Callable<V> {

    @NotNull
    private final Callable<V> callable;

    @NotNull
    private final ClassLoader classLoader;

    @Nullable
    private final CompletableFuture<V> completableFuture;

    WrappedCallable(
            final @NotNull Callable<V> callable, final @NotNull ClassLoader classLoader,
            final @Nullable CompletableFuture<V> completableFuture) {
        this.callable = callable;
        this.classLoader = classLoader;
        this.completableFuture = completableFuture;
    }

    @Nullable
    @Override
    public V call() throws Exception {

        final ClassLoader previousClassLoader = Thread.currentThread().getContextClassLoader();

        try {
            Thread.currentThread().setContextClassLoader(classLoader);
            final V call = callable.call();
            if (completableFuture != null) {
                completableFuture.complete(call);
            }
            return call;
        } catch (final Throwable t) {
            if (completableFuture != null) {
                completableFuture.completeExceptionally(t);
            }
            throw t;
        } finally {
            Thread.currentThread().setContextClassLoader(previousClassLoader);
        }
    }
}
