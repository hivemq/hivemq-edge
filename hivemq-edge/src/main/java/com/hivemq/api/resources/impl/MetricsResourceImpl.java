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
package com.hivemq.api.resources.impl;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableList;
import com.hivemq.api.AbstractApi;
import com.hivemq.api.model.ApiErrorMessages;
import com.hivemq.api.model.metrics.DataPoint;
import com.hivemq.api.model.metrics.Metric;
import com.hivemq.api.model.metrics.MetricList;
import com.hivemq.api.resources.MetricsApi;
import com.hivemq.api.utils.ApiErrorUtils;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.Response;
import java.util.Iterator;
import java.util.SortedMap;

/**
 * @author Simon L Johnson
 */
@Singleton
public class MetricsResourceImpl extends AbstractApi implements MetricsApi {

    private final @NotNull MetricRegistry metricsRegistry;

    @Inject
    public MetricsResourceImpl(final @NotNull MetricRegistry metricsRegistry) {
        this.metricsRegistry = metricsRegistry;
    }

    @Override
    public Response getMetrics() {
        logger.trace("Metrics API obtaining metrics listing");
        ImmutableList.Builder<Metric> builder = new ImmutableList.Builder<>();
        Iterator<String> itr = metricsRegistry.getMetrics().keySet().iterator();
        while (itr.hasNext()){
            builder.add(new Metric(itr.next()));
        }
        return Response.status(200).entity(new MetricList(builder.build())).build();
    }

    @Override
    public Response getSample(final String metricName) {
        ApiErrorMessages messages = ApiErrorUtils.createErrorContainer();
        ApiErrorUtils.validateRequiredField(messages, "metricName", metricName, false);
        if(ApiErrorUtils.hasRequestErrors(messages)){
            return ApiErrorUtils.badRequest(messages);
        } else {
            logger.trace("Metrics API obtaining latest sample for {} at {}", metricName, System.currentTimeMillis());
            SortedMap<String, Counter> metrics = metricsRegistry.getCounters(MetricFilter.contains(metricName));
            Counter counter = metrics.get(metricName);
            if(counter != null){
                DataPoint dataPoint = new DataPoint(System.currentTimeMillis(), counter.getCount());
                return Response.status(200).entity(dataPoint).build();
            } else {
                DataPoint dataPoint = new DataPoint(System.currentTimeMillis(), 0L);
                return Response.status(200).entity(dataPoint).build();
//                return Response.status(Response.Status.NOT_FOUND).build();
            }
        }
    }
}
