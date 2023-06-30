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
 * Bean to transport extension details across the API
 *
 * <hivemq-extension>
 *     <id>hivemq-allow-all-extension</id>
 *     <version>1.0.0</version>
 *     <name>Allow All Extension</name>
 *     <author>HiveMQ</author>
 *     <priority>0</priority>
 * </hivemq-extension>
 *
 * @author Simon L Johnson
 */
public class Extension {

    @JsonProperty("id")
    @Schema(description = "A mandatory ID associated with the Extension")
    private final @NotNull String id;

    @JsonProperty("version")
    @Schema(description = "The extension version")
    private final @NotNull String version;

    @JsonProperty("name")
    @Schema(description = "The extension name")
    private final @NotNull String name;

    @JsonProperty("description")
    @Schema(description = "The extension description", nullable = true)
    private final @Nullable String description;

    @JsonProperty("author")
    @Schema(description = "The extension author")
    private final @NotNull String author;

    @JsonProperty("priority")
    @Schema(description = "The extension priority")
    private final @NotNull Integer priority;

    @JsonProperty("installed")
    @Schema(description = "Is the extension installed", nullable = true)
    private final @Nullable Boolean installed;

    @JsonProperty("link")
    @Schema(description = "The extension link", nullable = true)
    private final @Nullable Link link;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public Extension(
            @NotNull @JsonProperty("id") final String id,
            @NotNull @JsonProperty("version") final String version,
            @NotNull @JsonProperty("name") final String name,
            @Nullable @JsonProperty("description") final String description,
            @NotNull @JsonProperty("author") final String author,
            @NotNull @JsonProperty("priority") final Integer priority,
            @NotNull @JsonProperty("installed") final Boolean installed,
            @Nullable @JsonProperty("link") final Link link) {
        this.id = id;
        this.version = version;
        this.name = name;
        this.description = description;
        this.author = author;
        this.priority = priority;
        this.installed = installed;
        this.link = link;
    }

    public String getId() {
        return id;
    }

    public String getVersion() {
        return version;
    }

    public String getName() {
        return name;
    }

    public String getAuthor() {
        return author;
    }

    public Integer getPriority() {
        return priority;
    }

    public String getDescription() {
        return description;
    }

    public Boolean getInstalled() {
        return installed;
    }

    public Link getLink() {
        return link;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Extension extension = (Extension) o;
        return Objects.equals(id, extension.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Extension{");
        sb.append("id='").append(id).append('\'');
        sb.append(", version='").append(version).append('\'');
        sb.append(", name='").append(name).append('\'');
        sb.append(", description='").append(description).append('\'');
        sb.append(", author='").append(author).append('\'');
        sb.append(", priority=").append(priority);
        sb.append(", installed=").append(installed);
        sb.append(", link=").append(link);
        sb.append('}');
        return sb.toString();
    }
}
