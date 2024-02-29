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
package com.hivemq.metrics.ioc;

import com.codahale.metrics.MetricRegistry;
import com.hivemq.bootstrap.ioc.Injector;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.logging.EventLog;
import com.hivemq.metrics.HiveMQMetrics;
import com.hivemq.metrics.MetricsHolder;
import com.hivemq.metrics.MetricsShutdownHook;
import com.hivemq.metrics.gauges.OpenConnectionsGauge;
import com.hivemq.metrics.gauges.RetainedMessagesGauge;
import com.hivemq.metrics.gauges.SessionsGauge;
import com.hivemq.metrics.jmx.JmxReporterBootstrap;
import com.hivemq.persistence.local.ClientSessionLocalPersistence;
import com.hivemq.persistence.retained.RetainedMessagePersistence;
import dagger.BindsInstance;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoSet;
import io.netty.channel.group.ChannelGroup;

import javax.inject.Singleton;

@Module
public class MetricsModule {

    @Provides
    @Singleton
    static @NotNull SessionsGauge sessionsGauge(
            final @NotNull ClientSessionLocalPersistence clientSessionLocalPersistence,
            final @NotNull MetricRegistry metricRegistry) {
        final SessionsGauge sessionsGauge = new SessionsGauge(clientSessionLocalPersistence);
        metricRegistry.register(HiveMQMetrics.CLIENT_SESSIONS_CURRENT.name(), sessionsGauge);
        return sessionsGauge;
    }

    @Provides
    @Singleton
    static @NotNull MetricsHolder metricsHolder(final @NotNull MetricRegistry metricRegistry) {
        return new MetricsHolder(metricRegistry);
    }

    @Provides
    @Singleton
    static @NotNull OpenConnectionsGauge openConnectionsGauge(
            final @NotNull MetricRegistry metricRegistry, final @NotNull ChannelGroup allChannels) {
        final OpenConnectionsGauge connectionsGauge = new OpenConnectionsGauge(allChannels);
        metricRegistry.register(HiveMQMetrics.CONNECTIONS_OVERALL_CURRENT.name(), connectionsGauge);
        return connectionsGauge;
    }

    @Provides
    @Singleton
    static @NotNull RetainedMessagesGauge retainedMessagesGauge(
            final @NotNull RetainedMessagePersistence retainedMessagePersistence,
            final @NotNull MetricRegistry metricRegistry) {
        final RetainedMessagesGauge gauge = new RetainedMessagesGauge(retainedMessagePersistence);
        metricRegistry.register(HiveMQMetrics.RETAINED_MESSAGES_CURRENT.name(), gauge);
        return gauge;
    }

    @Provides
    @IntoSet
    Boolean eagerSingletons(
            final @NotNull MetricsHolder metricsHolder,
            final @NotNull SessionsGauge sessionsGauge,
            final @NotNull OpenConnectionsGauge openConnectionsGauge,
            final @NotNull RetainedMessagesGauge retainedMessagesGauge,
            final @NotNull JmxReporterBootstrap jmxReporterBootstrap,
            final @NotNull MetricsShutdownHook metricsShutdownHook) {
        // this is used to instantiate all the params, similar to guice's asEagerSingleton and returns nothing
        return Boolean.TRUE;
    }
}
