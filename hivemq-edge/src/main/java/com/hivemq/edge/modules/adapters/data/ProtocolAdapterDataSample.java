package com.hivemq.edge.modules.adapters.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.base.Preconditions;
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
public class ProtocolAdapterDataSample {

    protected long timestamp = 0;
    protected String topic;
    protected int qos;

    //-- Handle multiple tags in the same sample
    protected List<DataPoint> dataPointValues = new CopyOnWriteArrayList<>();

    public ProtocolAdapterDataSample(final @NotNull String topic, final int qos) {
        this.topic = topic;
        this.qos = qos;
    }

    @JsonIgnore
    public String getTopic() {
        return topic;
    }

    public void setTopic(final String topic) {
        this.topic = topic;
    }

    @JsonIgnore
    public int getQos() {
        return qos;
    }

    public void setQos(final int qos) {
        this.qos = qos;
    }

    @JsonIgnore
    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(final long timestamp) {
        this.timestamp = timestamp;
    }

    public void addDataPoint(final @NotNull String tagName, final @NotNull Object data){
        Preconditions.checkNotNull(tagName);
        Preconditions.checkNotNull(data);
        dataPointValues.add(new DataPoint(tagName,data));
    }

    public List<DataPoint> getDataPoints(){
        return Collections.unmodifiableList(dataPointValues);
    }

    @JsonIgnore
    public DataPoint getLastDataPoint(){
        return dataPointValues.isEmpty() ? null : dataPointValues.get(dataPointValues.size() - 1);
    }

    public static class DataPoint {
        private final Object data;
        private final String tagName;
        public DataPoint(final @NotNull String tagName, final @NotNull Object data) {
            this.tagName = tagName;
            this.data = data;
        }

        public Object getData() {
            return data;
        }

        public String getTagName() {
            return tagName;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            final DataPoint dataPoint = (DataPoint) o;
            return Objects.equals(data, dataPoint.data) && Objects.equals(tagName, dataPoint.tagName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(data, tagName);
        }
    }
}
