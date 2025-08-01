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
package com.hivemq.metrics;

import com.hivemq.common.shutdown.HiveMQShutdownHook;
import com.hivemq.common.shutdown.ShutdownHooks;
import org.jetbrains.annotations.NotNull;
import com.hivemq.metrics.jmx.JmxReporterBootstrap;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * @author Lukas Brandl
 */
@Singleton
public class MetricsShutdownHook implements HiveMQShutdownHook {

    private final @NotNull ShutdownHooks shutdownHooks;
    private final @NotNull JmxReporterBootstrap jmxReporterBootstrap;

    @Inject
    public MetricsShutdownHook(
            final @NotNull ShutdownHooks shutdownHooks, final @NotNull JmxReporterBootstrap jmxReporterBootstrap) {
        this.shutdownHooks = shutdownHooks;
        this.jmxReporterBootstrap = jmxReporterBootstrap;
    }

    @Inject //method injection, this gets called once after instantiation
    public void postConstruct() {
        shutdownHooks.add(this);
    }

    @Override
    public void run() {
        jmxReporterBootstrap.stop();
    }

    @Override
    public @NotNull String name() {
        return "Metrics Shutdown";
    }
}
