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
import com.hivemq.adapter.sdk.api.config.PublishingConfig;
import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.data.ProtocolAdapterDataSample;
import com.hivemq.adapter.sdk.api.factories.DataPointFactory;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author HiveMQ Adapter Generator
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HttpData implements ProtocolAdapterDataSample {

    private final String requestUrl;
    private final String contentType;
    private int httpStatusCode;
    private final @NotNull DataPointFactory dataPointFactory;
    protected @NotNull PublishingConfig publishingConfig;

    //-- Handle multiple tags in the same sample
    protected @NotNull List<DataPoint> dataPoints = new CopyOnWriteArrayList<>();
    private @NotNull Long timestamp = System.currentTimeMillis();

    public HttpData(
            final @NotNull PublishingConfig publishingConfig,
            final @NotNull String requestUrl,
            final int httpStatusCode,
            final @NotNull String contentType,
            final @NotNull DataPointFactory dataPointFactory) {
        this.publishingConfig = publishingConfig;
        this.requestUrl = requestUrl;
        this.contentType = contentType;
        this.httpStatusCode = httpStatusCode;
        this.dataPointFactory = dataPointFactory;
    }


    public String getRequestUrl() {
        return requestUrl;
    }

    public String getContentType() {
        return contentType;
    }

    public int getHttpStatusCode() {
        return httpStatusCode;
    }

    @Override
    @JsonIgnore
    public @NotNull PublishingConfig getSubscription() {
        return publishingConfig;
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
    public void setDataPoints(@NotNull List<DataPoint> list) {
        this.dataPoints = list;
    }

    @Override
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public @NotNull List<DataPoint> getDataPoints() {
        return dataPoints;
    }

}
