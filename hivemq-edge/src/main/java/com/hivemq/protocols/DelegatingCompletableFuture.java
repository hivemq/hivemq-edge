/*
 *  Copyright 2019-present HiveMQ GmbH
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.hivemq.protocols;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public final class DelegatingCompletableFuture<T> extends CompletableFuture<T> {

    private volatile @Nullable CompletableFuture<T> future;

    public DelegatingCompletableFuture() {
        future = null;
    }

    public boolean isEmpty() {
        return future == null;
    }

    public @Nullable CompletableFuture<T> getFuture() {
        return future;
    }

    public void setFuture(@Nullable final CompletableFuture<T> future) {
        this.future = future;
    }

    @Override
    public T get(final long timeout, final @NotNull TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        if (isEmpty()) {
            return null;
        }
        return future.get(timeout, unit);
    }

    @Override
    public boolean cancel(final boolean mayInterruptIfRunning) {
        if (isEmpty()) {
            return false;
        }
        return future.cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean isCancelled() {
        if (isEmpty()) {
            return false;
        }
        return future.isCancelled();
    }

    @Override
    public boolean isDone() {
        if (isEmpty()) {
            return false;
        }
        return future.isDone();
    }

    @Override
    public @Nullable T join() {
        if (isEmpty()) {
            return null;
        }
        return future.join();
    }

    @Override
    public @Nullable T get() throws InterruptedException, ExecutionException {
        if (isEmpty()) {
            return null;
        }
        return future.get();
    }

    @Override
    public @NotNull <U> CompletableFuture<U> thenApply(@NotNull final Function<? super T, ? extends U> fn) {
        if (isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        return super.thenApply(fn);
    }

    @Override
    public @NotNull <U> CompletableFuture<U> thenApplyAsync(@NotNull final Function<? super T, ? extends U> fn) {
        if (isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        return future.thenApplyAsync(fn);
    }

    @Override
    public @NotNull <U> CompletableFuture<U> thenApplyAsync(
            @NotNull final Function<? super T, ? extends U> fn,
            final Executor executor) {
        if (isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        return future.thenApplyAsync(fn, executor);
    }

    @Override
    public @NotNull CompletableFuture<Void> thenAccept(@NotNull final Consumer<? super T> action) {
        if (isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        return future.thenAccept(action);
    }

    @Override
    public @NotNull CompletableFuture<Void> thenAcceptAsync(@NotNull final Consumer<? super T> action) {
        if (isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        return future.thenAcceptAsync(action);
    }

    @Override
    public @NotNull CompletableFuture<Void> thenAcceptAsync(
            @NotNull final Consumer<? super T> action,
            final @NotNull Executor executor) {
        if (isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        return future.thenAcceptAsync(action, executor);
    }

    @Override
    public @NotNull CompletableFuture<Void> thenRun(@NotNull final Runnable action) {
        if (isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        return future.thenRun(action);
    }

    @Override
    public @NotNull CompletableFuture<Void> thenRunAsync(@NotNull final Runnable action) {
        if (isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        return future.thenRunAsync(action);
    }

    @Override
    public @NotNull CompletableFuture<Void> thenRunAsync(
            @NotNull final Runnable action,
            final @NotNull Executor executor) {
        if (isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        return future.thenRunAsync(action, executor);
    }

    @Override
    public @NotNull <U, V> CompletableFuture<V> thenCombine(
            @NotNull final CompletionStage<? extends U> other,
            @NotNull final BiFunction<? super T, ? super U, ? extends V> fn) {
        if (isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        return future.thenCombine(other, fn);
    }

    @Override
    public @NotNull <U, V> CompletableFuture<V> thenCombineAsync(
            @NotNull final CompletionStage<? extends U> other,
            @NotNull final BiFunction<? super T, ? super U, ? extends V> fn) {
        if (isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        return future.thenCombineAsync(other, fn);
    }

    @Override
    public @NotNull <U, V> CompletableFuture<V> thenCombineAsync(
            @NotNull final CompletionStage<? extends U> other,
            @NotNull final BiFunction<? super T, ? super U, ? extends V> fn,
            final @NotNull Executor executor) {
        if (isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        return future.thenCombineAsync(other, fn, executor);
    }

    @Override
    public @NotNull <U> CompletableFuture<Void> thenAcceptBoth(
            @NotNull final CompletionStage<? extends U> other,
            @NotNull final BiConsumer<? super T, ? super U> action) {
        if (isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        return future.thenAcceptBoth(other, action);
    }

    @Override
    public @NotNull <U> CompletableFuture<Void> thenAcceptBothAsync(
            @NotNull final CompletionStage<? extends U> other,
            @NotNull final BiConsumer<? super T, ? super U> action) {
        if (isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        return future.thenAcceptBothAsync(other, action);
    }

    @Override
    public @NotNull <U> CompletableFuture<Void> thenAcceptBothAsync(
            @NotNull final CompletionStage<? extends U> other,
            @NotNull final BiConsumer<? super T, ? super U> action,
            final @NotNull Executor executor) {
        if (isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        return future.thenAcceptBothAsync(other, action, executor);
    }

    @Override
    public @NotNull CompletableFuture<Void> runAfterBoth(
            @NotNull final CompletionStage<?> other,
            @NotNull final Runnable action) {
        if (isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        return future.runAfterBoth(other, action);
    }

    @Override
    public @NotNull CompletableFuture<Void> runAfterBothAsync(
            @NotNull final CompletionStage<?> other,
            @NotNull final Runnable action) {
        if (isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        return future.runAfterBothAsync(other, action);
    }

    @Override
    public @NotNull CompletableFuture<Void> runAfterBothAsync(
            @NotNull final CompletionStage<?> other,
            @NotNull final Runnable action,
            final @NotNull Executor executor) {
        if (isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        return future.runAfterBothAsync(other, action, executor);
    }

    @Override
    public @NotNull <U> CompletableFuture<U> applyToEither(
            @NotNull final CompletionStage<? extends T> other,
            @NotNull final Function<? super T, U> fn) {
        if (isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        return future.applyToEither(other, fn);
    }

    @Override
    public @NotNull <U> CompletableFuture<U> applyToEitherAsync(
            @NotNull final CompletionStage<? extends T> other,
            @NotNull final Function<? super T, U> fn) {
        if (isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        return future.applyToEitherAsync(other, fn);
    }

    @Override
    public @NotNull <U> CompletableFuture<U> applyToEitherAsync(
            @NotNull final CompletionStage<? extends T> other,
            @NotNull final Function<? super T, U> fn,
            final @NotNull Executor executor) {
        if (isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        return future.applyToEitherAsync(other, fn, executor);
    }

    @Override
    public @NotNull CompletableFuture<Void> acceptEither(
            @NotNull final CompletionStage<? extends T> other,
            @NotNull final Consumer<? super T> action) {
        if (isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        return future.acceptEither(other, action);
    }

    @Override
    public @NotNull CompletableFuture<Void> acceptEitherAsync(
            @NotNull final CompletionStage<? extends T> other,
            @NotNull final Consumer<? super T> action) {
        if (isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        return future.acceptEitherAsync(other, action);
    }

    @Override
    public @NotNull CompletableFuture<Void> acceptEitherAsync(
            @NotNull final CompletionStage<? extends T> other,
            @NotNull final Consumer<? super T> action,
            final @NotNull Executor executor) {
        if (isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        return future.acceptEitherAsync(other, action, executor);
    }

    @Override
    public @NotNull CompletableFuture<Void> runAfterEither(
            @NotNull final CompletionStage<?> other,
            @NotNull final Runnable action) {
        if (isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        return future.runAfterEither(other, action);
    }

    @Override
    public @NotNull CompletableFuture<Void> runAfterEitherAsync(
            @NotNull final CompletionStage<?> other,
            @NotNull final Runnable action) {
        if (isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        return future.runAfterEitherAsync(other, action);
    }

    @Override
    public @NotNull CompletableFuture<Void> runAfterEitherAsync(
            @NotNull final CompletionStage<?> other,
            @NotNull final Runnable action,
            final @NotNull Executor executor) {
        if (isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        return future.runAfterEitherAsync(other, action, executor);
    }

    @Override
    public @NotNull <U> CompletableFuture<U> thenCompose(@NotNull final Function<? super T, ? extends CompletionStage<U>> fn) {
        if (isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        return future.thenCompose(fn);
    }

    @Override
    public @NotNull <U> CompletableFuture<U> thenComposeAsync(@NotNull final Function<? super T, ? extends CompletionStage<U>> fn) {
        if (isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        return future.thenComposeAsync(fn);
    }

    @Override
    public @NotNull <U> CompletableFuture<U> thenComposeAsync(
            @NotNull final Function<? super T, ? extends CompletionStage<U>> fn,
            final @NotNull Executor executor) {
        if (isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        return future.thenComposeAsync(fn, executor);
    }

    @Override
    public @NotNull CompletableFuture<T> whenComplete(@NotNull final BiConsumer<? super @UnknownNullability T, ? super @UnknownNullability Throwable> action) {
        if (isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        return future.whenComplete(action);
    }

    @Override
    public @NotNull CompletableFuture<T> whenCompleteAsync(@NotNull final BiConsumer<? super @UnknownNullability T, ? super @UnknownNullability Throwable> action) {
        if (isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        return future.whenCompleteAsync(action);
    }

    @Override
    public @NotNull CompletableFuture<T> whenCompleteAsync(
            @NotNull final BiConsumer<? super @UnknownNullability T, ? super @UnknownNullability Throwable> action,
            final @NotNull Executor executor) {
        if (isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        return future.whenCompleteAsync(action, executor);
    }

    @Override
    public @NotNull <U> CompletableFuture<U> handle(@NotNull final BiFunction<? super T, Throwable, ? extends U> fn) {
        if (isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        return future.handle(fn);
    }

    @Override
    public @NotNull <U> CompletableFuture<U> handleAsync(@NotNull final BiFunction<? super T, Throwable, ? extends U> fn) {
        if (isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        return future.handleAsync(fn);
    }

    @Override
    public @NotNull <U> CompletableFuture<U> handleAsync(
            @NotNull final BiFunction<? super T, Throwable, ? extends U> fn,
            final @NotNull Executor executor) {
        if (isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        return future.handleAsync(fn, executor);
    }

    @Override
    public CompletableFuture<T> toCompletableFuture() {
        if (isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        return future.toCompletableFuture();
    }

    @Override
    public @NotNull CompletableFuture<T> exceptionally(@NotNull final Function<Throwable, ? extends T> fn) {
        if (isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        return future.exceptionally(fn);
    }

    @Override
    public @NotNull CompletableFuture<T> exceptionallyAsync(@NotNull final Function<Throwable, ? extends T> fn) {
        if (isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        return future.exceptionallyAsync(fn);
    }

    @Override
    public @NotNull CompletableFuture<T> exceptionallyAsync(
            @NotNull final Function<Throwable, ? extends T> fn,
            final @NotNull Executor executor) {
        if (isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        return future.exceptionallyAsync(fn, executor);
    }

    @Override
    public @NotNull CompletableFuture<T> exceptionallyCompose(@NotNull final Function<Throwable, ? extends CompletionStage<T>> fn) {
        if (isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        return future.exceptionallyCompose(fn);
    }

    @Override
    public @NotNull CompletableFuture<T> exceptionallyComposeAsync(@NotNull final Function<Throwable, ? extends CompletionStage<T>> fn) {
        if (isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        return future.exceptionallyComposeAsync(fn);
    }

    @Override
    public @NotNull CompletableFuture<T> exceptionallyComposeAsync(
            @NotNull final Function<Throwable, ? extends CompletionStage<T>> fn,
            final @NotNull Executor executor) {
        if (isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        return future.exceptionallyComposeAsync(fn, executor);
    }

    @Override
    public boolean isCompletedExceptionally() {
        if (isEmpty()) {
            return false;
        }
        return future.isCompletedExceptionally();
    }

    @Override
    public void obtrudeValue(final T value) {
        if (future != null) {
            future.obtrudeValue(value);
        }
    }

    @Override
    public void obtrudeException(final Throwable ex) {
        if (future != null) {
            future.obtrudeException(ex);
        }
    }

    @Override
    public int getNumberOfDependents() {
        if (isEmpty()) {
            return 0;
        }
        return future.getNumberOfDependents();
    }

    @Override
    public @Nullable String toString() {
        if (isEmpty()) {
            return super.toString();
        }
        return future.toString();
    }

    @Override
    public <U> CompletableFuture<U> newIncompleteFuture() {
        if (isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        return future.newIncompleteFuture();
    }

    @Override
    public @NotNull Executor defaultExecutor() {
        if (isEmpty()) {
            return CompletableFuture.delayedExecutor(0, TimeUnit.MILLISECONDS);
        }
        return future.defaultExecutor();
    }

    @Override
    public @NotNull CompletableFuture<T> copy() {
        if (isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        return future.copy();
    }

    @Override
    public @NotNull CompletionStage<T> minimalCompletionStage() {
        if (isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        return future.minimalCompletionStage();
    }

    @Override
    public @NotNull CompletableFuture<T> completeAsync(
            @NotNull final Supplier<? extends T> supplier,
            final @NotNull Executor executor) {
        if (isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        return future.completeAsync(supplier, executor);
    }

    @Override
    public @NotNull CompletableFuture<T> completeAsync(@NotNull final Supplier<? extends T> supplier) {
        if (isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        return future.completeAsync(supplier);
    }

    @Override
    public @NotNull CompletableFuture<T> orTimeout(final long timeout, @NotNull final TimeUnit unit) {
        if (isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        return future.orTimeout(timeout, unit);
    }

    @Override
    public @NotNull CompletableFuture<T> completeOnTimeout(
            final @Nullable T value,
            final long timeout,
            @NotNull final TimeUnit unit) {
        if (isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        return future.completeOnTimeout(value, timeout, unit);
    }

    @Override
    public T getNow(final @Nullable T valueIfAbsent) {
        if (isEmpty()) {
            return null;
        }
        return future.getNow(valueIfAbsent);
    }

    @Override
    public boolean complete(final @Nullable T value) {
        if (isEmpty()) {
            return false;
        }
        return future.complete(value);
    }

    @Override
    public boolean completeExceptionally(final @Nullable Throwable ex) {
        if (isEmpty()) {
            return false;
        }
        return future.completeExceptionally(ex);
    }
}
