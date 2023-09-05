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
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Objects;

/**
 * Bean to transport module details across the API
 *
 * @author Simon L Johnson
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Module {

    @JsonProperty("id")
    @Schema(description = "A mandatory ID associated with the Module")
    private final @NotNull String id;

    @JsonProperty("version")
    @Schema(description = "The module version")
    private final @NotNull String version;

    @JsonProperty("name")
    @Schema(description = "The module name")
    private final @NotNull String name;

    @JsonProperty("description")
    @Schema(description = "The module description", nullable = true)
    private final @Nullable String description;

    @JsonProperty("author")
    @Schema(description = "The module author")
    private final @NotNull String author;

    @JsonProperty("priority")
    @Schema(description = "The module priority")
    private final @NotNull Integer priority;

    @JsonProperty("installed")
    @Schema(description = "Is the module installed", nullable = true)
    private final @Nullable Boolean installed;

    @JsonProperty("documentationLink")
    @Schema(description = "The module documentation link", nullable = true)
    private final @Nullable Link documentationLink;

    @JsonProperty("provisioningLink")
    @Schema(description = "The module provisioning link", nullable = true)
    private final @Nullable Link provisioningLink;

    @JsonProperty("logoUrl")
    @Schema(description = "The logo link", nullable = true)
    private final @Nullable Link logoUrl;

    @JsonProperty("moduleType")
    @Schema(description = "The type of the module", nullable = true)
    private final @Nullable String moduleType;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public Module(
            @NotNull @JsonProperty("id") final String id,
            @NotNull @JsonProperty("version") final String version,
            @NotNull @JsonProperty("name") final String name,
            @Nullable @JsonProperty("logoUrl") final Link logoUrl,
            @Nullable @JsonProperty("description") final String description,
            @NotNull @JsonProperty("author") final String author,
            @NotNull @JsonProperty("priority") final Integer priority,
            @NotNull @JsonProperty("installed") final Boolean installed,
            @Nullable @JsonProperty("moduleType") final String moduleType,
            @Nullable @JsonProperty("documentationLink") final Link documentationLink,
            @Nullable @JsonProperty("provisioningLink") final Link provisioningLink) {
        this.id = id;
        this.version = version;
        this.name = name;
        this.logoUrl = logoUrl;
        this.description = description;
        this.author = author;
        this.priority = priority;
        this.installed = installed;
        this.moduleType = moduleType;
        this.documentationLink = documentationLink;
        this.provisioningLink = provisioningLink;
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

    public Link getDocumentationLink() {
        return documentationLink;
    }

    public Link getProvisioningLink() {
        return provisioningLink;
    }

    public Link getLogoUrl() {
        return logoUrl;
    }

    public String getModuleType() {
        return moduleType;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Module extension = (Module) o;
        return Objects.equals(id, extension.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Module{");
        sb.append("id='").append(id).append('\'');
        sb.append(", version='").append(version).append('\'');
        sb.append(", name='").append(name).append('\'');
        sb.append(", description='").append(description).append('\'');
        sb.append(", author='").append(author).append('\'');
        sb.append(", priority=").append(priority);
        sb.append(", installed=").append(installed);
        sb.append(", documentationLink=").append(documentationLink);
        sb.append(", provisioningLink=").append(provisioningLink);
        sb.append('}');
        return sb.toString();
    }
}
