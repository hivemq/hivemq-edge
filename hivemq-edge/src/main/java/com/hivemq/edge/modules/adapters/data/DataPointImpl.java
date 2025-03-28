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

import com.hivemq.adapter.sdk.api.data.DataPoint;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class DataPointImpl implements DataPoint {
    private final @NotNull Object tagValue;
    private final @NotNull String tagName;
    private final @NotNull boolean treatAsJson;

    public DataPointImpl(final @NotNull String tagName, final @NotNull Object tagValue, final boolean treatAsJson) {
        this.tagName = tagName;
        this.tagValue = tagValue;
        this.treatAsJson = treatAsJson;
    }

    public DataPointImpl(final @NotNull String tagName, final @NotNull Object tagValue) {
        this(tagName, tagValue, false);
    }

    @Override
    public @NotNull Object getTagValue() {
        return tagValue;
    }

    @Override
    public @NotNull String getTagName() {
        return tagName;
    }

    @Override
    public @NotNull boolean treatTagValueAsJson() {
        return treatAsJson;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final DataPointImpl dataPoint = (DataPointImpl) o;
        return treatAsJson == dataPoint.treatAsJson &&
                Objects.equals(getTagValue(), dataPoint.getTagValue()) &&
                Objects.equals(getTagName(), dataPoint.getTagName());
    }

    @Override
    public String toString() {
        return "DataPointImpl{" +
                "tagValue=" +
                tagValue +
                ", tagName='" +
                tagName +
                '\'' +
                ", treatAsJson=" +
                treatAsJson +
                '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(getTagValue(), getTagName(), treatAsJson);
    }
}
