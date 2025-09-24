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
package com.hivemq.bootstrap;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Throwables;
import com.hivemq.exceptions.UnrecoverableException;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Dominik Obermaier
 */
public class HiveMQExceptionHandlerBootstrap {

    private static final Logger log = LoggerFactory.getLogger(HiveMQExceptionHandlerBootstrap.class);

    private static final AtomicReference<Runnable> terminator = new AtomicReference<>(() -> System.exit(1));

    /**
     * Adds an uncaught Exception Handler for UnrecoverableExceptions.
     * Logs the error and quits HiveMQ
     */
    public static void addUnrecoverableExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler(HiveMQExceptionHandlerBootstrap::handleUncaughtException);
    }

    public static void setTerminator(@NotNull final Runnable runnable) {
        terminator.set(runnable);
    }

    @VisibleForTesting
    static void handleUncaughtException(final Thread t, final Throwable e) {
        if (e instanceof UnrecoverableException) {
            if (((UnrecoverableException) e).isShowException()) {
                log.error("An unrecoverable Exception occurred. Exiting HiveMQ", t, e);
            }
            terminator.get().run();
        }
        final Throwable rootCause = Throwables.getRootCause(e);
        if (rootCause instanceof UnrecoverableException) {
            final boolean showException = ((UnrecoverableException) rootCause).isShowException();
            if (showException) {
                log.error("Cause: ", e);
            }
        } else {
            log.error("Uncaught Error:", e);
        }
    }


}
