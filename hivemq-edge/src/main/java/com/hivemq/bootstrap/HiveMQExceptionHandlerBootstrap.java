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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Dominik Obermaier
 */
public class HiveMQExceptionHandlerBootstrap {

    private static final Logger log = LoggerFactory.getLogger(HiveMQExceptionHandlerBootstrap.class);

    /**
     * Adds an uncaught Exception Handler for UnrecoverableExceptions.
     * Logs the error and quits HiveMQ
     */
    public static void addUnrecoverableExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(final Thread t, final Throwable e) {

                handleUncaughtException(t, e);
            }
        });
    }

    @VisibleForTesting
    static void handleUncaughtException(final Thread t, final Throwable e) {
        if (e instanceof UnrecoverableException) {
            if (((UnrecoverableException) e).isShowException()) {
                log.error("An unrecoverable Exception occurred. Exiting HiveMQ", t, e);
            }
            System.exit(1);
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
