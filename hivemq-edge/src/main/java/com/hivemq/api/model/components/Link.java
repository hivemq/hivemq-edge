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
package com.hivemq.api.model.components;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Objects;

/**
 * Bean to transport Link details across the API
 * @author Simon L Johnson
 */
public class Link {

    @JsonProperty("url")
    @Schema(description = "A mandatory URL associated with the Link", required = true)
    private final @NotNull String url;

    @JsonProperty("displayText")
    @Schema(description = "The link display text", nullable = true)
    private final @NotNull String displayText;

    @JsonProperty("description")
    @Schema(description = "The optional link display description", nullable = true)
    private final @Nullable String description;

    @JsonProperty("target")
    @Schema(description = "An optional target associated with the Link", nullable = true)
    private final @Nullable String target;

    @JsonProperty("imageUrl")
    @Schema(description = "An optional imageUrl associated with the Link", nullable = true)
    private final @Nullable String imageUrl;

    @JsonProperty("external")
    @Schema(description = "A mandatory Boolean indicating if the link is internal to the context or an external webLink")
    private final @NotNull Boolean external;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public Link(
            @Nullable @JsonProperty("displayText") final String displayText,
            @NotNull @JsonProperty("url") final String url,
            @Nullable @JsonProperty("description") final String description,
            @Nullable @JsonProperty("target") final String target,
            @Nullable @JsonProperty("imageUrl") final String imageUrl,
            @Nullable @JsonProperty("external") final Boolean external) {
        this.displayText = displayText;
        this.url = url;
        this.description = description;
        this.target = target;
        this.imageUrl = imageUrl;
        this.external = external;
    }

    public String getDisplayText() {
        return displayText;
    }

    public String getUrl() {
        return url;
    }

    public String getDescription() {
        return description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getTarget() {
        return target;
    }

    public Boolean getExternal() {
        return external;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Link{");
        sb.append("url='").append(url).append('\'');
        sb.append(", displayText='").append(displayText).append('\'');
        sb.append(", description='").append(description).append('\'');
        sb.append(", target='").append(target).append('\'');
        sb.append(", imageUrl='").append(imageUrl).append('\'');
        sb.append(", external=").append(external);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final Link link = (Link) o;
        return url.equals(link.url) &&
                Objects.equals(displayText, link.displayText) &&
                Objects.equals(description, link.description) &&
                Objects.equals(target, link.target) &&
                Objects.equals(imageUrl, link.imageUrl) &&
                Objects.equals(external, link.external);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, displayText, description, target, imageUrl, external);
    }
}
