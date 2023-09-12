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
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.uns.config.ISA95;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Bean to transport ISA95 details across the API
 * @author Simon L Johnson
 */
public class ISA95ApiBean {

    @JsonProperty("prefixAllTopics")
    @Schema(description = "Should all topics be prefixed with UNS placeholders")
    private final @NotNull Boolean prefixAllTopics;

    @JsonProperty("enabled")
    @Schema(description = "Should UNS be available")
    private final @NotNull Boolean enabled;

    @JsonProperty("workCell")
    @Schema(name = "workCell",
            description = "The workCell",
            nullable = true, pattern = "^[a-zA-Z0-9 -_]*$")
    private final @Nullable String workCell;

    @JsonProperty("productionLine")
    @Schema(name = "productionLine",
            description = "The productionLine",
            nullable = true, pattern = "^[a-zA-Z0-9 -_]*$")
    private final @Nullable String productionLine;

    @JsonProperty("area")
    @Schema(name = "area",
            description = "The area",
            nullable = true, pattern = "^[a-zA-Z0-9 -_]*$")
    private final @Nullable String area;

    @JsonProperty("site")
    @Schema(name = "site",
            description = "The site",
            nullable = true, pattern = "^[a-zA-Z0-9 -_]*$")
    private final @Nullable String site;

    @JsonProperty("enterprise")
    @Schema(name = "enterprise",
            description = "The enterprise",
            nullable = true, pattern = "^[a-zA-Z0-9 -_]*")
    private final @Nullable String enterprise;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public ISA95ApiBean(
            @NotNull @JsonProperty("enabled") final Boolean enabled,
            @NotNull @JsonProperty("prefixAllTopics") final Boolean prefixAllTopics,
            @Nullable @JsonProperty("enterprise") final String enterprise,
            @Nullable @JsonProperty("site") final String site,
            @Nullable @JsonProperty("area") final String area,
            @Nullable @JsonProperty("productionLine") final String productionLine,
            @Nullable @JsonProperty("workCell") final String workCell) {
        this.enabled = enabled;
        this.prefixAllTopics = prefixAllTopics;
        this.enterprise = enterprise;
        this.area = area;
        this.site = site;
        this.productionLine = productionLine;
        this.workCell = workCell;
    }

    public Boolean getPrefixAllTopics() {
        return prefixAllTopics;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public String getWorkCell() {
        return workCell;
    }

    public String getProductionLine() {
        return productionLine;
    }

    public String getArea() {
        return area;
    }

    public String getSite() {
        return site;
    }

    public String getEnterprise() {
        return enterprise;
    }

    public static ISA95ApiBean convert(ISA95 isa95Entity) {
        ISA95ApiBean bridge = new ISA95ApiBean(
                isa95Entity.isEnabled(),
                isa95Entity.isPrefixAllTopics(),
                isa95Entity.getEnterprise(),
                isa95Entity.getSite(),
                isa95Entity.getArea(),
                isa95Entity.getProductionLine(),
                isa95Entity.getWorkCell());
        return bridge;
    }

    public static ISA95 unconvert(@NotNull final ISA95ApiBean apiBean) {
        ISA95 bean = new ISA95(
                apiBean.getEnabled(),
                apiBean.getPrefixAllTopics(),
                apiBean.getEnterprise(),
                apiBean.getSite(),
                apiBean.getArea(),
                apiBean.getProductionLine(),
                apiBean.getWorkCell());
        return bean;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ISA95ApiBean{");
        sb.append("prefixAllTopics=").append(prefixAllTopics);
        sb.append(", enabled=").append(enabled);
        sb.append(", workCell='").append(workCell).append('\'');
        sb.append(", productionLine='").append(productionLine).append('\'');
        sb.append(", area='").append(area).append('\'');
        sb.append(", site='").append(site).append('\'');
        sb.append(", enterprise='").append(enterprise).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
