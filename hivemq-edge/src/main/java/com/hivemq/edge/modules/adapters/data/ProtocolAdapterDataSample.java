package com.hivemq.edge.modules.adapters.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hivemq.extension.sdk.api.annotations.NotNull;

/**
 * @author Simon L Johnson
 */
public class ProtocolAdapterDataSample {

    protected long timestamp = 0;
    protected Object data;
    protected String topic;
    protected int qos;

    public ProtocolAdapterDataSample(final @NotNull Object data, final @NotNull String topic, final int qos) {
        this.data = data;
        this.topic = topic;
        this.qos = qos;
    }

    public Object getData() {
        return data;
    }

    public void setData(final Object data) {
        this.data = data;
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
}
