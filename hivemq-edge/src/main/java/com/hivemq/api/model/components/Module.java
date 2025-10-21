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
import io.swagger.v3.oas.annotations.media.Schema;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Bean to transport module details across the API
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

    public @NotNull String getId() {
        return id;
    }

    public @NotNull String getVersion() {
        return version;
    }

    public @NotNull String getName() {
        return name;
    }

    public @NotNull String getAuthor() {
        return author;
    }

    public @NotNull Integer getPriority() {
        return priority;
    }

    public @Nullable String getDescription() {
        return description;
    }

    public @Nullable Boolean getInstalled() {
        return installed;
    }

    public @Nullable Link getDocumentationLink() {
        return documentationLink;
    }

    public @Nullable Link getProvisioningLink() {
        return provisioningLink;
    }

    public @Nullable Link getLogoUrl() {
        return logoUrl;
    }

    public @Nullable String getModuleType() {
        return moduleType;
    }

    @Override
    public boolean equals(final @Nullable Object o) {
        return this == o || (o instanceof final Module that && Objects.equals(id, that.id));
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public @NotNull String toString() {
        return "Module{" +
                "id='" +
                id +
                '\'' +
                ", version='" +
                version +
                '\'' +
                ", name='" +
                name +
                '\'' +
                ", description='" +
                description +
                '\'' +
                ", author='" +
                author +
                '\'' +
                ", priority=" +
                priority +
                ", installed=" +
                installed +
                ", documentationLink=" +
                documentationLink +
                ", provisioningLink=" +
                provisioningLink +
                '}';
    }
}
