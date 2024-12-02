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

import com.fasterxml.jackson.annotation.JsonInclude;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Simon L Johnson
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TagSample {

    private @Nullable String tagName;
    private @Nullable Object tagValue;

    public TagSample(final @Nullable String tagName, final @NotNull Object tagValue) {
        this.tagName = tagName;
        this.tagValue = tagValue;
    }

    public @Nullable String getTagName() {
        return tagName;
    }

    public void setTagName(final @Nullable String tagName) {
        this.tagName = tagName;
    }

    public @Nullable Object getTagValue() {
        return tagValue;
    }

    public void setTagValue(final @Nullable Object tagValue) {
        this.tagValue = tagValue;
    }
}
