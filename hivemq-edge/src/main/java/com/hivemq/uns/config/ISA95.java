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
package com.hivemq.uns.config;

import com.hivemq.edge.api.model.ISA95ApiBean;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Example: //enterprise/site/area/production-line/work-cell
 *
 * @author Simon L Johnson
 */
public class ISA95 {

    public static final String ENTERPRISE = "enterprise";
    public static final String SITE = "site";
    public static final String AREA = "area";
    public static final String PRODUCTION_LINE = "production-line";
    public static final String WORK_CELL = "work-cell";


    boolean enabled = false;
    boolean prefixAllTopics = false;

    private final @Nullable String enterprise;
    private final @Nullable String site;
    private final @Nullable String area;
    private final @Nullable String productionLine;
    private final @Nullable String workCell;

    public ISA95(
            final boolean enabled,
            final boolean prefixAllTopics,
            final @Nullable String enterprise,
            final @Nullable String site,
            final @Nullable String area,
            final @Nullable String productionLine,
            final @Nullable String workCell) {
        this.enabled = enabled;
        this.prefixAllTopics = prefixAllTopics;
        this.enterprise = enterprise;
        this.site = site;
        this.area = area;
        this.productionLine = productionLine;
        this.workCell = workCell;
    }

    public @Nullable String getEnterprise() {
        return enterprise;
    }

    public @Nullable String getSite() {
        return site;
    }

    public @Nullable String getArea() {
        return area;
    }

    public @Nullable String getProductionLine() {
        return productionLine;
    }

    public @Nullable String getWorkCell() {
        return workCell;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isPrefixAllTopics() {
        return prefixAllTopics;
    }

    public static class Builder {
        boolean enabled = false;
        boolean prefixAllTopics = false;
        private @Nullable String enterprise;
        private @Nullable String site;
        private @Nullable String area;
        private @Nullable String productionLine;
        private @Nullable String workCell;

        public @NotNull ISA95.Builder withEnterprise(final @NotNull String enterprise) {
            this.enterprise = enterprise;
            return this;
        }

        public @NotNull ISA95.Builder withSite(final @NotNull String site) {
            this.site = site;
            return this;
        }

        public @NotNull ISA95.Builder withArea(final @NotNull String area) {
            this.area = area;
            return this;
        }

        public @NotNull ISA95.Builder withProductionLine(final @NotNull String productionLine) {
            this.productionLine = productionLine;
            return this;
        }

        public @NotNull ISA95.Builder withWorkCell(final @NotNull String workCell) {
            this.workCell = workCell;
            return this;
        }

        public @NotNull ISA95.Builder withEnabled(final boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public @NotNull ISA95.Builder withPrefixAllTopics(final boolean prefixAllTopics) {
            this.prefixAllTopics = prefixAllTopics;
            return this;
        }

        public @NotNull ISA95 build() {
            return new ISA95(enabled, prefixAllTopics, enterprise, site, area, productionLine, workCell);
        }
    }

    public static @NotNull ISA95ApiBean convert(final @NotNull ISA95 isa95Entity) {
        return new ISA95ApiBean().enabled(isa95Entity.isEnabled())
                .prefixAllTopics(isa95Entity.isPrefixAllTopics())
                .enterprise(isa95Entity.getEnterprise())
                .site(isa95Entity.getSite())
                .area(isa95Entity.getArea())
                .productionLine(isa95Entity.getProductionLine())
                .workCell(isa95Entity.getWorkCell());
    }

    public static @NotNull ISA95 unconvert(final @NotNull ISA95ApiBean apiBean) {
        return  new ISA95(apiBean.getEnabled(),
                apiBean.getPrefixAllTopics(),
                apiBean.getEnterprise(),
                apiBean.getSite(),
                apiBean.getArea(),
                apiBean.getProductionLine(),
                apiBean.getWorkCell());
    }
}
