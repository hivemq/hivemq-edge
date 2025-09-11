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
package com.hivemq.edge.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.api.model.components.Extension;
import com.hivemq.api.model.components.Link;
import com.hivemq.api.model.components.Module;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class HiveMQEdgeRemoteConfiguration {

    private @JsonProperty("ctas")
    final @NotNull List<Link> ctas;
    private @JsonProperty("resources")
    final @NotNull List<Link> resources;
    private @JsonProperty("extensions")
    final @NotNull List<Extension> extensions;
    private @JsonProperty("modules")
    final @NotNull List<Module> modules;
    private @JsonProperty("properties")
    final @NotNull Map<String, Object> properties;
    private @JsonProperty("cloudLink")
    final @NotNull Link cloudLink;
    private @JsonProperty("gitHubLink")
    final @NotNull Link gitHubLink;
    private @JsonProperty("documentationLink")
    final @NotNull Link documentationLink;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public HiveMQEdgeRemoteConfiguration(
            @JsonProperty("ctas") final @NotNull List<Link> ctas,
            @JsonProperty("resources") final @NotNull List<Link> resources,
            @JsonProperty("extensions") final @NotNull List<Extension> extensions,
            @JsonProperty("modules") final @NotNull List<Module> modules,
            @JsonProperty("properties") final @NotNull Map<String, Object> properties,
            @JsonProperty("cloudLink") final @NotNull Link cloudLink,
            @JsonProperty("gitHubLink") final @NotNull Link gitHubLink,
            @JsonProperty("documentationLink") final @NotNull Link documentationLink) {
        this.ctas = ctas;
        this.resources = resources;
        this.extensions = extensions;
        this.modules = modules;
        this.properties = properties;
        this.cloudLink = cloudLink;
        this.gitHubLink = gitHubLink;
        this.documentationLink = documentationLink;
    }

    public @NotNull List<Link> getCtas() {
        return ctas;
    }

    public @NotNull List<Link> getResources() {
        return resources;
    }

    public @NotNull List<Extension> getExtensions() {
        return extensions;
    }

    public @NotNull List<Module> getModules() {
        return modules;
    }

    public @NotNull Link getCloudLink() {
        return cloudLink;
    }

    public @NotNull Link getGitHubLink() {
        return gitHubLink;
    }

    public @NotNull Link getDocumentationLink() {
        return documentationLink;
    }

    public @NotNull Map<String, Object> getProperties() {
        return properties;
    }

    @Override
    public @NotNull String toString() {
        return "HiveMQEdgeRemoteConfiguration{" +
                "ctas=" +
                ctas +
                ", resources=" +
                resources +
                ", extensions=" +
                extensions +
                ", modules=" +
                modules +
                ", properties=" +
                properties +
                ", cloudLink=" +
                cloudLink +
                ", documentationLink=" +
                documentationLink +
                ", gitHubLink=" +
                gitHubLink +
                '}';
    }
}
