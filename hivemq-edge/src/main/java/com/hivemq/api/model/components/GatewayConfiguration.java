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
import com.hivemq.api.model.firstuse.FirstUseInformation;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jetbrains.annotations.NotNull;

public class GatewayConfiguration {

    @JsonProperty("environment")
    @Schema(description = "A map of properties relating to the installation", nullable = true)
    private final @NotNull EnvironmentProperties environment;

    @JsonProperty("cloudLink")
    @Schema(description = "A referral link to HiveMQ Cloud")
    private final @NotNull Link cloudLink;

    @JsonProperty("gitHubLink")
    @Schema(description = "A link to the GitHub Project")
    private final @NotNull Link gitHubLink;

    @JsonProperty("documentationLink")
    @Schema(description = "A link to the documentation Project")
    private final @NotNull Link documentationLink;

    @JsonProperty("firstUseInformation")
    @Schema(description = "Information relating to the firstuse experience")
    private final @NotNull FirstUseInformation firstUseInformation;

    @JsonProperty("ctas")
    @Schema(description = "The calls main to action")
    private final @NotNull LinkList ctas;

    @JsonProperty("resources")
    @Schema(description = "A list of resources to render")
    private final @NotNull LinkList resources;

    @JsonProperty("modules")
    @Schema(description = "The modules available for installation")
    private final @NotNull ModuleList modules;

    @JsonProperty("extensions")
    @Schema(description = "The extensions available for installation")
    private final @NotNull ExtensionList extensions;

    @JsonProperty("hivemqId")
    @Schema(description = "The current id of hivemq edge. Changes at restart.")
    private final @NotNull String hivemqId;

    @JsonProperty("trackingAllowed")
    @Schema(description = "Is the tracking of user actions allowed.")
    private final boolean trackingAllowed;

    @JsonProperty("confidentialityAgreement")
    @Schema(description = "CA configuration")
    private final @NotNull ConfidentialityAgreement confidentialityAgreement;

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
            @JsonProperty("extensions") final @NotNull ExtensionList extensions,
            @JsonProperty("hivemqId") final @NotNull String hivemqId,
            @JsonProperty("trackingAllowed") final boolean trackingAllowed,
            @JsonProperty("confidentialityAgreement") final ConfidentialityAgreement confidentialityAgreement) {
        this.environment = environment;
        this.cloudLink = cloudLink;
        this.gitHubLink = gitHubLink;
        this.documentationLink = documentationLink;
        this.firstUseInformation = firstUseInformation;
        this.ctas = ctas;
        this.resources = resources;
        this.modules = modules;
        this.extensions = extensions;
        this.hivemqId = hivemqId;
        this.trackingAllowed = trackingAllowed;
        this.confidentialityAgreement = confidentialityAgreement;
    }

    public @NotNull EnvironmentProperties getEnvironment() {
        return environment;
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

    public @NotNull FirstUseInformation getFirstUseInformation() {
        return firstUseInformation;
    }

    public @NotNull LinkList getCtas() {
        return ctas;
    }

    public @NotNull LinkList getResources() {
        return resources;
    }

    public @NotNull ModuleList getModules() {
        return modules;
    }

    public @NotNull ExtensionList getExtensions() {
        return extensions;
    }

    public @NotNull String getHivemqId() {
        return hivemqId;
    }

    public boolean isTrackingAllowed() {
        return trackingAllowed;
    }

    public @NotNull ConfidentialityAgreement getConfidentialityAgreement() {
        return confidentialityAgreement;
    }
}
