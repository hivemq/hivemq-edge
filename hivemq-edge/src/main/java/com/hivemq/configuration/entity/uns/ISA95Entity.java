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
package com.hivemq.configuration.entity.uns;

import com.hivemq.configuration.entity.DisabledEntity;
import org.jetbrains.annotations.Nullable;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.Objects;

/**
 * @author Simon L Johnson
 */
@XmlRootElement(name = "isa95")
@XmlAccessorType(XmlAccessType.NONE)
public class ISA95Entity extends DisabledEntity {

    //enterprise/site/area/production-line/work-cell

    @XmlElement(name = "prefix-all-topics")
    private boolean prefixAllTopics = false;
    @XmlElement(name = "enterprise")
    private @Nullable String enterprise;
    @XmlElement(name = "site")
    private @Nullable String site;
    @XmlElement(name = "area")
    private @Nullable String area;
    @XmlElement(name = "production-line")
    private @Nullable String productionLine;
    @XmlElement(name = "work-cell")
    private @Nullable String workCell;

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
    public boolean isPrefixAllTopics() {
        return prefixAllTopics;
    }

    public void setPrefixAllTopics(final boolean prefixAllTopics) {
        this.prefixAllTopics = prefixAllTopics;
    }

    public void setEnterprise(final String enterprise) {
        this.enterprise = enterprise;
    }

    public void setSite(final String site) {
        this.site = site;
    }

    public void setArea(final String area) {
        this.area = area;
    }

    public void setProductionLine(final String productionLine) {
        this.productionLine = productionLine;
    }

    public void setWorkCell(final String workCell) {
        this.workCell = workCell;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final ISA95Entity that = (ISA95Entity) o;
        return isPrefixAllTopics() == that.isPrefixAllTopics() &&
                Objects.equals(getEnterprise(), that.getEnterprise()) &&
                Objects.equals(getSite(), that.getSite()) &&
                Objects.equals(getArea(), that.getArea()) &&
                Objects.equals(getProductionLine(), that.getProductionLine()) &&
                Objects.equals(getWorkCell(), that.getWorkCell());
    }

    @Override
    public int hashCode() {
        return Objects.hash(isPrefixAllTopics(),
                getEnterprise(),
                getSite(),
                getArea(),
                getProductionLine(),
                getWorkCell());
    }
}
