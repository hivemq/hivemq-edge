/*
 * Copyright 2023-present HiveMQ GmbH
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
package com.hivemq.edge.adapters.http.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.hivemq.adapter.sdk.api.config.PollingContext;
import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.data.ProtocolAdapterDataSample;
import com.hivemq.adapter.sdk.api.factories.DataPointFactory;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author HiveMQ Adapter Generator
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HttpData implements ProtocolAdapterDataSample {

    private final @NotNull String requestUrl;
    private final @NotNull String contentType;
    private final int httpStatusCode;
    private final @NotNull DataPointFactory dataPointFactory;
    private final @NotNull PollingContext pollingContext;

    //-- Handle multiple tags in the same sample
    protected @NotNull List<DataPoint> dataPoints = new CopyOnWriteArrayList<>();
    private @NotNull Long timestamp = System.currentTimeMillis();

    public HttpData(
            final @NotNull PollingContext pollingContext,
            final @NotNull String requestUrl,
            final int httpStatusCode,
            final @NotNull String contentType,
            final @NotNull DataPointFactory dataPointFactory) {
        this.pollingContext = pollingContext;
        this.requestUrl = requestUrl;
        this.contentType = contentType;
        this.httpStatusCode = httpStatusCode;
        this.dataPointFactory = dataPointFactory;
    }


    public @NotNull String getRequestUrl() {
        return requestUrl;
    }

    public @NotNull String getContentType() {
        return contentType;
    }

    public boolean isSuccessStatusCode() {
        return httpStatusCode >= 200 && httpStatusCode <= 299;
    }

    @Override
    @JsonIgnore
    public @NotNull PollingContext getPollingContext() {
        return pollingContext;
    }

    @Override
    @JsonIgnore
    public @NotNull Long getTimestamp() {
        return timestamp;
    }

    @Override
    public void addDataPoint(final @NotNull String tagName, final @NotNull Object tagValue) {
        dataPoints.add(dataPointFactory.create(tagName, tagValue));
    }

    @Override
    public void addDataPoint(final @NotNull DataPoint dataPoint) {
        dataPoints.add(dataPoint);
    }

    @Override
    public void setDataPoints(final @NotNull List<DataPoint> list) {
        this.dataPoints = list;
    }

    @Override
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public @NotNull List<DataPoint> getDataPoints() {
        return dataPoints;
    }

}
