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
package com.hivemq.bootstrap.factories;

import com.codahale.metrics.MetricRegistry;
import com.hivemq.combining.mapping.DataCombiningTransformationService;
import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.mqtt.services.PrePublishProcessorService;
import org.jetbrains.annotations.NotNull;

public interface DataCombiningTransformationServiceFactory {

    /**
     * @deprecated implement {@link #build(PrePublishProcessorService, MetricRegistry, ShutdownHooks)} instead,
     *         so the service can release whatever it holds when Edge shuts down. Retained because modules
     *         compiled against the older interface implement only this method.
     */
    @Deprecated
    @NotNull
    DataCombiningTransformationService build(
            final @NotNull PrePublishProcessorService prePublishProcessorService,
            final @NotNull MetricRegistry metricRegistry);

    /**
     * Builds the service and gives the module a chance to register its own cleanup.
     * <p>
     * A module that holds resources -- the Data Hub implementation owns a script runtime whose JavaScript
     * engine pools each run a guard daemon thread -- registers a shutdown hook here. Leaving that to the
     * module keeps <i>what</i> to release opaque to Edge: the core only learns that something wants to run at
     * shutdown, never what it does.
     * <p>
     * The default delegates to the two-argument overload and registers nothing, so a module compiled against
     * the older interface keeps working unchanged: default methods are resolved against the interface at
     * runtime, so such a module inherits this body without having been compiled with it. Threads it leaks are
     * invisible in production (the process exits at shutdown) and only accumulate in a JVM that starts and
     * stops an embedded Edge repeatedly.
     */
    default @NotNull DataCombiningTransformationService build(
            final @NotNull PrePublishProcessorService prePublishProcessorService,
            final @NotNull MetricRegistry metricRegistry,
            final @NotNull ShutdownHooks shutdownHooks) {
        return build(prePublishProcessorService, metricRegistry);
    }
}
