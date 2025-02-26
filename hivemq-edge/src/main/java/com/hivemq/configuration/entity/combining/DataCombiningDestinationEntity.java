package com.hivemq.configuration.entity.combining;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.bind.annotation.XmlElement;

// can not because of JaxB
@SuppressWarnings("ClassCanBeRecord")
public class DataCombiningDestinationEntity {

    @JsonProperty("topic")
    @XmlElement(name = "topic")
    private final @NotNull String topic;

    @JsonProperty("schema")
    @XmlElement(name = "schema")
    private final @NotNull String schema;

    public DataCombiningDestinationEntity() {
        this.topic = "";
        this.schema ="";
    }

    public DataCombiningDestinationEntity(@NotNull final String schema, @NotNull final String topic) {
        this.schema = schema;
        this.topic = topic;
    }

    public @NotNull String getSchema() {
        return schema;
    }

    public @NotNull String getTopic() {
        return topic;
    }

    @Override
    public boolean equals(final @Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final DataCombiningDestinationEntity that = (DataCombiningDestinationEntity) o;
        return topic.equals(that.topic) && schema.equals(that.schema);
    }

    @Override
    public int hashCode() {
        int result = topic.hashCode();
        result = 31 * result + schema.hashCode();
        return result;
    }
}
