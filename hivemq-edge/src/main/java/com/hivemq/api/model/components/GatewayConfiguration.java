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
import com.hivemq.api.model.capabilities.CapabilityList;
import com.hivemq.api.model.firstuse.FirstUseInformation;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * @author Simon L Johnson
 */
public class GatewayConfiguration {

    @JsonProperty("environment")
    @Schema(description = "A map of properties relating to the installation", nullable = true)
    private EnvironmentProperties environment;

    @JsonProperty("cloudLink")
    @Schema(description = "A referral link to HiveMQ Cloud")
    private Link cloudLink;

    @JsonProperty("gitHubLink")
    @Schema(description = "A link to the GitHub Project")
    private Link gitHubLink;

    @JsonProperty("documentationLink")
    @Schema(description = "A link to the documentation Project")
    private Link documentationLink;

    @JsonProperty("firstUseInformation")
    @Schema(description = "Information relating to the firstuse experience")
    private FirstUseInformation firstUseInformation;

    @JsonProperty("ctas")
    @Schema(description = "The calls main to action")
    private LinkList ctas;

    @JsonProperty("resources")
    @Schema(description = "A list of resources to render")
    private LinkList resources;

    @JsonProperty("modules")
    @Schema(description = "The modules available for installation")
    private ModuleList modules;

    @JsonProperty("extensions")
    @Schema(description = "The extensions available for installation")
    private ExtensionList extensions;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public GatewayConfiguration(
            @JsonProperty("environment") final @NotNull EnvironmentProperties environment,
            @JsonProperty("cloudLink") final @NotNull Link cloudLink,
            @JsonProperty("gitHubLink") final @NotNull Link gitHubLink,
            @JsonProperty("documentationLink") final @NotNull Link documentationLink,
            @JsonProperty("firstUseInformation") final @NotNull FirstUseInformation firstUseInformation,
            @JsonProperty("ctas") final @NotNull LinkList ctas,
            @JsonProperty("resources") final @NotNull LinkList resources,
            @JsonProperty("modules") final @NotNull ModuleList modules,
            @JsonProperty("extensions") final @NotNull ExtensionList extensions) {
        this.environment = environment;
        this.cloudLink = cloudLink;
        this.gitHubLink = gitHubLink;
        this.documentationLink = documentationLink;
        this.firstUseInformation = firstUseInformation;
        this.ctas = ctas;
        this.resources = resources;
        this.modules = modules;
        this.extensions = extensions;
    }

    public EnvironmentProperties getEnvironment() {
        return environment;
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

    public FirstUseInformation getFirstUseInformation() {
        return firstUseInformation;
    }

    public LinkList getCtas() {
        return ctas;
    }

    public LinkList getResources() {
        return resources;
    }

    public ModuleList getModules() {
        return modules;
    }

    public ExtensionList getExtensions() {
        return extensions;
    }
}
