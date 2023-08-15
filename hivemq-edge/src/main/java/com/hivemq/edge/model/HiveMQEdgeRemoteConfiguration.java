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
import com.hivemq.api.model.components.Module;
import com.hivemq.api.model.components.Link;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.util.List;
import java.util.Map;

/**
 * @author Simon L Johnson
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class HiveMQEdgeRemoteConfiguration {

    private @JsonProperty("ctas") @NotNull final List<Link> ctas;
    private @JsonProperty("resources") @NotNull final List<Link> resources;
    private @JsonProperty("extensions") @NotNull final List<Extension> extensions;
    private @JsonProperty("modules") @NotNull final List<Module> modules;
    private @JsonProperty("properties") @NotNull final Map<String, Object> properties;
    private @JsonProperty("cloudLink") @NotNull final Link cloudLink;
    private @JsonProperty("gitHubLink") @NotNull final Link gitHubLink;
    private @JsonProperty("documentationLink") @NotNull final Link documentationLink;

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

    public List<Link> getCtas() {
        return ctas;
    }

    public List<Link> getResources() {
        return resources;
    }

    public List<Extension> getExtensions() {
        return extensions;
    }

    public List<Module> getModules() {
        return modules;
    }

    public Link getCloudLink() {
        return cloudLink;
    }

    public Link getGitHubLink() {
        return gitHubLink;
    }

    public Link getDocumentationLink() {
        return documentationLink;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("HiveMQEdgeRemoteConfiguration{");
        sb.append("ctas=").append(ctas);
        sb.append(", resources=").append(resources);
        sb.append(", extensions=").append(extensions);
        sb.append(", modules=").append(modules);
        sb.append(", properties=").append(properties);
        sb.append(", cloudLink=").append(cloudLink);
        sb.append(", documentationLink=").append(documentationLink);
        sb.append(", gitHubLink=").append(gitHubLink);
        sb.append('}');
        return sb.toString();
    }
}
