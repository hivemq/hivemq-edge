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
package com.hivemq.configuration.entity;

import com.hivemq.configuration.service.InternalConfigurations;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.Objects;

/**
 * @author Simon L Johnson
 */
@XmlRootElement(name = "usage-tracking")
@XmlAccessorType(XmlAccessType.NONE)
@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
public class UsageTrackingConfigEntity {

    @XmlElement(name = "enabled", defaultValue = "true")
    private boolean enabled = InternalConfigurations.DEFAULT_USAGE_EVENTS_ENABLED.get();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final UsageTrackingConfigEntity that = (UsageTrackingConfigEntity) o;
        return isEnabled() == that.isEnabled();
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(isEnabled());
    }
}
