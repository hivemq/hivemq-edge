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
package com.hivemq.edge.adapters.file.payload;

import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.edge.adapters.file.tag.FileTag;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class FileDataPoint implements DataPoint {

    private final @NotNull Object tagValue;
    private final @NotNull FileTag tag;

    public FileDataPoint(final @NotNull FileTag tag, final @NotNull Object tagValue) {
        this.tag = tag;
        this.tagValue = tagValue;
    }

    @Override
    public @NotNull Object getTagValue() {
        return tagValue;
    }

    @Override
    public @NotNull String getTagName() {
        return tag.name();
    }

    public @NotNull FileTag getTag() {
        return tag;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final FileDataPoint that = (FileDataPoint) o;
        return Objects.equals(getTagValue(), that.getTagValue()) && Objects.equals(getTag(), that.getTag());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getTagValue(), getTag());
    }
}
