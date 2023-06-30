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
package com.hivemq.datagov.model.impl;

import com.google.common.base.Preconditions;
import com.hivemq.datagov.model.DataGovernanceEntity;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.util.Objects;

/**
 * @author Simon L Johnson
 */
public abstract class AbstractDataGovernanceEntity implements DataGovernanceEntity {

    private @NotNull final String id;
    private @NotNull final String name;
    private boolean enabled = true;
    private boolean mutable = false;

    public AbstractDataGovernanceEntity(final @NotNull String id, final @NotNull String name) {
        Preconditions.checkNotNull(id, "Id Must Exist On Policy Object");
        Preconditions.checkNotNull(name, "Name Must Exist On Policy Object");
        this.id = id;
        this.name = name;
    }

    public AbstractDataGovernanceEntity(final @NotNull String id) {
        this(id, id);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isMutable() {
        return mutable;
    }

    public void setMutable(final boolean mutable) {
        this.mutable = mutable;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AbstractDataGovernanceEntity that = (AbstractDataGovernanceEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
