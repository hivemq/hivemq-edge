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
package com.hivemq.common.executors.ioc;

import dagger.Module;
import dagger.Provides;
import org.apache.commons.lang3.ThreadUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import javax.swing.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

/**
 * @author Simon L Johnson
 */
@Module
public abstract class ExecutorsModule {

    static final String GROUP_NAME = "hivemq-edge-group";
    static final String SCHEDULED_WORKER_GROUP_NAME = "hivemq-edge-scheduled-group";
    static final String CACHED_WORKER_GROUP_NAME = "hivemq-edge-cached-group";
    private static final ThreadGroup coreGroup = new ThreadGroup(GROUP_NAME);
    @Provides
    @Singleton
    static ScheduledExecutorService scheduledExecutor() {
        return Executors.newScheduledThreadPool(4,
                new HiveMQEdgeThreadFactory(SCHEDULED_WORKER_GROUP_NAME));
    }

    @Provides
    @Singleton
    static ExecutorService executorService() {
        return Executors.newCachedThreadPool(new HiveMQEdgeThreadFactory(CACHED_WORKER_GROUP_NAME));
    }

    static class HiveMQEdgeThreadFactory implements ThreadFactory {
        private final String factoryName;
        private final ThreadGroup group;
        private volatile int counter = 0;

        public HiveMQEdgeThreadFactory(final String factoryName) {
            this.factoryName = factoryName;
            this.group = new ThreadGroup(coreGroup,  factoryName);
        }

        @Override
        public Thread newThread(final Runnable r) {
            synchronized (group) {
                Thread thread = new Thread(coreGroup, r, String.format(factoryName + "-%d", counter++));
                return thread;
            }
        }
    }
}
