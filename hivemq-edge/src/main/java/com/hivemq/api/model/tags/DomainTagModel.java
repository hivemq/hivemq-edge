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
package com.hivemq.api.model.tags;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.persistence.domain.DomainTag;
import io.swagger.v3.oas.annotations.media.Schema;

public class DomainTagModel {

    @JsonProperty("tagAddress")
    @Schema(description = "The address for the data point on the device")
    private final @NotNull TagAddress tagAddress;

    @JsonProperty("tag")
    @Schema(description = "The tag that ")
    private final @NotNull String tag;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public DomainTagModel(
            @JsonProperty("tagAddress") final @NotNull TagAddress tagAddress,
            @JsonProperty("tag") final @NotNull String tag) {
        this.tagAddress = tagAddress;
        this.tag = tag;
    }

    public @NotNull String getTag() {
        return tag;
    }

    public @NotNull TagAddress getTagAddress() {
        return tagAddress;
    }


    public static @NotNull DomainTagModel fromDomainTag(final @NotNull DomainTag domainTag) {
        return new DomainTagModel(new TagAddress(domainTag.getTagAddress()), domainTag.getTag());
    }
}
