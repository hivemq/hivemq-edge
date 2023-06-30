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

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;

/**
 * Example: //enterprise/site/area/production-line/work-cell
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

    private @Nullable String enterprise;
    private @Nullable String site;
    private @Nullable String area;
    private @Nullable String productionLine;
    private @Nullable String workCell;

    public ISA95(
            final boolean enabled,
            final boolean prefixAllTopics,
            final String enterprise,
            final String site,
            final String area,
            final String productionLine,
            final String workCell) {
        this.enabled = enabled;
        this.prefixAllTopics = prefixAllTopics;
        this.enterprise = enterprise;
        this.site = site;
        this.area = area;
        this.productionLine = productionLine;
        this.workCell = workCell;
    }

    public String getEnterprise() {
        return enterprise;
    }

    public String getSite() {
        return site;
    }

    public String getArea() {
        return area;
    }

    public String getProductionLine() {
        return productionLine;
    }

    public String getWorkCell() {
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

        public @NotNull ISA95.Builder withEnterprise(@NotNull final String enterprise) {
            this.enterprise = enterprise;
            return this;
        }

        public @NotNull ISA95.Builder withSite(@NotNull final String site) {
            this.site = site;
            return this;
        }

        public @NotNull ISA95.Builder withArea(@NotNull final String area) {
            this.area = area;
            return this;
        }

        public @NotNull ISA95.Builder withProductionLine(@NotNull final String productionLine) {
            this.productionLine = productionLine;
            return this;
        }

        public @NotNull ISA95.Builder withWorkCell(@NotNull final String workCell) {
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
            return new ISA95(enabled,
                    prefixAllTopics,
                    enterprise,
                    site,
                    area,
                    productionLine,
                    workCell);
        }
    }
}
