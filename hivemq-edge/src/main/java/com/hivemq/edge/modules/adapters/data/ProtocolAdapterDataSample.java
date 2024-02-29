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
package com.hivemq.edge.modules.adapters.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.base.Preconditions;
import com.hivemq.edge.modules.config.impl.AbstractProtocolAdapterConfig;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A protocol adapter sample, is a sampled measurement taken at a point in time. It can encapsulate more than one
 * tag and value pair, and will result in dataPointValues#size being published to the MQTT system.
 *
 * @author Simon L Johnson
 */
public class ProtocolAdapterDataSample<T extends AbstractProtocolAdapterConfig> {

    protected Long timestamp = System.currentTimeMillis();
    protected T.Subscription subscription;

    //-- Handle multiple tags in the same sample
    protected List<DataPoint> dataPoints = new CopyOnWriteArrayList<>();

    public ProtocolAdapterDataSample(final @NotNull T.Subscription subscription) {
        this.subscription = subscription;
    }

    @JsonIgnore
    public T.Subscription getSubscription() {
        return subscription;
    }

    @JsonIgnore
    public String getTopic() {
        return subscription.getDestination();
    }

    @JsonIgnore
    public int getQos() {
        return subscription.getQos();
    }

    @JsonIgnore
    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(final Long timestamp) {
        this.timestamp = timestamp;
    }

    public void addDataPoint(final @NotNull String tagName, final @NotNull Object tagValue){
        Preconditions.checkNotNull(tagName);
        Preconditions.checkNotNull(tagValue);
        dataPoints.add(new DataPoint(tagName,tagValue));
    }

    public List<DataPoint> getDataPoints(){
        return Collections.unmodifiableList(dataPoints);
    }

    public static class DataPoint {
        private final Object tagValue;
        private final String tagName;
        public DataPoint(final @NotNull String tagName, final @NotNull Object tagValue) {
            this.tagName = tagName;
            this.tagValue = tagValue;
        }

        public Object getTagValue() {
            return tagValue;
        }

        public String getTagName() {
            return tagName;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            final DataPoint dataPoint = (DataPoint) o;
            return Objects.equals(tagValue, dataPoint.tagValue) && Objects.equals(tagName, dataPoint.tagName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(tagValue, tagName);
        }
    }
}
