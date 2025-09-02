/*
 *  Copyright 2019-present HiveMQ GmbH
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.hivemq.configuration.entity.pulse;

import com.hivemq.configuration.entity.EntityValidatable;
import jakarta.xml.bind.ValidationEvent;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "managed-assets")
public class PulseAssetsEntity implements EntityValidatable {

    @XmlElement(name = "managed-asset")
    private @NotNull List<PulseAssetEntity> pulseAssetEntities;

    public PulseAssetsEntity() {
        this(new ArrayList<>());
    }

    public PulseAssetsEntity(final @NotNull List<PulseAssetEntity> pulseAssetEntities) {
        this.pulseAssetEntities = pulseAssetEntities;
    }

    @Override
    public boolean equals(final @Nullable Object o) {
        if (!(o instanceof final PulseAssetsEntity that)) {
            return false;
        }
        return Objects.equals(pulseAssetEntities, that.pulseAssetEntities);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(pulseAssetEntities);
    }

    public @NotNull List<PulseAssetEntity> getPulseAssetEntities() {
        return pulseAssetEntities;
    }

    public void setPulseAssetEntities(final @NotNull List<PulseAssetEntity> pulseAssetEntities) {
        this.pulseAssetEntities = pulseAssetEntities;
    }

    @Override
    public void validate(final @NotNull List<ValidationEvent> validationEvents) {
        pulseAssetEntities.forEach(entity -> entity.validate(validationEvents));
    }
}
