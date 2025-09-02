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
import jakarta.xml.bind.annotation.XmlRootElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

@XmlRootElement(name = "pulse")
@XmlAccessorType(XmlAccessType.NONE)
public class PulseEntity implements EntityValidatable {
    private final Object lock = new Object();

    @XmlElement(name = "managed-assets", required = true)
    private @NotNull PulseAssetsEntity pulseAssetsEntity;

    public PulseEntity() {
        this(new PulseAssetsEntity());
    }

    public PulseEntity(final @NotNull PulseAssetsEntity pulseAssetsEntity) {
        this.pulseAssetsEntity = pulseAssetsEntity;
    }

    public @NotNull Object getLock() {
        return lock;
    }

    @Override
    public boolean equals(final @Nullable Object o) {
        if (!(o instanceof final PulseEntity that)) {
            return false;
        }
        return Objects.equals(pulseAssetsEntity, that.pulseAssetsEntity);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(pulseAssetsEntity);
    }

    public @NotNull PulseAssetsEntity getPulseAssetsEntity() {
        return pulseAssetsEntity;
    }

    public void setPulseAssetsEntity(@NotNull final PulseAssetsEntity pulseAssetsEntity) {
        this.pulseAssetsEntity = pulseAssetsEntity;
    }

    @Override
    public void validate(final @NotNull List<ValidationEvent> validationEvents) {
        pulseAssetsEntity.validate(validationEvents);
    }
}
