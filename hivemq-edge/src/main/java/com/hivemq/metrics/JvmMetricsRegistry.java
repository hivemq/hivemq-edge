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

import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricSet;
import com.codahale.metrics.jvm.BufferPoolMetricSet;
import com.codahale.metrics.jvm.CachedThreadStatesGaugeSet;
import com.codahale.metrics.jvm.ClassLoadingGaugeSet;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.codahale.metrics.jvm.ThreadDeadlockDetector;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.lang.management.ManagementFactory;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Singleton
public class JvmMetricsRegistry {

    private static final Logger log = LoggerFactory.getLogger(JvmMetricsRegistry.class);

    public static final String JVM_METRIC_PREFIX = "com.hivemq.jvm.";

    private final @NotNull MetricRegistry metricRegistry;

    @Inject
    public JvmMetricsRegistry(
            final @NotNull MetricRegistry metricRegistry) {
        this.metricRegistry = metricRegistry;
    }

    //method injection for eager initialization
    @Inject
    public void postConstruct() {

        registerBufferPoolMetrics();
        registerThreadStateMetrics();
        registerClassLoaderMetrics();
        registerGarbageCollectorMetrics();
        registerMemoryMetrics();

        log.debug("Registered JVM metrics with prefix {}", JVM_METRIC_PREFIX);
    }

    private void registerMemoryMetrics() {
        try {

            registerAll("memory", new MemoryUsageGaugeSet(ManagementFactory.getMemoryMXBean(), ManagementFactory.getMemoryPoolMXBeans()));

        } catch (final Exception e) {
            log.warn("Not able to register JVM metrics for Memory usage, this is probably not supported by this JVM");
            if (log.isDebugEnabled()) {
                log.debug("original Exception", e);
            }
        }
    }

    private void registerGarbageCollectorMetrics() {
        try {

            registerAll("garbage-collector", new GarbageCollectorMetricSet(ManagementFactory.getGarbageCollectorMXBeans()));

        } catch (final Exception e) {
            log.warn("Not able to register JVM metrics for GarbageCollection, this is probably not supported by this JVM");
            if (log.isDebugEnabled()) {
                log.debug("original Exception", e);
            }
        }
    }

    private void registerClassLoaderMetrics() {
        try {

            registerAll("class-loader", new ClassLoadingGaugeSet(ManagementFactory.getClassLoadingMXBean()));

        } catch (final Exception e) {
            log.warn("Not able to register JVM metrics for ClassLoaders, this is probably not supported by this JVM");
            if (log.isDebugEnabled()) {
                log.debug("original Exception", e);
            }
        }
    }

    private void registerThreadStateMetrics() {
        try {

            registerAll("threads", new CachedThreadStatesGaugeSet(ManagementFactory.getThreadMXBean(),
                    new ThreadDeadlockDetector(ManagementFactory.getThreadMXBean()), 1, TimeUnit.SECONDS));

        } catch (final Exception e) {
            log.warn("Not able to register JVM metrics for Thread states, this is probably not supported by this JVM");
            if (log.isDebugEnabled()) {
                log.debug("original Exception", e);
            }
        }
    }

    private void registerBufferPoolMetrics() {
        try {

            registerAll("buffer-pool", new BufferPoolMetricSet(ManagementFactory.getPlatformMBeanServer()));

        } catch (final Exception e) {
            log.warn("Not able to register JVM metrics for BufferPools, this is probably not supported by this JVM");
            if (log.isDebugEnabled()) {
                log.debug("original Exception", e);
            }
        }
    }

    private void registerAll(final @NotNull String prefix, final @NotNull MetricSet metricSet) {
        for (final Map.Entry<String, Metric> entry : metricSet.getMetrics().entrySet()) {
            if (entry.getValue() instanceof MetricSet) {
                registerAll(prefix + "." + entry.getKey(), (MetricSet) entry.getValue());
            } else {
                register(prefix + "." + entry.getKey(), entry.getValue());
            }
        }
    }

    private void register(final @NotNull String prefix, final @NotNull Metric metric) {
        metricRegistry.register(JVM_METRIC_PREFIX + prefix, metric);
    }

}
