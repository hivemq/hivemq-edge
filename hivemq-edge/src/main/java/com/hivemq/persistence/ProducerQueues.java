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
package com.hivemq.persistence;

import com.google.common.util.concurrent.ListenableFuture;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author Daniel Krüger
 */
public interface ProducerQueues {


    <R> @NotNull ListenableFuture<R> submit(final @NotNull String key, final @NotNull SingleWriterService.Task<R> task);

    <R> @NotNull ListenableFuture<R> submit(final int bucketIndex, final @NotNull SingleWriterService.Task<R> task);


    <R> @Nullable ListenableFuture<R> submit(final int bucketIndex,
                                             final @NotNull SingleWriterService.Task<R> task,
                                             final @Nullable SingleWriterService.SuccessCallback<R> successCallback,
                                             final @Nullable SingleWriterService.FailedCallback failedCallback);

    @NotNull <R> List<ListenableFuture<R>> submitToAllBucketsParallel(final @NotNull SingleWriterService.Task<R> task);

    @NotNull <R> List<ListenableFuture<R>> submitToAllBucketsSequential(final @NotNull SingleWriterService.Task<R> task);

    int getBucket(final @NotNull String key);

    @NotNull ListenableFuture<Void> shutdown(final @Nullable SingleWriterService.Task<Void> finalTask);

}
