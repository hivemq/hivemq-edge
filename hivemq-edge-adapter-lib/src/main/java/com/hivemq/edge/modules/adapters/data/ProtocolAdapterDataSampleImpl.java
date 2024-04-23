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
import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.base.Preconditions;
import com.hivemq.edge.modules.config.AdapterSubscription;
import com.hivemq.edge.modules.config.UserProperty;
import com.hivemq.edge.modules.config.impl.AbstractProtocolAdapterConfig;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A protocol adapter sample, is a sampled measurement taken at a point in time. It can encapsulate more than one
 * tag and value pair, and will result in dataPointValues#size being published to the MQTT system.
 *
 * @author Simon L Johnson
 */
public class ProtocolAdapterDataSampleImpl<T extends AbstractProtocolAdapterConfig>
        implements ProtocolAdapterDataSample<T> {

    protected Long timestamp = System.currentTimeMillis();
    protected AdapterSubscription adapterSubscription;

    //-- Handle multiple tags in the same sample
    protected List<DataPoint> dataPoints = new CopyOnWriteArrayList<>();

    public ProtocolAdapterDataSampleImpl(final @NotNull AdapterSubscription adapterSubscription) {
        this.adapterSubscription = adapterSubscription;
    }

    @Override
    @JsonIgnore
    public AdapterSubscription getSubscription() {
        return adapterSubscription;
    }

    @Override
    @JsonIgnore
    public String getTopic() {
        return adapterSubscription.getDestination();
    }

    @Override
    @JsonIgnore
    public int getQos() {
        return adapterSubscription.getQos();
    }

    @Override
    @JsonIgnore
    public Long getTimestamp() {
        return timestamp;
    }

    @Override
    @JsonIgnore
    public List<UserProperty> getUserProperties(){
        return adapterSubscription.getUserProperties();
    }

    @Override
    public void setTimestamp(final Long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public void addDataPoint(final @NotNull String tagName, final @NotNull Object tagValue){
        Preconditions.checkNotNull(tagName);
        Preconditions.checkNotNull(tagValue);
        dataPoints.add(new DataPointImpl(tagName,tagValue));
    }

    @Override
    public void setDataPoints(List<DataPoint> list){
        this.dataPoints = list;
    }

    @Override
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public List<DataPoint> getDataPoints(){
        return dataPoints;
    }

}
