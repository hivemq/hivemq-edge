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
package com.hivemq.configuration.entity.bridge;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementRef;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
@XmlRootElement(name = "forwarded-topic")
@XmlAccessorType(XmlAccessType.NONE)
public class ForwardedTopicEntity {

    @XmlElementWrapper(name = "filters")
    @XmlElement(name = "mqtt-topic-filter")
    private @NotNull List<String> filters = new ArrayList<>();

    @XmlElement(name = "max-qos", required = true)
    private int maxQoS;

    @XmlElement(name = "preserve-retain", defaultValue = "false")
    private boolean preserveRetain;

    @XmlElement(name = "destination")
    private @Nullable String destination;

    @XmlElementWrapper(name = "custom-user-properties")
    @XmlElementRef(required = false)
    private @NotNull List<CustomUserPropertyEntity> customUserProperties = new ArrayList<>();

    @XmlElementWrapper(name = "excludes")
    @XmlElement(name = "mqtt-topic-filter")
    private @NotNull List<String> excludes = new ArrayList<>();

    @XmlElement(name = "queue-limit")
    private @Nullable Long queueLimit;

    public ForwardedTopicEntity() {
    }

    public @NotNull List<String> getFilters() {
        return filters;
    }

    public int getMaxQoS() {
        return maxQoS;
    }

    public boolean isPreserveRetain() {
        return preserveRetain;
    }

    public @Nullable String getDestination() {
        return destination;
    }

    public @NotNull List<CustomUserPropertyEntity> getCustomUserProperties() {
        return customUserProperties;
    }

    public @NotNull List<String> getExcludes() {
        return excludes;
    }

    public void setFilters(final List<String> filters) {
        this.filters = filters;
    }

    public void setMaxQoS(final int maxQoS) {
        this.maxQoS = maxQoS;
    }

    public void setPreserveRetain(final boolean preserveRetain) {
        this.preserveRetain = preserveRetain;
    }

    public void setDestination(final String destination) {
        this.destination = destination;
    }

    public void setCustomUserProperties(final List<CustomUserPropertyEntity> customUserProperties) {
        this.customUserProperties = customUserProperties;
    }

    public void setExcludes(final List<String> excludes) {
        this.excludes = excludes;
    }

    public @Nullable Long getQueueLimit() {
        return queueLimit;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final ForwardedTopicEntity that = (ForwardedTopicEntity) o;
        return getMaxQoS() == that.getMaxQoS() &&
                isPreserveRetain() == that.isPreserveRetain() &&
                Objects.equals(getFilters(), that.getFilters()) &&
                Objects.equals(getDestination(), that.getDestination()) &&
                Objects.equals(getCustomUserProperties(), that.getCustomUserProperties()) &&
                Objects.equals(getExcludes(), that.getExcludes()) &&
                Objects.equals(getQueueLimit(), that.getQueueLimit());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getFilters(),
                getMaxQoS(),
                isPreserveRetain(),
                getDestination(),
                getCustomUserProperties(),
                getExcludes(),
                getQueueLimit());
    }
}
