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
package com.hivemq.api.model.uns;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.api.model.bridge.Bridge;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.uns.NamespaceUtils;
import com.hivemq.uns.config.ISA95;
import com.hivemq.uns.config.NamespaceProfile;
import com.hivemq.uns.config.NamespaceSegment;
import com.hivemq.uns.config.impl.NamespaceProfileImpl;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Bean to transport details across the API
 * @author Simon L Johnson
 */
public class NamespaceProfileBean {

    @JsonProperty("enabled")
    @Schema(description = "Namespace enabled", requiredMode = Schema.RequiredMode.REQUIRED)
    private final @NotNull boolean enabled;

    @JsonProperty("prefixAllTopics")
    @Schema(description = "Prefix all topic with the UNS", requiredMode = Schema.RequiredMode.REQUIRED)
    private final @NotNull boolean prefixAllTopics;

    @JsonProperty("name")
    @Schema(description = "Name of the namespace", requiredMode = Schema.RequiredMode.REQUIRED)
    private final @NotNull String name;

    @JsonProperty("description")
    @Schema(description = "Description of the namespace", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private final @NotNull String description;

    @JsonProperty("segments")
    @Schema(description = "The segments that are contained in this profile", requiredMode = Schema.RequiredMode.REQUIRED)
    private final @NotNull List<NamespaceSegmentBean> segments;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public NamespaceProfileBean(
            @NotNull @JsonProperty("enabled") final boolean enabled,
            @NotNull @JsonProperty("prefixAllTopics") final boolean prefixAllTopics,
            @NotNull @JsonProperty("name") final String name,
            @Nullable @JsonProperty("description") final String description,
            @Nullable @JsonProperty("segments") final List<NamespaceSegmentBean> segments) {

        this.enabled = enabled;
        this.prefixAllTopics = prefixAllTopics;
        this.name = name;
        this.description = description;
        this.segments = segments;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public List<NamespaceSegmentBean> getSegments() {
        return segments;
    }

    public boolean getEnabled() {
        return enabled;
    }

    public boolean getPrefixAllTopics() {
        return prefixAllTopics;
    }

    public static NamespaceProfileBean convert(NamespaceProfile namespaceProfile) {
        return new NamespaceProfileBean(
                namespaceProfile.getEnabled(),
                namespaceProfile.getPrefixAllTopics(),
                namespaceProfile.getName(),
                namespaceProfile.getDescription(),
                namespaceProfile.getSegments().stream().map(NamespaceProfileBean::convert).
                        collect(Collectors.toList()));
    }

    public static NamespaceProfile unconvert(NamespaceProfileBean namespaceProfileBean, boolean enabled) {
        NamespaceProfileImpl impl = new NamespaceProfileImpl(
                namespaceProfileBean.getName(),
                namespaceProfileBean.getDescription(),
                namespaceProfileBean.getSegments().stream().map(NamespaceProfileBean::unconvert).
                        collect(Collectors.toList())
        );
        impl.setEnabled(enabled);
        impl.setPrefixAllTopics(namespaceProfileBean.getPrefixAllTopics());
        return impl;
    }

    static NamespaceSegmentBean convert(NamespaceSegment segment) {
        return new NamespaceSegmentBean(
                segment.getName(),
                segment.getValue(),
                segment.getDescription());
    }

    static NamespaceSegment unconvert(NamespaceSegmentBean segment) {
        return new NamespaceSegment(
                segment.getName(),
                segment.getValue(),
                segment.getDescription());
    }

    public static class NamespaceSegmentBean {
        @JsonProperty("name")
        @Schema(description = "Name of the segment")
        private final @NotNull String name;

        @JsonProperty("value")
        @Schema(description = "Value of the segment")
        private final @NotNull String value;

        @JsonProperty("description")
        @Schema(description = "Description of the segment")
        private final @Nullable String description;

        public NamespaceSegmentBean(@JsonProperty("name") final String name, @JsonProperty("value") final String value, @JsonProperty("description") final String description) {
            this.name = name;
            this.value = value;
            this.description = description;
        }

        public String getValue() {
            return value;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }
    }

}
