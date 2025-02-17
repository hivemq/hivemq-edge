package com.hivemq.configuration.entity.combining;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import java.util.List;


public class DataCombiningSourcesEntity {

    @JsonProperty("tags")
    @XmlElementWrapper(name = "tags")
    @XmlElement(name = "tag")
    private final @NotNull List<String> tags;

    @JsonProperty("topicFilters")
    @XmlElementWrapper(name = "topicFilters")
    @XmlElement(name = "topicFilter")
    private final @NotNull List<String> topicFilters;

    // no-arg for jaxb
    public DataCombiningSourcesEntity() {
        tags = List.of();
        topicFilters = List.of();
    }

    public DataCombiningSourcesEntity(@NotNull final List<String> tags, @NotNull final List<String> topicFilters) {
        this.tags = tags;
        this.topicFilters = topicFilters;
    }

    @Override
    public String toString() {
        return "DataCombiningSourcesEntity{" + "tags=" + tags + ", topicFilters=" + topicFilters + '}';
    }

    @Override
    public boolean equals(final @Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final DataCombiningSourcesEntity that = (DataCombiningSourcesEntity) o;
        return tags.equals(that.tags) && topicFilters.equals(that.topicFilters);
    }

    @Override
    public int hashCode() {
        int result = tags.hashCode();
        result = 31 * result + topicFilters.hashCode();
        return result;
    }

    public @NotNull List<String> getTags() {
        return tags;
    }

    public @NotNull List<String> getTopicFilters() {
        return topicFilters;
    }
}

