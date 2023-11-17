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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;

/**
 * @author Simon L Johnson
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProtocolAdapterPublisherJsonPayload extends AbstractProtocolAdapterJsonPayload {

    @JsonProperty("value")
    private @NotNull Object value;

    @JsonProperty("tagName")
    private @Nullable String tagName;

    public ProtocolAdapterPublisherJsonPayload(final @Nullable Long timestamp, final @NotNull TagSample sample) {
        super(timestamp);
        this.value = sample.getTagValue();
        this.tagName = sample.getTagName();
    }

    @NotNull
    public Object getValue() {
        return value;
    }

    @Nullable
    public String getTagName() {
        return tagName;
    }


}
